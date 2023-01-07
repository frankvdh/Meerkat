package com.meerkat;

import static com.meerkat.SettingsActivity.ownCallsign;
import static com.meerkat.SettingsActivity.simulate;
import static java.lang.Float.parseFloat;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.location.Location;

import com.meerkat.gdl90.Traffic;
import com.meerkat.log.Log;
import com.meerkat.map.MapActivity;
import com.meerkat.map.MapView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;

public class CsvPlayer {
    BufferedReader aircraftReader;
    BufferedReader gpsReader;
    Location gps;
    Traffic traffic;
    long prev;
    private final MapView mapView;
    private final boolean realTime;
    static final DateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.ENGLISH);

    public CsvPlayer(MapView mapView, File gpsFile, File aircraftFile, boolean realTime) throws IOException {
        this.mapView = mapView;
        this.realTime = true;
        Log.level(Log.Level.D);
        Log.i("new CsvPlayer");
        aircraftReader = new BufferedReader(new FileReader(aircraftFile));
        gpsReader = new BufferedReader(new FileReader(gpsFile));
        // Skip headings
        gpsReader.readLine();
        aircraftReader.readLine();
        gps = readGps();
        traffic = readAircraft();
        prev = Math.min(gps.getTime(), traffic.point.getTime());
        if (simulate == SettingsActivity.SimType.CsvSlow)
            start();
/*
        var timestamp = Math.min(gps == null ? System.currentTimeMillis() : gps.getTime(),
                traffic == null ? System.currentTimeMillis() : traffic.point.getTime());
//            Log.i("%s: %s | %s", sdf.format(timestamp), gps == null ? "null" : gps.toString(), traffic == null ? "null" : traffic.toString());
       while (gps != null || traffic != null) {
        if (realTime && prev < timestamp) {
            try {
                Thread.sleep(timestamp - prev);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            act();
            prev = timestamp;
        }
        }
 */

    }

    ScheduledFuture<?> thread;

    private void start() {
        thread = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::act, 1, 1, SECONDS);
    }

    private void act() {
        if (gps != null && (traffic == null || gps.getTime() < traffic.point.getTime())) {
            Gps.setLocation(gps);
            Compass.updateGeomagneticField();
            mapView.refresh(null);
            gps = readGps();
        } else if (traffic != null) {
            Log.v(traffic.toString());
            MapActivity.vehicleList.upsert(traffic.callsign, traffic.participantAddr, traffic.point, traffic.emitterType);
            traffic = readAircraft();
        }
    }

    private Location readGps() {
        String s = null;
        try {
            s = gpsReader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (s == null) {
            Log.v("GPS null");
            return null;
        }
        Location result = new Location("GPS");
        var fields = s.split(",");
        var time = fields[0].split(":");
        result.setTime(Integer.parseInt(time[0]) * 3600000L + Integer.parseInt(time[1]) * 60000L + Math.round(parseFloat(time[2]) * 1000L));
        result.setLatitude(Double.parseDouble(fields[1]));
        result.setLongitude(Double.parseDouble(fields[2]));
        result.setAltitude(Double.parseDouble(fields[3]));
        result.setSpeed(Float.parseFloat(fields[4]));
        result.setBearing(Float.parseFloat(fields[5]));
        Log.v("%s %s", fields[0], result.toString());
        return result;
    }

    private Traffic readAircraft() {
        String s = null;
        try {
            s = aircraftReader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (s == null) {
            Log.i("Traffic null");
            return null;
        }
        var fields = s.split(",");
        var time = fields[0].split(":");
        Traffic result = new Traffic(Integer.parseInt(time[0]) * 3600000L + Integer.parseInt(time[1]) * 60000L + Math.round(parseFloat(time[2]) * 1000L), Integer.parseInt(fields[1]), fields[2], fields[3],
                Double.parseDouble(fields[4]), Double.parseDouble(fields[5]), Integer.parseInt(fields[6]), Float.parseFloat(fields[7]), Float.parseFloat(fields[8]), Float.parseFloat(fields[9]));
        Log.i("%s %s", fields[0], result.toString());
        return result;
    }
}

