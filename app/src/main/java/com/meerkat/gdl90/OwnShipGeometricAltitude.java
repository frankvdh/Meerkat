package com.meerkat.gdl90;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.util.Locale;

public class OwnShipGeometricAltitude extends Gdl90Message {
    private final boolean warning;
    private final int alt;
    private final int vfom;

    // uAvionix - uAvionix-UCP-Transponder-ICD-Rev-Q.pdf
    public OwnShipGeometricAltitude(ByteArrayInputStream is) {
        super(is, 4, (byte) 11);
        alt = getShort();
        byte message3 = (byte) getByte();
        warning = (message3 & 0x80) != 0;
        vfom = (message3 & 0x7f) << 8 + getByte();
        checkCrc();
    }

    @NonNull
    public String toString() {
        return String.format(Locale.ENGLISH, "A%c: %d %c %s",
                crcValidChar(), alt, warning ? 'W' : '.',
                vfom == 0x7fff ? "Unknown" : ("" + vfom));
    }
}
