package com.meerkat.ui.aircraft;

import static com.meerkat.log.Log.useLogWriter;
import static com.meerkat.log.Log.viewLogWriter;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.meerkat.Gps;
import com.meerkat.MainActivity;
import com.meerkat.Vehicle;
import com.meerkat.VehicleList;
import com.meerkat.databinding.FragmentAircraftBinding;
import com.meerkat.log.Log;
import com.meerkat.measure.Speed;

import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Stream;

public class AircraftFragment extends Fragment {

    private TableLayout tableAircraft;
    private Context context;
    private ScheduledFuture<?> task;
    private Activity uiActivity;

    public AircraftFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        FragmentAircraftBinding binding = FragmentAircraftBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        tableAircraft = binding.tableAircraft;
        this.context = getContext();
        uiActivity = this.getActivity();

        useLogWriter(viewLogWriter, false);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("AircraftFragment resumed");

        task = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::refreshAircraftDisplay, 1, 1, SECONDS);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i("AircraftFragment paused");
        task.cancel(true);
    }

    private void refreshAircraftDisplay() {
        try {
            Log.v("swapData: ", VehicleList.vehicleList.keySet().size());
            Stream<Vehicle> s = VehicleList.vehicleList.getVehicles().stream().sorted();
            int i = 1; // row 0 is header
            for (Iterator<Vehicle> it = s.iterator(); it.hasNext(); i++) {
                Vehicle v = it.next();
                Log.i("" + v.toString());
                float distance = Gps.location.distanceTo(v.current) / 1852;
                float track = v.current.getTrack();
                Speed speed = v.current.getSpeedUnits();
                float vVel = v.current.getVVel();
                if (i < tableAircraft.getChildCount()) {
                    TableRow row = (TableRow) tableAircraft.getChildAt(i);
                    uiActivity.runOnUiThread(() -> {
                        ((TextView) row.getChildAt(0)).setText(v.getLabel());
                        ((TextView) row.getChildAt(1)).setText(String.format(Locale.ENGLISH, distance < 10 ? "%.1f%s" : "%.0f%s", distance, "nm"));
                        ((TextView) row.getChildAt(2)).setText(String.format(Locale.ENGLISH, "%03.0f", (Gps.location.bearingTo(v.current) + 360) % 360));
                        ((TextView) row.getChildAt(3)).setText(String.format(Locale.ENGLISH, "%s", v.current.getAlt()));
                        ((TextView) row.getChildAt(4)).setText(Float.isNaN(track) ? "---" : String.format(Locale.ENGLISH, "%03.0f", track));
                        ((TextView) row.getChildAt(5)).setText(speed == null ? "----" : speed.toString());
                        ((TextView) row.getChildAt(6)).setText(vVel == 32256 /* NaN */ ? "----" : String.format(Locale.ENGLISH, "%.0f%s", vVel, "fpm"));
                        row.postInvalidate();
                    });
                } else {
                    TableRow row = new TableRow(context);
                    row.setLayoutParams(tableAircraft.getChildAt(0).getLayoutParams()); // Copy layout from heading row
                    row.addView(view(v.getLabel()));
                    row.addView(view(String.format(Locale.ENGLISH, distance < 10 ? "%.1f%s" : "%.0f%s", distance, "nm")));
                    row.addView(view(String.format(Locale.ENGLISH, "%03.0f", (Gps.location.bearingTo(v.current) + 360) % 360)));
                    row.addView(view(String.format(Locale.ENGLISH, "%s", v.current.getAlt())));
                    row.addView(view(Float.isNaN(track) ? "---" : String.format(Locale.ENGLISH, "%03.0f", track)));
                    row.addView(view(speed == null || Float.isNaN(speed.value) ? "----" : speed.toString()));
                    row.addView(view(Float.isNaN(vVel) ? "----" : String.format(Locale.ENGLISH, "%.0f%s", vVel, "fpm")));
                    uiActivity.runOnUiThread(() -> tableAircraft.addView(row));
                 }
            }
            // If the number of aircraft in range decreases, there will be some entries in tableAircraft that are out of date and should not be displayed
            for (int j = tableAircraft.getChildCount() - 1; j >= i; j--) {
                Log.i("Remove %d from %d", j, tableAircraft.getChildCount());
                int finalJ = j;
                uiActivity.runOnUiThread(() -> tableAircraft.removeViewAt(finalJ));
            }
        } catch (Exception ex) {
            Log.e("Exception in AircraftFragment scheduled task: " + ex.getMessage());
        }
    }

    private TextView view(String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextColor(Color.BLACK);
        tv.setGravity(Gravity.CENTER);
        return tv;
    }
}
