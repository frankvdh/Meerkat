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

import static com.meerkat.SettingsActivity.*;
import static java.lang.Float.isNaN;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.meerkat.VehicleList;
import com.meerkat.log.Log;
import com.meerkat.measure.Position;

public class Background extends Drawable {
    private final Paint redPaint;
    private final Paint circlePaint;
    private final MapView mapView;
    private final CompassView compassView;
    private final TextView compassText, scaleText;

    public Background(MapView mapView, CompassView compassView, TextView compassText, TextView scaleText) {
        this.mapView = mapView;
        this.compassView = compassView;
        this.compassText = compassText;
        this.scaleText = scaleText;
        Paint whitePaint = new Paint();
        whitePaint.setColor(Color.WHITE);
        whitePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        redPaint = new Paint();
        redPaint.setColor(Color.RED);
        redPaint.setStrokeWidth(3);
        redPaint.setStyle(Paint.Style.STROKE);
        circlePaint = new Paint();
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setColor((Color.argb(64, 128, 128, 128)));
        Path path = new Path();
        path.addCircle(0, 0, 4, Path.Direction.CW);
        circlePaint.setPathEffect(new PathDashPathEffect(path, 16, 0, PathDashPathEffect.Style.TRANSLATE));
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Log.d("draw background");
        canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC);
        Rect bounds = getBounds();
        float xCentre = bounds.width() / 2f;
        float yCentre = bounds.height() * (100f - screenYPosPercent) / 100;
        canvas.drawLine(xCentre, 0, xCentre, bounds.height(), circlePaint);
        // Translate canvas so that 0,0 is at specified location
        // All screen locations are relative to this point
        canvas.translate(xCentre, yCentre);
        canvas.drawLine(-xCentre, 0, xCentre, 0, circlePaint);
        canvas.drawCircle(0, 0, dangerRadius * mapView.scaleFactor, redPaint);

        if (autoZoom) {
            mapView.scaleFactor = getScaleFactor(canvas.getClipBounds(), VehicleList.getMaxDistance());
            Log.v("Scale factor %.0f", mapView.scaleFactor);
        }
        float radiusStep = circleRadiusStep * mapView.scaleFactor;
        for (float rad = radiusStep; rad < bounds.height(); rad += radiusStep) {
            canvas.drawCircle(0, 0, rad, circlePaint);
        }
        float rot = -MapView.displayRotation();
        String compassLetter;
        if (isNaN(rot)) {
            rot = 0;
            compassLetter = "!";
        } else {
            compassView.setRotation(rot);
            compassLetter = displayOrientation.toString().substring(0, 1);
        }
        compassText.setText(compassLetter);
        float screenDistance = bounds.width() / (mapView.scaleFactor * 2);
        scaleText.setText(String.format(screenDistance < 10 ? "%.1f%s" : "%.0f%s", screenDistance, distanceUnits.label));
        Log.v("finished draw background... rot = %.0f", rot);
    }

    private float getScaleFactor(Rect bounds, Position furthest) {
        if (furthest == null) return mapView.defaultScaleFactor;
        Point aircraftPoint = mapView.screenPoint(furthest);

        if (aircraftPoint.x == 0) {
            if (aircraftPoint.y == 0) return mapView.defaultScaleFactor;
            // X coordinate is 0 -- Scale the Y coordinate to the edge of the screen
            if (aircraftPoint.y < 0)
                return mapView.scaleFactor * (float) (bounds.top + 32) / aircraftPoint.y;
            return mapView.scaleFactor * (float) (bounds.bottom - 32) / aircraftPoint.y;
        }
        float xScale = (float) (aircraftPoint.x < 0 ? (bounds.left + 64) : (bounds.right - 64)) / aircraftPoint.x;
        if (aircraftPoint.y == 0) return mapView.scaleFactor * xScale;
        float yScale = (float) (aircraftPoint.y < 0 ? (bounds.top + 64) : (bounds.bottom - 64)) / aircraftPoint.y;
        Log.v("xScale %f yScale %f", xScale, yScale);
        return mapView.scaleFactor * (Math.min(xScale, yScale));
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
