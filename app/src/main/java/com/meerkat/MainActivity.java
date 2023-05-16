package com.meerkat;

import static android.os.Environment.MEDIA_MOUNTED;
import static com.meerkat.log.Log.useFileWriter;
import static com.meerkat.ui.settings.SettingsViewModel.appendLogFile;
import static com.meerkat.ui.settings.SettingsViewModel.fileLog;
import static com.meerkat.ui.settings.SettingsViewModel.keepScreenOn;
import static com.meerkat.ui.settings.SettingsViewModel.loadPrefs;
import static com.meerkat.ui.settings.SettingsViewModel.logReplay;
import static com.meerkat.ui.settings.SettingsViewModel.port;
import static com.meerkat.ui.settings.SettingsViewModel.simulate;
import static com.meerkat.ui.settings.SettingsViewModel.useCupFile;
import static com.meerkat.ui.settings.SettingsViewModel.wifiName;
import static java.lang.Thread.sleep;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.meerkat.databinding.ActivityMainBinding;
import com.meerkat.log.Log;
import com.meerkat.map.Cup;
import com.meerkat.map.CupFile;
import com.meerkat.map.MapView;
import com.meerkat.wifi.PingComms;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    private static boolean firstRun = true;
    public static Gps gps;
    public static Compass compass;
    PingComms pingComms;
    public static VehicleList vehicleList;
    public static MapView mapView;
    public static ArrayList<Cup> groundLocations;
    private static Button gpsButton, adsbButton, hdgButton, modecButton, alertButton;
    private static MainActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        instance = this;
        super.onCreate(savedInstanceState);
        Log.level(Log.Level.I);
        loadPrefs(getApplicationContext());

        if (fileLog && Environment.getExternalStorageState().equals(MEDIA_MOUNTED)) {
            File logFile = new File(this.getExternalFilesDir(null), "meerkat.log");
            useFileWriter(logFile, appendLogFile);
        }
        Log.i("Starting in %s mode", simulate ? "Simulation" : logReplay ? "Log Replay" : "Live");

        groundLocations = useCupFile ? CupFile.readAll(new File(this.getExternalFilesDir(null), "waypoints.cup")) : new ArrayList<>();
        com.meerkat.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.getRoot().setKeepScreenOn(keepScreenOn);

        BottomNavigationView navView = findViewById(R.id.nav_menu);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(navView, navController);

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

        getPermissions();

        gpsButton = findViewById(R.id.gpsstatus);
        adsbButton = findViewById(R.id.adsbstatus);
        hdgButton = findViewById(R.id.hdgstatus);
        modecButton = findViewById(R.id.modecstatus);
        alertButton = findViewById(R.id.alertstatus);
        gps = new Gps(getApplicationContext());
        compass = new Compass(getApplicationContext());
        vehicleList = new VehicleList();

        if (firstRun) {

            if (logReplay) {
                try {
                    new LogReplay(vehicleList, new File(this.getExternalFilesDir(null), "meerkat.save.log")).start();
                } catch (Exception e) {
                    Log.e("Log replay exception: %s", e.getMessage());
                }
                return;
            }
            if (simulate) {
                try {
                    Simulator.startAll(vehicleList);
                } catch (Exception e) {
                    Log.e("Simulator exception: %s", e.getMessage());
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

        if (!(logReplay || simulate)) {
            Log.i("Connecting: %s", wifiName, port);
            if (wifiName == null) {
                var navSettings = findViewById(R.id.navigation_settings);
                navSettings.setPressed(true);
            } else {
                // Already configured
                if (pingComms == null)
                    pingComms = new PingComms(getApplicationContext());
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

    private void getPermissions() {
        Log.i("Checking permissions");
        String[] permissions = {android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_WIFI_STATE, android.Manifest.permission.CHANGE_WIFI_STATE,
                android.Manifest.permission.ACCESS_NETWORK_STATE, android.Manifest.permission.CHANGE_NETWORK_STATE, Manifest.permission.INTERNET
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
            for (var n : needed) {
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
    }

    public static void blinkGps() {
        instance.runOnUiThread(() -> gpsButton.setPressed(!gpsButton.isPressed()));
    }

    public static void blinkAdsb() {
        instance.runOnUiThread(() -> adsbButton.setPressed(!adsbButton.isPressed()));
    }

    public static void setAdsb(boolean set, String msg, int len) {
        instance.runOnUiThread(() -> {
            adsbButton.setPressed(set);
            Toast.makeText(instance.getApplicationContext(), msg, len).show();
        });
    }

    public static void blinkHdg() {
        instance.runOnUiThread(() -> hdgButton.setPressed(!hdgButton.isPressed()));
    }

    public static void setModeC(int colour) {
        instance.runOnUiThread(() -> modecButton.setBackgroundColor(colour));
    }

    public static void setAlert(boolean set) {
        instance.runOnUiThread(() -> alertButton.setBackgroundColor(set ? Color.RED : Color.TRANSPARENT));
    }

    public void quitClick(View v) {
        Log.i("quit", "click");
        finishAndRemoveTask();
    }
}