package com.meerkat.measure;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

public class Speed {
    public enum Units {
        MPS("mps", 1),
        KPH("kph", 1000f/3600),
        KNOTS("kts", 0.5144444f);

        public final String label;
        public final float factor;

        Units(String label, float factor) {
            this.label = label;
            this.factor = factor;
        }
    }

    public float value;
    public Units units;

    public Speed(Float v, Units u) {
        value = v;
        units = u;
    }

    @NonNull
    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("%.0f%s", value, units.label);
    }
}
