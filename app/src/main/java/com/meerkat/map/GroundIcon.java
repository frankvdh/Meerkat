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

public class GroundIcon {
/* 7. Waypoint styleNum as at XCSoar v7.8
     1 - Normal (Light crosshairs)
     2 - AirfieldGrass (Pink circle)
     3 - Outlanding (Pink diamond)
     4 - GliderSite (Pink circle)
     5 - AirfieldSolid (Pink circle)
     6 - MtPass (No longer a Black circle... road through mountains)
     7 - MtTop (No longer a Black triangle... mountain peak)
     8 - Transmitter (Red emitting tower)
     9 - Vor (light blue hex in square)
     10 - Ndb (light blue fuzzy circle)
     11 - CoolTower (Black Castle tower)
     12 - Dam (black wall with water)
     13 - Tunnel (Black arch over road)
     14 - Bridge (Black bridge)
     15 - PowerPlant (Black factory)
     16 - Castle (Black castle 2 towers)
     17 - Intersection (Black cross outline)
     18 - Light blue flag
     19 - Light blue triangle in circle
	 20 - green hanglider launch
	 21 - same as 1
	 22 - green hanglider
	 23 - same as 1
		 */

    public enum Icons {
        Unknown(R.drawable.ic_questionmark),
        Normal(R.drawable.ic_add),
        AirfieldGrass(R.drawable.ic_plane),
        Outlanding(R.drawable.ic_dangerous),
        GliderSite(R.drawable.ic_glider),
        AirfieldSolid(R.drawable.ic_dash8),
        MtPass(R.drawable.ic_doubledown),
        MtTop(R.drawable.ic_arrowup),
        Transmitter(R.drawable.ic_celltower),
        Vor(R.drawable.ic_hexagon),
        Ndb(R.drawable.ic_circle),
        CoolTower(R.drawable.ic_apartment),
        Dam(R.drawable.ic_bakery),
        Tunnel(R.drawable.ic_pokemon),
        Bridge(R.drawable.ic_nat),
        PowerPlant(R.drawable.ic_factory),
        Castle(R.drawable.ic_castle),
        Intersection(R.drawable.ic_games),
        Flag(R.drawable.ic_flag),
        Triangle(R.drawable.ic_arrowdropdown),
        HangGliderLaunch(R.drawable.ic_paragliding),
        Normal1(R.drawable.ic_add),
        HangGlider(R.drawable.ic_parachute),
        Normal2(R.drawable.ic_add);
        final public int iconId;
        public Bitmap bitmap;

        Icons(int iconId) {
            this.iconId = iconId;
        }
    }
}
