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
package com.meerkat.map;

import android.graphics.Bitmap;

import com.meerkat.R;

public class VehicleIcon {
    public enum Emitter {
        Unknown(R.drawable.ic_ufo, false),
        Light(R.drawable.ic_plane, true),
        Small(R.drawable.ic_dash8, true),
        Large(R.drawable.ic_737, true),
        VLarge(R.drawable.ic_787, true),
        Heavy(R.drawable.ic_c17, true),
        Aerobatic(R.drawable.ic_plane, true),
        Rotor(R.drawable.ic_helicopter, true),
        Unused(R.drawable.ic_ufo, false),
        Glider(R.drawable.ic_glider, true),
        Balloon(R.drawable.ic_balloon, false),
        Skydiver(R.drawable.ic_parachute, false),
        Ultralight(R.drawable.ic_plane, true),
        UAV(R.drawable.ic_uav, true),
        Spacecraft(R.drawable.ic_rocket, false),
        Emergency_Vehicle(R.drawable.ic_ambulance, true),
        Service_Vehicle(R.drawable.ic_pickup, true),
        Point_Obstacle(R.drawable.ic_crisis, false),
        Cluster_Obstacle(R.drawable.ic_crisis, false),
        Line_Obstacle(R.drawable.ic_crisis, false);
        final public int iconId;
        final public boolean canRotate;
        public Bitmap bitmap;

        Emitter(int iconId, boolean canRotate) {
            this.iconId = iconId;
            this.canRotate = canRotate;
        }
    }
}
