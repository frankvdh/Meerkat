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
 */
package com.meerkat;

import static com.meerkat.Settings.minHeadingChange;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.meerkat.log.Log;
import com.meerkat.ui.map.MapFragment;

public class Compass extends Service implements SensorEventListener {

    static GeomagneticField geoField;
    static public float Declination = 0;
    public volatile static boolean locationChanged;

    SensorManager sensorManager;
    float[] lastMagFields = new float[3];
    float[] lastAccels = new float[3];
    // These must
    private final float[] rotationMatrix = new float[16];
    private final float[] orientation = new float[4];

    static public float Heading;
    static public float Pitch;
    static public float Roll;

    public Compass(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        resume();
    }

    public static float degTrue() {
        if (locationChanged) {
            geoField = new GeomagneticField((float) Gps.location.getLatitude(), (float) Gps.location.getLongitude(), (float) Gps.location.getAltitude(), System.currentTimeMillis());
            Declination = geoField.getDeclination();
            locationChanged = false;
        }
        return (Heading + Declination) % 360;
    }

    public void resume() {
        locationChanged = true;
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
    }

    public void pause() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, lastAccels, 0, 3);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, lastMagFields, 0, 3);
                break;
            default:
                return;
        }

        Log.v("%s %9.2f %9.2f %9.2f", event.sensor.getType() == Sensor.TYPE_ACCELEROMETER ? "Accel" : "Mag", event.values[0], event.values[1], event.values[2]);

        // Compute the three orientation angles based on the most recent readings from the accelerometer and magnetometer.
        // Update rotation matrix, which is needed to update orientation angles.
        if (SensorManager.getRotationMatrix(rotationMatrix, null, lastAccels, lastMagFields)) {
            SensorManager.getOrientation(rotationMatrix, orientation);
            // "orientationAngles" has azimuth (Z axis angle relative to mag north), pitch, roll

            var prevHeading = Heading;
            Heading = (float) (Math.toDegrees(orientation[0]));
            Pitch = (float) Math.toDegrees(orientation[1]);
            Roll = (float) Math.toDegrees(orientation[2]);

            int change = (int) Math.abs(Heading - prevHeading);
            if (change > minHeadingChange ) {
                 Log.v("Mag %5.1f %5.1f %5.1f | Acc %5.1f %5.1f %5.1f | Mag deg %3.0f",
                        lastMagFields[0], lastMagFields[1], lastMagFields[2],
                        lastAccels[0], lastAccels[1], lastAccels[2],
                        Heading);
                MapFragment.refresh(null);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {  return null; }
}

