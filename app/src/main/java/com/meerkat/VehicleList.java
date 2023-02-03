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
import static com.meerkat.SettingsActivity.purgeSeconds;
import static java.lang.Double.isNaN;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.log.Log;
import com.meerkat.map.MapView;
import com.meerkat.measure.Position;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Executors;

public class VehicleList extends HashMap<Integer, Vehicle> {
    private final MapView mapView;
    private Vehicle nearest = null;
    private Vehicle furthest = null;

    private void purge() {
        if (this.isEmpty()) return;
        float maxDistance = 0;
        float minDistance = 1e6f;
        furthest = null;
        nearest = null;
        Instant purgeTime = Instant.now().minus(purgeSeconds, ChronoUnit.SECONDS);
        Log.i("Purge before %s", purgeTime.toString());
        synchronized (this) {
            Iterator<HashMap.Entry<Integer, Vehicle>> iterator = this.entrySet().iterator();
            while (iterator.hasNext()) {
                HashMap.Entry<Integer, Vehicle> entry = iterator.next();
                Vehicle v = entry.getValue();
                if (v.lastValid == null) continue;
                if (v.lastValid.getInstant().isBefore(purgeTime)) {
                    iterator.remove();
                    Log.i("Purge %s", v.callsign);
                    v.layer.setVisible(false, false);
                    mapView.layers.invalidateDrawable(v.layer);
                    continue;
                }
                if (v.distance > maxDistance) {
                    maxDistance = v.distance;
                    furthest = v;
                }
                if (v.distance < minDistance) {
                    minDistance = v.distance;
                    nearest = v;
                }
            }
        }
    }

    public VehicleList(MapView mapView) {
        super();
        this.mapView = mapView;
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::purge, purgeSeconds, purgeSeconds, SECONDS);
    }

    public void upsert(int crc, String callsign, int participantAddr, Position point, Gdl90Message.Emitter emitterType, Instant time) {
        Vehicle v = get(participantAddr);
        point.setInstant(time);
        if (v != null) {
            if (v.lastCrc != crc)
                v.update(crc, point, callsign, emitterType);
        } else {
            v = new Vehicle(crc, participantAddr, callsign, point, emitterType, mapView);
            put(participantAddr, v);
        }
        if (isNaN(v.distance)) return;

        var backgroundChanged = false;
        if (nearest == null || v.distance < nearest.distance) {
            nearest = v;
            // Change to threat circle needed
            backgroundChanged = true;
        }
        if (autoZoom && (furthest == null || v.distance > furthest.distance)) {
            furthest = v;
            // Change to zoom level needed
            backgroundChanged = true;
        }
        mapView.refresh(backgroundChanged ? null : v.layer);
    }

    public Collection<Vehicle> getVehicles() {
        synchronized (this) {
            return values();
        }
    }

    public Vehicle getNearest() {
        synchronized(this) {
            return nearest;
        }
    }

    public Vehicle getFurthest() {
        synchronized(this) {
            return furthest;
        }
    }
}
