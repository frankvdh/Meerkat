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

import static com.meerkat.SettingsActivity.wifiName;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.arch.core.executor.ArchTaskExecutor;

import com.meerkat.log.Log;

public class WifiConnection {
    private boolean available;
    private final Context context;
    public String ssId;

    public void waitForWifiConnection() {
        Log.d("Waiting for wifi connection: %s", ssId);
        while (!available) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public WifiConnection(Context context, String ssId, String password) {
        this.context = context;
        this.ssId = ssId;
        String activeSsid = null;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null && !connectionInfo.getSSID().isBlank()) {
                activeSsid = connectionInfo.getSSID().replaceAll("\"", "");
            }
        }
        if (activeSsid != null && activeSsid.equals(ssId)) {
            Log.i("Already connected to Wifi %s", ssId);
            cm.bindProcessToNetwork(cm.getActiveNetwork());
            available = true;
        } else {
            Log.i("Connecting to Wifi %s", ssId);


            WifiNetworkSpecifier.Builder builder = new WifiNetworkSpecifier.Builder().setSsid(ssId);
            if (!password.isBlank()) {
                builder.setWpa2Passphrase(password);
            }

            WifiNetworkSpecifier wifiNetworkSpecifier = builder.build();
            NetworkRequest.Builder networkRequestBuilder = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                    .setNetworkSpecifier(wifiNetworkSpecifier);
            NetworkRequest networkRequest = networkRequestBuilder.build();
            // bind so all api calls are performed over this new network
            ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    Log.i("Connected to wifi %s", ssId);
                    available = true;
                    // bind so all api calls are performed over this new network
                    cm.bindProcessToNetwork(network);
                }

                @Override
                public void onUnavailable() {
                    super.onUnavailable();
                    Log.i("Wifi unavailable: %s", ssId);
                    available = false;
                }
            };
            cm.requestNetwork(networkRequest, networkCallback);
        }
    }

    public void close() {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifi.disconnect();
        context.registerReceiver(new DisconnectWifi(), new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
    }

    public static class DisconnectWifi extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE) != SupplicantState.SCANNING)
                ((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).disconnect();
        }
    }

    @NonNull
    @Override
    public String toString() {
        return wifiName + ": " + (available ? "available" : "disconnected");
    }
}
