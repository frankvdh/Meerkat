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
package com.meerkat.measure;

import static java.lang.Double.isNaN;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

public class Units {
    private final String label;
    private final float factor;

    private Units(String label, float factor) {
        this.label = label;
        this.factor = factor;
    }

    @NonNull
    @SuppressLint("DefaultLocale")
    private String toString(double value) {
        if (isNaN(value)) return "----";
        value /= factor;
        return String.format(value < 10 ? "%.1f%s" : "%.0f%s", value, label);
    }

    @SuppressWarnings("SameParameterValue")
    private String toString(String format, double value) {
        if (isNaN(value)) return "----";
        value /= factor;
        return String.format(format, value, label);
    }

    private double toStandard(double value) {
        if (isNaN(value)) return Double.NaN;
        return value * factor;
    }

    @SuppressWarnings("unused")
    public enum Speed {
        MPS("mps", 1),
        KPH("kph", 1000f / 3600),
        KNOTS("kts", 0.5144444f);

        public final Units units;

        Speed(String label, float factor) {
            this.units = new Units(label, factor);
        }

        public String toString(float value) {
            return units.toString(value);
        }

        public double toMps(double value) {
            return units.toStandard(value);
        }
    }

    @SuppressWarnings("unused")
    public enum VertSpeed {
        MPS("mps", 1),
        FPM("fpm", 0.00508f);

        public final Units units;

        VertSpeed(String label, float factor) {
            this.units = new Units(label, factor);
        }

        public String toString(double value) {
            return units.toString(value);
        }

        public double toMps(float value) {
            return units.toStandard(value);
        }
    }

    @SuppressWarnings("unused")
    public enum Height {
        M("m", 1),
        FT("ft", .3048f);

        public final Units units;

        Height(String label, float factor) {
            this.units = new Units(label, factor);
        }

        public double toM(double value) {
            return units.toStandard(value);
        }

        public String toString(double value) {
            return units.toString("+%.0f%s", value);
        }
    }

    @SuppressWarnings("unused")
    public enum Distance {
        M("m", 1),
        KM("km", 1000f),
        NM("nm", 1852f);

        public final Units units;

        Distance(String label, float factor) {
            this.units = new Units(label, factor);
        }

        public double toM(float value) {
            return units.toStandard(value);
        }

        public String toString(double value) {
            return units.toString(value);
        }
    }

}

