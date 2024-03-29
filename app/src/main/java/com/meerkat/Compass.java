/*  From MultiWii EZ-GUI
    Copyright (C) <2012>  Bartosz Szczygiel (eziosoft)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

  https://developer.android.com/guide/topics/sensors/sensors_position
  https://github.com/phishman3579/android-compass @author Justin Wetherell (phishman3579@gmail.com)

 */
package com.meerkat;

import static com.meerkat.ui.settings.SettingsViewModel.magFieldUpdateDistance;
import static com.meerkat.ui.settings.SettingsViewModel.sensorSmoothingConstant;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.meerkat.log.Log;

import java.time.Instant;

public class Compass extends Service implements SensorEventListener {

    static GeomagneticField geoField;
    private static float Declination = 0;
    private static SensorManager sensorManager;
    private static final float[] mag = new float[3];
    private static final float[] grav = new float[3];
    private static final float[] rotationMatrix = new float[16];
    private static final float[] orientation = new float[4];
    private static final Location GpsLocation = new Location("");
    private static float Heading;

    public Compass(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        resume();
    }

    public static float degTrue() {
        return (Heading + Declination) % 360;
    }

    public static void updateGeomagneticField() {
        // In NZ, declination changes by about 0.7deg/46nm = 1 deg/65nm = 1 deg/120km
        // So long as we're within 60km of the last location where declination was computed, it will be within 0.5 degrees
        // Other places nearer the magnetic poles will have faster changes in the field
        if (Gps.distanceTo(GpsLocation) < magFieldUpdateDistance) return;
        Gps.getLatLonAltTime(GpsLocation);
        geoField = new GeomagneticField((float) GpsLocation.getLatitude(), (float) GpsLocation.getLongitude(), (float) GpsLocation.getAltitude(), GpsLocation.getTime());
        var newDeclination = geoField.getDeclination();
        Log.i("Mag declination changed from %.1f to %.1f", Declination, newDeclination);
        Declination = newDeclination;
    }

    public void resume() {
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        updateGeomagneticField();
    }

    @SuppressWarnings("unused")
    public void pause() {
        sensorManager.unregisterListener(this);
    }

    public void stop() {
        pause();
        this.stopSelf();
        Log.i("Compass stopped");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private static Instant lastRead = Instant.MIN;

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                lowPassFilter(event, grav);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                lowPassFilter(event, mag);
                break;
            default:
                return;
        }

        var now = Instant.now();
        Log.v("%s %9.2f %9.2f %9.2f", event.sensor.getType() == Sensor.TYPE_ACCELEROMETER ? "Accel" : "Mag", event.values[0], event.values[1], event.values[2]);
        if (!lastRead.isBefore(now.minusSeconds(1))) return;
        lastRead = now;
        // Compute the three orientation angles based on the most recent readings from the accelerometer and magnetometer.
        // Update rotation matrix, which is needed to update orientation angles.
        if (SensorManager.getRotationMatrix(rotationMatrix, null, grav, mag)) {
            SensorManager.getOrientation(rotationMatrix, orientation);
            MainActivity.blinkHdg();
            // "orientation" has azimuth (Z axis angle relative to mag north), pitch, roll
            int prevHeading = (int) Heading;
            Heading = (float) (Math.toDegrees(orientation[0]));
            //          Get "Pitch" and "Roll" from elements 1 and 2 of the array

            Log.v("Mag %5.1f %5.1f %5.1f | Acc %5.1f %5.1f %5.1f | Mag deg %3.0f",
                    mag[0], mag[1], mag[2],
                    grav[0], grav[1], grav[2],
                    Heading);
            if ((int) Heading != prevHeading)
                MainActivity.mapView.refresh(null);
        }
    }

    /**
     * Filter the given input against the previous values and return a low-pass filtered result.
     *
     * @param event SensorEvent with values array to smooth.
     * @param prev  float array representing the previous values.
     */
    static void lowPassFilter(SensorEvent event, float[] prev) {
        if (event == null)
            throw new NullPointerException("event must be non-NULL");
        for (int i = 0; i < prev.length; i++) {
            prev[i] += sensorSmoothingConstant * (event.values[i] - prev[i]);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

