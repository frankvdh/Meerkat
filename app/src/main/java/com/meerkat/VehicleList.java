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

import static com.meerkat.SettingsActivity.ownId;
import static com.meerkat.SettingsActivity.preferAdsbPosition;
import static com.meerkat.SettingsActivity.purgeSeconds;
import static com.meerkat.SettingsActivity.simulate;
import static java.lang.Double.isNaN;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.log.Log;
import com.meerkat.map.MapView;
import com.meerkat.measure.Position;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Executors;

public class VehicleList extends HashMap<Integer, Vehicle> {
    private final MapView mapView;
    public Vehicle nearest = null;
    public Vehicle furthest = null;

    private void purge() {
        if (this.isEmpty()) return;
        long purgeTime = (simulate ? LogReplay.clock.millis() : Instant.now().toEpochMilli()) - purgeSeconds * 1000L;
        Log.i("Purge before %s", Instant.ofEpochMilli(purgeTime).toString());
        var backgroundChanged = false;
        synchronized (this) {
            Iterator<HashMap.Entry<Integer, Vehicle>> iterator = this.entrySet().iterator();
            while (iterator.hasNext()) {
                HashMap.Entry<Integer, Vehicle> entry = iterator.next();
                Vehicle v = entry.getValue();
                synchronized (v) {
                    if (v.lastValid == null) continue;
                    if (v.lastValid.getTime() < purgeTime) {
                        iterator.remove();
                        Log.i("Purged %s", v.callsign);
                        v.layer.setVisible(false, false);
                        mapView.layers.invalidateDrawable(v.layer);
                        continue;
                    }
                    // NB side-effect to store nearest & furthest -- both must be executed
                    backgroundChanged |= v.checkAndMakeNearest(this);
                    backgroundChanged |= v.checkAndMakeFurthest(this);
                }
            }
        }
        if (backgroundChanged) mapView.refresh(null);
    }

    public VehicleList(MapView mapView) {
        super();
        this.mapView = mapView;
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::purge, purgeSeconds, purgeSeconds, SECONDS);
    }

    public void upsert(int crc, String callsign, int participantAddr, Position point, Gdl90Message.Emitter emitterType) {
        Vehicle v = get(participantAddr);
        if (v != null) {
            if (v.lastCrc != crc)
                v.update(crc, point, callsign, emitterType);
        } else {
            v = new Vehicle(crc, participantAddr, callsign, point, emitterType, mapView);
            put(participantAddr, v);
        }
        if (isNaN(v.distance) || v.lastValid == null) return;
        if (preferAdsbPosition && participantAddr == ownId)
            Gps.setLocation(point);
        // NB side-effect to store nearest & furthest -- both must be executed
        var backgroundChanged = v.checkAndMakeNearest(this);
        backgroundChanged |= v.checkAndMakeFurthest(this);
        mapView.refresh(backgroundChanged ? null : v.layer);
    }

    public Collection<Vehicle> getVehicles() {
        synchronized (this) {
            return values();
        }
    }

    public Vehicle getNearest() {
        synchronized (this) {
            return nearest;
        }
    }

    public Vehicle getFurthest() {
        synchronized (this) {
            return furthest;
        }
    }
}
