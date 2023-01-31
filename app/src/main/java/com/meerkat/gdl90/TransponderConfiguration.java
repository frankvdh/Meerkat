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

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import com.meerkat.log.Log;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class TransponderConfiguration extends Gdl90Message {
    private final byte msgVersion;
    private final int sil, sda, maxSpeed;
    private final boolean extBarometer, testMode;
    private final int participantAddr;
    private final boolean tx1090ES, modeSReply, modeCReply, modeAReply;
    private final AdsbIn adsbIn;
    private final AircraftLengthWeight avLw;
    private final LateralGpsOfs antOfslat;
    private final int antOfsLon;
    private final String callsign;
    private final float stallSpeed;
    private final Emitter emitterType;
    private final int baudRate;
    private long validityMask;
    private boolean baro100ftResolution;
    private int modeASquawk;

    final static int[] maxSpeedLookup = {0, 75, 150, 300, 600, 1200, 10000};
    final static int[] baudrateLookup = {1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200, 921600};

    public enum AdsbIn {None, MHz1090, MHz978, Both}

    public final AdsbIn[] adsbInLookup = {AdsbIn.None, AdsbIn.MHz1090, AdsbIn.MHz978, AdsbIn.Both};

    public enum Protocol {UCP, UCP_HD, Apollo, Mavlink}

    public ArrayList<Protocol> inputProtocol, outputProtocol;

    // uAvionix - uAvionix-UCP-Transponder-ICD-Rev-Q.pdf
    public TransponderConfiguration(ByteArrayInputStream is)  throws UnsupportedEncodingException {
        super(is, 19, (byte) 43);
        msgVersion = (byte) getByte();
        //noinspection ConditionalExpressionWithIdenticalBranches
        int msgSize = msgVersion == 1 ? 19 : msgVersion == 2 ? 21 : msgVersion == 3 ? 22 : msgVersion == 4 ? 29 : 29;
        if (is.available() < msgSize + 1) {
              throw new UnsupportedEncodingException ("Message too short: expected " + msgSize + " but received " + (is.available() - 1));
        }
        participantAddr = (getByte() << 16) + (getByte() << 8) + getByte();
        byte b = (byte) getByte();
        sil = (b >> 6) & 0x03;
        sda = (b >> 4) & 0x03;
        extBarometer = (b & 0x08) != 0;
        maxSpeed = maxSpeedLookup[(b & 0x07)];
        b = (byte) getByte();
        testMode = ((b >> 6) & 0x03) != 0;
        adsbIn = adsbInLookup[(b >> 4) & 0x03];
        avLw = AircraftLengthWeightLookup[b & 0x0f];
        b = (byte) getByte();
        antOfslat = lateralGpsOfsLookup[b >> 5];
        antOfsLon = b & 0x1f;
        callsign = getString(8).trim();
        stallSpeed = getByte() / 100f;
        emitterType = emitterLookup[(byte) getByte()];
        b = (byte) getByte();
        tx1090ES = (b & 0x80) != 0;
        modeSReply = (b & 0x40) != 0;
        modeCReply = (b & 0x20) != 0;
        modeAReply = (b & 0x10) != 0;
        baudRate = baudrateLookup[b & 0x0f];
        if (msgVersion > 1) {
            modeASquawk = getShort();
            if (msgVersion > 2) {
                validityMask = getInt();
                if (msgVersion > 3) {
                    baro100ftResolution = (getByte() & 0x80) != 0;
                    inputProtocol = protocols(getShort());
                    outputProtocol = protocols(getShort());
                    //noinspection StatementWithEmptyBody
                    if (msgVersion > 4) {
                        // 5 is the same as 4
                    }
                }
            }
        }
        checkCrc();
    }

    private ArrayList<Protocol> protocols(int b) {
        ArrayList<Protocol> result = new ArrayList<>();
        if ((b & 0x02) != 0) result.add(Protocol.UCP);
        if ((b & 0x0400) != 0) result.add(Protocol.UCP_HD);
        if ((b & 0x0200) != 0) result.add(Protocol.Apollo);
        if ((b & 0x01) != 0) result.add(Protocol.Mavlink);
        return result;
    }

    @SuppressLint("DefaultLocale")
    @NonNull
    public String toString() {
        return String.format("C%c: %d %d %d %d %06x %s %c%c%c%c%c%c %d %s %.0fkts %s %s %s %s %s",
                crcValidChar(), msgVersion, sil, sda, maxSpeed,
                participantAddr, callsign,
                tx1090ES ? 'T' : '.',
                modeSReply ? 'S' : '.',
                modeCReply ? 'C' : '.',
                modeAReply ? 'A' : '.',
                testMode ? 'T' : '.',
                extBarometer ? 'B' : '.',
                baudRate,
                emitterType, stallSpeed,
                avLw, antOfslat, antOfsLon, adsbIn,
                msgVersion < 2 ? "" : String.format("%04d %s", modeASquawk,
                        msgVersion < 3 ? "" : String.format("%08x %s %s", validityMask, baro100ftResolution ? "100ft" : "25ft",
                                msgVersion < 4 ? "" : "" + inputProtocol.size() + ", " + outputProtocol.size())));
    }
}
