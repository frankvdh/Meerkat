package com.meerkat.ui.map;

import static com.meerkat.Settings.circleRadiusStep;
import static com.meerkat.Settings.screenWidth;
import static com.meerkat.Settings.screenYPosPercent;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.meerkat.log.Log;

public class Background extends Drawable {
    private final Paint whitePaint, redPaint;
    private final Paint circlePaint;

    public Background(){
        whitePaint = new Paint();
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
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Log.d("draw background");
        canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC);
        canvas.drawLine(canvas.getWidth()/2, 0, canvas.getWidth()/2, canvas.getHeight(), circlePaint);
        canvas.translate(canvas.getWidth()/2, canvas.getHeight()*(100-screenYPosPercent)/100);
        canvas.drawLine(-canvas.getWidth()/2, 0, canvas.getWidth()/2, 0, circlePaint);
        canvas.drawCircle(0, 0, 100, redPaint);
        float radiusStep = circleRadiusStep / screenWidth * canvas.getWidth()/2;
        for (float rad = radiusStep; rad < canvas.getHeight(); rad += radiusStep) {
            canvas.drawCircle(0, 0, rad, circlePaint);
        }
//        Log.d("finished draw background");
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
