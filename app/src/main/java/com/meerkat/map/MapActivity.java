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
import static com.meerkat.SettingsActivity.load;
import static com.meerkat.SettingsActivity.port;
import static com.meerkat.SettingsActivity.simulate;
import static com.meerkat.SettingsActivity.wifiName;
import static java.lang.Thread.sleep;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.meerkat.AircraftListActivity;
import com.meerkat.Compass;
import com.meerkat.Gps;
import com.meerkat.R;
import com.meerkat.SettingsActivity;
import com.meerkat.Simulator;
import com.meerkat.databinding.ActivityFullscreenBinding;
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
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    static final int AUTO_HIDE_DELAY_MILLIS = 5000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    public static MapView mapView;
    private View actionbarView;

    final Handler hideHandler = new Handler(Looper.myLooper());
    private boolean actionbarVisible;
    final Runnable hideRunnable = this::hide;
    private static boolean firstRun = true;
    private Gps gps;
    private Compass compass;
    PingComms pingComms;

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        actionbarView.setVisibility(View.GONE);
        actionbarVisible = false;

        // Schedule removal of the status and navigation bar after a small delay
        hideHandler.removeCallbacks(showPart2);
        hideHandler.postDelayed(hidePart2, UI_ANIMATION_DELAY);
    }

    private final Runnable hidePart2 = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar
            if (Build.VERSION.SDK_INT >= 30) {
                mapView.getWindowInsetsController().hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            } else {
                // Note that some of these constants are new as of API 16 (Jelly Bean)
                // and API 19 (KitKat). It is safe to use them, as they are inlined
                // at compile-time and do nothing on earlier devices.
                mapView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
        }
    };

    private void show() {
        // Show the system bar
        if (Build.VERSION.SDK_INT >= 30) {
            mapView.getWindowInsetsController().show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        } else {
            mapView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        actionbarVisible = true;

        // Schedule a runnable to display UI elements after a delay
        hideHandler.removeCallbacks(hidePart2);
        hideHandler.postDelayed(showPart2, UI_ANIMATION_DELAY);
    }

    private final Runnable showPart2 = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            actionbarView.setVisibility(View.VISIBLE);
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        load(getApplicationContext());
        ActivityFullscreenBinding binding = ActivityFullscreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mapView = binding.mapView;
        actionbarVisible = true;
        actionbarView = binding.fullscreenContentControls;
        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        binding.btnSettings.setOnTouchListener(touchListenerDelayHide);
        binding.btnAircraftList.setOnTouchListener(touchListenerDelayHide);
        binding.btnLog.setOnTouchListener(touchListenerDelayHide);
        binding.btnQuit.setOnTouchListener(touchListenerDelayHide);

        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
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
                Log.i("Requesting permissions ");
                ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
                    for (String s : isGranted.keySet())
                        if (Boolean.TRUE.equals(isGranted.get(s))) {
                            Log.i("Permission granted: " + s);
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
                if (needed.isEmpty()) break;
                String[] x = {};
                requestPermissionLauncher.launch(needed.toArray(x));

                try {
                    //noinspection BusyWait
                    sleep(1000);
                } catch (InterruptedException e) {
                    // do nothing
                }
            } while (!needed.isEmpty());
            Log.level(Log.Level.D);
            Log.d("Permissions OK");

            gps = new Gps((LocationManager) this.getSystemService(LOCATION_SERVICE));
            compass = new Compass(getApplicationContext());

            Log.i("Connecting: %s", wifiName, port);
            if (wifiName == null) {
                Intent taskIntent = new Intent(this, SettingsActivity.class);
                this.startActivity(taskIntent);
            } else {
                // Already configured
                Log.i("Starting Ping comms: %s %d", wifiName, port);
                pingComms = new PingComms(getApplicationContext());
            }
            firstRun = false;
        }

        CompassView compassView = binding.compassView;
        TextView compassText = binding.compassText;
        compassText.setTop(compassView.getTop());
        compassText.setRight(compassView.getRight());
        compassText.setLeft(compassView.getLeft());
        compassText.setBottom(compassView.getBottom());
        compassText.setTranslationX(135);
        compassText.setTranslationY(135);
        compassText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Wait briefly before hiding the UI controls are available.
        delayedHide(10000);

        Background background = new Background(findViewById(R.id.mapView), findViewById(R.id.compassView), findViewById(R.id.compassText), findViewById(R.id.scaleText));
        mapView.layers.addLayer(background);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onResume() {
        super.onResume();
        compass.resume();
        if (simulate) {
            Simulator.startAll();
            firstRun = false;
            return;
        }

        if (fileLog && Environment.getExternalStorageState().equals(MEDIA_MOUNTED)) {
            File logFile = new File(this.getExternalFilesDir(null), "meerkat.log");
            try {
                Log.useFileWriter(logFile, appendLogFile);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        compass.pause();
    }

    @Override
    protected void onDestroy() {
        pingComms.stop();
        gps.pause();
        super.onDestroy();
    }

    private void toggle() {
        if (actionbarVisible) {
            hide();
        } else {
            show();
        }
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener touchListenerDelayHide = (view, motionEvent) -> {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (AUTO_HIDE) {
                    delayedHide(AUTO_HIDE_DELAY_MILLIS);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (view.getId() == R.id.btnSettings) {
                    Log.i("Click Settings");
                    Intent taskIntent = new Intent(this, SettingsActivity.class);
                    this.startActivity(taskIntent);
                    return true;
                }
                if (view.getId() == R.id.btnAircraftList) {
                    Log.i("Click Aircraft List");
                    Intent taskIntent = new Intent(this, AircraftListActivity.class);
                    this.startActivity(taskIntent);
                    return true;
                }
                if (view.getId() == R.id.btnLog) {
                    Log.i("Click Log");
                    Intent taskIntent = new Intent(this, LogActivity.class);
                    this.startActivity(taskIntent);
                    return true;
                }
                if (view.getId() == R.id.btnQuit) {
                    Log.i("Click Quit");
                    finish();
                    return true;
                }
                view.performClick();
                break;
            default:
                break;
        }
        return false;
    };

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        hideHandler.removeCallbacks(hideRunnable);
        hideHandler.postDelayed(hideRunnable, delayMillis);
    }

    // If the back button is pressed (or swipe right->left), display the toolbar
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            Log.i("back button pressed");
            if (AUTO_HIDE)
                show();
            else
                toggle();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}