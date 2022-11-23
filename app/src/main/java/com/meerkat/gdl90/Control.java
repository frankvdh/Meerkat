package com.meerkat.gdl90;
import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.util.Locale;

public class Control extends Gdl90Message {
    private final byte msgVersion;
    private boolean airborne;
    private final boolean tx1090ES, modeSReply, modeCReply, modeAReply, ident, baroChecked;
    private final int pressureAlt; // in metres
    private final int modeASquawk; // decimal -- 1200 = 0x4b0
    Priority priority;
    final private String callsign;

    // uAvionix - uAvionix-UCP-Transponder-ICD-Rev-Q.pdf
    public Control(ByteArrayInputStream is) {
        super(is, 10, (byte) 45);
        msgVersion = (byte) getByte();
        byte b = (byte) getByte();
        tx1090ES = (b & 0x80) != 0;
        modeSReply = (b & 0x40) != 0;
        modeCReply = (b & 0x20) != 0;
        modeAReply = (b & 0x10) != 0;
        ident = (b & 0x08) != 0;
        byte airGroundState = (byte) ((b & 0x6) >> 1);
        switch (airGroundState) {
            case 0:
            case 1: airborne = true; break;
            case 2: airborne = false;
        }
        baroChecked = (b & 0x01) != 0;
        pressureAlt = (int) getInt();
        modeASquawk = getShort();
        byte p = (byte) getByte();
        priority = priorityLookup[p < Priority.values().length ? p : Priority.values().length - 1];
        callsign = getString(8).trim();
        checkCrc();
    }

    @NonNull
    public String toString() {
        return String.format(Locale.ENGLISH, "C%c: %s %d %c%c%c%c%c%c%c %.0fft %04d %s",
                crcValidChar(), callsign, msgVersion,
                tx1090ES ? 'T':'.',
                modeSReply ? 'S':'.',
                modeCReply ? 'C':'.',
                modeAReply ? 'A':'.',
                ident ? 'I':'.',
                airborne ? 'A':'.',
                baroChecked ? 'B':'.',
                pressureAlt  * 3.28084,
                modeASquawk, priority
                );
    }
}
