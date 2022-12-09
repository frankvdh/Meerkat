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
import static com.meerkat.SettingsActivity.keepScreenOn;
import static com.meerkat.SettingsActivity.screenWidth;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

import com.meerkat.Compass;
import com.meerkat.Gps;
import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.log.Log;
import com.meerkat.measure.Polar;
import com.meerkat.measure.Position;

public class MapView extends androidx.appcompat.widget.AppCompatImageView {

    public enum DisplayOrientation {NorthUp, TrackUp, HeadingUp}

    public LayerDrawable layers;
    final float defaultScaleFactor;
    float scaleFactor;
    // Used to detect pinch zoom gesture.
    private ScaleGestureDetector scaleGestureDetector;

    public MapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        Log.d("createView");
        setKeepScreenOn(keepScreenOn);
        defaultScaleFactor = getWidth(getContext()) / screenWidth;
        for (var emitterType : Gdl90Message.Emitter.values()) {
            emitterType.bitmap = AircraftLayer.loadIcon(getContext(), emitterType.iconId);
        }

        // Attach a pinch zoom listener to the map view
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new PinchListener(this));
        Log.d("finished creating");
        setOnTouchListener(touchListener);
    }

    //get width screen
    public static int getWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    static float displayRotation() {
        if (displayOrientation == DisplayOrientation.HeadingUp) return Compass.degTrue();
        if (displayOrientation == DisplayOrientation.TrackUp) return Gps.getTrack();
        return 0;
    }


    public Point screenPoint(Position p) {
        final Polar spPolar = new Polar();
        Gps.getPolar(p, spPolar);
        double b = Position.bearingToRad(spPolar.bearing - displayRotation());
        // new point is relative to (0, 0) of the canvas, which is at the ownShip position
        return new Point((int) (cos(b) * spPolar.distance.value * scaleFactor), (int) (-sin(b) * spPolar.distance.value * scaleFactor));
    }

    private final View.OnTouchListener handleTouch = (view, event) -> {
        this.performClick();
        // Dispatch activity on touch event to the scale gesture detector.
        return scaleGestureDetector.onTouchEvent(event);
    };

    public void refresh(AircraftLayer layer) {
        if (layer == null)
            layers.invalidateSelf();
        else
            layers.invalidateDrawable(layer);
    }

    /* This listener is used to listen pinch zoom gesture. */
    private class PinchListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        private final MapView mapView;

        // The default constructor pass context and imageview object.
        public PinchListener(MapView mapView) {
            this.mapView = mapView;
        }

        // When pinch zoom gesture occurred.
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            // Scale the image with pinch zoom value.
            scaleFactor *= detector.getScaleFactor() * mapView.getScaleX();
            refresh(null);
            return true;
        }
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    final View.OnTouchListener touchListener = (view, motionEvent) -> performClick();

    public boolean performClick() {
        super.performClick();
        if (displayOrientation == DisplayOrientation.HeadingUp)
            displayOrientation = DisplayOrientation.TrackUp;
        else if (displayOrientation == DisplayOrientation.TrackUp)
            displayOrientation = DisplayOrientation.NorthUp;
        else displayOrientation = DisplayOrientation.HeadingUp;
        refresh(null);
        return true;
    }
}
