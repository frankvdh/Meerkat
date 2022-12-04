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

import static com.meerkat.Settings.circleRadiusStep;
import static com.meerkat.Settings.displayOrientation;
import static com.meerkat.Settings.distanceUnits;
import static com.meerkat.Settings.screenYPosPercent;
import static com.meerkat.ui.map.MapFragment.scaleFactor;

import android.content.Context;
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
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.meerkat.databinding.FragmentMapBinding;
import com.meerkat.log.Log;

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
        redPaint.setColor(Color.WHITE);
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
        canvas.drawLine(xCentre, 0, xCentre, bounds.height(), circlePaint);
        // Translate canvas so that 0,0 is at specified location
        // All screen locations are relative to this point
        canvas.translate(xCentre, bounds.height() * (100f - screenYPosPercent) / 100);
        canvas.drawLine(-xCentre, 0, xCentre, 0, circlePaint);
        canvas.drawCircle(0, 0, 100, redPaint);
        float radiusStep = circleRadiusStep * scaleFactor;
        for (float rad = radiusStep; rad < bounds.height(); rad += radiusStep) {
            canvas.drawCircle(0, 0, rad, circlePaint);
        }
        float rot = -MapFragment.displayRotation();
        compassView.setRotation(rot);
        compassText.setText(displayOrientation.toString().substring(0, 1));
        float screenDistance  = getBounds().width() / (scaleFactor*2);
        scaleText.setText(String.format(screenDistance < 10 ? "%.1f%s" :"%.0f%s", screenDistance, distanceUnits.label));
        Log.v("finished draw background... rot = %d", rot);
    }

    @Override
    public void setAlpha(int alpha) {  }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {  }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
