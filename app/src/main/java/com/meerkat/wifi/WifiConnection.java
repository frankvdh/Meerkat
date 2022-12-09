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

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.meerkat.log.Log;

public class WifiConnection {
    static private boolean available;
    static public String ssId;

    public static void waitForWifiConnection() {
        Log.i("Waiting for wifi connection: %s", ssId);
        while (!available) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static void init(Context context, String ssId, String password) {
        Log.i("Connecting to Wifi %s", ssId);
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiNetworkSpecifier.Builder builder = new WifiNetworkSpecifier.Builder().setSsid(ssId);
        if (password != null) {
                builder.setWpa2Passphrase(password);
        }

        WifiNetworkSpecifier wifiNetworkSpecifier = builder.build();
            NetworkRequest.Builder networkRequestBuilder = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                    .setNetworkSpecifier(wifiNetworkSpecifier);
        NetworkRequest networkRequest = networkRequestBuilder.build();
         cm.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                Log.i("Connected to wifi %s", ssId);
                available = true;
                // bind so all api calls are performed over this new network
                cm.bindProcessToNetwork(network);
                context.startService(new Intent(context, PingComms.class));
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                Log.i("Wifi unavailable: %s", ssId);
                available = false;
            }
        });
        WifiConnection.ssId = ssId;
        waitForWifiConnection();
    }

    @NonNull
    @Override
    public String toString() {
        return wifiName + ": " + (available ? "available" : "disconnected");
    }
}
