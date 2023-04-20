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

import static com.meerkat.SettingsActivity.autoZoom;
import static com.meerkat.SettingsActivity.circleRadiusStepMetres;
import static com.meerkat.SettingsActivity.dangerRadiusMetres;
import static com.meerkat.SettingsActivity.displayOrientation;
import static com.meerkat.SettingsActivity.distanceUnits;
import static com.meerkat.SettingsActivity.screenYPosPercent;
import static java.lang.Float.isNaN;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.meerkat.Vehicle;
import com.meerkat.VehicleList;
import com.meerkat.log.Log;

public class Background extends Drawable {
    private final Paint dangerPaint;
    private final Paint circlePaint;
    private final MapView mapView;
    private final VehicleList vehicleList;
    private final CompassView compassView;
    private final TextView compassText, scaleText;

    /**
     * @param mapView     mapView that contains this Background
     * @param vehicleList vehicle list that controls zoom level
     * @param compassView compassView contained in this background
     * @param compassText textView in compassView to show orientation mode
     * @param scaleText   textView in this Background to display zoom level
     */
    public Background(MapView mapView, VehicleList vehicleList, CompassView compassView, TextView compassText, TextView scaleText) {
        this.mapView = mapView;
        this.vehicleList = vehicleList;
        this.compassView = compassView;
        this.compassText = compassText;
        this.scaleText = scaleText;
        Paint whitePaint = new Paint();
        whitePaint.setColor(Color.WHITE);
        whitePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        circlePaint = new Paint();
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setColor((Color.argb(64, 128, 128, 128)));
        Path path = new Path();
        path.addCircle(0, 0, 4, Path.Direction.CW);
        circlePaint.setPathEffect(new PathDashPathEffect(path, 16, 0, PathDashPathEffect.Style.TRANSLATE));
        dangerPaint = new Paint();
        dangerPaint.setColor(Color.YELLOW);
        dangerPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Log.v("draw background");
        canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC);

        Rect bounds = getBounds();
        float xCentre = bounds.width() / 2f;
        float yCentre = bounds.height() * (100f - screenYPosPercent) / 100;
        canvas.drawLine(xCentre, 0, xCentre, bounds.height(), circlePaint);
        // Translate canvas so that 0,0 is at specified location
        // All screen locations are relative to this point
        canvas.translate(xCentre, yCentre);
        canvas.drawLine(-xCentre, 0, xCentre, 0, circlePaint);

        if (autoZoom) {
            mapView.adjustScaleFactor(canvas.getClipBounds(), vehicleList.getFurthest());
        }
        float radiusStep = circleRadiusStepMetres * mapView.pixelsPerMetre;
        Log.v("Radius step = %f", radiusStep);
        if (radiusStep > 5)
            for (float rad = radiusStep; rad < bounds.height(); rad += radiusStep) {
                canvas.drawCircle(0, 0, rad, circlePaint);
            }

        float rot = -MapView.displayRotation();
        String compassLetter;
        if (isNaN(rot)) {
            rot = 0;
            compassLetter = "!";
        } else {
            compassLetter = displayOrientation.toString().substring(0, 1);
        }
        compassView.setRotation(rot);
        compassText.setText(compassLetter);

        //noinspection IntegerDivisionInFloatingPointContext
        scaleText.setText(distanceUnits.toString((bounds.width() / 2) / mapView.pixelsPerMetre));

        Vehicle nearest = vehicleList.getNearest();
        if (nearest == null) return;
        if (nearest.callsign.contains("ZKTHK"))
            Log.i("hit");
        int thickness = (int) (nearest.distance <= dangerRadiusMetres ? dangerRadiusMetres / 2f :
                dangerRadiusMetres * 10 / nearest.distance);
        if (thickness >= dangerRadiusMetres * mapView.pixelsPerMetre)
            thickness = (int) (dangerRadiusMetres * mapView.pixelsPerMetre);
        Log.d("Nearest = %s %s, %d, thickness = %d", nearest.callsign, nearest.distance, dangerRadiusMetres, thickness);
        if (thickness > 0) {
            dangerPaint.setStrokeWidth(thickness);
            canvas.drawCircle(0, 0, dangerRadiusMetres * mapView.pixelsPerMetre, dangerPaint);
        }


        Log.v("finished draw background");
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
