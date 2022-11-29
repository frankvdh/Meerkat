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

import static com.meerkat.Settings.*;

import androidx.annotation.NonNull;

import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.log.Log;
import com.meerkat.measure.Position;
import com.meerkat.measure.Speed;
import com.meerkat.ui.map.AircraftLayer;
import com.meerkat.ui.map.MapFragment;

import java.util.ArrayList;
import java.util.Locale;

public class Vehicle implements Comparable<Vehicle> {
    public final int id;
    public String callsign;
    public final ArrayList<Position> history;
    public final ArrayList<Position> predicted;
    public @NonNull
    Gdl90Message.Emitter emitterType;
    public final Position current;
    public Position predictedPosition;
    private float distance;
    final AircraftLayer layer;

    public Vehicle(int id, String callsign, Position point, @NonNull Gdl90Message.Emitter emitterType) {
        this.id = id;
        this.callsign = callsign;
        this.emitterType = emitterType;
        this.history = new ArrayList<>();
        this.predicted = new ArrayList<>();
        current = point;

        history.add(current);
        distance = Gps.location.distanceTo(current);
        if (showLinearPredictionTrack)
            predictedPosition = current.linearPredict(predictionSeconds);
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
//        Log.d(String.format("%06x, \"%s\", \"%s\", %s", id, callsign, emitterType, point.toString()));
        synchronized (current) {
            current.set(point);
        }
        distance = Gps.location.distanceTo(point); // metres
//        Log.i("Current: " + current.toString());
        history.add(point);
        if (emitterType != Gdl90Message.Emitter.Unknown) {
            if (this.emitterType != emitterType) {
                this.emitterType = emitterType;
            }
        }
        if (callsign != null)
            this.callsign = callsign;
        if (showLinearPredictionTrack)
            predictedPosition = current.linearPredict(predictionSeconds);
        else
            predictedPosition = null;

        if (showPolynomialPredictionTrack) {
            PolynomialRegression prSpeedTrack = new PolynomialRegression(current.getTime(), 2);
            final long maxAge = point.getTime() - historySeconds * 1000L;
            synchronized(history) {
                history.removeIf(e -> e.getTime() < maxAge);
            }
            float prevTrack = history.get(0).getTrack();
            float prevTime = history.get(0).getTime();
            for (int i = 1; i < history.size(); i++) {
                var p = history.get(i);
                var time = p.getTime();
                if (time == prevTime) continue;
                // Use the angle between one reading and the next and their timestamps to calculate
                // rate of turn for prediction. This is to avoid clock arithmetic issues,
                // and to allow predictions that exceed 180 degrees total turn
                var track = p.getTrack();

                var turnRate = (track - prevTrack) / (time - prevTime)/1000;
                if (turnRate < -180) turnRate += 180;
                else if (turnRate > 180) turnRate -= 180;
                prSpeedTrack.add(time, p.getSpeedUnits().value, turnRate);
                prevTrack = track;
                prevTime = time;
            }
            double[][] cSpeedTrack = prSpeedTrack.getCoefficients();
            synchronized (predicted) {
                predicted.clear();
                if (cSpeedTrack != null) {
                    Log.v("Speed coeffs %.1f %.3f %.5f", cSpeedTrack[0][0], cSpeedTrack[0][1], cSpeedTrack[0][2]);
                    Log.v("Track coeffs %.1f %.3f %.5f", cSpeedTrack[1][0], cSpeedTrack[1][1], cSpeedTrack[1][2]);
                    if (current.isValid()) {
                        Position p = current;
                        for (int t = polynomialPredictionStepSeconds; t <= predictionSeconds; t += polynomialPredictionStepSeconds) {
                            p = p.linearPredict(polynomialPredictionStepSeconds);
                            float speed = (float) (cSpeedTrack[0][0] + cSpeedTrack[0][1] * t * 1000 + cSpeedTrack[0][2] * t * t * 1000000);
                            float track = (float) (cSpeedTrack[1][0] + cSpeedTrack[1][1] * t * 1000 + cSpeedTrack[1][2] * t * t * 1000000);
                            p.setSpeed(new Speed(speed, Speed.Units.KNOTS));
                            p.setTrack(p.getTrack() + track * polynomialPredictionStepSeconds * 1000);
                            predicted.add(p);
                            Log.d("%s Speed %.1f Track %.1f", callsign, speed, track);
                        }
                    }
                }
            }
        }
        MapFragment.refresh(layer);
    }

    @NonNull
    public String toString() {
        return String.format(Locale.ENGLISH, "%06x %-8s %c %s", id, callsign, current.isCrcValid() ? ' ' : '!', current);
    }

    @Override
    public int compareTo(Vehicle o) {
        return Float.compare(this.distance, o.distance);
    }
}
