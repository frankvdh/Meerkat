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

import static com.meerkat.SettingsActivity.dangerRadiusMetres;
import static com.meerkat.SettingsActivity.displayOrientation;
import static com.meerkat.SettingsActivity.keepScreenOn;
import static com.meerkat.SettingsActivity.screenWidthMetres;
import static com.meerkat.SettingsActivity.screenYPosPercent;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.view.ScaleGestureDetector;

import androidx.annotation.Nullable;

import com.meerkat.Compass;
import com.meerkat.Gps;
import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.log.Log;
import com.meerkat.measure.Position;

public class MapView extends androidx.appcompat.widget.AppCompatImageView {

    public enum DisplayOrientation {NorthUp, TrackUp, HeadingUp}

    public final LayerDrawable layers;
    final float defaultPixelsPerMetre, minPixelsPerMetre;
    float pixelsPerMetre;
    // Used to detect pinch zoom gesture.
    private final ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(getContext(), new PinchListener(this));

    public MapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        Log.d("createView");
        setKeepScreenOn(keepScreenOn);
        // Dispatch activity on touch event to the scale gesture detector.
        OnTouchListener handleTouch = (view, event) -> {
            this.performClick();
            return scaleGestureDetector.onTouchEvent(event);
        };
        setOnTouchListener(handleTouch);
        // Attach a pinch zoom listener to the map view
        defaultPixelsPerMetre = (float) getWidth(getContext()) / screenWidthMetres;
        minPixelsPerMetre = (float) Math.min(getHeight(getContext())*screenYPosPercent/100, getWidth(getContext()) /2) / dangerRadiusMetres;
        pixelsPerMetre = defaultPixelsPerMetre;
        for (var emitterType : Gdl90Message.Emitter.values()) {
            emitterType.bitmap = loadIcon(getContext(), emitterType.iconId);
        }
        layers = new LayerDrawable(new Drawable[]{});
        setImageDrawable(layers);
        Log.d("finished creating");
    }

    //get width screen
    public static int getWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    //get width screen
    public static int getHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    static float displayRotation() {
        if (displayOrientation == DisplayOrientation.HeadingUp) return Compass.degTrue();
        if (displayOrientation == DisplayOrientation.TrackUp) return Gps.getTrack();
        return 0;
    }

    static private Bitmap loadIcon(Context context, int iconId) {
        Icon icon = Icon.createWithResource(context, iconId);
        Drawable drawable = icon.loadDrawable(context);
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            return bitmapDrawable.getBitmap();
        }
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            return ((BitmapDrawable) Icon.createWithResource(context, Gdl90Message.Emitter.Unknown.iconId).loadDrawable(context)).getBitmap();
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    // Avoid constructing
    public Point screenPoint(Position p) {
        var distance = Gps.distanceTo(p);
        var bearing = Gps.bearingTo(p);
        double b = Position.bearingToRad(bearing - displayRotation());
        // new point is relative to (0, 0) of the canvas, which is at the ownShip position
        return new Point((int) (cos(b) * distance * pixelsPerMetre), (int) (-sin(b) * distance * pixelsPerMetre));
    }

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
            pixelsPerMetre *= detector.getScaleFactor() * mapView.getScaleX();
            refresh(null);
            return true;
        }
    }
}
