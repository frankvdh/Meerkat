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
package com.meerkat.wifi;

import static com.meerkat.ui.settings.SettingsViewModel.wifiName;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.rtt.RangingRequest;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.meerkat.databinding.ActivityWifiScanBinding;
import com.meerkat.log.Log;
import com.meerkat.ui.settings.SettingsViewModel;

import java.util.ArrayList;
import java.util.List;

public class WifiScanActivity extends AppCompatActivity implements ApListAdapter.ScanResultClickListener {
    private ApListAdapter myAdapter;
    private ArrayList<String> accessPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.meerkat.databinding.ActivityWifiScanBinding binding = ActivityWifiScanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.wifiRescanButton.setOnTouchListener(clickRescan);

        accessPoints = new ArrayList<>();
        myAdapter = new ApListAdapter(accessPoints, this);
        Log.i("Creating WifiScanActivity");
        wifiName = null;
        getApplicationContext().registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        RecyclerView apListView = binding.apListView;
        // Improve performance if you know that changes in content do not change the layout size of the RecyclerView
        apListView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(apListView.getContext());
        apListView.setLayoutManager(layoutManager);
        apListView.setAdapter(myAdapter);
        apListView.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show());
        scan();
    }

    private void scan() {
        Log.i("Scanning WiFi");
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        boolean success = wifiManager.startScan();
        if (!success) {
            Toast.makeText(getApplicationContext(), "No Wifi networks found", Toast.LENGTH_LONG).show();
        }
    }

    BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        // This is checked via mLocationPermissionApproved boolean
        @SuppressLint("NotifyDataSetChanged")
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)) {
                // success
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                @SuppressLint("MissingPermission") List<ScanResult> scanResults = wifiManager.getScanResults();
                Log.i("%d APs discovered.", scanResults.size());
                // User to select from available Wifi networks
                for (ScanResult scanResult : scanResults) {
                    @SuppressWarnings("RegExpRedundantEscape")
                    String ssid = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU ?
                            String.valueOf(scanResult.getWifiSsid()) :
                            scanResult.SSID).replaceAll("\\\"", "");
                    if (ssid.isEmpty()) continue;
                    accessPoints.add(ssid);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        if (accessPoints.size() >= RangingRequest.getMaxPeers()) break;
                    }
                }
                myAdapter.notifyDataSetChanged();
                Log.i("WiFi networks displayed");
            } else {
                Toast.makeText(getApplicationContext(), "No Wifi networks found", Toast.LENGTH_LONG).show();
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onScanResultItemClick(String scanResult) {
        wifiName = scanResult;
        Log.d("onScanResultItemClick(): ssid: %s", wifiName);

        // User has selected a Wifi SSID -- save it for future use
        SettingsViewModel.savePrefs();
        Log.i("Wifi changed to %s", wifiName);
        finish();
        // When MapActivity reloads, it will start PingComms up again with the new network
    }

    @SuppressLint("ClickableViewAccessibility")
    private final Button.OnTouchListener clickRescan = (view, motionEvent) -> {
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            Log.i("Click Rescan");
            myAdapter.clear();
            scan();
            view.performClick();
            return true;
        }
        view.performClick();
        return super.onTouchEvent(motionEvent);
    };

}