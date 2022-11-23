package com.meerkat.gdl90;
import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Locale;

public class Invalid extends Gdl90Message {
    ArrayList<Byte> data;

    public Invalid(byte messageId, ByteArrayInputStream is) {
        super(is, 0, messageId);
        data = new ArrayList<>();
        do {
            byte b = (byte) is.read();
            if ((b & 0x7f) == 0x7e) break;
            data.add(b);
        } while (is.available() > 0);
    }

    @NonNull
    public String toString() {
        StringBuilder sb = new StringBuilder(String.format(Locale.ENGLISH, "%3d: ", messageId));
        for (Byte b: data)
            sb.append(String.format("%02x", b));
       return sb.toString();

    }
}
