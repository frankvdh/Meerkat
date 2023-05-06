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
import static com.meerkat.SettingsActivity.logReplay;
import static com.meerkat.SettingsActivity.ownId;
import static com.meerkat.SettingsActivity.preferAdsbPosition;
import static com.meerkat.SettingsActivity.purgeSeconds;
import static com.meerkat.SettingsActivity.replaySpeedFactor;
import static com.meerkat.SettingsActivity.simulate;
import static java.lang.Double.isNaN;

import com.meerkat.log.Log;
import com.meerkat.map.MapView;
import com.meerkat.map.VehicleIcon;
import com.meerkat.measure.Position;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class VehicleList extends HashMap<Integer, Vehicle> {
    private final MapView mapView;
    public Vehicle nearest = null;
    public Vehicle furthest = null;
    private ScheduledFuture sched;

    private void purge() {
        try {
            if (this.isEmpty()) return;
            long purgeTime = (logReplay || simulate ? LogReplay.clock.millis() : Instant.now().toEpochMilli()) - purgeSeconds * 1000L;
            Log.i("Locking vehicleList to purge all last updated before %s", Instant.ofEpochMilli(purgeTime).toString());
            var backgroundChanged = false;
            synchronized (this) {
                Iterator<HashMap.Entry<Integer, Vehicle>> iterator = this.entrySet().iterator();
                while (iterator.hasNext()) {
                    HashMap.Entry<Integer, Vehicle> entry = iterator.next();
                    Vehicle v = entry.getValue();
                    Log.v("Waiting for %s", v.toString());
                    synchronized (v.lock) {
                        if (v.lastUpdate < purgeTime) {
                            iterator.remove();
                            Log.i("Purged %s", v.toString());
                            v.layer.setVisible(false, false);
                            mapView.layers.invalidateDrawable(v.layer);
                            continue;
                        }
                        if (v.position != null) {
                            // NB side-effect to store nearest & furthest -- both must be executed
                            backgroundChanged |= checkAndMakeNearest(v);
                            backgroundChanged |= checkAndMakeFurthest(v);
                        }
                        Log.v("Exited %s", v.toString());
                    }
                }
            }
            Log.i("Purge complete");
            if (backgroundChanged) mapView.refresh(null);
        } catch (Exception ex) {
            Log.e("Exception in VehicleList purge %s", ex.getMessage());
        }
    }

    public VehicleList(MapView mapView) {
        super();
        this.mapView = mapView;
        var interval = logReplay || simulate ? Math.round(purgeSeconds * 1000f / replaySpeedFactor) : purgeSeconds * 1000;
        Log.i("Purge at %d millisecond intervals", interval);
        sched = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::purge, interval, interval, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        sched.cancel(true);
        Log.i("VehicleList purge cancelled");
    }

    private boolean checkAndMakeNearest(Vehicle v) {
        if (nearest != null) {
            if (nearest == v) {
                if (v.id == ownId && ownId != 0) {
                    // If ownShip has erroneously been identified as nearest (e.g. a message
                    // with a valid id & position but no callsign was received,
                    // then a message with callsign & id is received),
                    // remove ownShip from nearest, so that the actual nearest aircraft will update
                    nearest = null;
                }
                // This is nearest and has received an update, so assume relative positions have changed
                Log.v("Nearest: %s %.0f %s vs %.0f", v.toString(), v.distance, v.position == null ? "null" : v.position.isAirborne(), nearest == null ? Float.NaN : nearest.distance);
                return true;
            }
            return false;
        }
        if (v.id == ownId && ownId != 0) return false;
        Log.v("Nearest: %s %.0f %s vs %.0f", this.toString(), v.distance, v.position == null ? "null" : v.position.isAirborne(), nearest == null ? Float.NaN : nearest.distance);
        // Change to threat circle needed
        nearest = v;
        return true;
    }

    private boolean checkAndMakeFurthest(Vehicle v) {
        if (!autoZoom || !v.position.isAirborne() || v.id == ownId) return false;
        if (furthest != null) {
            if (furthest == v || v.distance < furthest.distance) return false;
        }
        Log.v("Furthest: %s %.0f %s vs %.0f", v.callsign, v.distance, v.position == null ? "null" : v.position.isAirborne(), furthest == null ? Float.NaN : furthest.distance);
        // Change to zoom level needed
        furthest = v;
        return true;
    }

    public void upsert(int crc, String callsign, int participantAddr, Position point, VehicleIcon.Emitter emitterType) {
        Vehicle v = get(participantAddr);
        if (v != null) {
            // Ping often sends the same message several times... throw away the duplicates
            if (v.lastCrc == crc) return;
            v.update(crc, point, callsign, emitterType);
        } else {
            v = new Vehicle(crc, participantAddr, callsign, point, emitterType, mapView);
            synchronized (this) {
                put(participantAddr, v);
            }
        }
        if (isNaN(v.distance) || v.position == null) return;
        if (preferAdsbPosition && participantAddr == ownId)
            Gps.setLocation(point);
        // NB side-effect to store nearest & furthest -- both must be executed
        var backgroundChanged = checkAndMakeNearest(v);
        backgroundChanged |= checkAndMakeFurthest(v);
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
