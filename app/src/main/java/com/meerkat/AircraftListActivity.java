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

import static com.meerkat.SettingsActivity.keepScreenOn;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.meerkat.Gps;
import com.meerkat.Vehicle;
import com.meerkat.VehicleList;
import com.meerkat.databinding.ActivityAircraftListBinding;
import com.meerkat.log.Log;
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
            synchronized (tableAircraft) {
                int i = 1; // row 0 is header
                for (Iterator<Vehicle> it = s.iterator(); it.hasNext(); i++) {
                    Vehicle v = it.next();
                    Log.i(v.toString());
                    float distance = Gps.distanceTo(v.lastValid) / 1852;
                    float track = v.lastValid.getTrack();
                    Speed speed = v.lastValid.getSpeedUnits();
                    VertSpeed vVel = v.lastValid.getVVel();
                    if (i < tableAircraft.getChildCount()) {
                        TableRow row = (TableRow) tableAircraft.getChildAt(i);
                        runOnUiThread(() -> {
                            ((TextView) row.getChildAt(0)).setText(v.getLabel());
                            ((TextView) row.getChildAt(1)).setText(String.format(Locale.ENGLISH, distance < 10 ? "%.1f%s" : "%.0f%s", distance, "nm"));
                            ((TextView) row.getChildAt(2)).setText(String.format(Locale.ENGLISH, "%03.0f", (Gps.bearingTo(v.lastValid) + 360) % 360));
                            ((TextView) row.getChildAt(3)).setText(String.format(Locale.ENGLISH, "%s", v.lastValid.getAlt()));
                            ((TextView) row.getChildAt(4)).setText(Float.isNaN(track) ? "---" : String.format(Locale.ENGLISH, "%03.0f", track));
                            ((TextView) row.getChildAt(5)).setText(speed == null ? "----" : speed.toString());
                            ((TextView) row.getChildAt(6)).setText(vVel == null ? "----" : vVel.toString());
                            row.postInvalidate();
                        });
                    } else {
                        TableRow row = new TableRow(getApplicationContext());
                        row.setLayoutParams(tableAircraft.getChildAt(0).getLayoutParams()); // Copy layout from heading row
                        row.addView(view(v.getLabel()));
                        row.addView(view(String.format(Locale.ENGLISH, distance < 10 ? "%.1f%s" : "%.0f%s", distance, "nm")));
                        row.addView(view(String.format(Locale.ENGLISH, "%03.0f", (Gps.bearingTo(v.lastValid) + 360) % 360)));
                        row.addView(view(String.format(Locale.ENGLISH, "%s", v.lastValid.getAlt())));
                        row.addView(view(Float.isNaN(track) ? "---" : String.format(Locale.ENGLISH, "%03.0f", track)));
                        row.addView(view(speed == null || Float.isNaN(speed.value) ? "----" : speed.toString()));
                        row.addView(view(vVel == null ? "----" : vVel.toString()));
                        runOnUiThread(() -> tableAircraft.addView(row));
                    }
                }
                // If the number of aircraft in range decreases, there will be some entries in tableAircraft that are out of date and should not be displayed
                for (int j = tableAircraft.getChildCount() - 1; j >= i; j--) {
                    Log.i("Remove %d from %d", j, tableAircraft.getChildCount());
                    int finalJ = j;
                    runOnUiThread(() -> tableAircraft.removeViewAt(finalJ));
                }
                tableAircraft.notifyAll();
            }
        } catch (Exception ex) {
            Log.e("Exception in AircraftFragment scheduled task: %s", ex.getMessage());
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