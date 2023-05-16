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

import static com.meerkat.ui.settings.SettingsViewModel.autoZoom;
import static com.meerkat.ui.settings.SettingsViewModel.dangerRadiusMetres;
import static com.meerkat.ui.settings.SettingsViewModel.logReplay;
import static com.meerkat.ui.settings.SettingsViewModel.ownId;
import static com.meerkat.ui.settings.SettingsViewModel.preferAdsbPosition;
import static com.meerkat.ui.settings.SettingsViewModel.purgeSeconds;
import static com.meerkat.ui.settings.SettingsViewModel.replaySpeedFactor;
import static com.meerkat.ui.settings.SettingsViewModel.simulate;
import static java.lang.Double.MAX_VALUE;
import static java.lang.Double.isNaN;

import android.graphics.Color;

import com.meerkat.log.Log;
import com.meerkat.map.AircraftLayer;
import com.meerkat.map.VehicleIcon;
import com.meerkat.measure.Position;
import com.meerkat.ui.settings.SettingsViewModel;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class VehicleList extends HashMap<Integer, Vehicle> {
    public Vehicle nearest = null;
    public Vehicle furthest = null;
    private final ScheduledFuture sched;

    private void purge() {
        try {
            if (this.isEmpty()) return;
            long purgeTime = (logReplay || simulate ? LogReplay.clock.millis() : Instant.now().toEpochMilli()) - purgeSeconds * 1000L;
            Log.i("Locking vehicleList to purge all last updated before %s", Instant.ofEpochMilli(purgeTime).toString());
            var changeBackground = false;
            var modeCAlt = MAX_VALUE;
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
                            MainActivity.mapView.layers.invalidateDrawable(v.layer);
                            continue;
                        }
                        if (v.position != null)
                            if (v.position.hasAccuracy()) {
                                // NB side-effect to store nearest & furthest -- both must be executed
                                changeBackground |= checkAndMakeNearest(v);
                                changeBackground |= checkAndMakeFurthest(v);
                            } else if (v.position.hasAltitude()) {
                                if (Math.abs(v.position.heightAboveOwnship()) < Math.abs(modeCAlt))
                                    modeCAlt = v.position.heightAboveOwnship();
                            }
                        Log.v("Exited %s", v.toString());
                    }
                }
            }
            Log.i("Purge complete");
            if (changeBackground) MainActivity.mapView.refresh(null);
            // If no Mode-C traffic found, set button colour to transparent to show toolbar colour
            MainActivity.setModeC(modeCAlt == MAX_VALUE ? Color.alpha(0) : AircraftLayer.altColour(modeCAlt, true));
            MainActivity.setAlert(nearest.distance < dangerRadiusMetres &&
                    Math.abs(nearest.position.heightAboveOwnship()) < SettingsViewModel.gradientMinimumDiff);
        } catch (Exception ex) {
            Log.e("Exception in VehicleList purge %s", ex.getMessage());
        }
    }

    public VehicleList() {
        super();
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
            v = new Vehicle(crc, participantAddr, callsign, point, emitterType);
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
        MainActivity.mapView.refresh(backgroundChanged ? null : v.layer);
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
