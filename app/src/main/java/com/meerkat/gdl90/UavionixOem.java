package com.meerkat.gdl90;

import androidx.annotation.NonNull;

import com.meerkat.log.Log;

import java.io.ByteArrayInputStream;
import java.util.Locale;

public class UavionixOem extends Gdl90Message {
    private final char signature;
    private final int subType;
    private byte msgVersion;
    private double lowTrimSlope, lowTrimYIntercept, highTrimSlope, highTrimYIntercept, lowPointAlt, midPointAlt, highPointAlt;
    private int icao;
    private Emitter emitterType;
    private String callsign;
    private float stallSpeed;
    private AircraftLengthWeight avLw;
    private LateralGpsOfs antOfslat;
    private int antOfsLon;
    private int baudRate, numHops;
    private boolean qiMode;

    // uAvionix - uAvionix-UCP-Transponder-ICD-Rev-Q.pdf

    public UavionixOem(ByteArrayInputStream is) {
        super(is, 4, (byte) 117);
        signature = Character.highSurrogate(getByte());
        subType = getByte();
        switch (subType) {
            case 38:
                // QI Mode
                msgVersion = (byte) getByte();
                int msgSize = msgVersion == 1 ? 4 : 29;
                if (is.available() < msgSize + 1) {
                    Log.i("Message too short: expected " + msgSize + " but received " + (is.available() - 1));
                    throw new RuntimeException("Message too short: expected " + msgSize + " but received " + (is.available() - 1));
                }
                qiMode = getByte() == 0;
                if (msgVersion > 1) {
                    int i = (int) getInt();
                    lowTrimSlope = i == 0x7fffffff ? Double.NaN : i / 1e4;
                    i = (int) getInt();
                    lowTrimYIntercept = i == 0x7fffffff ? Double.NaN : i;
                    i = (int) getInt();
                    highTrimSlope = i == 0x7fffffff ? Double.NaN : i / 1e4;
                    i = (int) getInt();
                    highTrimYIntercept = i == 0x7fffffff ? Double.NaN : i;
                    i = (int) getInt();
                    lowPointAlt = i == 0x7fffffff ? Double.NaN : i / 1e3;
                    i = (int) getInt();
                    midPointAlt = i == 0x7fffffff ? Double.NaN : i / 1e3;
                    i = (int) getInt();
                    highPointAlt = i == 0x7fffffff ? Double.NaN : i / 1e3;
                }
                break;
            case 0xfe:
                // System Command - Enter Update Mode
                msgVersion = (byte) getByte();
                msgSize = 8;
                if (is.available() < msgSize + 1) {
                    Log.i("Message too short: expected " + msgSize + " but received " + (is.available() - 1));
                    throw new RuntimeException("Message too short: expected " + msgSize + " but received " + (is.available() - 1));
                }
                baudRate = (int) getInt();
                numHops = getByte();
            default:
                icao = (getByte() << 16) + (getByte() << 8) + getByte();
                emitterType = emitterLookup[(byte) getByte()];
                StringBuilder sb = new StringBuilder();
                for (int i = 8; i < 16; i++)
                    sb.append((char) getByte());
                callsign = sb.toString().trim();
                stallSpeed = getByte()/100f;
                avLw = AircraftLengthWeightLookup[getByte()];
                byte b = (byte) getByte();
                antOfslat = lateralGpsOfsLookup[b >> 5];
                antOfsLon = b & 0x1f;
        }
        checkCrc();
    }

    @NonNull
    public String toString() {
        String antOffsetLon = antOfsLon == 0 ? "NO_DATA" : antOfsLon == 1 ? "Applied by sensor" :
                String.format(Locale.ENGLISH, "%dm", antOfsLon * 2 - 1);
        return String.format(Locale.ENGLISH, "I%c: %c %c %d %o %s %s %.0f %s %s %s %c",
                crcValidChar(),
                signature, subType, msgVersion, icao, emitterType, callsign, stallSpeed,
                avLw, antOfslat, antOffsetLon, qiMode);
    }
}
