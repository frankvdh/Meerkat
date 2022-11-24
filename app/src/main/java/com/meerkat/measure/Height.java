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
 */package com.meerkat.measure;

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
