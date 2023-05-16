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

import static com.meerkat.ui.settings.SettingsViewModel.altUnits;
import static com.meerkat.ui.settings.SettingsViewModel.logReplay;
import static com.meerkat.ui.settings.SettingsViewModel.minGpsUpdateIntervalSeconds;
import static com.meerkat.ui.settings.SettingsViewModel.preferAdsbPosition;
import static com.meerkat.ui.settings.SettingsViewModel.simulate;
import static com.meerkat.ui.settings.SettingsViewModel.speedUnits;
import static java.lang.Float.NaN;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.widget.Toast;

import com.meerkat.log.Log;

import java.time.Instant;

public class Gps extends Service implements LocationListener {
    public static volatile boolean isEnabled;
    private static final Location location = new Location("gps");
    private static LocationManager locationManager;
    private static Context context;
    private static Gps instance;

    public Gps(Context ctx) {
        locationManager = (LocationManager) ctx.getSystemService(LOCATION_SERVICE);
        context = ctx;
        instance = this;
        resume();
    }

    public static float distanceTo(Location other) {
        checkEnabled();
        synchronized (location) {
            if (other.hasAccuracy() && location.hasAccuracy())
                return location.distanceTo(other);
        }
        return NaN;
    }

    public static double getAltitude() {
        checkEnabled();
        synchronized (location) {
            if (location.hasAltitude())
                return location.getAltitude();
        }
        return NaN;
    }

    public static float getTrack() {
        checkEnabled();
        synchronized (location) {
            if (location.hasBearing())
                return location.getBearing();
        }
        return NaN;
    }

    public static float bearingTo(Location other) {
        checkEnabled();
        synchronized (location) {
            if (other.hasAccuracy() && location.hasAccuracy())
                return location.bearingTo(other);
        }
        return NaN;
    }

    public static void getLatLonAltTime(Location copy) {
        checkEnabled();
        synchronized (location) {
            copy.set(location);
        }
    }

    // For simulator
    public static void setLocation(Location newLocation) {
        synchronized (location) {
            location.set(newLocation);
        }
        Compass.updateGeomagneticField();
        MainActivity.mapView.refresh(null);
    }

    @SuppressLint("MissingPermission")
    private static void checkEnabled() {
        if (isEnabled) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // This is a new method provided in API 28
            isEnabled = locationManager.isLocationEnabled();
        } else {
            // This was deprecated in API 28
            int mode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF);
            isEnabled = mode != Settings.Secure.LOCATION_MODE_OFF;
        }
        if (!isEnabled) return;
        MainActivity.blinkGps();
        Log.i("Gps Location Enabled");
        Toast.makeText(context, "GPS Recovered", Toast.LENGTH_LONG).show();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minGpsUpdateIntervalSeconds * 1000L, 0, Gps.instance);
        Location lastknown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastknown != null)
            location.set(lastknown);
    }

    /**
     * Stop using GPS listener
     */

    public void pause() {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @SuppressLint("MissingPermission")
    public void resume() {
        if (logReplay || simulate) {
            isEnabled = true;
            return;
        }
        try {
            // getting GPS status
            checkEnabled();
            if (!isEnabled)
                Toast.makeText(context, "GPS Not Available", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            throw new RuntimeException("GPS setup failed: " + e.getMessage());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        MainActivity.blinkGps();
        if (logReplay || simulate)
            return;
        // Only use phone GPS if it is preferred or if it's been too long since an ADS-B
        // own ship Traffic message has updated it
        if (preferAdsbPosition && location.getTime() > Instant.now().toEpochMilli() - minGpsUpdateIntervalSeconds * 1000L)
            return;
        setLocation("GPS", location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getSpeed(), location.getBearing(), location.getTime());
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.w("GPS status changed: %d", status);
    }

    // For simulator & ADS-B derived location
    public static void setLocation(String provider, double lat, double lon, double alt, float speed, float trk, long millis) {
        synchronized (location) {
            location.setProvider(provider);
            location.setLatitude(lat);
            location.setLongitude(lon);
            location.setAltitude(alt);
            location.setSpeed(speed);
            location.setBearing(trk);
            location.setTime(millis);
        }
        Log.i("%s (%.5f, %.5f) %s, %s %3.0f%c", location.getProvider(), location.getLatitude(), location.getLongitude(),
                altUnits.toString(location.getAltitude()), speedUnits.toString(location.getSpeed()),
                location.getBearing(), location.hasBearing() ? ' ' : '!');
        Compass.updateGeomagneticField();
        MainActivity.mapView.refresh(null);
    }

    @Override
    public void onProviderDisabled(String provider) {
        isEnabled = false;
        Log.w("GPS Disabled");
        Toast.makeText(context, "GPS Lost", Toast.LENGTH_LONG).show();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onProviderEnabled(String provider) {
        Location lastknown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastknown != null)
            location.set(lastknown);
        isEnabled = true;
        Toast.makeText(context, "GPS Available", Toast.LENGTH_SHORT).show();
        Log.w("GPS Enabled");
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}
