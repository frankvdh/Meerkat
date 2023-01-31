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
package com.meerkat.gdl90;

import static com.meerkat.SettingsActivity.ownCallsign;
import static java.lang.Float.NaN;

import android.hardware.GeomagneticField;

import androidx.annotation.NonNull;

import com.meerkat.VehicleList;
import com.meerkat.log.Log;
import com.meerkat.measure.Position;
import com.meerkat.measure.Units;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.Locale;

public class Traffic extends Gdl90Message {
    public final Emitter emitterType;
    private final boolean ownShip;
    private final int alertStatus;
    private final AddrType addrType;
    public final int participantAddr;
    public final Position point;
    private final boolean extrapolated;
    final int nic;
    final int nac;
    public final String callsign;
    final Priority priority;
    public final boolean airborne;

    public enum TrackType {Invalid, TRK, Mag, True}

    private enum AddrType {ICAO_ADSB, self_ADSB, ICAO_TISB, file_TISB, SFC_Vehicle, GND_Beacon, Reserved}

    final static private AddrType[] AddrTypeLookup = new AddrType[]{AddrType.ICAO_ADSB, AddrType.self_ADSB, AddrType.ICAO_TISB, AddrType.file_TISB, AddrType.SFC_Vehicle, AddrType.GND_Beacon, AddrType.Reserved};
    final static private TrackType[] trackTypeLookup = new TrackType[]{TrackType.Invalid, TrackType.TRK, TrackType.Mag, TrackType.True};

    // uAvionix - uAvionix-UCP-Transponder-ICD-Rev-Q.pdf 6.21 (Ownship) & 6.2.

    public Traffic(byte messageId, Position point, ByteArrayInputStream is) throws UnsupportedEncodingException {
        super(is, 28, messageId);
        this.point = point;
        ownShip = messageId == 10;
        short b = getByte();
        alertStatus = b >>> 4;
        b &= 0x0f;
        int addrTypeNum = b < AddrType.values().length ? b : AddrType.values().length - 1;
        addrType = AddrTypeLookup[addrTypeNum];
        participantAddr = (addrTypeNum << 24) + (getByte() << 16) + (getByte() << 8) + getByte();
        double lat = get3BytesDegrees();
        double lon = get3BytesDegrees();

        int alt = getByte() << 4;
        b = getByte();
        alt += (b & 0xf0) >> 4; // MSB first
        // The 0xFFF value represents that the pressure altitude is invalid.
        if (alt == 0xfff) alt = -100000;
        else alt = alt * 25 - 1000;

        // Misc bitmap
        TrackType trackType = trackTypeLookup[b & 0x03];
        extrapolated = (b & 0x04) != 0;
        airborne = (b & 0x08) != 0;

        b = getByte();
        nic = b >> 4;
        nac = b & 0x0f;
        // A target with no valid position has Latitude, Longitude, and NIC all set to zero.
        if (nic == 0 && lat == 0 && lon == 0) {
            lat = Double.NaN;
            lon = Double.NaN;
        }
        int hSpeed = getByte() << 4; // MSB first
        b = getByte();
        hSpeed += ((b & 0xf0) >> 4);
        int vVel = ((b & 0x0f) << 8) + getByte(); // MSB first, signed
        if ((vVel & 0x800) != 0) {
            vVel -= 0x1000;
        }
        vVel *= 64;

        float track = getByte() * 360.0f / 256;
        emitterType = emitterLookup[getByte()];
        callsign = getString(8).trim();
        b = getByte();
        int p = b >> 4;
        priority = priorityLookup[p < Priority.values().length ? p : Priority.values().length - 1];
        checkCrc();
        point.setProvider("ADS-B");
        point.setLatitude(lat);
        point.setLongitude(lon);
        if (Double.isNaN(lat) || Double.isNaN(lon))
            point.removeAccuracy();
        else
            point.setAccuracy(20);
        if (alt < -1000) point.removeAltitude();
        else point.setAltitude(Units.Height.FT.toM(alt));
        if (hSpeed == 0xfff) {
            point.removeSpeed();
            point.removeBearing();
        } else {
            point.setSpeed((float) Units.Speed.KNOTS.toMps(hSpeed));
            point.setTrack(trueTrack(track, trackType, lat, lon, alt));
        }
        point.setVVel(Units.VertSpeed.FPM.toMps(vVel));
        point.setCrcValid(crcValid);
        point.setAirborne(airborne);
        Log.v(point.toString());
    }

    public Traffic(Instant time, int id, String callsign, String type, double lat, double lng, double alt, double speed, float track, float vVel) {
        this.participantAddr = id;
        this.callsign = callsign;
        emitterType = Emitter.valueOf(type);

        point = new Position("ADS-B", lat, lng, alt < -1000 ? Float.NaN : Units.Height.FT.toM(alt),
                (float) Units.Speed.KNOTS.toMps(speed), track,
                (float) Units.VertSpeed.FPM.toMps(vVel), true, true, time);
        ownShip = callsign.equals(ownCallsign);
        nac = 100;
        nic = 100;
        priority = Priority.Normal;
        alertStatus = 0;
        extrapolated = false;
        addrType = AddrType.ICAO_ADSB;
        airborne = true;
    }

    private float trueTrack(float track, TrackType trackType, double lat, double lon, int alt) {
        return switch (trackType) {
            // Heading rather than track
            case True -> track;
            case Mag ->
                    (track + new GeomagneticField((float) lat, (float) lon, alt, Instant.now().toEpochMilli()).getDeclination()) % 360;
            // Track
            case TRK -> track;
            case Invalid -> NaN;
        };
    }

    public void upsert(VehicleList vehicleList, Instant time) {
        vehicleList.upsert(crc, callsign, participantAddr, point, emitterType, time);
    }

    @NonNull
    public String toString() {
        return String.format(Locale.ENGLISH, "%c%c: %8s %s %s %s NIC=%2d NAC=%2d %s %s %o",
                ownShip ? 'O' : 'T', crcValidChar(),
                callsign, point,
                priority, (alertStatus == 0 ? "No alert" : "Traffic Alert"), nic, nac, (extrapolated ? "Extrap" : "Report"),
                addrType, participantAddr);
    }
}
