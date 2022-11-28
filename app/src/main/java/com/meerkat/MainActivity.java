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

import static android.os.Environment.MEDIA_MOUNTED;
import static com.meerkat.Settings.fileLog;
import static com.meerkat.Settings.port;
import static com.meerkat.Settings.simulate;
import static com.meerkat.Settings.wifiName;
import static com.meerkat.databinding.ActivityMainBinding.inflate;
import static com.meerkat.ui.map.AircraftLayer.loadIcon;
import static java.lang.Thread.sleep;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.Display;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.meerkat.databinding.ActivityMainBinding;
import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    static boolean firstRun = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Settings.load(this.getApplicationContext());

        ActivityMainBinding binding = inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_aircraft, R.id.navigation_map, R.id.navigation_log, R.id.navigation_wifi_connection).build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

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
                    sleep(100);
                } catch (InterruptedException e) {
                    // do nothing
                }
            } while (!needed.isEmpty());
            for (var emitterType : Gdl90Message.Emitter.values()) {
                loadIcon(getApplicationContext(), emitterType);
            }
            Log.level(Log.Level.D);
            Log.d("Permissions OK");

            new Gps((LocationManager) this.getSystemService(LOCATION_SERVICE));
            VehicleList.vehicleList = new VehicleList();

            if (simulate) {
                Simulator.startAll();
                firstRun = false;
                return;
            }

            if (fileLog && Environment.getExternalStorageState().equals(MEDIA_MOUNTED)) {
                File appSpecificExternalDir = new File(this.getExternalFilesDir(null), "meerkat.log");
                try {
                    Log.useFilePrinter(appSpecificExternalDir);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }

            Log.i("Connecting");
            ConnectivityManager connMgr = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (wifiName == null) {
                navView.setSelectedItemId(R.id.navigation_wifi_connection);
            } else {
                // Already configured
                WifiConnection.init(connMgr, wifiName, "");
                Log.i("Connected to Wifi %s", WifiConnection.ssId);
                PingComms.pingComms = new PingComms(port);

                PingComms.pingComms.start();
            }
            firstRun = false;
        }
    }

    public void onStop() {
        super.onStop();
        Log.i("Stop");
        Log.close();
    }
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("Request Permissions Result: %d", requestCode);
        for (int i = 0; i < permissions.length; i++)
            Log.d("%s -> %d", permissions[i], grantResults[i]);
    }
}