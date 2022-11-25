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

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;
import android.widget.Toast;

import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.log.Log;
import com.meerkat.measure.Distance;
import com.meerkat.measure.Height;
import com.meerkat.measure.Polar;
import com.meerkat.measure.Speed;
import com.meerkat.ui.map.MapFragment;

import java.util.Locale;

public class Gps extends Service implements LocationListener {

    public static volatile boolean isEnabled;

    public static volatile Location location;

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_UPDATE_INTERVAL = 60000; // 1 minute

    // Declaring a Location Manager
    private LocationManager locationManager;

    @SuppressLint("MissingPermission")
    public Gps(LocationManager locationManager) {
        location = new Location("gps");
        if (Settings.simulate)
            return;
        try {
            // getting GPS status
            isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (isEnabled) {
                // First get location from Network Provider
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_UPDATE_INTERVAL, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null)
                    Gps.location.set(location);
                Log.d("GPS Enabled: " + location);
            }
        } catch (Exception e) {
            throw new RuntimeException("GPSTracker setup failed: " + e.getMessage());
        }
    }

    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app
     */

    public void pause() {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
            locationManager = null;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        synchronized (Gps.location) {
            Gps.location.set(location);
        }
        Log.d("GPS: " + String.format(Locale.ENGLISH, "(%.5f, %.5f) @%.0fm, %.0f %3.0f", location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getSpeed(), location.getBearing()));
        MapFragment.refresh(null);
    }

    @Override
    public void onProviderDisabled(String provider) {
        isEnabled = false;
        Log.w("GPS Disabled");
        Toast.makeText(this.getApplicationContext(), "No GPS Fix", Toast.LENGTH_LONG).show();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onProviderEnabled(String provider) {
        location.set(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
        isEnabled = true;
        Toast.makeText(this.getApplicationContext(), "GPS Fix", Toast.LENGTH_SHORT).show();
        Log.w("GPS Enabled");
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}
