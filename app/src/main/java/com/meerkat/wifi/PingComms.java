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

import static com.meerkat.ui.settings.SettingsViewModel.logDecodedMessages;
import static com.meerkat.ui.settings.SettingsViewModel.logRawMessages;
import static com.meerkat.ui.settings.SettingsViewModel.port;
import static com.meerkat.ui.settings.SettingsViewModel.wifiName;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.meerkat.MainActivity;
import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.gdl90.Traffic;
import com.meerkat.log.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.time.Instant;
import java.util.Arrays;

public class PingComms extends Service {
    private SocketThread thread;
    private final Context context;
    private String currentWifiName;
    private int currentPort;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public PingComms(Context context) {
        this.context = context;
        Log.i("PingComms constructor");
        if (!connectToExistingWifi(wifiName))
            startWifi(wifiName, null);
        currentWifiName = wifiName;
        currentPort = port;
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
        Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) return false;
        NetworkCapabilities actNw = cm.getNetworkCapabilities(activeNetwork);
        if (actNw == null || !actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return false;
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
        if (thread.isAlive()) {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e) {
                // Do nothing
            }
            thread = null;
        }
    }

    public void start() {
        if (currentWifiName != null && (currentPort != port || !currentWifiName.equals(wifiName))) {
            stop();
            currentWifiName = wifiName;
            currentPort = port;
        }

        if (thread == null) {
            thread = new SocketThread();
            try {
                thread.start();
            } catch (IllegalThreadStateException e) {
                throw new RuntimeException("Thread illegal state: " + e.getMessage());
            }
        }
        if (!thread.isAlive()) {
            Log.i("Starting Ping comms");
            try {
                thread.start();
            } catch (IllegalThreadStateException e) {
                throw new RuntimeException("Thread illegal state: " + e.getMessage());
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
                        MainActivity.setAdsb(false, "Failed to connect to " + wifiName, Toast.LENGTH_LONG);
                        throw new RuntimeException(fatal);
                    }
                }
            }
        }

        private boolean interrupted = false;

        @Override
        public void interrupt() {
            Log.v("Interrupt");
            try {
                interrupted = true;
                retryHandler.disable();
                recvSocket.close();
                recvSocket = null;
            } finally {
                super.interrupt();
            }
        }

        @Override
        public void run() {
            MainActivity.setAdsb(true, "Wifi connection to " + wifiName + " started", Toast.LENGTH_SHORT);
            Log.d("receiveData");
            while (recvSocket != null) {  // Loop is exited via an interrupt which closes the socket
                try {
                    //                     if (recvSocket == null) return;
                    Log.v("Waiting for next datagram: %s", recvSocket.isBound());
                    // Blocks until a message returns on this socket from a remote host.
                    recvSocket.receive(recvDatagram);
                } catch (IOException e) {
                    Log.v("Interrupted");
                    if (interrupted) {
                        break;
                    }
                    try {
                        Log.v("Retry");
                        retryHandler.exceptionOccurred();
                    } catch (Exception fatal) {
                        Log.a("Socket read IO Exception: %s", fatal.getMessage());
                        recvSocket.close();
                        recvSocket = null;
                        MainActivity.setAdsb(false, "Wifi connection to " + wifiName + " lost", Toast.LENGTH_LONG);
                    }
                    continue;
                }

                // Successfully received a message
                retryHandler.reset();
                var numBytes = recvDatagram.getLength();
                Log.v("received datagram %d bytes", numBytes);
                byte[] packet = Arrays.copyOfRange(recvDatagram.getData(), 0, numBytes);
                if (logRawMessages) {
                    StringBuilder sb = new StringBuilder("GDL90 ");
                    for (byte b : packet)
                        sb.append(String.format("%02x", b));
                    Log.i(sb.toString());
                }
                ByteArrayInputStream is = new ByteArrayInputStream(packet);
                while (is.available() > 0) {
                    Gdl90Message message = Gdl90Message.getMessage(is);
                    if (message == null) continue;
                    if (logDecodedMessages)
                        Log.i(message.toString());
                    if (message instanceof Traffic traffic1) {
                        if (traffic1.callsign.equals("********") || traffic1.point.getLatitude() == 0 && traffic1.point.getLongitude() == 0)
                            continue;
                        traffic1.point.setTime(Instant.now().toEpochMilli());
                        traffic1.upsert(MainActivity.vehicleList);
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

