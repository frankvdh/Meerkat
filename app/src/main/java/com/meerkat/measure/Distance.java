package com.meerkat.measure;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

public class Distance {
    public enum Units {
        M("m", 1),
        KM("km", 1000f),
        NM("nm", 1852f);

        public final String label;
        public final float factor;

        Units(String label, float factor) {
            this.label = label;
            this.factor = factor;
        }
    }

    public float value;
    public Units units;

    public Distance(Float v, Units u) {
        value = v;
        units = u;
    }

    public Distance() {
    }


    public void set(Float v, Units u) {
        value = v;
        units = u;
    }
    @NonNull
    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format(value < 10 ? "%.1f%s":"%.0f%s", value, units.label);
    }
}
