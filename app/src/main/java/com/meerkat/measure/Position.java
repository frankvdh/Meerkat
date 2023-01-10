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
package com.meerkat.measure;

import static com.meerkat.SettingsActivity.*;
import static java.lang.Double.isNaN;
import static java.lang.Math.PI;
import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

import android.annotation.SuppressLint;
import android.location.Location;

import androidx.annotation.NonNull;

import java.time.Instant;

public class Position extends Location {
    double vVel;
    private boolean crcValid;
    private boolean airborne;
    private Instant time;

    public Position(String provider) {
        super(provider);
    }

    public Position(String provider, double lat, double lon, double alt, double speed, double track, double vVel, boolean crcValid, boolean airborne, Instant time) {
        super(provider);
        this.time = time;
        setTime(time.toEpochMilli());
        if (!isNaN(lat)) setLatitude(lat);
        if (!isNaN(lon)) setLongitude(lon);
        if (!isNaN(alt)) setAltitude(alt);
        else removeAltitude();
        if (!isNaN(speed))
            setSpeed((float) speed);
        else super.removeSpeed();
        if (hasTrack()) setTrack((float) track);
        else removeBearing();
        this.vVel = (float) vVel;
        this.crcValid = crcValid;
        this.airborne = airborne;
    }

    public Position(String provider, double lat, double lon, double alt, Instant time) {
        this(provider, lat, lon, alt, Float.NaN, Float.NaN, Float.NaN, true, true, time);
    }

    @SuppressWarnings("CopyConstructorMissesField")
    public Position(Position position) {
        super(position.getProvider());
        set(position);
    }

    public void setInstant(Instant i) {
        super.setTime(i.toEpochMilli());
        time = i;
    }

    public Instant getInstant() {
        return time;
    }

    public void setAirborne(boolean airborne) {
        this.airborne = airborne;
    }

    public void setCrcValid(boolean valid) {
        this.crcValid = valid;
    }

    public static double latLonDegToRad(double angle) {
        return angle * PI / 180d;
    }

    public static double rad2latLonDeg(double rad) {
        return rad * 180d / PI;
    }

    public static double bearingToRad(double b) {
        return ((450 - b) % 360 * PI / 180 + PI) % (PI * 2) - PI;
    }

    public static double radToBearing(double r) {
        return (450 - r * 180 / PI) % 360;
    }

    // Move this Position by the given lateral and vertical distance
    public void setRelative(Position p, float distance, float bearing, float altDifference, int elapsedMilliS) {
        double lat = p.getLatitude();
        double lon = p.getLongitude();
        if (isNaN(lat) || isNaN(lon) || isNaN(bearing) || isNaN(distance)) return;

        var bearingRad = latLonDegToRad(bearing);
        var latRad = latLonDegToRad(lat);
        var lonRad = latLonDegToRad(lon);
        var distFraction = distance / 6371000d; // earth Radius In Metres
        var cosLat = cos(latRad);
        var sinLat = sin(latRad);
        var sinDist = sin(distFraction);
        var cosDist = cos(distFraction);

        var latitudeResult = asin(sinLat * cosDist + cosLat * sinDist * cos(bearingRad));
        var a = atan2(sin(bearingRad) * sinDist * cosLat, cosDist - sinLat * sin(latitudeResult));
        var longitudeResult = (lonRad + a + 3 * PI) % (2 * PI) - PI;
        setProvider("from " + p.getProvider());
        setLatitude(rad2latLonDeg(latitudeResult));
        setLongitude(rad2latLonDeg(longitudeResult));
        setAltitude(getAltitude() + altDifference);
        setTime(p.getTime() + elapsedMilliS);
        setAirborne(p.airborne);
        setCrcValid(p.crcValid);
    }

    // Move this Position assuming unchanged speed, track, and VS
    public void moveBy(int elapsedMillis) {
        float elapsedSecs = elapsedMillis / 1000f;
        setRelative(this, getSpeed() * elapsedSecs, getTrack(), (float) (getVVel() * elapsedSecs), elapsedMillis);
    }

    public void set(Position p) {
        super.set(p);
        this.crcValid = p.crcValid;
        this.airborne = p.airborne;
        setInstant(p.time);
        setVVel(p.vVel);
    }

    // Return new Position seconds into the future, given initial coordinates, altitude, speed and track, and vertical speed
    public Position linearPredict(int elapsedMillis, Position dest) {
        float elapsedSecs = elapsedMillis / 1000f;
        dest.setRelative(this, getSpeed() * elapsedSecs, getTrack(), (float) (vVel * elapsedSecs), elapsedMillis);
        return dest;
    }

    static public double heightAbove(Position p1, Location that) {
        return p1.getAltitude() - that.getAltitude();
    }

    public float getTrack() {
        return getBearing();
    }

    public void setTrack(float track) {
        if (Float.isNaN(track)) removeBearing();
        else setBearing(track);
    }

    public boolean hasTrack() {
        return hasBearing();
    }

    public double getVVel() {
        return vVel;
    }

    public void setVVel(double vVel) {
        this.vVel = vVel;
    }

    public boolean isCrcValid() {
        return crcValid;
    }

    @SuppressLint("DefaultLocale")
    static public String DMS(double deg) {
        if (isNaN(deg)) return "-----";
        String sign = "";
        if (deg < 0) {
            deg = -deg;
            sign = "-";
        }
        int d = (int) deg;
        int m = (int) ((deg - d) * 60);
        float s = (float) ((deg - d) * 3600 - m * 60);
        return String.format("%s%d:%02d:%04.1f", sign, d, m, s);
    }

    @SuppressLint("DefaultLocale")
    @NonNull
    public String toString() {
        double lat = getLatitude();
        double lon = getLongitude();
        return (isNaN(lat) ? "(-----, -----) " : String.format("(%.5f, %.5f) ", lat, lon)) + String.format("%s, %s, %03.0f, %s", altUnits.toString(getAltitude()), speedUnits.toString(getSpeed()), getTrack(), Units.VertSpeed.FPM.toString(vVel));
    }

    public boolean isAirborne() {
        return airborne;
    }

    public boolean isValid() {
        return !isNaN(getLatitude()) && !isNaN(getLongitude());
    }
}
