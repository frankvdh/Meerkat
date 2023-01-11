package com.meerkat;

import static com.meerkat.SettingsActivity.simulate;
import static com.meerkat.SettingsActivity.simulateSpeedFactor;

import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.gdl90.Traffic;
import com.meerkat.log.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogReplay extends Thread {
    private final VehicleList vehicleList;
    private final BufferedReader logReader;
    private Instant prev = null;
    private Instant prevRealtime = null;
    static final Pattern rawPattern = Pattern.compile("(.*?\\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\dZ)\\s(PingComms.*?)/[AEWIVD]\\sRAW\\s(.*)");

    public LogReplay(VehicleList v, File logFile) throws IOException {
        this.vehicleList = v;
        Log.level(Log.Level.D);
        Log.i("new LogReplay");
        logReader = new BufferedReader(new FileReader(logFile));
    }

    public void run() {
        while (true) {
            String s;
            try {
                s = logReader.readLine();
            } catch (IOException e) {
                Log.e("Log replay IO Exception: %s", e.getMessage());
                return;
            }
            if (s == null) {
                Log.i("Traffic EOF");
                return;
            }
            Matcher m = rawPattern.matcher(s);
            if (!m.matches()) continue;
            // Some datagrams contain 2 messages
            var data = m.group(3);
            if (data == null || !data.contains("7e14"))
                continue;

            var timestamp = Instant.parse(m.group(1));
            byte[] raw = new byte[data.length() / 2];
            for (int j = 0; j < data.length(); j += 2) {
                raw[j / 2] = (byte) Integer.parseInt(data.substring(j, j + 2), 16);
            }
            ByteArrayInputStream is = new ByteArrayInputStream(raw);
            while (is.available() > 0) {
                if ((byte) is.read() != 0x7e) continue;
                Gdl90Message msg = Gdl90Message.getMessage(is);
                if (!(msg instanceof Traffic)) continue;
                Traffic t = (Traffic) msg;
                var now = Instant.now();
                var msgTime = t.point.getInstant();
                long delay = prev == null ? 0 : Math.min(2000, (prev.until(msgTime, ChronoUnit.MILLIS)) - (prevRealtime.until(now, ChronoUnit.MILLIS)));
                prev = msgTime;
                prevRealtime = now;
                if (simulate)
                    delay /= simulateSpeedFactor;

                if (delay > 0) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                t.upsert(vehicleList, timestamp);
            }
        }
    }
}

