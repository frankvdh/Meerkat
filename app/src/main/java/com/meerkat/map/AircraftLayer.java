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

import static com.meerkat.SettingsActivity.altUnits;
import static com.meerkat.SettingsActivity.gradientMaximumDiff;
import static com.meerkat.SettingsActivity.gradientMinimumDiff;
import static com.meerkat.SettingsActivity.historySeconds;
import static com.meerkat.SettingsActivity.showLinearPredictionTrack;
import static com.meerkat.SettingsActivity.showPolynomialPredictionTrack;
import static java.lang.Double.isNaN;
import static java.lang.Math.abs;
import static java.lang.Math.min;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PathEffect;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.meerkat.Gps;
import com.meerkat.Vehicle;
import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.log.Log;
import com.meerkat.measure.Position;

import java.util.List;

public class AircraftLayer extends Drawable {
    private static final PathEffect historyEffect;
    private static final PathEffect predictEffect;
    private static final Paint textPaint = new Paint(Color.BLACK);
    private static final Paint trackPaint = new Paint();
    Vehicle vehicle;
    private final MapView mapView;

    static {
        trackPaint.setStrokeWidth(10);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(48);
        textPaint.setTextAlign(Paint.Align.LEFT);
        Path path = new Path();
        path.addCircle(0, 0, 4, Path.Direction.CW);
        historyEffect = new PathDashPathEffect(path, 8, 0, PathDashPathEffect.Style.TRANSLATE);
        predictEffect = new PathDashPathEffect(path, 16, 0, PathDashPathEffect.Style.TRANSLATE);
    }

    public void set(Vehicle v) {
        this.vehicle = v;
        setVisible(true, true);
        mapView.layers.invalidateDrawable(this);
    }

    public AircraftLayer(Vehicle vehicle, MapView view) {
        mapView = view;
        set(vehicle);
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

    private int altColour(double altDifference, boolean airborne) {
        if (!airborne) return Color.BLACK;
        if (isNaN(altDifference)) return Color.RED;
        double magnitude = abs(altDifference);
        if (magnitude < gradientMinimumDiff) return Color.RED;
        int BG = (int) min(255, (magnitude - gradientMinimumDiff) / (gradientMaximumDiff - gradientMinimumDiff) * 255);
        int R = 255 - BG;
        return 0xff000000 | R << 16 | BG << (altDifference > 0 ? 0 : 8);
    }

    private Point line(Canvas canvas, Point current, Position p, PathEffect effect) {
        Point point = mapView.screenPoint(p);
        if (current.x == point.x && current.y == point.y)
            return current;
        trackPaint.setColor(altColour((int) (p.getAltitude() - Gps.getAltitude()), p.isAirborne()));
        trackPaint.setPathEffect(effect);
        canvas.drawLine(current.x, current.y, point.x, point.y, trackPaint);
        return point;
    }

    private void polyLine(Canvas canvas, Point start, final List<Position> list, PathEffect effect) {
        Point current = new Point(start);
        if (list == null || list.size() == 0) return;
        for (Position p : list) {
            current = line(canvas, current, p, effect);
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        float track;
        boolean isAirborne;
        Gdl90Message.Emitter emitter;
        Position currentPos;
        if (!this.isVisible() || vehicle.lastValid == null) return;
        synchronized (this) {
            currentPos = new Position(vehicle.lastValid);
            emitter = vehicle.emitterType;
            //       Log.d("draw %06x %s %s %s", vehicle.id, vehicle.callsign, emitter, currentPos);
            track = currentPos.getTrack();
            isAirborne = currentPos.isAirborne();
            // Canvas is already translated so that 0,0 is at the ownShip point
            Rect bounds = canvas.getClipBounds();
            Point aircraftPoint = mapView.screenPoint(currentPos);
            // Draw the icon if part of it is visible
            var bmpWidth = emitter.bitmap.getWidth();
            double altDiff = currentPos.getAltitude() - Gps.getAltitude();
            var displayAngle = MapView.displayRotation();
            if (aircraftPoint.x > bounds.left - bmpWidth / 2 && aircraftPoint.x < bounds.right + bmpWidth / 2 && aircraftPoint.y > bounds.top - bmpWidth / 2 && aircraftPoint.y < bounds.bottom + bmpWidth / 2) {
                canvas.drawBitmap(replaceColor(emitter.bitmap, altColour(altDiff, isAirborne)),
                        positionMatrix(emitter.bitmap.getWidth() / 2, emitter.bitmap.getHeight() / 2, aircraftPoint.x, aircraftPoint.y,
                                (isNaN(track) ? 0 : track) - (isNaN(displayAngle) ? 0 : displayAngle)), null);
                int lineHeight = (int) (textPaint.getTextSize() + 1);
                String[] text = {vehicle.getLabel(), isNaN(altDiff) ? "" : altUnits.toString(altDiff)};
                drawText(canvas, aircraftPoint, lineHeight, text, bounds, bmpWidth);
            }
            if (showLinearPredictionTrack && vehicle.predictedPosition != null) {
                synchronized (vehicle.predictedPosition) {
                    if (vehicle.predictedPosition.isValid()) {
                        line(canvas, aircraftPoint, vehicle.predictedPosition, predictEffect);
                    }
                }
            }

            if (showPolynomialPredictionTrack) {
                polyLine(canvas, aircraftPoint, vehicle.predicted, predictEffect);
            }

            if (historySeconds > 0) {
                polyLine(canvas, aircraftPoint, vehicle.history, historyEffect);
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

    private void drawText(Canvas canvas, final Point aircraftPos, int textHeight, String[] text, Rect bounds, int bmpWidth) {
        Log.v("drawText: " + text[0] + ", " + text[1]);
        // Assume first line of text is always the longest
        float textWidth = textPaint.measureText(text[0]);
        if (aircraftPos.x + bmpWidth / 2f + textWidth <= bounds.left) return;
        if (aircraftPos.x - bmpWidth / 2f - textWidth >= bounds.right) return;
        if (aircraftPos.y + textHeight <= bounds.top) return;
        if (aircraftPos.y - textHeight >= bounds.bottom) return;

        // Some part of text will be visible
        int x = aircraftPos.x;
        if (x < bounds.left) x = bounds.left;
        else if (x >= bounds.right) x = bounds.right - 1;
        if (x > bounds.width() / 4) {
            textPaint.setTextAlign(Paint.Align.RIGHT);
        } else if (x < -bounds.width() / 4) {
            textPaint.setTextAlign(Paint.Align.LEFT);
        }
        x += (textPaint.getTextAlign() == Paint.Align.LEFT ? bmpWidth : -bmpWidth) / 2;

        int y = aircraftPos.y;
        if (y < bounds.top + textHeight) y = bounds.top + textHeight;
        else if (y >= bounds.bottom - textHeight)
            y = bounds.bottom - textHeight - 1;
        canvas.drawText(text[0], x, y, textPaint);
        if (text[1] != null && !text[1].isEmpty())
            canvas.drawText(text[1], x, y + textHeight, textPaint);
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

