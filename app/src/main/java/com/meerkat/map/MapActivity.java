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

import static android.os.Environment.MEDIA_MOUNTED;
import static com.meerkat.SettingsActivity.appendLogFile;
import static com.meerkat.SettingsActivity.fileLog;
import static com.meerkat.SettingsActivity.initToolbarDelayMilliS;
import static com.meerkat.SettingsActivity.loadPrefs;
import static com.meerkat.SettingsActivity.port;
import static com.meerkat.SettingsActivity.simulate;
import static com.meerkat.SettingsActivity.wifiName;
import static java.lang.Thread.sleep;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.meerkat.AircraftListActivity;
import com.meerkat.Compass;
import com.meerkat.Gps;
import com.meerkat.LogReplay;
import com.meerkat.R;
import com.meerkat.SettingsActivity;
import com.meerkat.VehicleList;
import com.meerkat.databinding.ActivityMapBinding;
import com.meerkat.log.Log;
import com.meerkat.log.LogActivity;
import com.meerkat.wifi.PingComms;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Full-screen activity that shows and hides the status bar and navigation/system bar.
 */
public class MapActivity extends AppCompatActivity {

    private static boolean firstRun = true;
    private Gps gps;
    private Compass compass;
    PingComms pingComms;
    private ActionBar actionBar;
    private static VehicleList vehicleList;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadPrefs(getApplicationContext());

        if (fileLog && Environment.getExternalStorageState().equals(MEDIA_MOUNTED)) {
            File logFile = new File(this.getExternalFilesDir(null), "meerkat.log");
            Log.useFileWriter(logFile, appendLogFile);
        }
        Log.i("Starting in %s mode", simulate ? "Simulation" : "Live");

        ActivityMapBinding binding = ActivityMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // get generic toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        actionBar = getSupportActionBar();
        assert actionBar != null : "Action bar not found";

        // Set up custom layout
        //      actionBar.setCustomView(R.layout.actionbar_layout);
        //      actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        //      actionBar.setDisplayShowCustomEnabled(true);
        showActionbar(initToolbarDelayMilliS);

        Display display =
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R ?
                        this.getDisplay() :
                        ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        int orientation = display.getRotation();
        if (orientation == 0) {
            Log.i("Portrait upright");
        } else if (orientation == 1) {
            /* The device is rotated to the right. */
            Log.i("Landscape Left");
        } else if (orientation == 2) {
            /* The device is rotated to the left. */
            Log.i("Portrait inverted");
        } else if (orientation == 3) {
            /* The device is rotated to the right. */
            Log.i("Landscape Right");
        } else {
            Log.i("Orientation: %s", orientation);
        }
        if (firstRun) {
            Log.level(Log.Level.I);
            Log.i("Checking permissions");
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.CHANGE_NETWORK_STATE, Manifest.permission.INTERNET
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE not needed after API 19
            };
            ArrayList<String> needed = new ArrayList<>();
            do {
                needed.clear();
                for (String s : permissions) {
                    if (this.checkSelfPermission(s) != PackageManager.PERMISSION_GRANTED) {
                        needed.add(s);
                    }
                }
                if (needed.isEmpty()) break;
                // Handle the user's response to the system permissions dialog. Save the return value, an instance of
                // ActivityResultLauncher, as an instance variable.
                Log.i("Need permissions: %s", String.join(", ", needed));
                ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
                    for (String s : isGranted.keySet())
                        if (Boolean.TRUE.equals(isGranted.get(s))) {
                            Log.i("Permission granted: %s", s);
                            Iterator<String> it = needed.iterator();
                            while (it.hasNext()) {
                                String n = it.next();
                                if (n.equals(s)) {
                                    it.remove();
                                    break;
                                }
                            }
                        } else {
                            Log.i("Permission denied: " + s);
                            throw new RuntimeException("Permission denied");
                            // Explain to the user that the feature is unavailable because the
                            // features requires a permission that the user has denied. At the
                            // same time, respect the user's decision. Don't link to system
                            // settings in an effort to convince the user to change their
                            // decision.
                        }
                });
                // Can request only one set of permissions at a time with a single request
                for (var n: needed) {
                    requestPermissionLauncher.launch(new String[]{n});
                }
                if (needed.isEmpty()) break;

                try {
                    //noinspection BusyWait
                    sleep(1000);
                } catch (InterruptedException e) {
                    // do nothing
                }
            } while (!needed.isEmpty());
            Log.level(Log.Level.D);
            Log.d("Permissions OK");

            vehicleList = new VehicleList(binding.mapView);
            gps = new Gps(getApplicationContext(), binding.mapView);
            compass = new Compass(getApplicationContext(), binding.mapView);
            CompassView compassView = binding.compassView;
            compassView.setMap(binding.mapView, binding.compassText);
            Background background = new Background(binding.mapView, vehicleList, binding.compassView, binding.compassText, binding.scaleText);
            binding.mapView.layers.addLayer(background);

            if (simulate) {
                try {
                    new LogReplay(vehicleList, new File(this.getExternalFilesDir(null), "meerkat.save.log")).start();
                } catch (IOException e) {
                    Log.e("Log replay file error: %s", "meerkat.save.log");
                }
                return;
            }
            firstRun = false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onResume() {
        Log.i("Resume");
        super.onResume();
        if (compass != null)
            compass.resume();

        if (!simulate) {
            Log.i("Connecting: %s", wifiName, port);
            if (wifiName == null) {
                this.startActivity(new Intent(this, SettingsActivity.class));
            } else {
                // Already configured
                if (pingComms == null)
                    pingComms = new PingComms(getApplicationContext(), vehicleList);
                Log.i("Starting Ping comms: %s %d", wifiName, port);
                pingComms.start();
            }
        }
    }

    @Override
    protected void onPause() {
        Log.i("Pause");
        super.onPause();
        if (compass != null)
            compass.pause();
    }

    @Override
    protected void onDestroy() {
        Log.i("Destroy");
        if (!isFinishing())
            finish();
        if (pingComms != null)
            pingComms.stop();
        if (gps != null)
            gps.pause();
        Log.close();
        super.onDestroy();
    }

    // If the back button is pressed (or swipe right->left), display the toolbar
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //       Log.i("key code %d, vis %d", keyCode, actionbarView.getVisibility());
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (actionBar.isShowing()) {
                actionBar.hide();
            } else {
                showActionbar(SettingsActivity.toolbarDelayMilliS);
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Schedules a call to hide() in milliseconds, canceling any previously scheduled calls.
     */
    @SuppressWarnings("SameParameterValue")
    void delayedHide(@SuppressWarnings("SameParameterValue") int delayMillis) {
        hideHandler.removeCallbacks(hideRunnable);
        hideHandler.postDelayed(hideRunnable, delayMillis);
    }

    Handler hideHandler = new Handler(Looper.myLooper());
    Runnable hideRunnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            actionBar.hide();
        }
    };

    void showActionbar(int visibleTime) {
        actionBar.show();
        delayedHide(visibleTime);
    }

    // Inflate the options menu when the user opens the menu for the first time
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Log.i("Click Settings");
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.action_aircraft_list:
                Log.i("Click Aircraft List");
                startActivity(new Intent(this, AircraftListActivity.class));
                return true;

            case R.id.action_log:
                Log.i("Click Log");
                startActivity(new Intent(this, LogActivity.class));
                return true;

            case R.id.action_quit:
                Log.i("Click Quit");
                this.finish();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Let the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    public static VehicleList getVehicleList() {
        return vehicleList;
    }
}