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

import static com.meerkat.SettingsActivity.displayOrientation;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class CompassView extends androidx.appcompat.widget.AppCompatImageView {
    public CompassView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.setOnTouchListener(touchListener);
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    final View.OnTouchListener touchListener = (view, motionEvent) -> performClick();

    public boolean performClick() {
        super.performClick();
        if (displayOrientation == MapView.DisplayOrientation.HeadingUp) displayOrientation = MapView.DisplayOrientation.TrackUp;
        else if (displayOrientation == MapView.DisplayOrientation.TrackUp) displayOrientation = MapView.DisplayOrientation.NorthUp;
        else displayOrientation = MapView.DisplayOrientation.HeadingUp;
        MapActivity.mapView.refresh(null);
        return true;
    }
}
