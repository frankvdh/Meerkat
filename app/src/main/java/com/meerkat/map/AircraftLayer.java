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

import static com.meerkat.ui.settings.SettingsViewModel.altUnits;
import static com.meerkat.ui.settings.SettingsViewModel.historySeconds;
import static com.meerkat.ui.settings.SettingsViewModel.ownId;
import static com.meerkat.ui.settings.SettingsViewModel.showLinearPredictionTrack;
import static com.meerkat.ui.settings.SettingsViewModel.showPolynomialPredictionTrack;
import static java.lang.Double.isNaN;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PathEffect;
import android.graphics.Point;

import androidx.annotation.NonNull;

import com.meerkat.Gps;
import com.meerkat.MainActivity;
import com.meerkat.Vehicle;
import com.meerkat.log.Log;
import com.meerkat.measure.Position;

import java.util.List;

public class AircraftLayer extends MapIcon {
    private static final PathEffect historyEffect;
    private static final PathEffect[] predictEffect;
    private static final Paint trackPaint = new Paint();
    Vehicle vehicle;

    static {
        trackPaint.setStrokeWidth(10);
        Path path = new Path();
        path.addCircle(0, 0, 4, Path.Direction.CW);
        historyEffect = new PathDashPathEffect(path, 8, 0, PathDashPathEffect.Style.TRANSLATE);
        predictEffect = new PathEffect[]{
                new PathDashPathEffect(path, 16, 0, PathDashPathEffect.Style.TRANSLATE),
                new PathDashPathEffect(path, 32, 0, PathDashPathEffect.Style.TRANSLATE),
        };
    }

    public AircraftLayer(Vehicle vehicle) {
        super();
        this.vehicle = vehicle;
        setVisible();
    }

    private Point line(Canvas canvas, Point current, Position p, PathEffect effect) {
        Point point = MainActivity.mapView.screenPoint(p);
        if (current.x == point.x && current.y == point.y)
            return current;
//        Log.d("%s Alt: %.0f %.0f", vehicle.callsign, p.getAltitude(), Gps.getAltitude());
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
        Position pos;
        if (!this.isVisible() || vehicle.position == null) return;
        synchronized (vehicle.position) {
            pos = new Position(vehicle.position);
        }
        synchronized (this) {
            track = pos.getTrack();
            var altDiff = pos.getAltitude() - Gps.getAltitude();
            var displayAngle = MainActivity.mapView.displayRotation();
            var iconAngle = !vehicle.emitterType.canRotate || !pos.hasTrack() || isNaN(displayAngle) ? 0 : track - displayAngle;
            super.drawIcon(canvas, pos, vehicle.emitterType.bitmap, iconAngle,
                    altColour(altDiff, pos.isAirborne()),
                    (vehicle.id == ownId) ?
                            altUnits.toString("%,.0f%s", pos.getAltitude()) :
                            vehicle.getLabel() + (isNaN(altDiff) ? "" : ('\n' + altUnits.toString(altDiff))));
            Log.v("draw %06x %s %s %s", vehicle.id, vehicle.callsign, vehicle.emitterType, pos);
            // Canvas is already translated so that 0,0 is at the ownShip point
            Point aircraftPoint = MainActivity.mapView.screenPoint(pos);
            if (showLinearPredictionTrack && vehicle.predictedPosition != null) {
                Log.v("%s predict %b %f %f %f", vehicle.callsign, vehicle.predictedPosition.hasAltitude(), vehicle.predictedPosition.getAltitude(), Gps.getAltitude(), vehicle.predictedPosition.heightAboveOwnship());
                synchronized (vehicle.predictedPosition) {
                    if (vehicle.predictedPosition.hasAccuracy()) {
                        line(canvas, aircraftPoint, vehicle.predictedPosition, predictEffect[vehicle.predictedPosition.hasAltitude() && !Double.isNaN(Gps.getAltitude()) ? 0 : 1]);
                    }
                }
            }

            if (showPolynomialPredictionTrack && vehicle.predicted.get(0).hasAccuracy()) {
                polyLine(canvas, aircraftPoint, vehicle.predicted, predictEffect[!Double.isNaN(Gps.getAltitude()) ? 0 : 1]);
            }

            if (historySeconds > 0) {
                polyLine(canvas, aircraftPoint, vehicle.history, historyEffect);
            }
        }
    }
}

