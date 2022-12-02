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

import static com.meerkat.Settings.gradientMaximumDiff;
import static com.meerkat.Settings.gradientMinimumDiff;
import static com.meerkat.Settings.historySeconds;
import static com.meerkat.Settings.showLinearPredictionTrack;
import static com.meerkat.Settings.showPolynomialPredictionTrack;
import static java.lang.Double.isNaN;
import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.min;
import static java.lang.Math.sin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PathEffect;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.meerkat.Gps;
import com.meerkat.Vehicle;
import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.log.Log;
import com.meerkat.measure.Height;
import com.meerkat.measure.Polar;
import com.meerkat.measure.Position;

import java.util.List;
import java.util.Locale;

public class AircraftLayer extends Drawable {
    private Vehicle v;
    private static final PathEffect historyEffect;
    private static final PathEffect predictEffect;
    private static final Paint textPaint = new Paint(Color.BLACK);
    private static final Paint trackPaint = new Paint();

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
        this.v = v;
        setVisible(true, true);
        MapFragment.layers.invalidateDrawable(this);
    }

    public AircraftLayer(Vehicle v) {
        set(v);
    }

    public Bitmap replaceColor(Bitmap src, int targetColor) {
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

    int altColour(Height altDifference, boolean airborne) {
        if (!airborne) return Color.BLACK;
        if (isNaN(altDifference.value)) return Color.RED;
        float magnitude = abs(altDifference.value);
        if (magnitude < gradientMinimumDiff) return Color.RED;
        int BG = (int) min(255, (magnitude - gradientMinimumDiff) / (gradientMaximumDiff - gradientMinimumDiff) * 255);
        int R = 255 - BG;
        return 0xff000000 | R << 16 | BG << (altDifference.value > 0 ? 0 : 8);
    }

    Point screenPoint(Position p) {
        Polar spPolar = new Polar();
        spPolar.set(Gps.location, p);
        double b = Position.bearingToRad(spPolar.bearing - MapFragment.displayRotation());
        // new point is relative to (0, 0) of the canvas, which is at the ownShip position
        return new Point((int) (cos(b) * spPolar.distance.value * MapFragment.scaleFactor), (int) (-sin(b) * spPolar.distance.value * MapFragment.scaleFactor));
    }

    static public Bitmap loadIcon(Context context, int iconId) {
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

    private Point line(Canvas canvas, Point current, Position p, PathEffect effect) {
        Point point = screenPoint(p);
        if (current.x == point.x && current.y == point.y)
            return current;
        trackPaint.setColor(altColour(altDiff(p.getAlt()), p.isAirborne()));
        trackPaint.setPathEffect(effect);
        canvas.drawLine(current.x, current.y, point.x, point.y, trackPaint);
        return point;
    }

    private void polyLine(Canvas canvas, Point c1, final List<Position> l, PathEffect effect) {
        Point current = new Point(c1);
        if (l == null || l.size() == 0) return;
        synchronized (l) {
            for (Position p: l) {
                current = line(canvas, current, p, effect);
            }
        }
    }

    private Height altDiff(Height h) {
        return new Height(h.value - (float) Gps.location.getAltitude() / h.units.factor, h.units);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        float track;
        boolean isAirborne;
        Gdl90Message.Emitter emitter;
        Position currentPos;
        String callsign;
        if (!this.isVisible()) return;
        synchronized (this) {
            synchronized (v.lastValid) {
                track = v.lastValid.getTrack();
                isAirborne = v.lastValid.isAirborne();
                emitter = v.emitterType;
                currentPos = new Position(v.lastValid);
                callsign = v.callsign;
            }
            Log.d("draw %d %s %s", v.id, callsign, currentPos);
            // Canvas is already translated so that 0,0 is at the ownShip point
            Rect bounds = canvas.getClipBounds();
            var bmpWidth = emitter.bitmap.getWidth();
            Point aircraftPoint = screenPoint(currentPos);
            // Draw the icon if part of it is visible
            Height altDiff = new Height((float) ((currentPos.getAltitude() - Gps.location.getAltitude()) / currentPos.getAlt().units.factor), currentPos.getAlt().units);
            if (aircraftPoint.x > bounds.left - bmpWidth / 2 && aircraftPoint.x < bounds.right + bmpWidth / 2 && aircraftPoint.y > bounds.top - bmpWidth / 2 && aircraftPoint.y < bounds.bottom + bmpWidth / 2) {
                canvas.drawBitmap(replaceColor(emitter.bitmap, altColour(altDiff, isAirborne)),
                        MapFragment.positionMatrix(emitter.bitmap.getWidth() / 2, emitter.bitmap.getHeight() / 2, aircraftPoint.x, aircraftPoint.y,
                                track - MapFragment.displayRotation()), null);
                int lineHeight = (int) (textPaint.getTextSize() + 1);
                String[] text = {String.format(Locale.ENGLISH, "%s%c", v.getLabel(), v.isValid() ? ' ' : '!'),
                        isNaN(altDiff.value) ? "" : altDiff.toString()};
                drawText(canvas, aircraftPoint, lineHeight, text, bounds, bmpWidth);
            }

            if (showLinearPredictionTrack && v.predictedPosition != null) {
                synchronized (v.predictedPosition) {
                    if (v.predictedPosition.isValid()) {
                        line(canvas, aircraftPoint, v.predictedPosition, predictEffect);
                    }
                }
            }

             if (showPolynomialPredictionTrack) {
                polyLine(canvas, aircraftPoint, v.predicted, predictEffect);
             }

            if (historySeconds > 0) {
                polyLine(canvas, aircraftPoint, v.history, historyEffect);
            }
        }
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
        if (x > bounds.width()/4) {
            textPaint.setTextAlign(Paint.Align.RIGHT);
        } else if (x < -bounds.width()/4) {
            textPaint.setTextAlign(Paint.Align.LEFT);
        }
        x += (textPaint.getTextAlign() == Paint.Align.LEFT ? bmpWidth : -bmpWidth) / 2;

        int y = aircraftPos.y;
        if (y < bounds.top + textHeight) y = bounds.top + textHeight;
        else if (y >= bounds.bottom - textHeight)
            y = bounds.bottom - textHeight - 1;
        canvas.drawText(text[0], x, y, textPaint);
        if (!text[1].isBlank()) canvas.drawText(text[1], x, y + textHeight, textPaint);
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

