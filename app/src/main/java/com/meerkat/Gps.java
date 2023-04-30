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

import static com.meerkat.SettingsActivity.altUnits;
import static com.meerkat.SettingsActivity.minGpsDistanceChangeMetres;
import static com.meerkat.SettingsActivity.minGpsUpdateIntervalSeconds;
import static com.meerkat.SettingsActivity.preferAdsbPosition;
import static com.meerkat.SettingsActivity.logReplay;
import static com.meerkat.SettingsActivity.simulate;
import static com.meerkat.SettingsActivity.speedUnits;
import static java.lang.Float.NaN;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;
import android.widget.Toast;

import com.meerkat.log.Log;
import com.meerkat.map.MapView;

import java.time.Instant;

public class Gps extends Service implements LocationListener {
    private static MapView _mapView;
    public static volatile boolean isEnabled;

    private static final Location location = new Location("gps");

    // Declaring a Location Manager
    private final LocationManager locationManager;

    public Gps(Context context, MapView mapView) {
        _mapView = mapView;
        this.locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        resume();
    }

    public static float distanceTo(Location other) {
        synchronized (location) {
            return location.distanceTo(other);
        }
    }

    public static double getAltitude() {
        synchronized (location) {
            if (location.hasAltitude())
                return location.getAltitude();
        }
        return NaN;
    }

    public static float getTrack() {
        synchronized (location) {
            if (location.hasBearing())
                return location.getBearing();
        }
        return NaN;
    }

    public static float bearingTo(Location other) {
        synchronized (location) {
            return location.bearingTo(other);
        }
    }

    public static void getLatLonAltTime(Location copy) {
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
        _mapView.refresh(null);
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
        _mapView.refresh(null);
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
            isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (isEnabled) {
                // First get location from Network Provider
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minGpsUpdateIntervalSeconds * 1000L, minGpsDistanceChangeMetres, this);
                Location lastknown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastknown != null)
                    location.set(lastknown);
                Log.d("GPS Enabled: %s", location);
            }
        } catch (Exception e) {
            throw new RuntimeException("GPS setup failed: " + e.getMessage());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (logReplay || simulate)
            return;
        // Only use phone GPS if it is preferred or if it's been too long since an ADS-B
        // own ship Traffic message has updated it
        if (!preferAdsbPosition || location.getTime() + minGpsUpdateIntervalSeconds * 1000L > Instant.now().toEpochMilli())
            return;
        setLocation("GPS", location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getSpeed(), location.getBearing(), location.getTime());
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
