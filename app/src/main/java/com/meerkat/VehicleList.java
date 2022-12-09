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

import static com.meerkat.SettingsActivity.purgeSeconds;
import static java.util.concurrent.TimeUnit.MINUTES;

import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.log.Log;
import com.meerkat.map.MapActivity;
import com.meerkat.measure.Position;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Executors;

public class VehicleList extends HashMap<Integer, Vehicle> {
    static public VehicleList vehicleList;

    private void purge() {
        long now = System.currentTimeMillis();
        synchronized (this) {
            Iterator<HashMap.Entry<Integer, Vehicle> > iterator = this.entrySet().iterator();
            while (iterator.hasNext()) {
                HashMap.Entry<Integer, Vehicle> entry = iterator.next();
                long age = now - entry.getValue().history.get(entry.getValue().history.size()-1).getTime();
                if (age > purgeSeconds * 1000L) {
                    Log.i("Purge: %s, %d",entry.getValue().callsign, age);
                    entry.getValue().layer.setVisible(false, false);
                    MapActivity.mapView.layers.invalidateDrawable(entry.getValue().layer);
                    iterator.remove();
                }
            }
        }
     }

    public VehicleList() {
        super();
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::purge, 1, 1, MINUTES);
    }

    public void upsert(String callsign, int participantAddr, Position point, Gdl90Message.Emitter emitterType) {
        Vehicle v = get(participantAddr);
        if (v != null) {
            v.update(point, callsign, emitterType);
        } else {
            v = new Vehicle(participantAddr, callsign, point, emitterType);
            put(participantAddr, v);
        }
        MapActivity.mapView.refresh(v.layer);
    }

    public Collection<Vehicle> getVehicles() {
        synchronized (this) {
            return values();
        }
    }

    static public Position getMaxDistance() {
        if (vehicleList == null) return null;
        synchronized (vehicleList) {
            if (vehicleList.isEmpty()) return null;
            float maxDistance = 0;
            Position furthest = null;
            for (Vehicle v: vehicleList.values()) {
                if (v.distance > maxDistance) {
                    maxDistance = v.distance;
                    furthest = v.lastValid;
                }
            }
            return furthest;
        }
    }
}
