/*
 * Copyright 2022 Frank van der Hulst drifter.frank@gmail.com
 *
 * This software is made available under a Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0) License
 * https://creativecommons.org/licenses/by-nc/4.0/
 *
 * You are free to share (copy and redistribute the material in any medium or format) and
 * adapt (remix, transform, and build upon the material) this software under the following terms:
 * Attribution — You must give appropriate credit, provide a link to the license, and indicate if changes were made.
 * You may do so in any reasonable manner, but not in any way that suggests the licensor endorses you or your use.
 * NonCommercial — You may not use the material for commercial purposes.
 */
package com.meerkat;

import static com.meerkat.ui.settings.SettingsViewModel.countryCode;
import static com.meerkat.ui.settings.SettingsViewModel.historySeconds;
import static com.meerkat.ui.settings.SettingsViewModel.polynomialHistoryMilliS;
import static com.meerkat.ui.settings.SettingsViewModel.polynomialPredictionStepMilliS;
import static com.meerkat.ui.settings.SettingsViewModel.predictionMilliS;
import static com.meerkat.ui.settings.SettingsViewModel.showLinearPredictionTrack;
import static com.meerkat.ui.settings.SettingsViewModel.showPolynomialPredictionTrack;

import androidx.annotation.NonNull;

import com.meerkat.log.Log;
import com.meerkat.map.AircraftLayer;
import com.meerkat.map.VehicleIcon;
import com.meerkat.measure.Position;
import com.meerkat.measure.Units;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;

public class Vehicle implements Comparable<Vehicle> {
    public final int id;
    public String callsign;
    public final LinkedList<Position> history;
    public final ArrayList<Position> predicted;
    public @NonNull
    VehicleIcon.Emitter emitterType;
    public Position position;
    public long lastUpdate;
    public final Position predictedPosition;
    public float distance;
    public final AircraftLayer layer;
    int lastCrc;
    public final Object lock = new Object();
    final float MAX_TURN_RATE = 360f / 60;
    final float MAX_CLIMB_RATE = 3000f * Units.VertSpeed.FPM.units.factor / 60;
    final float MAX_DIVE_RATE = 3000f * Units.VertSpeed.FPM.units.factor / 60;
    final float MAX_SPEED = 300f * Units.Speed.KNOTS.units.factor;

    public Vehicle(int crc, int id, String callsign, Position point, @NonNull VehicleIcon.Emitter emitterType) {
        this.lastCrc = crc;
        this.lastUpdate = point.getTime();
        this.id = id;
        this.callsign = callsign;
        this.emitterType = emitterType;
        // History & Predicted go in opposite directions... in each case, the last entry is the furthest away from the current position of the aircraft.
        // So history is in decreasing time order, and predicted is in increasing time order
        this.history = new LinkedList<>();
        // always create predictedPosition for synchronization
        layer = findLayer(this);
        predictedPosition = new Position("predicted");
        predicted = new ArrayList<>(predictionMilliS / polynomialPredictionStepMilliS + 1);
        if (showPolynomialPredictionTrack) {
            for (int i = 0; i <= predictionMilliS; i += polynomialPredictionStepMilliS)
                predicted.add(new Position("predicted"));
        }
        if (point.hasAccuracy()) {
            addPoint(point);
            if (showLinearPredictionTrack) {
                point.linearPredict(predictionMilliS, predictedPosition);
                Log.v("Predict from %s to %s", point.toString(), predictedPosition);
            }
        } else {
            if (!point.hasAltitude())
                position = null;
            else {
                addPoint(point);
            }
            distance = Float.NaN;
            predictedPosition.removeAccuracy();
        }
    }

    // If an invisible layer exists, re-use it. Otherwise create a new layer for this vehicle
    AircraftLayer findLayer(Vehicle v) {
        AircraftLayer result = (AircraftLayer) MainActivity.mapView.layers.findDrawableByLayerId(v.id);
        if (result != null) {
            // If a vehicle has returned after being purged but before its layer has been
            // reused, its layer will now be invisible
            if (!result.isVisible())
                result.setVisible(true, true);
            return result;
        }
        for (int i = 1; i < MainActivity.mapView.layers.getNumberOfLayers(); i++) {
            AircraftLayer d = (AircraftLayer) MainActivity.mapView.layers.getDrawable(i);
            if (!d.isVisible()) {
                Log.i("ReUse layer %d was %d", i, MainActivity.mapView.layers.getId(i));
                synchronized (MainActivity.mapView.layers) {
                    MainActivity.mapView.layers.setId(i, v.id);
                    d.setVisible();
                    return d;
                }
            }
        }
        AircraftLayer d = new AircraftLayer(v);
        synchronized (MainActivity.mapView.layers) {
            MainActivity.mapView.layers.addLayer(d);
            return d;
        }
    }

    public String getLabel() {
        char valChar = isValid() ? ' ' : '!';
        if (callsign.isBlank())
            return String.format("%06x%c", id, valChar);
        if (countryCode != null && !countryCode.isEmpty()) {
            if (callsign.toUpperCase().startsWith(countryCode)) {
                int start = countryCode.length();
                if (callsign.charAt(start) == '-') start++;
                return callsign.substring(start).toUpperCase() + valChar;
            }
        }
        return callsign + valChar;
    }

    public void update(int crc, Position point, String callsign, @NonNull VehicleIcon.Emitter emitterType) {
        Log.d(String.format("Update %06x, %s, %s, %s", id, callsign, emitterType, point.toString()));

        synchronized (layer) {
            this.lastCrc = crc;
            if (emitterType != VehicleIcon.Emitter.Unknown) {
                this.emitterType = emitterType;
            }
            if (callsign != null && !callsign.equals(this.callsign)) {
                this.callsign = callsign;
            }

            if (point.hasAccuracy()) {
                addPoint(point);
            }
        }

        // Assume that an aircraft does not turn while stopped
        if (position != null && !point.hasTrack()) point.setTrack(position.getTrack());

        lastUpdate = point.getTime();
        // Remove aged-out history entries, and add the new point
        final long maxAge = lastUpdate - historySeconds * 1000L;
        synchronized (layer) {
            Iterator<Position> it = history.descendingIterator();
            while (it.hasNext()) {
                var p = it.next();
                if (p.getTime() > maxAge) break;
                it.remove();
            }
        }

        if (showLinearPredictionTrack && position != null && position.hasTrack() && position.hasSpeed()) {
            synchronized (layer) {
                position.linearPredict((int) (lastUpdate - position.getTime() + predictionMilliS), predictedPosition);
                Log.v("Predict from %s to %s", position, predictedPosition);
            }
        }

        if (showPolynomialPredictionTrack && !history.isEmpty()) {
            // Only reading history, so need to synchronize
            PolynomialRegression prSpeedTrack = new PolynomialRegression(history.getLast().getTime(), 3);
            // History only contains valid points, which all have a track value
            float prevTrack = Float.NaN;
            long prevTime = -1;
            synchronized (lock) {
                Iterator<Position> it = history.descendingIterator();
                while (it.hasNext()) {
                    var p = it.next();
                    var time = p.getTime();
                    if (!point.hasSpeed() || time <= prevTime) continue;
                    if (time < position.getTime() - polynomialHistoryMilliS) {
                        prevTrack = p.getTrack();
                        continue;
                    }
                    var speed = p.getSpeed();
                    var track = p.getTrack();
                    if (!p.hasAltitude() || speed == 0 || Float.isNaN(speed))
                        continue;
                    // Unwind modulo arithmetic so that turns in the same direction keep incrementing, even though the result is > 360 or < 0
                    while (track < prevTrack - 180) track += 360;
                    while (track > prevTrack + 180) track -= 360;
                    Log.v("Add %d %5.0f %5.0f %5.0f", time - position.getTime(), speed, track, p.getAltitude());
                    prSpeedTrack.add(time, speed, track, (float) p.getAltitude());
                    prevTrack = track;
                    prevTime = time;
                }
            }
            double[][] cSpeedTrack = prSpeedTrack.getCoefficients();
            synchronized (lock) {
                if (cSpeedTrack != null) {
                    Log.v("Speed coeffs %.1f %.3f %.5f %d", cSpeedTrack[0][0], cSpeedTrack[0][1], cSpeedTrack[0][2], (long) cSpeedTrack[0][3] - lastUpdate);
                    Log.v("Track coeffs %.1f %.3f %.5f", cSpeedTrack[1][0], cSpeedTrack[1][1], cSpeedTrack[1][2]);
                    Log.v("Alt   coeffs %.1f %.3f %.5f", cSpeedTrack[2][0], cSpeedTrack[2][1], cSpeedTrack[2][2]);
                    Position p = position;
                    for (int i = 0; i <= predicted.size() - 1; i++) {
                        var speed = p.getSpeed();
                        var track = p.getTrack();
                        var alt = (float) p.getAltitude();
                        var t1 = lastUpdate - (long) cSpeedTrack[0][3] + (long) i * polynomialPredictionStepMilliS;
                        var t2 = t1 * t1;
                        p.linearPredict(polynomialPredictionStepMilliS, predicted.get(i));
                        p = predicted.get(i);
                        var newSpeed = (float) (cSpeedTrack[0][0] + cSpeedTrack[0][1] * t1 + cSpeedTrack[0][2] * t2);
                        var newTrack = (float) (cSpeedTrack[1][0] + cSpeedTrack[1][1] * t1 + cSpeedTrack[1][2] * t2);
                        var newAlt = (float) (cSpeedTrack[2][0] + cSpeedTrack[2][1] * t1 + cSpeedTrack[2][2] * t2);
                        newTrack = Math.min(Math.max(newTrack, track - MAX_TURN_RATE * polynomialPredictionStepMilliS / 1000f), track + MAX_TURN_RATE * polynomialPredictionStepMilliS / 1000f);
                        newAlt = Math.min(Math.max(newAlt, alt - MAX_DIVE_RATE * polynomialPredictionStepMilliS / 1000f), alt + MAX_CLIMB_RATE * polynomialPredictionStepMilliS / 1000f);
                        // Limit speed changes to +/-10%
                        newSpeed = Math.min(Math.max(newSpeed, speed * .9f), Math.min(MAX_SPEED, speed * 1.1f));
                        p.setAltitude(Float.isNaN(newAlt) ? alt : Float.isNaN(alt) ? newAlt : 0.5f * alt + 0.5f * newAlt);
                        p.setSpeed(Float.isNaN(newSpeed) ? speed : Float.isNaN(speed) ? newSpeed : 0.5f * speed + 0.5f * newSpeed);
                        p.setTrack((Float.isNaN(newTrack) ? track : Float.isNaN(track) ? newTrack : 0.9f * track + 0.1f * newTrack) % 360);
                        p.setTime(position.getTime() + (long) i * polynomialPredictionStepMilliS);
                        Log.v("%s Predict Speed %.1f Track %.1f Alt %.1f %s", callsign, newSpeed, newTrack, alt, p);
                    }
                }
            }
        }
        MainActivity.mapView.refresh(layer);
    }

    private void addPoint(Position point) {
        position = point;
        distance = Gps.distanceTo(point); // metres
        history.addFirst(point);
    }

    public boolean isValid() {
        return position != null && position.isCrcValid() && position.hasAccuracy();
    }

    @NonNull
    public String toString() {
        return String.format(Locale.ENGLISH, "%06x %-8s %c %s", id, callsign, isValid() ? ' ' : '!', position);
    }

    @Override
    public int compareTo(Vehicle o) {
        return Float.compare(this.distance, o.distance);
    }
}
