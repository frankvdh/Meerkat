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

import static com.meerkat.Settings.wifiName;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiNetworkSpecifier;

import androidx.annotation.NonNull;

import com.meerkat.log.Log;

public class WifiConnection {
    static private boolean available;
    static public String ssId;

    public static void waitForWifiConnection() {
        Log.i("Waiting for wifi connection: " + ssId);
        while (!available) {
            try {
                //noinspection BusyWait
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    public static void init(ConnectivityManager cm, String ssId, String password) {
        Log.i("Connecting to Wifi " + ssId);
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
                Log.i("Connected to wifi " + ssId);
                available = true;
                // bind so all api calls are performed over this new network
                cm.bindProcessToNetwork(network);
                if (PingComms.pingComms != null) PingComms.pingComms.start();
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                Log.i("Wifi unavailable: " + ssId);
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
