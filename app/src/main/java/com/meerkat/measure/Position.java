package com.meerkat.measure;

import static android.util.Half.NaN;
import static com.meerkat.Settings.altUnits;
import static com.meerkat.Settings.speedUnits;
import static java.lang.Math.PI;
import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

import android.annotation.SuppressLint;
import android.location.Location;

import androidx.annotation.NonNull;

public class Position extends Location {
    private float vVelFpm; // feet per minute
    private boolean crcValid;
    private boolean airborne;
    private Speed speed;
    private Height alt;

    public Position(String provider, double lat, double lon, Height alt, Speed speed, float track, float vVelFpm, boolean crcValid, boolean airborne, long time) {
        super(provider);
        setTime(time);
        if (!Double.isNaN(lat)) setLatitude(lat);
        if (!Double.isNaN(lon)) setLongitude(lon);
        if (alt != null && !Float.isNaN(alt.value)) setAltitude(alt.value * alt.units.factor);
        if (speed != null && !Float.isNaN(speed.value))
            setSpeed(speed.value * speed.units.factor);
        if (!Float.isNaN(track)) setBearing(track);
        this.vVelFpm = vVelFpm;
        this.crcValid = crcValid;
        this.airborne = airborne;
        this.speed = speed;
        this.alt = alt;
    }

    public Position(String provider, double lat, double lon, Height alt, long time) {
        this(provider, lat, lon, alt, null, Float.NaN, NaN, true, true, time);
    }

    public Position(Location l, Polar p) {
        super(l.getProvider());
        set(l);
        setSpeed(new Speed(l.getSpeed() / speedUnits.factor, speedUnits));
        setAlt(new Height((float) (l.getAltitude() / altUnits.factor), altUnits));
        moveBy(p);
    }

    public Position(Position p) {
        super(p.getProvider());
        set(p);
        setVVel(p.vVelFpm);
        this.crcValid = p.crcValid;
        this.airborne = p.airborne;
        setSpeed(new Speed(p.speed.value, p.speed.units));
        setAlt(new Height(p.alt.value, p.alt.units));
    }

    public void setSpeed(Speed speed) {
        super.setSpeed(speed.value * speed.units.factor);
        this.speed = speed;
    }

    public Speed getSpeedUnits() {
        return speed;
    }

    public Height getAlt() {
        return alt;
    }

    public void setAlt(Height alt) {
        super.setAltitude(alt.value * alt.units.factor);
        this.alt = alt;
    }

    public void setAirborne(boolean airborne) {
        this.airborne = airborne;
    }

    public void setCrcValid(boolean valid) {
        this.crcValid = valid;
    }

    public float getSpeedMps() {
        return super.getSpeed();
    }

    static double latLonDegToRad(double angle) {
        return angle * PI / 180d;
    }

    static double rad2latLonDeg(double rad) {
        return rad * 180d / PI;
    }

    public static double bearingToRad(double b) {
        return ((450 - b) % 360 * PI / 180 + PI) % (PI * 2) - PI;
    }

    public static double radToBearing(double r) {
        return (450 - r * 180 / PI) % 360;
    }

    //        Return new Point given initial coordinates, altitude, speed and track, and vertical speed
    public Position moveBy(Polar p) {
        double lat = getLatitude();
        double lon = getLongitude();
        if (Double.isNaN(lat) || Double.isNaN(lon))
            return this;
        float bearing = (float) p.bearing;
        float distance = p.distance.value * p.distance.units.factor;
        var brngRad = latLonDegToRad(bearing);
        var latRad = latLonDegToRad(lat);
        var lonRad = latLonDegToRad(lon);
        var distFrac = distance / 6371000d; // earth Radius In Metres
        var cosLat = cos(latRad);
        var sinLat = sin(latRad);
        var sinDist = sin(distFrac);
        var cosDist = cos(distFrac);

        var latitudeResult = asin(sinLat * cosDist + cosLat * sinDist * cos(brngRad));
        var a = atan2(sin(brngRad) * sinDist * cosLat, cosDist - sinLat * sin(latitudeResult));
        var longitudeResult = (lonRad + a + 3 * PI) % (2 * PI) - PI;
         setProvider("predicted from " + getProvider());
        setLatitude(rad2latLonDeg(latitudeResult));
        setLongitude(rad2latLonDeg(longitudeResult));
        assert alt.units == p.altDifference.units: "Mismatched alt units: " + alt.units + " vs " + p.altDifference.units;
        setAlt(new Height(alt.value + p.altDifference.value, alt.units));
        return this;
    }


    //        Return new Point given initial coordinates, altitude, speed and track, and vertical speed
    public Position linearPredict(int seconds) {
        Polar p = new Polar(new Distance(getSpeedMps()*seconds, Distance.Units.M), getTrack(), new Height(vVelFpm * seconds/60, Height.Units.FT));
        return new Position(this).moveBy(p);
    }

    static public Height heightAbove(Position p1, Location that) {
        return new Height((float) ((p1.getAltitude() - that.getAltitude()) / p1.alt.units.factor), p1.alt.units);
    }

    public float getTrack() {
        return getBearing();
    }

    public void setTrack(float track) {
        setBearing(track);
    }

    public float getVVel() {
        return vVelFpm;
    }

    public void setVVel(float vVel) {
        this.vVelFpm = vVel;
    }

    public boolean isCrcValid() {
        return crcValid;
    }

    @SuppressLint("DefaultLocale")
    static public String DMS(double deg) {
        if (Double.isNaN(deg)) return "-----";
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
        Height alt = getAlt();
        return (Double.isNaN(lat) ? "(-----, -----) " : String.format("(%.5f, %.5f) ", lat, lon)) + " " + alt.toString() + ", " + speed.toString() + ", " + getTrack() + ", " + vVelFpm;
    }

    public boolean isAirborne() {
        return airborne;
    }

    public boolean isValid() {
        return !Double.isNaN(getLatitude()) && !Double.isNaN(getLongitude());
    }
}
