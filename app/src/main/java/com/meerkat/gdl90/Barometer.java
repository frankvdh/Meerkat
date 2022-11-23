package com.meerkat.gdl90;
import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.util.Locale;

public class Barometer extends Gdl90Message {
    private final byte sensorType;
    private final double pressureAlt; // in metres
    private final double pressMbar;
    private final double sensorTemp;

    public Barometer(ByteArrayInputStream is) {
        super(is, 10, (byte) 40);
        sensorType = (byte) getByte();
        long p = getInt();
        pressMbar = p == 0xffffffff ? Double.NaN : p /100.0;
        p = getInt();
        pressureAlt = p == 0xffffffff ? Double.NaN : p /1000.0;
        p = getShort();
        sensorTemp = p == 0xffff ? Double.NaN : p/100.0;
        checkCrc();
    }

    @NonNull
    public String toString() {
        return String.format(Locale.ENGLISH, "B%c: %d %fmBar %fft %fC",
                crcValidChar(), sensorType, pressMbar, pressureAlt  * 3.28084, sensorTemp);
    }
}
