package com.meerkat;

import static com.meerkat.SettingsActivity.simulate;
import static com.meerkat.SettingsActivity.simulateSpeedFactor;

import android.location.Location;

import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.gdl90.Traffic;
import com.meerkat.log.Log;
import com.meerkat.measure.Units;

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
    static final Pattern rawPattern = Pattern.compile("(.*?\\d\\d:\\d\\d:\\d\\d\\.\\d+Z)\\s.*?\\s(RAW|GPS):?\\s(.*)");
    static final Pattern gpsPattern = Pattern.compile("^\\(([\\-+]?\\d+\\.\\d+),\\s*([\\-+]?\\d+\\.\\d+)\\)\\s*([+\\-]?\\d+)ft,\\s*(\\d+\\.\\d+)kts\\s*(\\d+)[!\\s]$");

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
            if (m.groupCount() < 3)
                continue;
            var msgType = m.group(2);
            var data = m.group(3);
            if (data == null || msgType == null) continue;
            if (msgType.equals("GPS")) {
                Matcher g = gpsPattern.matcher(data);
                if (!g.matches()) continue;
                if (g.groupCount() < 5) continue;
                var lat = g.group(1);
                if (lat == null) continue;
                var lon = g.group(2);
                if (lon == null) continue;
                var alt = g.group(3);
                if (alt == null) continue;
                var spd = g.group(4);
                if (spd == null) continue;
                var trk = g.group(5);
                if (trk == null) continue;
                try {
                    Location l = new Location("log");
                    l.setLatitude(Double.parseDouble(lat));
                    l.setLongitude(Double.parseDouble(lon));
                    l.setAltitude(Units.Height.FT.toM(Double.parseDouble(alt)));
                    l.setSpeed((float) Units.Speed.KNOTS.toMps(Float.parseFloat(spd)));
                    l.setBearing(Float.parseFloat(trk));
                    Gps.setLocation(l);
                } catch (NumberFormatException ex) {
                    // do nothing... continue
                }
            } else if (msgType.equals("RAW")) {
                // Some datagrams contain 2 messages
                if (!data.contains("7e14"))
                    continue;

                var timestamp = Instant.parse(m.group(1));
                byte[] raw = new byte[data.length() / 2];
                for (int j = 0; j < data.length(); j += 2) {
                    try {
                        raw[j / 2] = (byte) Integer.parseInt(data.substring(j, j + 2), 16);
                    } catch (StringIndexOutOfBoundsException | NumberFormatException ex) {
                        Log.e("Invalid raw message log entry: %s", data);
                        break;
                    }
                }
                ByteArrayInputStream is = new ByteArrayInputStream(raw);
                while (is.available() > 0) {
                    if ((byte) is.read() != 0x7e) continue;
                    Gdl90Message msg = Gdl90Message.getMessage(is);
                    if (!(msg instanceof Traffic t)) continue;
                    var now = Instant.now();
                    var msgTime = t.point.getInstant();
                    long delay = prev == null ? 0 : Math.min(2000, (prev.until(msgTime, ChronoUnit.MILLIS)) - (prevRealtime.until(now, ChronoUnit.MILLIS)));
                    prev = msgTime;
                    prevRealtime = now;
                    if (simulate)
                        delay /= simulateSpeedFactor;

                    if (delay > 0) {
                        try {
                            Log.d("Delay %d", delay);
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
}

