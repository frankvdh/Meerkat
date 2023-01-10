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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    public Position lastValid;
    public final Position predictedPosition;
    public float distance;
    final AircraftLayer layer;
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
        this.predicted = new LinkedList<>();
        // always create predictedPosition for synchronization
        layer = findLayer(this);
        predictedPosition = new Position("predicted");
        if (point.isValid()) {
            addPoint(point);
            if (showLinearPredictionTrack)
                point.linearPredict(predictionMilliS, predictedPosition);
        } else {
            lastValid = null;
            distance = Float.NaN;
            predictedPosition.setLatitude(Double.NaN);
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
        Log.d(String.format("%06x, %s, %s, %s", id, callsign, emitterType, point.toString()));

        synchronized (layer) {
            this.lastCrc = crc;
            if (emitterType != Gdl90Message.Emitter.Unknown) {
                this.emitterType = emitterType;
            }
            if (callsign != null && !callsign.equals(this.callsign))
                this.callsign = callsign;

            if (point.isValid()) {
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

        if (showLinearPredictionTrack && lastValid != null && !Float.isNaN(lastValid.getTrack()) && point.hasSpeed()) {
            synchronized (layer) {
                lastValid.linearPredict((int) (lastValid.getInstant().until(now, ChronoUnit.MILLIS)) + predictionMilliS, predictedPosition);
            }
        }

        if (showPolynomialPredictionTrack && !history.isEmpty()) {
            // Only reading history, so need to synchronize
            PolynomialRegression prSpeedTrack = new PolynomialRegression(history.getLast().getInstant(), 3);
            // Assume that the oldest history point is valid, and not turning
            float prevTrack = history.getLast().getTrack();
            var prevTime = history.getLast().getInstant();
            synchronized (layer) {
                Iterator<Position> it = history.descendingIterator();
                while (it.hasNext()) {
                    var p = it.next();
                    var time = p.getInstant();
                    if (time.isBefore(prevTime) || time.isBefore(lastValid.getInstant().minus(polynomialHistoryMilliS, ChronoUnit.MILLIS)))
                        continue;
                    if (!p.hasTrack() || !p.hasSpeed() || !p.hasAltitude() || p.getSpeed() == 0) continue;
                    var track = p.getTrack();
                    var speed = p.getSpeed();
                    // Unwind modulo arithmetic so that turns in the same direction keep incrementing, even though the result is > 360 or < 0
                    var turn = track - prevTrack;
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
                predicted.clear();
                if (cSpeedTrack != null) {
                    Log.v("Speed coeffs %.1f %.3f %.5f", cSpeedTrack[0][0], cSpeedTrack[0][1], cSpeedTrack[0][2]);
                    Log.v("Track coeffs %.1f %.3f %.5f", cSpeedTrack[1][0], cSpeedTrack[1][1], cSpeedTrack[1][2]);
                    Log.v("Alt   coeffs %.1f %.3f %.5f", cSpeedTrack[2][0], cSpeedTrack[2][1], cSpeedTrack[2][2]);
                    Position p = lastValid;
                    for (int t = polynomialPredictionStepMilliS; t <= predictionMilliS; t += polynomialPredictionStepMilliS) {
                        long t1 = Instant.ofEpochMilli((long) cSpeedTrack[0][3]).until(now, ChronoUnit.MILLIS) + t;
                        var t2 = t1 * t1;
                        p = p.linearPredict(polynomialPredictionStepMilliS, new Position("poly"));
                        float speed = (float) (cSpeedTrack[0][0] + cSpeedTrack[0][1] * t1 + cSpeedTrack[0][2] * t2);
                        float track = (float) (cSpeedTrack[1][0] + cSpeedTrack[1][1] * t1 + cSpeedTrack[1][2] * t2);
                        float alt = (float) (cSpeedTrack[2][0] + cSpeedTrack[2][1] * t1 + cSpeedTrack[2][2] * t2);
                        p.setAltitude(p.getAltitude()/2 + alt/2);
                        p.setSpeed(p.getSpeed() /2 + speed/2);
                        p.setTrack((p.getTrack()/2 + track/2) % 360);
                        p.setInstant(now.plus(t1, ChronoUnit.MILLIS));
                        predicted.addLast(p);
                        Log.v("%s Predict Speed %.1f Track %.1f Alt %.1f %s", callsign, speed, track, alt, p);
                    }
                }
            }
        }
        mapView.refresh(layer);
    }

    private void addPoint(Position point) {
        lastValid = point;
        history.addFirst(point);
        distance = Gps.distanceTo(point); // metres
    }

    public boolean isValid() {
        return !history.isEmpty() && history.getFirst().isCrcValid();
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
