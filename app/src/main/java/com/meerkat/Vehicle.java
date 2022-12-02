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

import static com.meerkat.Settings.countryCode;
import static com.meerkat.Settings.historySeconds;
import static com.meerkat.Settings.polynomialHistorySeconds;
import static com.meerkat.Settings.polynomialPredictionStepSeconds;
import static com.meerkat.Settings.predictionSeconds;
import static com.meerkat.Settings.showLinearPredictionTrack;
import static com.meerkat.Settings.showPolynomialPredictionTrack;

import androidx.annotation.NonNull;

import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.log.Log;
import com.meerkat.measure.Position;
import com.meerkat.ui.map.AircraftLayer;
import com.meerkat.ui.map.MapFragment;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;

public class Vehicle implements Comparable<Vehicle> {
    public final int id;
    public String callsign;
    public final LinkedList<Position> history;
    public final LinkedList<Position> predicted;
    public @NonNull
    Gdl90Message.Emitter emitterType;
    public Position lastValid;
    public Position predictedPosition;
    private float distance;
    final AircraftLayer layer;

    public Vehicle(int id, String callsign, Position point, @NonNull Gdl90Message.Emitter emitterType) {
        this.id = id;
        this.callsign = callsign;
        this.emitterType = emitterType;
        // History & Predicted go in opposite directions... in each case, the last entry is the furthest away from the current position of the aircraft.
        // So history is in decreasing time order, and predicted is in increasing time order
        this.history = new LinkedList<>();
        this.predicted = new LinkedList<>();

        history.addFirst(point);
        // Always accept the first point, even if it isn't valid
        lastValid = point;
        distance = Gps.location.distanceTo(point);
        if (showLinearPredictionTrack)
            predictedPosition = point.linearPredict(predictionSeconds * 1000L);
        else
            predictedPosition = null;
        layer = findLayer(this);
    }

    // If an invisible layer exists, re-use it. Otherwise create a new layer for this vehicle
    AircraftLayer findLayer(Vehicle v) {
        AircraftLayer result = (AircraftLayer) MapFragment.layers.findDrawableByLayerId(v.id);
        if (result != null) return result;
        for (int i = 1; i < MapFragment.layers.getNumberOfLayers(); i++) {
            AircraftLayer d = (AircraftLayer) MapFragment.layers.getDrawable(i);
            if (!d.isVisible()) {
                Log.i("ReUse layer %d was %d", i, MapFragment.layers.getId(i));
                synchronized (MapFragment.layers) {
                    MapFragment.layers.setId(i, v.id);
                    d.set(v);
                    return d;
                }
            }
        }
        AircraftLayer d = new AircraftLayer(v);
        synchronized (MapFragment.layers) {
            MapFragment.layers.addLayer(d);
            return d;
        }
    }

    public String getLabel() {
        if (callsign == null)
            return String.format("%07x", id);
        if (countryCode.isBlank()) return callsign;
        if (callsign.toUpperCase().startsWith(countryCode)) {
            int start = countryCode.length();
            if (callsign.charAt(start) == '-') start++;
            return callsign.substring(start).toUpperCase();
        }
        return callsign;
    }

    public void update(Position point, String callsign, @NonNull Gdl90Message.Emitter emitterType) {
        Log.d(String.format("%06x, \"%s\", \"%s\", %s", id, callsign, emitterType, point.toString()));
        var now = System.currentTimeMillis();
        final long maxAge = now - historySeconds * 1000L;
        synchronized (history) {
            Iterator<Position> it = history.descendingIterator();
            while (it.hasNext()) {
                var p = it.next();
                if (p.getTime() > maxAge) break;
                it.remove();
            }
            history.addFirst(point);
        }
        synchronized (this) {
            if (point.isValid())
                lastValid = point;
            distance = Gps.location.distanceTo(point); // metres

        Log.v("Current: %s", lastValid.toString());
            if (emitterType != Gdl90Message.Emitter.Unknown) {
                if (this.emitterType != emitterType) {
                    this.emitterType = emitterType;
                }
            }
            if (callsign != null && !callsign.equals(this.callsign))
                this.callsign = callsign;

            if (showLinearPredictionTrack && lastValid != null) {
                predictedPosition = lastValid.linearPredict(now - lastValid.getTime() + predictionSeconds * 1000L);
                predictedPosition.setTime(now + predictionSeconds * 1000L);
            }
        }
        if (showPolynomialPredictionTrack) {
            if (!history.isEmpty()) {
                PolynomialRegression prSpeedTrack = new PolynomialRegression(history.getLast().getTime(), 3);
                // Assume that the oldest history point is valid, and not turning
                float prevTrack = history.getLast().getTrack();
                long prevTime = history.getLast().getTime() - 1;
                Iterator<Position> it = history.descendingIterator();
                while (it.hasNext()) {
                    var p = it.next();
                    var time = p.getTime();
                    if (time == prevTime || time < lastValid.getTime() - polynomialHistorySeconds * 1000L || !p.isValid())
                        continue;
                    // Unwind modulo arithmetic so that turns in the same direction keep incrementing, even though the result is > 360 or < 0
                    var track = p.getTrack();
                    var turn = track - prevTrack;
                    while (turn > 180) turn -= 360;
                    while (turn < -180) turn += 180;
                    Log.v("Add %d %5.0f %5.0f %5.0f", time, p.getSpeedMps(), prevTrack + turn, p.getAltitude());
                    prSpeedTrack.add(time, p.getSpeedMps(), prevTrack + turn, (float) p.getAltitude());
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
                        for (int t = polynomialPredictionStepSeconds; t <= predictionSeconds; t += polynomialPredictionStepSeconds) {
                            var t1 = now - (long) cSpeedTrack[0][3] + t * 1000L;
                            var t2 = t1 * t1;
                            p = p.linearPredict(polynomialPredictionStepSeconds * 1000L);
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
        }
        MapFragment.refresh(layer);
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
