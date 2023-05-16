package com.meerkat.ui.aircraftlist;

import static com.meerkat.ui.settings.SettingsViewModel.altUnits;
import static com.meerkat.ui.settings.SettingsViewModel.distanceUnits;
import static com.meerkat.ui.settings.SettingsViewModel.keepScreenOn;
import static com.meerkat.ui.settings.SettingsViewModel.speedUnits;
import static com.meerkat.ui.settings.SettingsViewModel.vertSpeedUnits;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.lifecycle.ViewModel;

import com.meerkat.Gps;
import com.meerkat.MainActivity;
import com.meerkat.Vehicle;
import com.meerkat.databinding.FragmentAircraftlistBinding;
import com.meerkat.log.Log;

import java.util.Iterator;
import java.util.Locale;
import java.util.stream.Stream;

public class AircraftListViewModel extends ViewModel {
    private TableLayout tableAircraft;

    public AircraftListViewModel() {
    }

    public void init(FragmentAircraftlistBinding binding) {
        tableAircraft = binding.tableAircraft;
        tableAircraft.setKeepScreenOn(keepScreenOn);
    }

    private void refreshAircraftDisplay() {
        try {
            Log.d("refreshAircraftDisplay: %d vehicles, %d rows", MainActivity.vehicleList.keySet().size(), tableAircraft.getChildCount());
            Stream<Vehicle> s = MainActivity.vehicleList.getVehicles().stream().sorted();
            int i = 1; // row 0 is header

            for (Iterator<Vehicle> it = s.iterator(); it.hasNext(); i++) {
                Vehicle v = it.next();
                Log.i(v.toString());
                double distance;
                String bearing = "???";
                String track = "???";
                float speed = 0;
                double vVel = 0;
                double alt = -10000;

                synchronized (v) {
                    distance = v.distance;
                    if (v.position != null) {
                        bearing = String.format(Locale.getDefault(), "%03d", (int) (Gps.bearingTo(v.position) + 360) % 360);
                        track = Float.isNaN(v.position.getTrack()) ? "---" : String.format(Locale.getDefault(), "%03.0f", v.position.getTrack());
                        speed = v.position.getSpeed();
                        vVel = v.position.getVVel();
                        alt = v.position.getAltitude();
                    }
                }
                if (i < tableAircraft.getChildCount()) {
                    try {
                        TableRow row = (TableRow) tableAircraft.getChildAt(i);
                        String finalBearing = bearing;
                        double finalAlt = alt;
                        String finalTrack = track;
                        float finalSpeed = speed;
                        double finalVVel = vVel;
                        //                       runOnUiThread(() -> {
                        ((TextView) row.getChildAt(0)).setText(v.getLabel());
                        ((TextView) row.getChildAt(1)).setText(distanceUnits.toString(distance));
                        ((TextView) row.getChildAt(2)).setText(finalBearing);
                        ((TextView) row.getChildAt(3)).setText(altUnits.toString(finalAlt));
                        ((TextView) row.getChildAt(4)).setText(finalTrack);
                        ((TextView) row.getChildAt(5)).setText(speedUnits.toString(finalSpeed));
                        ((TextView) row.getChildAt(6)).setText(vertSpeedUnits.toString(finalVVel));
                        row.setVisibility(View.VISIBLE);
                        row.postInvalidate();
                        //                       });
                    } catch (Exception ex) {
                        Log.e("Exception in getting tableAircraft row %d: %s", i, ex.getMessage());
                    }
                } else {
                    try {
                        TableRow row = new TableRow(tableAircraft.getContext());
                        row.setLayoutParams(tableAircraft.getChildAt(0).getLayoutParams()); // Copy layout from heading row
                        row.addView(view(v.getLabel()));
                        row.addView(view(distanceUnits.toString(distance)));
                        row.addView(view(bearing));
                        row.addView(view(altUnits.toString(alt)));
                        row.addView(view(track));
                        row.addView(view(speedUnits.toString(speed)));
                        row.addView(view(vertSpeedUnits.toString(vVel)));
//                        runOnUiThread(() ->
                        tableAircraft.addView(row);
                    } catch (Exception ex) {
                        Log.e("Exception in creating tableAircraft row %d: %s", i, ex.getMessage());
                    }
                }
            }

            // If the number of aircraft in range decreases, there will be some entries in tableAircraft that are out of date and should not be displayed
            for (; i < tableAircraft.getChildCount(); i++) {
                Log.i("Hide tablerow %d from %d", i, tableAircraft.getChildCount());
                TableRow row = (TableRow) tableAircraft.getChildAt(i);
                row.setVisibility(View.INVISIBLE);
            }


            //noinspection SynchronizeOnNonFinalField
            synchronized (tableAircraft) {
                tableAircraft.notifyAll();
            }
        } catch (Exception e) {
            Log.e("Exception: %s", e.toString());
        }
    }

    private TextView view(String txt) {
        TextView tv = new TextView(tableAircraft.getContext());
        tv.setText(txt);
        tv.setTextColor(Color.BLACK);
        tv.setGravity(Gravity.CENTER);
        return tv;
    }
}