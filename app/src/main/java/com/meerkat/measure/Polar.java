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

import static com.meerkat.Settings.altUnits;
import static com.meerkat.Settings.distanceUnits;

import android.location.Location;

import androidx.annotation.NonNull;

public class Polar {
    public double bearing;
    public final Distance distance;
    public final Height altDifference;

    public Polar() {
        distance = new Distance();
        altDifference = new Height();
    }

    public void set(Location base, Position point) {
        distance.set(base.distanceTo(point) / distanceUnits.factor, distanceUnits);
        bearing = base.bearingTo(point);
        altDifference.set(point.isAirborne() ?(float) (point.getAltitude()-base.getAltitude()) / altUnits.factor : Float.NaN, altUnits);
    }

    public Polar(Distance d, double b, Height a) {
        distance = d;
        bearing = b;
        altDifference = a;
    }

    @NonNull
    public String toString() {
        return String.format("%s @ %s, %s", distance, bearing, altDifference);
    }
}

