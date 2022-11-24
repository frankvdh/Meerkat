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

import static com.meerkat.Settings.logDecodedMessages;
import static com.meerkat.Settings.logRawMessages;

import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.gdl90.Traffic;
import com.meerkat.log.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.Date;

public class PingComms {
    private final Thread thread;
    private final int port;
    public static long lastVehicleChange;
    private boolean stopped = false;
    static public PingComms pingComms;

    public PingComms(final int port) {
        Log.i("constructor");
        this.port = port;
        thread = new Thread(receiveData);
    }

    public void start() {
        Log.i("start");
        stopped = false;
        try {
            thread.start();
        } catch (IllegalThreadStateException e) {
            // do nothing
        }
    }

    public void stop() {
        Log.i("stop");
        stopped = true;
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
                try {
                    if (recvSocket == null) {
                        recvSocket = new DatagramSocket(port);
                    }
                    recvSocket.setSoTimeout(10000);
                    recvSocket.setBroadcast(true);
                    recvSocket.setReuseAddress(true);
                } catch (IOException e) {
                    Log.e("IO Exception: " + e);
                    recvSocket = null;
                    continue;
                }

                while (!stopped && recvSocket != null) {
                    try {
                        //                     if (recvSocket == null) return;
                        Log.v("Waiting for next datagram: " + recvSocket.isBound());
                        // Blocks until a message returns on this socket from a remote host.
                        recvSocket.receive(recvDatagram);
                        Log.v("received datagram " + recvDatagram.getLength() + " bytes");
                        byte[] packet = Arrays.copyOfRange(recvDatagram.getData(), 0, recvDatagram.getLength());
                        if (logRawMessages) {
                            StringBuilder sb = new StringBuilder();
                            for (byte b : packet)
                                sb.append(String.format("%02x", b));
                            Log.i(sb.toString());
                        }
                        ByteArrayInputStream is = new ByteArrayInputStream(packet);
                        long now = new Date().getTime();
                        while (is.available() > 0) {
                            Gdl90Message message = Gdl90Message.getMessage(is, now);
                            if (message == null) continue;
                            if (logDecodedMessages)
                                Log.i(message.toString());
                            if (message instanceof Traffic) {
                                Traffic traffic1 = (Traffic) message;
                                if (traffic1.callsign.equals("********") || traffic1.point.getLatitude() == 0 && traffic1.point.getLongitude() == 0)
                                    continue;
                                lastVehicleChange = now;
                                VehicleList.vehicleList.upsert(traffic1.callsign, traffic1.participantAddr, traffic1.point, traffic1.emitterType);
                            }
                        }
                    } catch (IOException e) {
                        Log.e("IO Exception: " + e);
                        recvSocket = null;
                    }
                }
                Log.i("Closing socket");
                if (recvSocket != null && !recvSocket.isClosed())
                    recvSocket.close();
            }
        }
    };
}