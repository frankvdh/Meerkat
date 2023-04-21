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
import static com.meerkat.SettingsActivity.maxZoom;
import static com.meerkat.SettingsActivity.minZoom;
import static com.meerkat.SettingsActivity.screenWidthMetres;
import static com.meerkat.SettingsActivity.screenYPosPercent;
import static com.meerkat.SettingsActivity.showLinearPredictionTrack;
import static com.meerkat.SettingsActivity.showPolynomialPredictionTrack;
import static java.lang.Math.cos;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.view.ScaleGestureDetector;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.meerkat.Compass;
import com.meerkat.Gps;
import com.meerkat.Vehicle;
import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.log.Log;
import com.meerkat.measure.Position;

import java.time.Instant;

public class MapView extends androidx.appcompat.widget.AppCompatImageView {

    public enum DisplayOrientation {NorthUp, TrackUp, HeadingUp}

    public final LayerDrawable layers;
    final float defaultPixelsPerMetre, minPixelsPerMetre, maxPixelsPerMetre;
    float pixelsPerMetre;
    // Used to detect pinch zoom gesture.
    private final ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(getContext(), new PinchListener());

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
        maxPixelsPerMetre = (float) getWidth(getContext()) / (float) min(minZoom, dangerRadiusMetres);
        minPixelsPerMetre = (float) getWidth(getContext()) / (float) max(screenWidthMetres, maxZoom);
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
    @SuppressWarnings("unused")
    public static int getHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    float displayRotation() {
        // Do not allow return of NaN from direction sensors... this causes cos() & sin() to
        // both return 0.
        float rot;
        if (displayOrientation == DisplayOrientation.HeadingUp) {
            rot = Compass.degTrue();
            if (!Float.isNaN(rot)) return rot;
            Log.e("Heading mode failed");
            Toast.makeText(getContext(), "Heading information lost", Toast.LENGTH_LONG).show();
        } else if (displayOrientation == DisplayOrientation.TrackUp) {
            rot = Gps.getTrack();
            if (!Float.isNaN(rot)) return rot;
            Log.e("Track mode failed");
            Toast.makeText(getContext(), "Tracking information lost", Toast.LENGTH_LONG).show();
        }
        // Default if both the above fail
        return 0;
    }

    static private Bitmap loadIcon(Context context, int iconId) {
        Icon icon = Icon.createWithResource(context, iconId);
        Drawable drawable = icon.loadDrawable(context);
        if (drawable instanceof BitmapDrawable bitmapDrawable) {
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
//        Log.d("Refresh %s", layer == null ? "ALL" : layer.vehicle.callsign);
        if (layer == null)
            layers.invalidateSelf();
        else
            layers.invalidateDrawable(layer);
    }

    /* This listener is used to listen pinch zoom gesture. */
    private class PinchListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        // When pinch zoom gesture occurred.
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            // Scale the image with pinch zoom value.
            double scalefactor = detector.getScaleFactor();
            if (scalefactor == 1.0) return false;
            pixelsPerMetre *= scalefactor;
//            Log.i("Scale factor = %f", scalefactor);
            if (pixelsPerMetre < minPixelsPerMetre)
                pixelsPerMetre = minPixelsPerMetre;
            if (pixelsPerMetre > maxPixelsPerMetre)
                pixelsPerMetre = maxPixelsPerMetre;
            refresh(null);
            return true;
        }
    }

    /**
     * Calculate scale factor in pixels per metre to place the furthest aircraft or one of its
     * predicted points at the edge of the screen.
     * It's possible that a nearer aircraft's predicted points may end up off the screen,
     * but that will be OK.
     * No aircraft -> default scale factor set by user
     *
     * @param bounds   Bounds of visible window
     * @param furthest Furthest aircraft position
     * @return pixels per metre
     */
    private float updateScaleFactor(Rect bounds, Vehicle furthest) {
        if (furthest == null || furthest.position == null) return defaultPixelsPerMetre;
        if (furthest.distance < minZoom) return maxPixelsPerMetre;
        if (furthest.distance > maxZoom) return minPixelsPerMetre;

        Point furthestPoint = new Point(0, 0);
        extend(furthestPoint, furthest.position);
        if (showLinearPredictionTrack)
            extend(furthestPoint, furthest.predictedPosition);
        if (showPolynomialPredictionTrack)
            extend(furthestPoint, furthest.predicted.get(furthest.predicted.size() - 1));
        Log.v("Furthest (%d, %d) %s", furthestPoint.x, furthestPoint.y, furthest);

        // Both points can't be 0 because then furthest.distance must be < minZoom
        if (furthestPoint.x == 0) {
            // X coordinate is 0 -- Scale the Y coordinate to the edge of the screen
            return pixelsPerMetre * (float) (bounds.top + 32) / furthestPoint.y;
        }
        float xScale = (float) (bounds.right - 32) / furthestPoint.x;
        Log.v("xScale: %f", xScale);
        if (furthestPoint.y == 0) return pixelsPerMetre * xScale;
        float yScale = (float) (bounds.bottom - 32) / furthestPoint.y;
        Log.v("xScale %f yScale %f", xScale, yScale);
        return pixelsPerMetre * (min(xScale, yScale));
    }

    private void extend(Point furthest, Position pos) {
        if (pos == null || !pos.hasAccuracy()) return;
        Point p = screenPoint(pos);
        // Screen size is symmetric around vertical axis
        if (Math.abs(p.x) > furthest.x) furthest.x = Math.abs(p.x);
        // Scale negative Y coordinates so that a negative value at the top edge of the screen maps to the bottom edge of the screen
        if (p.y < 0) p.y = (int) (-p.y * screenYPosPercent / (100f - screenYPosPercent));
        if (p.y > furthest.y) furthest.y = p.y;
    }

    private double previousFurthest;
    private Instant nextZoomAllowed = Instant.EPOCH;

    /**
     * Adjust the screen zoom factor to show the furthest away aircraft and it's predicted positions
     *
     * @param bounds   Bounds of the Background (i.e. the visible map) where 0,0 is the ownship position
     * @param furthest Vehicle whose position is furthest from the ownship position
     */
    void adjustScaleFactor(Rect bounds, Vehicle furthest) {
        if (furthest == null) return;
        Instant now = Instant.now();
        if (previousFurthest > furthest.distance || now.isAfter(nextZoomAllowed)) {
            nextZoomAllowed = now.plusSeconds(5);
            previousFurthest = furthest.distance;
            float newScale = updateScaleFactor(bounds, furthest);
            pixelsPerMetre = Math.max(minPixelsPerMetre, Math.min(newScale, maxPixelsPerMetre));
        }
    }
}
