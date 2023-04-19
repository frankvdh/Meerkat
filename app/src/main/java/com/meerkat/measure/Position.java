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

import static com.meerkat.SettingsActivity.altUnits;
import static com.meerkat.SettingsActivity.speedUnits;
import static com.meerkat.SettingsActivity.vertSpeedUnits;
import static java.lang.Double.NaN;
import static java.lang.Double.isNaN;
import static java.lang.Math.PI;
import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

import android.annotation.SuppressLint;
import android.location.Location;

import androidx.annotation.NonNull;

import com.meerkat.Gps;

public class Position extends Location {
    double vVel;
    private boolean crcValid;
    private boolean airborne;

    /**
     * A Position contains the position and motion components of a Vehicle at a specific Instant
     * If it has a Latitude & Longitude, then hasAccuracy() will return true.
     * If it has an Altitude, then hasAltitude() will return true.
     * If the Vehicle is in motion (both a Speed and a Track), then both hasSpeed() and hasTrack() will return true. getSpeed may return 0.
     * If the Vehicle is in vertical motion (a VVel), then hasVVel() will return true. getVVel may return 0.
     * The Instant that this Position relates to can be retrieved via getInstant(). All Positions have a valid Instant... however, the Instant may
     * be changed when, e.g. calculating a future Position from an existing one.
     *
     * @param provider Location provider
     */
    public Position(String provider) {
        super(provider);
    }

    public Position(String provider, double lat, double lon, double alt, double speed, double track, double vVel, boolean crcValid, boolean airborne, long time) {
        super(provider);
        setTime(time);
        if (!isNaN(lat)) setLatitude(lat);
        if (!isNaN(lon)) setLongitude(lon);
        if (!isNaN(lat) && !isNaN(lon))
            setAccuracy(20);
        else
            removeAccuracy();
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

    public Position(Position position) {
        super(position.getProvider());
        set(position);
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

    // Set this Position to be the given lateral and vertical distance from "from"
    public void setRelative(Position from, float distance, float track, float altDifference, int elapsedMilliS) {
        if (!from.hasAccuracy() || Float.isNaN(track) || Float.isNaN(distance)) return;

        var trackRad = latLonDegToRad(track);
        var latRad = latLonDegToRad(from.getLatitude());
        var lonRad = latLonDegToRad(from.getLongitude());
        var distFraction = distance / 6371000d; // earth Radius In Metres
        var cosLat = cos(latRad);
        var sinLat = sin(latRad);
        var sinDist = sin(distFraction);
        var cosDist = cos(distFraction);

        var latitudeResult = asin(sinLat * cosDist + cosLat * sinDist * cos(trackRad));
        var a = atan2(sin(trackRad) * sinDist * cosLat, cosDist - sinLat * sin(latitudeResult));
        var longitudeResult = (lonRad + a + 3 * PI) % (2 * PI) - PI;
        setProvider("from " + from.getProvider());
        setLatitude(rad2latLonDeg(latitudeResult));
        setLongitude(rad2latLonDeg(longitudeResult));
        setAccuracy(from.getAccuracy());
        if (isNaN(altDifference)) removeAltitude();
        else setAltitude(from.getAltitude() + altDifference);

        setTime(from.getTime() + elapsedMilliS);
        setAirborne(from.airborne);
        setCrcValid(from.crcValid);
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
        setVVel(p.vVel);
    }

    // Return new Position seconds into the future, given initial coordinates, altitude, speed and track, and vertical speed
    public void linearPredict(int elapsedMillis, Position dest) {
        float elapsedSecs = elapsedMillis / 1000f;
        dest.setRelative(this, getSpeed() * elapsedSecs, getTrack(), (float) (vVel * elapsedSecs), elapsedMillis);
    }

    public double heightAboveGps() {
        return getAltitude() - Gps.getAltitude();
    }

    @SuppressLint("Range")
    @Override
    public double getAltitude() {
        if (!hasAltitude()) return NaN;
        return super.getAltitude();
    }

    @SuppressLint("Range")
    @Override
    public float getSpeed() {
        if (!hasSpeed()) return Float.NaN;
        return super.getSpeed();
    }

    @Override
    public float getBearing() {
        throw new RuntimeException("Call to getBearing()... deprecated -- use getTrack()");
    }

    @Override
    public void setBearing(float bearing) {
        throw new RuntimeException("Call to setBearing()... deprecated -- use setTrack()");
    }

    @Override
    public boolean hasBearing() {
        throw new RuntimeException("Call to hasBearing()... deprecated -- use hasTrack()");
    }

    public float getTrack() {
        if (!hasTrack()) return Float.NaN;
        return super.getBearing();
    }

    public void setTrack(float track) {
        if (Float.isNaN(track)) super.removeBearing();
        else super.setBearing(track);
    }

    public boolean hasTrack() {
        return super.hasBearing();
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
        return (isNaN(lat) ? "(-----, -----) " : String.format("(%.5f, %.5f) ", lat, lon)) + String.format("%s, %s, %03.0f, %s", altUnits.toString(getAltitude()), speedUnits.toString(getSpeed()), getTrack(), vertSpeedUnits.toString(vVel));
    }

    public boolean isAirborne() {
        return airborne;
    }
}
