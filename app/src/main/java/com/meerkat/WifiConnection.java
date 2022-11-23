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
    private boolean available;

    public WifiConnection() {
        while (!available) {
            try {
                //noinspection BusyWait
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }

    }

    public WifiConnection(ConnectivityManager cm, String ssId, String password) {
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
        Log.i("Waiting for wifi connection: " + ssId);

        while (!available) {
            try {
                //noinspection BusyWait
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                // do nothing
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        return wifiName + ": " + (available ? "available" : "disconnected");
    }
}
