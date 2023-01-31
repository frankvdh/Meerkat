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

public class OwnShipGeometricAltitude extends Gdl90Message {
    private final boolean warning;
    private final int alt;
    private final int vfom;

    // uAvionix - uAvionix-UCP-Transponder-ICD-Rev-Q.pdf
    public OwnShipGeometricAltitude(ByteArrayInputStream is)  throws UnsupportedEncodingException {
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
