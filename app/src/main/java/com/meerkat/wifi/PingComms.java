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

import static com.meerkat.SettingsActivity.logDecodedMessages;
import static com.meerkat.SettingsActivity.logRawMessages;
import static com.meerkat.SettingsActivity.port;
import static com.meerkat.SettingsActivity.wifiName;

import android.app.Service;
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
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.gdl90.Traffic;
import com.meerkat.log.Log;
import com.meerkat.map.MapActivity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

public class PingComms extends Service {
    private final SocketThread thread;
    private Context context;
public static PingComms instance;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public PingComms(Context context) {
        this.context = context;
        Log.i("constructor");
        thread = new SocketThread();
        if (!connectToExistingWifi(wifiName))
            startWifi(wifiName, null);
        instance = this;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    // Used to restart the service if network is changed
    public PingComms() {
        Log.i("constructor");
        thread = new SocketThread();
        if (!connectToExistingWifi(wifiName))
            startWifi(wifiName, null);
        instance = this;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public boolean connectToExistingWifi(String ssId) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) return false;
        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
        if (connectionInfo == null || connectionInfo.getSSID().isBlank()) return false;
        String activeSsid = connectionInfo.getSSID().replaceAll("\"", "");
        if (!activeSsid.equals(ssId)) return false;
        Log.i("Already connected to Wifi %s", ssId);
        // bind so all api calls are performed over this network
        cm.bindProcessToNetwork(cm.getActiveNetwork());
        start();
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void startWifi(String ssId, @SuppressWarnings("SameParameterValue") @Nullable String password) {
        WifiNetworkSpecifier.Builder builder = new WifiNetworkSpecifier.Builder().setSsid(ssId);
        if (password != null && !password.isBlank()) {
            builder.setWpa2Passphrase(password);
        }

        WifiNetworkSpecifier wifiNetworkSpecifier = builder.build();
        NetworkRequest request =
                new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                        .setNetworkSpecifier(wifiNetworkSpecifier)
                        .build();
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                if (wifiName.equals(activeNetworkName())) {
                    Log.i("Connected to wifi %s", ssId);
                    // bind so all api calls are performed over this new network
                    cm.bindProcessToNetwork(network);
                } else {
                    // Connected to some other Wifi network. Try to go back to the selected one
                    disconnect();
                    startWifi(wifiName, null);
                }
                start();
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                stop();
                Log.i("Wifi unavailable: %s", ssId);
            }
        };
        cm.registerNetworkCallback(request, networkCallback);
    }

    public String activeNetworkName() {
        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
        if (connectionInfo == null || connectionInfo.getSSID().isBlank())
            return "Unidentified network";
        return connectionInfo.getSSID().replaceAll("\"", "");
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void disconnect() {
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

    public void stop() {
        Log.i("Stopping Ping comms");
        if (thread.isAlive())
            thread.interrupt();
        instance = null;
    }

    private void start() {
        if (!thread.isAlive()) {
            try {
                thread.start();
            } catch (IllegalThreadStateException e) {
                throw new RuntimeException("Thread illegal state");
            }
        }
    }

    private static class SocketThread extends Thread {
        private DatagramSocket recvSocket;
        private final DatagramPacket recvDatagram;
        // For handling retries
        private final RetryOnException retryHandler;

        private SocketThread() {
            retryHandler = new RetryOnException(10, 1000);
            byte[] recvBuffer = new byte[132];
            recvDatagram = new DatagramPacket(recvBuffer, recvBuffer.length);
            while (true) {
                try {
                    recvSocket = new DatagramSocket(port);
                    recvSocket.setSoTimeout(10000);
                    recvSocket.setBroadcast(true);
                    recvSocket.setReuseAddress(true);
                    retryHandler.reset();
                    break;
                } catch (IOException ex) {
                    Log.w("Socket create IO Exception: %s", ex);
                    // Catch exception and retry.
                    try {
                        // If beyond retry limit, this will throw an exception.
                        retryHandler.exceptionOccurred();
                        if (recvSocket != null)
                            recvSocket.close();
                        recvSocket = null;
                    } catch (Exception fatal) {
                        Log.a("Socket create failed: %s", fatal.getMessage());
                        throw new RuntimeException(fatal);
                    }
                }
            }
        }

        private boolean interrupted = false;

        @Override
        public void interrupt() {
            try {
                interrupted = true;
                retryHandler.disable();
                recvSocket.close();
            } finally {
                super.interrupt();
            }
        }

        @Override
        public void run() {
            Log.d("receiveData");
            while (recvSocket != null) {  // Loop is exited via an interrupt which closes the socket
                try {
                    //                     if (recvSocket == null) return;
                    Log.v("Waiting for next datagram: %s", recvSocket.isBound());
                    // Blocks until a message returns on this socket from a remote host.
                    recvSocket.receive(recvDatagram);
                } catch (IOException e) {
                    if (interrupted) {
                        break;
                    }
                    try {
                        retryHandler.exceptionOccurred();
                    } catch (Exception fatal) {
                        Log.a("Socket read IO Exception: %s", fatal.getMessage());
                        recvSocket.close();
                        recvSocket = null;
                    }
                    continue;
                }

                // Successfully received a message
                retryHandler.reset();
                var numBytes = recvDatagram.getLength();
                Log.v("received datagram %d bytes", numBytes);
                byte[] packet = Arrays.copyOfRange(recvDatagram.getData(), 0, numBytes);
                if (logRawMessages) {
                    StringBuilder sb = new StringBuilder();
                    for (byte b : packet)
                        sb.append(String.format("%02x", b));
                    Log.i(sb.toString());
                }
                ByteArrayInputStream is = new ByteArrayInputStream(packet);
                long now = System.currentTimeMillis();
                while (is.available() > 0) {
                    Gdl90Message message = Gdl90Message.getMessage(is, now);
                    if (message == null) continue;
                    if (logDecodedMessages)
                        Log.i(message.toString());
                    if (message instanceof Traffic) {
                        Traffic traffic1 = (Traffic) message;
                        if (traffic1.callsign.equals("********") || traffic1.point.getLatitude() == 0 && traffic1.point.getLongitude() == 0)
                            continue;
                        MapActivity.vehicleList.upsert(traffic1.callsign, traffic1.participantAddr, traffic1.point, traffic1.emitterType);
                    }
                }
            }
            Log.i("Socket thread stopped");
            if (recvSocket != null)
                recvSocket.close();
            recvSocket = null;
        }
    }


    /**
     * Encapsulates retry-on-exception operations
     */
    private static class RetryOnException {
        private int numRetries;
        private final int maxRetries, timeToWaitMS;

        @SuppressWarnings("SameParameterValue")
        RetryOnException(int maxRetries, int timeToWaitMS) {
            this.numRetries = this.maxRetries = maxRetries;
            this.timeToWaitMS = timeToWaitMS;
        }

        void reset() {
            this.numRetries = maxRetries;
        }

        void disable() {
            this.numRetries = 0;
        }

        /**
         * @return True if retries attempts remain; else false
         */
        boolean shouldRetry() {
            return (numRetries >= 0);
        }

        /**
         * Waits for timeToWaitMS. Ignores any interrupted exception
         */
        public void waitUntilNextTry() {
            try {
                Thread.sleep(timeToWaitMS);
            } catch (InterruptedException iex) {
                // Do nothing
            }
        }

        /**
         * Called when an exception has occurred in the block. If the
         * retry limit is exceeded, throws an exception.
         * Else waits for the specified time.
         */
        public void exceptionOccurred() throws Exception {
            numRetries--;
            if (!shouldRetry()) {
                throw new Exception("Retry limit exceeded.");
            }
            waitUntilNextTry();
        }
    }
}

