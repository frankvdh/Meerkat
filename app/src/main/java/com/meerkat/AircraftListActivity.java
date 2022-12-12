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

import static com.meerkat.SettingsActivity.distanceUnits;
import static com.meerkat.SettingsActivity.keepScreenOn;
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
import com.meerkat.measure.Distance;
import com.meerkat.measure.Height;
import com.meerkat.measure.Speed;
import com.meerkat.measure.VertSpeed;

import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Stream;

public class AircraftListActivity extends AppCompatActivity {

    private TableLayout tableAircraft;
    private ScheduledFuture<?> task;

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

        task = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::refreshAircraftDisplay, 1, 1, SECONDS);
    }

    @Override
    public void onPause() {
        super.onPause();
        task.cancel(true);
        Log.i("%s paused", this.getLocalClassName());
    }

    private void refreshAircraftDisplay() {
        try {
            Log.d("refreshAircraftDisplay: ", VehicleList.vehicleList.keySet().size());
            Stream<Vehicle> s = VehicleList.vehicleList.getVehicles().stream().sorted();
            int i = 1; // row 0 is header
            for (Iterator<Vehicle> it = s.iterator(); it.hasNext(); i++) {
                Vehicle v = it.next();
                synchronized (v.lastValid) {
                    Log.i(v.toString());
                    Distance distance;
                    String bearing;
                    String track;
                    Speed speed;
                    VertSpeed vVel;
                    Height alt;
                    distance = new Distance(v.distance / distanceUnits.factor, distanceUnits);
                    bearing = String.format("%03d", (int) (Gps.bearingTo(v.lastValid) + 360) % 360);
                    track = Float.isNaN(v.lastValid.getTrack()) ? "---" : String.format("%03.0f", v.lastValid.getTrack());
                    speed = v.lastValid.getSpeedUnits();
                    vVel = v.lastValid.getVVel();
                    alt = v.lastValid.getAlt();
                    if (i < tableAircraft.getChildCount()) {
                        TableRow row = (TableRow) tableAircraft.getChildAt(i);
                        runOnUiThread(() -> {
                            ((TextView) row.getChildAt(0)).setText(v.getLabel());
                            ((TextView) row.getChildAt(1)).setText(distance.toString());
                            ((TextView) row.getChildAt(2)).setText(bearing);
                            ((TextView) row.getChildAt(3)).setText(alt.toString());
                            ((TextView) row.getChildAt(4)).setText(track);
                            ((TextView) row.getChildAt(5)).setText(speed == null ? "----" : speed.toString());
                            ((TextView) row.getChildAt(6)).setText(vVel == null ? "----" : vVel.toString());
                            row.postInvalidate();
                        });
                    } else {
                        TableRow row = new TableRow(getApplicationContext());
                        row.setLayoutParams(tableAircraft.getChildAt(0).getLayoutParams()); // Copy layout from heading row
                        row.addView(view(v.getLabel()));
                        row.addView(view(distance.toString()));
                        row.addView(view(bearing));
                        row.addView(view(alt.toString()));
                        row.addView(view(track));
                        row.addView(view(speed == null || Float.isNaN(speed.value) ? "----" : speed.toString()));
                        row.addView(view(vVel == null ? "----" : vVel.toString()));
                        runOnUiThread(() -> tableAircraft.addView(row));
                    }
                }
            }

            // If the number of aircraft in range decreases, there will be some entries in tableAircraft that are out of date and should not be displayed
            for (int j = tableAircraft.getChildCount() - 1; j >= i; j--) {
                Log.i("Remove %d from %d", j, tableAircraft.getChildCount());
                int finalJ = j;
                runOnUiThread(() -> tableAircraft.removeViewAt(finalJ));
            }
            synchronized (tableAircraft) {
                tableAircraft.notifyAll();
            }
        } catch (Exception ex) {
            Log.e("Exception: %s", ex.getMessage());
        }
    }

    private TextView view(String text) {
        TextView tv = new TextView(getApplicationContext());
        tv.setText(text);
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