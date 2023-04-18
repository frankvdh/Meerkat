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

import static com.meerkat.SettingsActivity.autoZoom;
import static com.meerkat.SettingsActivity.countryCode;
import static com.meerkat.SettingsActivity.historySeconds;
import static com.meerkat.SettingsActivity.ownId;
import static com.meerkat.SettingsActivity.polynomialHistoryMilliS;
import static com.meerkat.SettingsActivity.polynomialPredictionStepMilliS;
import static com.meerkat.SettingsActivity.predictionMilliS;
import static com.meerkat.SettingsActivity.showLinearPredictionTrack;
import static com.meerkat.SettingsActivity.showPolynomialPredictionTrack;

import androidx.annotation.NonNull;

import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.log.Log;
import com.meerkat.map.AircraftLayer;
import com.meerkat.map.MapView;
import com.meerkat.measure.Position;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;

public class Vehicle implements Comparable<Vehicle> {
    public final int id;
    private final MapView mapView;
    public String callsign;
    public final LinkedList<Position> history;
    public final ArrayList<Position> predicted;
    public @NonNull
    Gdl90Message.Emitter emitterType;
    public Position lastValid;
    public final Position predictedPosition;
    public float distance;
    public final AircraftLayer layer;
    int lastCrc;

    public Vehicle(int crc, int id, String callsign, Position point, @NonNull Gdl90Message.Emitter emitterType, @NonNull MapView mapView) {
        this.lastCrc = crc;
        this.id = id;
        this.mapView = mapView;
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
            for (int i = 0; i <= predictionMilliS / polynomialPredictionStepMilliS; i++)
                predicted.add(new Position("predicted"));
        }
        if (point.hasAccuracy()) {
            addPoint(point);
            if (showLinearPredictionTrack) {
                point.linearPredict(predictionMilliS, predictedPosition);
                Log.d("Predict from %s to %s", point.toString(), predictedPosition);
            }
        } else {
            lastValid = null;
            distance = Float.NaN;
            predictedPosition.removeAccuracy();
        }
    }

    // If an invisible layer exists, re-use it. Otherwise create a new layer for this vehicle
    AircraftLayer findLayer(Vehicle v) {
        AircraftLayer result = (AircraftLayer) mapView.layers.findDrawableByLayerId(v.id);
        if (result != null) return result;
        for (int i = 1; i < mapView.layers.getNumberOfLayers(); i++) {
            AircraftLayer d = (AircraftLayer) mapView.layers.getDrawable(i);
            if (!d.isVisible()) {
                Log.i("ReUse layer %d was %d", i, mapView.layers.getId(i));
                synchronized (mapView.layers) {
                    mapView.layers.setId(i, v.id);
                    d.set(v);
                    return d;
                }
            }
        }
        AircraftLayer d = new AircraftLayer(v, mapView);
        synchronized (mapView.layers) {
            mapView.layers.addLayer(d);
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

    public void update(int crc, Position point, String callsign, @NonNull Gdl90Message.Emitter emitterType) {
        Log.d(String.format("Update %06x, %s, %s, %s", id, callsign, emitterType, point.toString()));

        synchronized (layer) {
            this.lastCrc = crc;
            if (emitterType != Gdl90Message.Emitter.Unknown) {
                this.emitterType = emitterType;
            }
            if (callsign != null && !callsign.equals(this.callsign))
                this.callsign = callsign;

            if (point.hasAccuracy()) {
                addPoint(point);
            }
        }
        var now = point.getInstant();
        // Remove aged-out history entries, and add the new point
        final Instant maxAge = now.minus(historySeconds, ChronoUnit.SECONDS);
        synchronized (layer) {
            Iterator<Position> it = history.descendingIterator();
            while (it.hasNext()) {
                var p = it.next();
                if (p.getInstant().isAfter(maxAge)) break;
                it.remove();
            }
        }

        if (showLinearPredictionTrack && lastValid != null && lastValid.hasTrack() && lastValid.hasSpeed()) {
            synchronized (layer) {
                lastValid.linearPredict((int) (lastValid.getInstant().until(now, ChronoUnit.MILLIS)) + predictionMilliS, predictedPosition);
                Log.d("Predict from %s to %s", lastValid, predictedPosition);
            }
        }

        if (showPolynomialPredictionTrack && !history.isEmpty()) {
            // Only reading history, so need to synchronize
            PolynomialRegression prSpeedTrack = new PolynomialRegression(history.getLast().getInstant(), 3);
            // History only contains valid points, assume not turning
            float prevTrack = history.getLast().hasTrack() ? history.getLast().getTrack() : 0;
            var prevTime = history.getLast().getInstant();
            synchronized (layer) {
                Iterator<Position> it = history.descendingIterator();
                while (it.hasNext()) {
                    var p = it.next();
                    var time = p.getInstant();
                    if (!p.hasTrack() || !point.hasSpeed() || time.isBefore(prevTime) || time.isBefore(lastValid.getInstant().minus(polynomialHistoryMilliS, ChronoUnit.MILLIS)))
                        continue;
                    var speed = p.getSpeed();
                    var track = p.getTrack();
                    if (!p.hasTrack() || !p.hasAltitude() || speed == 0 || Float.isNaN(speed))
                        continue;
                    // Unwind modulo arithmetic so that turns in the same direction keep incrementing, even though the result is > 360 or < 0
                    var turn = Float.isNaN(prevTrack) ? 0 : track - prevTrack;
                    while (turn > 180) turn -= 360;
                    while (turn < -180) turn += 180;
                    Log.v("Add %d %5.0f %5.0f %5.0f", time, speed, prevTrack + turn, p.getAltitude());
                    prSpeedTrack.add(time, speed, prevTrack + turn, (float) p.getAltitude());
                    prevTrack = track;
                    prevTime = time;
                }
            }
            double[][] cSpeedTrack = prSpeedTrack.getCoefficients();
            synchronized (layer) {
                if (cSpeedTrack != null) {
                    Log.v("Speed coeffs %.1f %.3f %.5f", cSpeedTrack[0][0], cSpeedTrack[0][1], cSpeedTrack[0][2]);
                    Log.v("Track coeffs %.1f %.3f %.5f", cSpeedTrack[1][0], cSpeedTrack[1][1], cSpeedTrack[1][2]);
                    Log.v("Alt   coeffs %.1f %.3f %.5f", cSpeedTrack[2][0], cSpeedTrack[2][1], cSpeedTrack[2][2]);
                    Position p = lastValid;
                    for (int i = 0; i <= predicted.size() - 1; i++) {
                        long t1 = Instant.ofEpochMilli((long) cSpeedTrack[0][3]).until(now, ChronoUnit.MILLIS) + (long) i * polynomialPredictionStepMilliS;
                        var t2 = t1 * t1;
                        p.linearPredict(polynomialPredictionStepMilliS, predicted.get(i));
                        p = predicted.get(i);
                        var speed = p.getSpeed();
                        var track = p.getTrack();
                        var alt = p.getAltitude();
                        var newSpeed = (float) (cSpeedTrack[0][0] + cSpeedTrack[0][1] * t1 + cSpeedTrack[0][2] * t2);
                        var newTrack = (float) (cSpeedTrack[1][0] + cSpeedTrack[1][1] * t1 + cSpeedTrack[1][2] * t2);
                        var newAlt = (float) (cSpeedTrack[2][0] + cSpeedTrack[2][1] * t1 + cSpeedTrack[2][2] * t2);
                        p.setAltitude(Double.isNaN(alt) ? newAlt : Float.isNaN(newAlt) ? alt : 0.9f * alt + 0.1f * newAlt);
                        p.setSpeed(Float.isNaN(speed) ? newSpeed : Float.isNaN(newSpeed) ? speed : 0.9f * speed + 0.1f * newSpeed);
                        p.setTrack((Float.isNaN(track) ? newTrack : Float.isNaN(newTrack) ? track : 0.9f * track + 0.1f * newTrack) % 360);
                        Log.v("%s Predict Speed %.1f Track %.1f Alt %.1f %s", callsign, speed, track, alt, p);
                    }
                }
            }
        }
        mapView.refresh(layer);
    }

    private void addPoint(Position point) {
        lastValid = point;
        distance = Gps.distanceTo(point); // metres
        history.addFirst(point);
    }

    public boolean isValid() {
        return lastValid != null && lastValid.isCrcValid() && lastValid.hasAccuracy();
    }


    public boolean checkAndMakeNearest(VehicleList vl) {
        if (!lastValid.isAirborne() || id == ownId) return false;
        if (vl.nearest != null) {
            if (vl.nearest == this || distance > vl.nearest.distance) return false;
        }
        Log.d("Nearest: %s %.0f %s vs %.0f", this.toString(), distance, lastValid.isAirborne(), vl.nearest == null ? Float.NaN: vl.nearest.distance);
        // Change to threat circle needed
        vl.nearest = this;
        return true;
    }

    public boolean checkAndMakeFurthest(VehicleList vl) {
        if (!autoZoom || !lastValid.isAirborne() || id == ownId) return false;
        if (vl.furthest != null) {
            if (vl.furthest == this || distance < vl.furthest.distance) return false;
        }
        Log.d("Furthest: %s %.0f %s vs %.0f", callsign, distance, lastValid.isAirborne(), vl.furthest == null ? Float.NaN: vl.furthest.distance);
        // Change to threat circle needed
        vl.furthest = this;
        return true;
    }

    @NonNull
    public String toString() {
        return String.format(Locale.ENGLISH, "%06x %-8s %c %s", id, callsign, isValid() ? ' ' : '!', lastValid);
    }

    @Override
    public int compareTo(Vehicle o) {
        return Float.compare(this.distance, o.distance);
    }
}
