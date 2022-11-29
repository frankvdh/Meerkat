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
import static com.meerkat.Settings.headingUp;
import static com.meerkat.Settings.historySeconds;
import static com.meerkat.Settings.showLinearPredictionTrack;
import static com.meerkat.Settings.showPolynomialPredictionTrack;
import static com.meerkat.Settings.trackUp;
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

import com.meerkat.Compass;
import com.meerkat.Gps;
import com.meerkat.Vehicle;
import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.log.Log;
import com.meerkat.measure.Height;
import com.meerkat.measure.Polar;
import com.meerkat.measure.Position;

import java.util.Calendar;
import java.util.Locale;

public class AircraftLayer extends Drawable {
    private Vehicle v;
    private final Polar polar = new Polar();
    private static final PathEffect historyEffect;
    private static final PathEffect predictEffect;
    private static final Paint textPaint = new Paint(Color.BLACK);
    private static final Paint trackPaint = new Paint();

    static {
        trackPaint.setStrokeWidth(10);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(48);
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

    Point screenPoint(Polar polar) {
        double b = Position.bearingToRad(polar.bearing - (headingUp ? Compass.degTrue() : trackUp ? Gps.location.getBearing() : 0));
        // new point is relative to (0, 0) of the canvas, which is at the ownShip position
        return new Point((int) (cos(b) * polar.distance.value * MapFragment.scaleFactor), (int) (-sin(b) * polar.distance.value * MapFragment.scaleFactor));
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

    android.graphics.Point current;

    private void lineTo(Canvas canvas, android.graphics.Point p, int colour, PathEffect effect) {
        trackPaint.setColor(colour);
        trackPaint.setPathEffect(effect);
        canvas.drawLine(current.x, current.y, p.x, p.y, trackPaint);
        current = p;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        float track;
        boolean isAirborne, isValid;
        if (!this.isVisible()) return;
        synchronized (v.current) {
            track = v.current.getTrack();
            isAirborne = v.current.isAirborne();
            isValid = v.current.isValid();
        }
        Log.v("draw " + v.id + " " + v.callsign);
        // Canvas is already translated so that 0,0 is at the ownShip point
        Rect bounds = canvas.getClipBounds();
        var bmpWidth = v.emitterType.bitmap.getWidth();
        Point aircraftPoint;
        if (isValid) {
            polar.set(Gps.location, v.current);
            aircraftPoint = screenPoint(polar);
            // Draw the icon if part of it is visible
            if (aircraftPoint.x > bounds.left - bmpWidth / 2 && aircraftPoint.x < bounds.right + bmpWidth / 2 && aircraftPoint.y > bounds.top - bmpWidth / 2 && aircraftPoint.y < bounds.bottom + bmpWidth / 2) {
                canvas.drawBitmap(replaceColor(v.emitterType.bitmap, altColour(polar.altDifference, isAirborne)),
                        MapFragment.positionMatrix(v.emitterType.bitmap.getWidth() / 2, v.emitterType.bitmap.getHeight() / 2, aircraftPoint.x, aircraftPoint.y,
                                track - (headingUp ? Compass.degTrue() : trackUp ? track : 0)), null);
            }
        } else {
            isValid = !v.history.isEmpty();
            if (isValid) {
                polar.set(Gps.location, v.history.get(v.history.size() - 1));
                aircraftPoint = screenPoint(polar);
            } else aircraftPoint = null;
        }
        if (isValid) {
            if (aircraftPoint != null) {
                int lineHeight = (int) (textPaint.getTextSize() + 1);
                String[] text = {String.format(Locale.ENGLISH, "%s%c", v.getLabel(), v.current.isCrcValid() ? ' ' : '!'),
                        isNaN(polar.altDifference.value) ? "" : polar.altDifference.toString()};
                drawText(canvas, aircraftPoint, lineHeight, text, bounds, bmpWidth);
            }

            if (showLinearPredictionTrack && v.predictedPosition != null && v.predictedPosition.isValid()) {
                current = aircraftPoint;
                polar.set(Gps.location, v.predictedPosition);
                Log.v(v.callsign + ": " + polar + String.format(" %08x", altColour(polar.altDifference, isAirborne)));
                lineTo(canvas, screenPoint(polar), altColour(polar.altDifference, isAirborne), predictEffect);
            }

            if (showPolynomialPredictionTrack) {
                current = aircraftPoint;
                synchronized (v.predicted) {
                    for (Position p1 : v.predicted) {
                        polar.set(Gps.location, p1);
                        android.graphics.Point sxy1 = screenPoint(polar);
                        if (current.x == sxy1.x && current.y == sxy1.y)
                            continue;
                        Log.v("%s: %s %08x", v.callsign, polar, altColour(polar.altDifference, isAirborne));
                        lineTo(canvas, sxy1, altColour(polar.altDifference, isAirborne), predictEffect);
                    }
                }
            }
        }

        long now = Calendar.getInstance().getTime().getTime();
        if (historySeconds > 0) {
            final long maxAge = now - historySeconds * 1000L;
            current = new Point(aircraftPoint);
            synchronized (v.history) {
                for (int i = v.history.size() - 2; i >= 0; i--) {
                    Position p = v.history.get(i);
                    if (p.getTime() < maxAge) break;
                    polar.set(Gps.location, p);
                    android.graphics.Point sxy1 = screenPoint(polar);
                    if (sxy1.x == current.x && sxy1.y == current.y)
                        continue;
                    lineTo(canvas, sxy1, altColour(polar.altDifference, p.isAirborne()), historyEffect);
                }
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
        if (x > 0) {
            x -= bmpWidth / 2;
            textPaint.setTextAlign(Paint.Align.RIGHT);
        } else {
            x += bmpWidth / 2;
            textPaint.setTextAlign(Paint.Align.LEFT);
        }

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

