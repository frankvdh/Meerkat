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

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class TransponderStatus extends Gdl90Message {
    private final byte msgVersion;
    private boolean airborne, fault, intSinceLast;
    private final boolean tx1090ES, modeSReply, modeCReply, modeAReply, ident;
    private int modeARepliesPerSec;
    private int modeCRepliesPerSec;
    private int modeSRepliesPerSec;
    private final int modeASquawk;
    private int nic;
    private int nac;
    private double lat, lon, alt, speed, vVel;
    private int boardTemp;

    // uAvionix - uAvionix-UCP-Transponder-ICD-Rev-Q.pdf
    public TransponderStatus(ByteArrayInputStream is)  throws UnsupportedEncodingException {
        super(is, 9, (byte) 47);
        msgVersion = (byte) getByte();
        int msgSize = msgVersion == 1 ? 9 : msgVersion == 2 ? 15 : 16;
        if (is.available() < msgSize + 1) {
            throw new  UnsupportedEncodingException ("Message too short: expected " + msgSize + " but received " + (is.available() - 1));
        }
        byte b = (byte) getByte();
        tx1090ES = (b & 0x80) != 0;
        modeSReply = (b & 0x40) != 0;
        modeCReply = (b & 0x20) != 0;
        modeAReply = (b & 0x10) != 0;
        ident = (b & 0x08) != 0;
        if (msgVersion == 1) {
            modeARepliesPerSec = getShort();
            modeCRepliesPerSec = getShort();
            modeSRepliesPerSec = getShort();
            modeASquawk = getShort();
        } else {
            fault = (b & 0x04) != 0;    // version 2,3
            intSinceLast = (b & 0x02) != 0;    // version 2,3
            airborne = (b & 0x01) != 0;    // version 2,3
            lat = get3BytesDegrees();
            lon = get3BytesDegrees();
            long l = getLong();
            alt = ((l >> 20) & 0x7ff) * 25 - 1000;
            speed = ((l >> 8) & 0xfff);
            vVel = (l & 0xff) * 360.0/256;
            modeASquawk = getShort();
            b = (byte) getByte();
            nac = b >> 4;
            nic = b & 0x0f;
            if (msgVersion > 2) {
                boardTemp = getByte();
            }
        }
        checkCrc();
    }

    @NonNull
    public String toString() {
        return String.format(Locale.ENGLISH, "S%c: %d (%f, %f)@%fft %f %f %c%c%c%c%c%c%c%c %04d %d %d %dC %d %d %d",
                crcValidChar(), msgVersion, lat, lon, alt * 3.28084, speed, vVel,
                tx1090ES ? 'T':'.',
                modeSReply ? 'S':'.',
                modeCReply ? 'C':'.',
                modeAReply ? 'A':'.',
                ident ? 'I':'.',
                fault ? 'F':'.',
                intSinceLast ? 'I':'.',
                airborne ? 'A':'.',
                modeASquawk, nac, nic, boardTemp,
                modeARepliesPerSec, modeCRepliesPerSec, modeSRepliesPerSec);
    }
}
