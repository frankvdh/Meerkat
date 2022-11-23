package com.meerkat.gdl90;
import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.util.Locale;

public class Heartbeat extends Gdl90Message {
    private final byte status1;
    private final boolean validPos;
    private final  boolean maintReq;
    private final boolean ident;
    private final boolean addrType;
    private final boolean lowBatt;
    private final boolean ratcs;
    private final boolean reserved;
    private final boolean init;
    private final byte status2;
    private final boolean timestampMS;
    private final boolean csaReq;
    private final boolean csaNA;
    private final boolean reserved4, reserved3, reserved2, reserved1;
    private final boolean utcOk;
    private final int hour, minute, second;
    private final int uplinkMessages;
    private final int reservedCounts;
    private final int basicLongMessageCounts;

    // uAvionix - uAvionix-UCP-Transponder-ICD-Rev-Q.pdf

    public Heartbeat(ByteArrayInputStream is) {
        super(is, 7, (byte) 0);
        status1 = (byte) getByte();
        validPos = (status1 & 0x80) != 0;
        maintReq = (status1 & 0x40) != 0;
        ident = (status1 & 0x20) != 0;
        addrType = (status1 & 0x10) != 0;
        lowBatt = (status1 & 0x08) != 0;
        ratcs = (status1 & 0x04) != 0;
        reserved = (status1 & 0x02) != 0;
        init = (status1 & 0x01) != 0;
        status2 = (byte) getByte();
        timestampMS = (status2 & 0x80) != 0;
        csaReq = (status2 & 0x40) != 0;
        csaNA = (status2 & 0x20) != 0;
        reserved4 = (status2 & 0x10) != 0;
        reserved3 = (status2 & 0x08) != 0;
        reserved2 = (status2 & 0x04) != 0;
        reserved1 = (status2 & 0x02) != 0;
        utcOk = (status2 & 0x01) != 0;
        int timestamp = getByte() + (getByte() << 8) + ((status2 & 0x80) << 9);
        hour = timestamp / 3600;
        minute = (timestamp % 3600) / 60;
        second = timestamp % 60;
        byte message5 = (byte) getByte();
        uplinkMessages = message5 >> 3;
        reservedCounts = (message5 & 0x04) >> 2;
        basicLongMessageCounts = (message5 & 0x03) << 8 + getByte();
        checkCrc();
    }

    @NonNull
    public String toString() {
        return String.format(Locale.ENGLISH, "H%c: %02x %c%c%c%c%c%c%c%c %02x %c%c%c%c%c%c%c%c %02d:%02d:%02d %d %d %d",
                crcValidChar(),
                status1,
                validPos ? 'V' : '.', maintReq ? 'M' : '.', ident ? 'I' : '.', addrType ? 'T' : '.',
                lowBatt ? 'B' : '.', ratcs ? 'A' : '.', reserved ? 'R' : '.', init ? 'I' : '.',
                status2,
                timestampMS ? 'T' : '.', csaReq ? 'C' : '.', csaNA ? 'N' : '.', reserved4 ? 'R' : '.',
                reserved3 ? 'R' : '.', reserved2 ? 'R' : '.', reserved1 ? 'R' : '.', utcOk ? 'U' : '.',
                hour, minute, second, uplinkMessages, reservedCounts, basicLongMessageCounts);
    }
}
