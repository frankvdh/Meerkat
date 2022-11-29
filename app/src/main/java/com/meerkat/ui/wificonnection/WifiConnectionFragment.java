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
package com.meerkat.ui.wificonnection;

import static com.meerkat.Settings.port;
import static com.meerkat.Settings.wifiName;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.rtt.RangingRequest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.meerkat.PingComms;
import com.meerkat.Settings;
import com.meerkat.WifiConnection;
import com.meerkat.databinding.FragmentWifiConnectionBinding;
import com.meerkat.log.Log;

import java.util.ArrayList;
import java.util.List;

public class WifiConnectionFragment extends Fragment implements ApListAdapter.ScanResultClickListener, MaterialButton.OnClickListener {

    private static ApListAdapter myAdapter;
    ArrayList<String> accessPoints;
    private Context context;
    private FragmentWifiConnectionBinding binding;

    public WifiConnectionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentWifiConnectionBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        this.context = getContext();
        accessPoints = new ArrayList<>();
        Button b = binding.scanWifiButton;
        b.setOnClickListener(this);
        b.setVisibility(View.VISIBLE);
        myAdapter = new ApListAdapter(accessPoints, this);
        RecyclerView apListView = binding.apListView;
        apListView.setVisibility(View.INVISIBLE);
        // Improve performance if you know that changes in content do not change the layout size of the RecyclerView
        apListView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(apListView.getContext());
        apListView.setLayoutManager(layoutManager);
        apListView.setAdapter(myAdapter);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("WifiConnectionFragment resumed");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i("WifiConnectionFragment paused");
    }

    @Override
    public void onClick(View v) {
        Button b = binding.scanWifiButton;
        b.setVisibility(View.INVISIBLE);
        RecyclerView apList = binding.apListView;
        apList.setVisibility(View.VISIBLE);
        Log.i("Scanning WiFi");
        wifiName = null;
        if (PingComms.pingComms != null)
            PingComms.pingComms.stop();
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        context.registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        boolean success = wifiManager.startScan();
        if (!success) {
            Toast.makeText(getContext(), "No Wifi networks found", Toast.LENGTH_LONG).show();
        }
    }

    BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        // This is checked via mLocationPermissionApproved boolean
        @SuppressLint("NotifyDataSetChanged")
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)) {
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                @SuppressLint("MissingPermission") List<ScanResult> scanResults = wifiManager.getScanResults();
                Log.i("%d APs discovered.", scanResults.size());
                // User to select from available Wifi networks
                for (ScanResult scanResult : scanResults) {
                    if (scanResult.SSID.isEmpty()) continue;
                    accessPoints.add(scanResult.SSID);
                    if (accessPoints.size() >= RangingRequest.getMaxPeers()) break;
                }
                myAdapter.notifyDataSetChanged();
                Log.i("Select the ADS-B device WiFi network");
            } else {
                Toast.makeText(getContext(), "No Wifi networks found", Toast.LENGTH_LONG).show();
            }
         }
    };

    @Override
    public void onScanResultItemClick(String scanResult) {
        wifiName = scanResult;
        Log.d("onScanResultItemClick(): ssid: %s", wifiName);

        // User has selected a Wifi SSID -- save it for future use, and connect to it
        Settings.save();
        binding.apListView.setVisibility(View.INVISIBLE);
        Log.i("Connecting to " + wifiName);
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiConnection.init(connMgr, wifiName, "");
        Log.i("Starting Ping comms");
        PingComms.pingComms = new PingComms(port);
        // Configured, but not connected to Wifi, or connected to the wrong Wifi
        PingComms.pingComms.start();
    }
}
