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

import static com.meerkat.SettingsActivity.countryCode;
import static com.meerkat.SettingsActivity.historySeconds;
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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;

public class Vehicle implements Comparable<Vehicle> {
    public final int id;
    private final MapView mapView;
    public String callsign;
    public final LinkedList<Position> history;
    public final LinkedList<Position> predicted;
    public @NonNull
    Gdl90Message.Emitter emitterType;
    public final Position lastValid;
    public final Position predictedPosition;
    public float distance;
    final AircraftLayer layer;

    public Vehicle(int id, String callsign, Position point, @NonNull Gdl90Message.Emitter emitterType, @NonNull MapView mapView) {
        this.id = id;
        this.mapView = mapView;
        this.callsign = callsign;
        this.emitterType = emitterType;
        // History & Predicted go in opposite directions... in each case, the last entry is the furthest away from the current position of the aircraft.
        // So history is in decreasing time order, and predicted is in increasing time order
        this.history = new LinkedList<>();
        this.predicted = new LinkedList<>();

        history.addFirst(point);
        // Always accept the first point, even if it isn't valid
        lastValid = point;
        distance = Gps.distanceTo(point); // metres
        // always create predictedPosition for synchronization
        predictedPosition = new Position("predicted");
        layer = findLayer(this);
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
            return String.format("%07x%c", id, valChar);
        if (countryCode != null && !countryCode.isEmpty()) {
            if (callsign.toUpperCase().startsWith(countryCode)) {
                int start = countryCode.length();
                if (callsign.charAt(start) == '-') start++;
                return callsign.substring(start).toUpperCase() + valChar;
            }
        }
        return callsign + valChar;
    }

    public void update(Position point, String callsign, @NonNull Gdl90Message.Emitter emitterType) {
        var now = System.currentTimeMillis();
        Log.d(String.format("%06x, %s, %s, %s", id, callsign, emitterType, point.toString()));
        if (point.isValid())
            synchronized (lastValid) {
                lastValid.set(point);
                distance = Gps.distanceTo(point); // metres
                Log.v("Current: %s", lastValid.toString());

                if (emitterType != Gdl90Message.Emitter.Unknown) {
                    this.emitterType = emitterType;
                }
                if (callsign != null && !callsign.equals(this.callsign))
                    this.callsign = callsign;
            }

        if (showLinearPredictionTrack) {
            synchronized (predictedPosition) {
                lastValid.linearPredict((int) (now - lastValid.getTime()) + predictionMilliS, predictedPosition);
            }
        }

        // Remove aged-out history entries, and add the new point
        final long maxAge = now - historySeconds * 1000L;
        synchronized (history) {
            Iterator<Position> it = history.descendingIterator();
            while (it.hasNext()) {
                var p = it.next();
                if (p.getTime() >= maxAge) break;
                it.remove();
            }
            history.addFirst(point);
        }

        if (showPolynomialPredictionTrack) {
            // History is never empty... it always contains at least the last point
            // Only reading history, so need to synchronize
            PolynomialRegression prSpeedTrack = new PolynomialRegression(history.getLast().getTime(), 3);
            // Assume that the oldest history point is valid, and not turning
            float prevTrack = history.getLast().getTrack();
            long prevTime = history.getLast().getTime() - 1;
            Iterator<Position> it = history.descendingIterator();
            while (it.hasNext()) {
                var p = it.next();
                var time = p.getTime();
                if (time == prevTime || time < lastValid.getTime() - polynomialHistoryMilliS || !p.isValid())
                    continue;
                // Unwind modulo arithmetic so that turns in the same direction keep incrementing, even though the result is > 360 or < 0
                var track = p.getTrack();
                var turn = track - prevTrack;
                while (turn > 180) turn -= 360;
                while (turn < -180) turn += 180;
                Log.v("Add %d %5.0f %5.0f %5.0f", time, p.getSpeed(), prevTrack + turn, p.getAltitude());
                prSpeedTrack.add(time, p.getSpeed(), prevTrack + turn, (float) p.getAltitude());
                prevTrack = track;
                prevTime = time;
            }

            double[][] cSpeedTrack = prSpeedTrack.getCoefficients();
            synchronized (predicted) {
                predicted.clear();
                if (cSpeedTrack != null) {
                    Log.v("Speed coeffs %.1f %.3f %.5f", cSpeedTrack[0][0], cSpeedTrack[0][1], cSpeedTrack[0][2]);
                    Log.v("Track coeffs %.1f %.3f %.5f", cSpeedTrack[1][0], cSpeedTrack[1][1], cSpeedTrack[1][2]);
                    Log.v("Alt   coeffs %.1f %.3f %.5f", cSpeedTrack[2][0], cSpeedTrack[2][1], cSpeedTrack[2][2]);
                    Position p = lastValid;
                    for (int t = polynomialPredictionStepMilliS; t <= predictionMilliS; t += polynomialPredictionStepMilliS) {
                        var t1 = now - (long) cSpeedTrack[0][3] + t;
                        var t2 = t1 * t1;
                        p.moveBy(polynomialPredictionStepMilliS);
                        float speed = (float) (cSpeedTrack[0][0] + cSpeedTrack[0][1] * t1 + cSpeedTrack[0][2] * t2);
                        float track = (float) (cSpeedTrack[1][0] + cSpeedTrack[1][1] * t1 + cSpeedTrack[1][2] * t2);
                        float alt = (float) (cSpeedTrack[2][0] + cSpeedTrack[2][1] * t1 + cSpeedTrack[2][2] * t2);
                        p.setSpeed(speed);
                        p.setTrack(track % 360);
                        p.setAltitude(alt);
                        p.setTime(now + t1);
                        predicted.addLast(p);
                        Log.v("%s Predict Speed %.1f Track %.1f Alt %.1f %s", callsign, speed, track, alt, p);
                    }
                }
            }
        }
        mapView.refresh(layer);
    }

    public boolean isValid() {
        return history.getFirst().isCrcValid();
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
