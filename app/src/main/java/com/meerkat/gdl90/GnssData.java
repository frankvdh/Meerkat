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

import androidx.annotation.NonNull;

import com.meerkat.log.Log;

import java.io.ByteArrayInputStream;
import java.util.Locale;

public class GnssData extends Gdl90Message {
    public enum FixQuality {Unknown, No_Fix, Fix_2D, Fix_3D, Fix_Differential, Fix_RTK}

    final static FixQuality[] fixQualityLookup = new FixQuality[]{FixQuality.Unknown, FixQuality.No_Fix, FixQuality.Fix_2D, FixQuality.Fix_3D, FixQuality.Fix_Differential, FixQuality.Fix_RTK};
    private final byte msgVersion;
    private final long seconds; // since Epoch, UTC
    private final double lat, lon, alt, horizProtectLevel, vertProtectLevel, horizMerit, vertMerit, horizSpeedMerit, vVelMerit, vVel, NSVel, EWVel;
    private final int numSatellites;
    private final FixQuality fixQuality;
    private final boolean hplActive, fault, magNorthRef;

    // uAvionix - uAvionix-UCP-Transponder-ICD-Rev-Q.pdf
    public GnssData(ByteArrayInputStream is) {
        super(is, 44, (byte) 46);
        msgVersion = (byte) getByte();
        int msgSize = msgVersion == 2 ? 48 : 44;
        if (is.available() < msgSize + 1) {
            Log.i("Message too short: expected " + msgSize + " but received " + (is.available() - 1));
            throw new RuntimeException("Message too short: expected " + msgSize + " but received " + (is.available() - 1));
        }
        seconds = getInt() & 0xffff;
        int i = (int) getInt();
        lat = i == Integer.MAX_VALUE ? Double.NaN : i / 1e7;
        i = (int) getInt();
        lon = i == Integer.MAX_VALUE ? Double.NaN : i / 1e7;
        i = (int) getInt();
        alt = i == Integer.MAX_VALUE ? Double.NaN : i / 1e3;
        i = (int) getInt();
        horizProtectLevel = i == Integer.MAX_VALUE ? Double.NaN : i / 1e3;
        i = (int) getInt();
        vertProtectLevel = i == Integer.MAX_VALUE ? Double.NaN : i / 1e3;
        i = (int) getInt();
        horizMerit = i == Integer.MAX_VALUE ? Double.NaN : i / 1e3;
        i = getShort();
        vertMerit = i == Integer.MAX_VALUE ? Double.NaN : i / 1e2;
        i = getShort();
        horizSpeedMerit = i == Integer.MAX_VALUE ? Double.NaN : i / 1e3;
        i = getShort();
        vVelMerit = i == Integer.MAX_VALUE ? Double.NaN : i / 1e3;
        i = getShort();
        vVel = i == Integer.MAX_VALUE ? Double.NaN : i / 1e2;
        if (msgVersion == 1) {
            i = getShort();
            NSVel = i == Integer.MAX_VALUE ? Double.NaN : i / 1e1;
            i = getShort();
            EWVel = i == Integer.MAX_VALUE ? Double.NaN : i / 1e1;
        } else {
            i = (int) getInt();
            NSVel = i == Integer.MAX_VALUE ? Double.NaN : i / 1e3;
            i = (int) getInt();
            EWVel = i == Integer.MAX_VALUE ? Double.NaN : i / 1e3;
        }
        byte p = (byte) getByte();
        fixQuality = p < FixQuality.values().length ? fixQualityLookup[p] : FixQuality.Unknown;
        byte n = (byte) getByte();
        hplActive = (n & 0x01) != 0;
        fault = (n & 0x02) != 0;
        magNorthRef = (n & 0x04) != 0;
        numSatellites = (byte) getByte();
        checkCrc();
    }

    @NonNull
    public String toString() {
        return String.format(Locale.ENGLISH, "C%c: msg v%d %dsecs (%f, %f)@%fft %f %f %f, %f %f, %f %f, %f %f, %c%c%c %d %s",
                crcValidChar(), msgVersion, seconds, lat, lon, alt * 3.28084, vVel, NSVel, EWVel,
                horizProtectLevel, vertProtectLevel, horizMerit, vertMerit, horizSpeedMerit, vVelMerit,
                hplActive ? 'H' : '.',
                fault ? 'F' : '.',
                magNorthRef ? 'M' : '.',
                numSatellites, fixQuality);
    }
}
