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

import static com.meerkat.ui.settings.SettingsViewModel.gradientMaximumDiff;
import static com.meerkat.ui.settings.SettingsViewModel.gradientMinimumDiff;
import static java.lang.Double.isNaN;
import static java.lang.Math.abs;
import static java.lang.Math.min;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.meerkat.MainActivity;
import com.meerkat.log.Log;

public class MapIcon extends Drawable {
    protected static final Paint textPaint = new Paint(Color.BLACK);
    protected static final Paint whitePaint = new Paint(Color.WHITE);
    protected final Rect bounds;

    static {
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(48);
        textPaint.setTextAlign(Paint.Align.LEFT);
        whitePaint.setStyle(Paint.Style.FILL);
        whitePaint.setColor(Color.WHITE);
    }

    public void setVisible() {
        setVisible(true, true);
        MainActivity.mapView.layers.invalidateDrawable(this);
    }

    public MapIcon() {
        bounds = new Rect();
    }

    public static int altColour(double altDifference, boolean airborne) {
        if (!airborne) return Color.BLACK;
        if (isNaN(altDifference)) return Color.RED;
        double magnitude = abs(altDifference);
        if (magnitude < gradientMinimumDiff) return Color.RED;
        int BG = (int) min(255, (magnitude - gradientMinimumDiff) / (gradientMaximumDiff - gradientMinimumDiff) * 255);
        int R = 255 - BG;
        return 0xff000000 | R << 16 | BG << (altDifference > 0 ? 0 : 8);
    }

    private Bitmap replaceColor(Bitmap src, int targetColor) {
        if (src == null) {
            return null;
        }
        // Source image size
        int width = src.getWidth();
        int height = src.getHeight();
        int[] pixels = new int[width * height];
        //get pixels
        src.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int x = 0; x < pixels.length; ++x) {
            pixels[x] = (pixels[x] != Color.TRANSPARENT) ? targetColor : pixels[x];
        }
        // create result bitmap output
        Bitmap result = Bitmap.createBitmap(width, height, src.getConfig());
        //set pixels
        result.setPixels(pixels, 0, width, 0, 0, width, height);
        return result;
    }

    public void draw(@NonNull Canvas canvas) {
        Log.a("MapIcon.draw(canvas) called");
    }

    public void drawIcon(@NonNull Canvas canvas, Location location, Bitmap icon, float iconAngle, int colour, String text) {
        synchronized (this) {
            // Canvas is already translated so that 0,0 is at the ownShip point
            Rect clipBounds = canvas.getClipBounds();
            Point aircraftPoint = MainActivity.mapView.screenPoint(location);
            // Draw the icon if part of it is visible
            var bmpWidth = icon.getWidth();
            var bmpHeight = icon.getHeight();
            bounds.set(aircraftPoint.x - bmpWidth / 2, aircraftPoint.y - bmpHeight / 2, aircraftPoint.x + bmpWidth / 2, aircraftPoint.y + bmpHeight / 2);
            if (bounds.right > clipBounds.left && bounds.left < clipBounds.right && bounds.bottom > clipBounds.top && bounds.top < clipBounds.bottom) {
                canvas.drawBitmap(replaceColor(icon, colour),
                        positionMatrix(bmpWidth / 2, bmpHeight / 2, aircraftPoint.x, aircraftPoint.y,
                                iconAngle), null);
                int lineHeight = (int) (textPaint.getTextSize() + 1);
                // Display absolute altitude next to ownShip, callsign & relative altitude next to others
                drawText(canvas, aircraftPoint, lineHeight, text, clipBounds, bmpWidth);
            }
        }
    }

    private final Matrix matrix = new Matrix(); // Avoid constructing

    private Matrix positionMatrix(int centreX, int centreY, float x, float y, float angle) {
        matrix.reset();
        // Rotate about centre of icon & translate to bitmap position
        if (!isNaN(angle))
            matrix.setRotate(angle, centreX, centreY);
        matrix.postTranslate(x - centreX, y - centreY);
        return matrix;
    }

    protected void drawText(Canvas canvas, final Point aircraftPos, int textHeight, String text, Rect clipBounds, int bmpWidth) {
        var textLines = text.split("\\n");
        Log.v("drawText: " + String.join(", ", textLines));
        float textWidth = 0;
        for (var s : textLines) {
            textWidth = Math.max(textWidth, textPaint.measureText(s));
        }
        if (aircraftPos.x + bmpWidth / 2f + textWidth <= clipBounds.left) return;
        if (aircraftPos.x - bmpWidth / 2f - textWidth >= clipBounds.right) return;
        var vertOffset = (textHeight * textLines.length) / 2;
        if (aircraftPos.y + vertOffset <= clipBounds.top) return;
        if (aircraftPos.y - vertOffset >= clipBounds.bottom) return;

        // Some part of text will be visible, so make all of it visible
        int x = aircraftPos.x;
        if (x < clipBounds.left) x = clipBounds.left;
        else if (x >= clipBounds.right) x = clipBounds.right - 1;
        // If icon is in the left quarter of the window, put the left-aligned label to the right of it
        // If icon is in the right quarter of the window, put the right-aligned label to the left of it
        // If the icon is in the middle 2 quarters, don't change the alignment
        if (x > clipBounds.width() / 4) {
            textPaint.setTextAlign(Paint.Align.RIGHT);
        } else if (x < -clipBounds.width() / 4) {
            textPaint.setTextAlign(Paint.Align.LEFT);
        }
        x += (textPaint.getTextAlign() == Paint.Align.LEFT ? bmpWidth : -bmpWidth) / 2;

        int y = aircraftPos.y;
        if ((textLines.length & 1) != 0) y -= textHeight / 2;
        if (y < clipBounds.top + textHeight) y = clipBounds.top + textHeight;
        else if (y >= clipBounds.bottom - textHeight)
            y = clipBounds.bottom - textHeight - 1;
        for (String textLine : textLines) {
            if (textPaint.getTextAlign() == Paint.Align.LEFT)
                canvas.drawRect(x, y - textHeight, x + textPaint.measureText(textLine), y, whitePaint);
            else
                canvas.drawRect(x - textPaint.measureText(textLine), y - textHeight, x, y, whitePaint);
            canvas.drawText(textLine, x, y, textPaint);
            y += textHeight;
        }
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }
}

