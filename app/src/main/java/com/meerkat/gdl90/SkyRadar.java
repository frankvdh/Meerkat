package com.meerkat.gdl90;

import androidx.annotation.NonNull;

import com.meerkat.log.Log;

import java.io.ByteArrayInputStream;
import java.util.Locale;

public class SkyRadar extends Gdl90Message {
    private final byte fwVersion, debugData, hours, minutes;
    private final char fixQuality;
    private final long numMessages;
    private final int debugData2;
    private byte hwVersion, hwStatus;
    private int reserved;
    private long recvHwId;
    private double hdop;

    public SkyRadar(ByteArrayInputStream is) {
        super(is, 9, (byte) 101);
        fwVersion = (byte) getByte();
        int msgSize = fwVersion < 42 ? 9 : fwVersion < 45 ? 11 : 20;
        if (is.available() < msgSize + 1) {
            Log.i("Message too short: expected " + msgSize + " but received " + (is.available() - 1));
            throw new RuntimeException("Message too short: expected " + msgSize + " but received " + (is.available() - 1));
        }
        debugData = (byte) getByte();
        fixQuality = getChar();
        numMessages = getByte() + (getByte() << 8) + (getByte() << 16); // 3 bytes LSB first
        hours = (byte) getByte();
        minutes = (byte) getByte();
        debugData2 = getShort();
        if (fwVersion >= 42) {
            hwVersion = (byte) getByte();
            if (fwVersion >= 45) {
                int i = getShort();
                hdop = i == 5000 ? Double.NaN : i / 10.0;
                reserved = getShort();
                recvHwId = getLong();
                hwStatus = (byte) getByte();
            }
        }
        checkCrc();
    }

    @NonNull
    public String toString() {
        return String.format(Locale.ENGLISH, "I%c: %d %02x %c %d %02d:%02d %02x %s",
                crcValidChar(), fwVersion, debugData, fixQuality, numMessages, hours, minutes, debugData2,
                fwVersion < 42 ? "" : String.format(Locale.ENGLISH, " %d %s", hwVersion,
                fwVersion < 45 ? "" : String.format(Locale.ENGLISH, " %f %04x %08x %02x", hdop, reserved , recvHwId, hwStatus)));
    }
}
