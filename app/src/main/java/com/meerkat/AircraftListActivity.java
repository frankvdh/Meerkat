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
package com.meerkat;

import static com.meerkat.SettingsActivity.altUnits;
import static com.meerkat.SettingsActivity.distanceUnits;
import static com.meerkat.SettingsActivity.keepScreenOn;
import static com.meerkat.SettingsActivity.speedUnits;
import static com.meerkat.SettingsActivity.vertSpeedUnits;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.meerkat.databinding.ActivityAircraftListBinding;
import com.meerkat.log.Log;
import com.meerkat.map.MapActivity;

import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Stream;

public class AircraftListActivity extends AppCompatActivity {

    private TableLayout tableAircraft;
    private ScheduledFuture<?> task;
    private VehicleList vehicleList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.meerkat.databinding.ActivityAircraftListBinding binding = ActivityAircraftListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tableAircraft = binding.tableAircraft;
        tableAircraft.setKeepScreenOn(keepScreenOn);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("%s resumed", this.getLocalClassName());
        vehicleList = MapActivity.getVehicleList();
        task = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::refreshAircraftDisplay, 1, 1, SECONDS);
    }

    @Override
    public void onPause() {
        super.onPause();
        task.cancel(true);
        Log.i("%s paused", this.getLocalClassName());
    }

    @SuppressWarnings("unused")
    private void refreshAircraftDisplay() {
        try {
            Log.d("refreshAircraftDisplay: ", vehicleList.keySet().size());
            Stream<Vehicle> s = vehicleList.getVehicles().stream().sorted();
            int i = 1; // row 0 is header
            for (Iterator<Vehicle> it = s.iterator(); it.hasNext(); i++) {
                Vehicle v = it.next();
                Log.i(v.toString());
                double distance;
                String bearing;
                String track;
                float speed;
                double vVel;
                double alt;
                synchronized (v) {
                    distance = v.distance;
                    bearing = String.format(Locale.getDefault(), "%03d", (int) (Gps.bearingTo(v.lastValid) + 360) % 360);
                    track = Float.isNaN(v.lastValid.getTrack()) ? "---" : String.format(Locale.getDefault(), "%03.0f", v.lastValid.getTrack());
                    speed = v.lastValid.getSpeed();
                    vVel = v.lastValid.getVVel();
                    alt = v.lastValid.getAltitude();
                }
                if (i < tableAircraft.getChildCount()) {
                    TableRow row = (TableRow) tableAircraft.getChildAt(i);
                    runOnUiThread(() -> {
                        ((TextView) row.getChildAt(0)).setText(v.getLabel());
                        ((TextView) row.getChildAt(1)).setText(distanceUnits.toString(distance));
                        ((TextView) row.getChildAt(2)).setText(bearing);
                        ((TextView) row.getChildAt(3)).setText(altUnits.toString(alt));
                        ((TextView) row.getChildAt(4)).setText(track);
                        ((TextView) row.getChildAt(5)).setText(speedUnits.toString(speed));
                        ((TextView) row.getChildAt(6)).setText(vertSpeedUnits.toString(vVel));
                        row.postInvalidate();
                    });
                } else {
                    TableRow row = new TableRow(getApplicationContext());
                    row.setLayoutParams(tableAircraft.getChildAt(0).getLayoutParams()); // Copy layout from heading row
                    row.addView(view(v.getLabel()));
                    row.addView(view(distanceUnits.toString(distance)));
                    row.addView(view(bearing));
                    row.addView(view(altUnits.toString(alt)));
                    row.addView(view(track));
                    row.addView(view(speedUnits.toString(speed)));
                    row.addView(view(vertSpeedUnits.toString(vVel)));
                    runOnUiThread(() -> tableAircraft.addView(row));
                }
            }

            // If the number of aircraft in range decreases, there will be some entries in tableAircraft that are out of date and should not be displayed
            for (int j = tableAircraft.getChildCount() - 1; j >= i; j--) {
                Log.i("Remove %d from %d", j, tableAircraft.getChildCount());
                int finalJ = j;
                runOnUiThread(() -> tableAircraft.removeViewAt(finalJ));
            }
            //noinspection SynchronizeOnNonFinalField
            synchronized(tableAircraft) {
                tableAircraft.notifyAll();
            }
        } catch (Exception ex) {
            Log.e("Exception: %s", ex.getMessage());
        }
    }

    private TextView view(@SuppressWarnings("unused") String txt) {
        TextView tv = new TextView(getApplicationContext());
        tv.setText(txt);
        tv.setTextColor(Color.BLACK);
        tv.setGravity(Gravity.CENTER);
        return tv;
    }

    // The "Home" button is clicked
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}