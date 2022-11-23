package com.meerkat.measure;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

public class Height {
    public enum Units {
        M("m", 1),
        FT("ft", .3048f);

        public final String label;
        public final float factor;

        Units(String label, float factor) {
            this.label = label;
            this.factor = factor;
        }
    }

    public float value;
    public Units units;

    public Height(Float v, Units u) {
        value = v;
        units = u;
    }

    public Height() {}


    public void set(Float v, Units u) {
        value = v;
        units = u;
    }

    @NonNull
    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("%+.0f%s", value, units.label);
    }
}
