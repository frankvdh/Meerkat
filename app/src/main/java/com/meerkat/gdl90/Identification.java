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
import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class Identification extends Gdl90Message {
    private final byte msgVersion;
    private final byte priFwMajorVersion, priFwMinorVersion, priFwBuildVersion, priHwId;
    private final byte secFwMajorVersion, secFwMinorVersion, secFwBuildVersion, secHwId;
    private final long priSerialNo, secSerialNo;
    private byte priFwId, secFwId;
    private long priFwCrc, secFwCrc;
    private String priFwPartNo, secFwPartNo;

    // uAvionix - uAvionix-UCP-Transponder-ICD-Rev-Q.pdf

    public Identification(ByteArrayInputStream is)  throws UnsupportedEncodingException {
        super(is, 22, (byte) 37);
        msgVersion = (byte) getByte();
        int msgSize = msgVersion == 1 ? 18 : msgVersion == 2 ? 36 : 66;
        if (is.available() < msgSize + 1) {
            throw new UnsupportedEncodingException(String.format("Message too short: expected %d but received %d",msgSize, is.available()-1));
        }
        priFwMajorVersion = (byte) getByte();
        priFwMinorVersion = (byte) getByte();
        priFwBuildVersion = (byte) getByte();
        priHwId = (byte) getByte();
        priSerialNo = getLong();
        secFwMajorVersion = (byte) getByte();
        secFwMinorVersion = (byte) getByte();
        secFwBuildVersion = (byte) getByte();
        secHwId = (byte) getByte();
        secSerialNo = getLong();
        if (msgVersion > 1) {
            priFwId = (byte) getByte();
            priFwCrc = getInt();
            secFwId = (byte) getByte();
            secFwCrc = getInt();
            if (msgVersion > 2) {
                priFwPartNo = getString(15).trim();
                secFwPartNo = getString(15).trim();
            }
        }
        checkCrc();
    }

    @NonNull
    public String toString() {
        return String.format(Locale.ENGLISH, "I%c: msg v%d FW v%d.%d.%d HW %d ser %08x, v%d.%d.%d HW %d ser %08x %s",
                crcValidChar(), msgVersion,
                priFwMajorVersion, priFwMinorVersion, priFwBuildVersion, priHwId, priSerialNo,
                secFwMajorVersion, secFwMinorVersion, secFwBuildVersion, secHwId, secSerialNo,
                msgVersion < 2 ? "" : (String.format(Locale.ENGLISH, "FW %d %04x %d %04x ", priFwId, priFwCrc, secFwId, secFwCrc) +
                        (msgVersion < 3 ? "" : String.format(Locale.ENGLISH, "Part# %s,%s", priFwPartNo, secFwPartNo))));
    }
}
