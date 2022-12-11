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
import static com.meerkat.SettingsActivity.wifiName;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.RequiresApi;

import com.meerkat.SettingsActivity;
import com.meerkat.VehicleList;
import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.gdl90.Traffic;
import com.meerkat.log.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class PingComms {
    private final int port;
    private WifiConnection wifiConnection = null;
    private boolean stopped = true;
    private final Thread thread;

     @RequiresApi(api = Build.VERSION_CODES.Q)
     public PingComms(Context context) {
        Log.i("constructor %s", stopped ? "stopped" : "running");
        assert stopped: "PingComms started while already running";

        if (wifiConnection == null) {
            wifiConnection = new WifiConnection(context, wifiName, "");
            wifiConnection.waitForWifiConnection();
        }
        this.port = SettingsActivity.port;
        thread = new Thread(receiveData);
        stopped = false;
        try {
            thread.start();
        } catch (IllegalThreadStateException e) {
            throw new RuntimeException("Thread illegal state");
        }
    }

    public void stop() {
        Log.i("Stopping Ping comms");
        stopped = true;
        try {
            if (thread != null)
                thread.join(1000);
        } catch (InterruptedException e) {
            // Do nothing
        }
        if (wifiConnection != null) {
            wifiConnection.close();
            wifiConnection = null;
        }
    }

    final Runnable receiveData = new Runnable() {
        @Override
        public void run() {
            Log.d("receiveData");
            DatagramSocket recvSocket = null;
            byte[] recvBuffer = new byte[132];
            DatagramPacket recvDatagram = new DatagramPacket(recvBuffer, recvBuffer.length);
            while (!stopped) {
                Log.d("init socket");
                // For handling retries
                RetryOnException retryHandler = new RetryOnException(120, 1000);

                while (!stopped) {
                    wifiConnection.waitForWifiConnection();
                    try {
                        recvSocket = new DatagramSocket(port);
                        recvSocket.setSoTimeout(10000);
                        recvSocket.setBroadcast(true);
                        recvSocket.setReuseAddress(true);
                        retryHandler.reset();
                        break;
                    } catch (IOException ex) {
                        // Catch exception and retry.
                        // If beyond retry limit, this will throw an exception.
                        try {
                            retryHandler.exceptionOccurred();
                            if (recvSocket != null)
                                recvSocket.close();
                            recvSocket = null;
                        } catch (Exception fatal) {
                            Log.a("Socket create IO Exception: %s", fatal.getMessage());
                            throw new RuntimeException(fatal);
                        }
                    }
                }
                while (!stopped && recvSocket != null) {
                    try {
                        //                     if (recvSocket == null) return;
                        Log.v("Waiting for next datagram: %s", recvSocket.isBound());
                        // Blocks until a message returns on this socket from a remote host.
                        recvSocket.receive(recvDatagram);
                        retryHandler.reset();
                        Log.v("received datagram %d bytes", recvDatagram.getLength());
                        byte[] packet = Arrays.copyOfRange(recvDatagram.getData(), 0, recvDatagram.getLength());
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
                                VehicleList.vehicleList.upsert(traffic1.callsign, traffic1.participantAddr, traffic1.point, traffic1.emitterType);
                            }
                        }
                    } catch (IOException e) {
                        try {
                            retryHandler.exceptionOccurred();
                        } catch (Exception fatal) {
                            Log.a("Socket read IO Exception: %s", fatal.getMessage());
                            recvSocket.close();
                            recvSocket = null;
                        }
                    }
                }
                Log.i("Socket thread stopped");
                if (recvSocket != null)
                    recvSocket.close();
                recvSocket = null;
            }
        }
    };

    /**
     * Encapsulates retry-on-exception operations
     */
    private static class RetryOnException {
        private int numRetries;
        private final int maxRetries, timeToWaitMS;

        RetryOnException(int maxRetries, int timeToWaitMS) {
            this.numRetries = this.maxRetries = maxRetries;
            this.timeToWaitMS = timeToWaitMS;
        }

        void reset() {
            this.numRetries = maxRetries;
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

