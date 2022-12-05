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
package com.meerkat.ui.map;

import static com.meerkat.Settings.autoZoom;
import static com.meerkat.Settings.circleRadiusStep;
import static com.meerkat.Settings.displayOrientation;
import static com.meerkat.Settings.distanceUnits;
import static com.meerkat.Settings.screenYPosPercent;
import static com.meerkat.ui.map.MapFragment.defaultScaleFactor;
import static com.meerkat.ui.map.MapFragment.scaleFactor;
import static com.meerkat.ui.map.MapFragment.screenPoint;

import android.content.Context;
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
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.meerkat.VehicleList;
import com.meerkat.databinding.FragmentMapBinding;
import com.meerkat.log.Log;
import com.meerkat.measure.Position;

public class Background extends Drawable {
    private final Paint redPaint;
    private final Paint circlePaint;
    private final ImageView compassView;
    private final TextView compassText;
    private final TextView scaleText;

    public Background(Context context, FragmentMapBinding binding) {
        this.compassView = binding.compassView;
        this.compassText = binding.compassText;
        this.scaleText = binding.scaleText;
        Paint whitePaint = new Paint();
        whitePaint.setColor(Color.WHITE);
        whitePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        redPaint = new Paint();
        redPaint.setColor(Color.RED);
        redPaint.setStyle(Paint.Style.STROKE);
        circlePaint = new Paint();
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setColor((Color.argb(64, 128, 128, 128)));
        Path path = new Path();
        path.addCircle(0, 0, 4, Path.Direction.CW);
        circlePaint.setPathEffect(new PathDashPathEffect(path, 16, 0, PathDashPathEffect.Style.TRANSLATE));

        compassText.setTop(compassView.getTop());
        compassText.setRight(compassView.getRight());
        compassText.setLeft(compassView.getLeft());
        compassText.setBottom(compassView.getBottom());
        compassText.setTranslationX(135);
        compassText.setTranslationY(135);
        compassText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
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
        canvas.drawCircle(0, 0, 100, redPaint);

        if (autoZoom) {
            scaleFactor = getScaleFactor(canvas.getClipBounds(), VehicleList.vehicleList.getMaxDistance());
            Log.v("Scale factor %f", scaleFactor);
        }
        float radiusStep = circleRadiusStep * scaleFactor;
        for (float rad = radiusStep; rad < bounds.height(); rad += radiusStep) {
            canvas.drawCircle(0, 0, rad, circlePaint);
        }
        float rot = -MapFragment.displayRotation();
        compassView.setRotation(rot);
        compassText.setText(displayOrientation.toString().substring(0, 1));
        float screenDistance = bounds.width() / (scaleFactor * 2);
        scaleText.setText(String.format(screenDistance < 10 ? "%.1f%s" : "%.0f%s", screenDistance, distanceUnits.label));
        Log.v("finished draw background... rot = %d", rot);
    }

    private float getScaleFactor(Rect bounds, Position furthest) {
        if (furthest == null) return defaultScaleFactor;
        Point aircraftPoint = screenPoint(furthest);

        if (aircraftPoint.x == 0) {
            if (aircraftPoint.y == 0) return defaultScaleFactor;
            // X coordinate is 0 -- Scale the Y coordinate to the edge of the screen
            if (aircraftPoint.y < 0)
                return scaleFactor * (float) (bounds.top + 32) / aircraftPoint.y;
            return scaleFactor * (float) (bounds.bottom - 32) / aircraftPoint.y;
        }
        float xScale = (float) (aircraftPoint.x < 0 ? (bounds.left + 64) : (bounds.right - 64)) / aircraftPoint.x;
        if (aircraftPoint.y == 0) return scaleFactor * xScale;
        float yScale = (float) (aircraftPoint.y < 0 ? (bounds.top + 64) : (bounds.bottom - 64)) / aircraftPoint.y;
        Log.v("xScale %f yScale %f", xScale, yScale);
        return scaleFactor * (Math.min(xScale, yScale));
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
