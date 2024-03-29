package com.meerkat;

import static com.meerkat.ui.settings.SettingsViewModel.replaySpeedFactor;

import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.gdl90.Traffic;
import com.meerkat.log.Log;
import com.meerkat.measure.Units;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogReplay extends Thread {
    private final VehicleList vehicleList;
    private final BufferedReader logReader;
    private long prevTimestamp = 0;
    private long prevRealtime = 0;
    static final Pattern timestampPattern = Pattern.compile("^(\\d\\d):(\\d\\d):(\\d\\d\\.\\d+)\\s.*?\\s(GDL90|GPS):?\\s(.*)");
    static final Pattern gpsPattern = Pattern.compile("^\\(([\\-+]?\\d+\\.\\d+),\\s*([\\-+]?\\d+\\.\\d+)\\)\\s*([+\\-]?\\d+)ft,\\s*(\\d+(?:\\.\\d+)?)kts\\s*(\\d+)[!\\s]?$");

    public static Clock clock;

    public LogReplay(VehicleList v, File logFile) throws IOException {
        this.vehicleList = v;
        Log.level(Log.Level.D);
        Log.i("new LogReplay");
        logReader = new BufferedReader(new FileReader(logFile));
    }

    public void run() {
        var today = Instant.now().truncatedTo(ChronoUnit.DAYS);
        while (true) {
            String s;
            try {
                s = logReader.readLine();
            } catch (IOException e) {
                Log.e("Log replay IO Exception: %s", e.getMessage());
                return;
            }
            if (s == null) {
                Log.i("Log file EOF: VehicleList = %d", vehicleList.size());
                if (vehicleList.isEmpty()) break;
                clock = Clock.fixed(Instant.ofEpochMilli(clock.millis() + 1000), ZoneId.systemDefault());
            } else {
                Matcher m = timestampPattern.matcher(s);
                if (!m.matches()) continue;
                if (m.groupCount() < 5)
                    continue;

                if (m.group(1) == null || m.group(2) == null || m.group(3) == null) continue;
                clock = Clock.fixed(today.plusMillis(Integer.parseInt(Objects.requireNonNull(m.group(1))) * 3600000L +
                        Integer.parseInt(Objects.requireNonNull(m.group(2))) * 60000L +
                        (long) Float.parseFloat(Objects.requireNonNull(m.group(3))) * 1000), ZoneId.systemDefault());
                var msgType = m.group(4);
                var data = m.group(5);
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
                        Gps.setLocation("GPS", Double.parseDouble(lat), Double.parseDouble(lon),
                                Units.Height.FT.toM(Double.parseDouble(alt)),
                                (float) Units.Speed.KNOTS.toMps(Float.parseFloat(spd)), Float.parseFloat(trk),
                                clock.instant().toEpochMilli());
                    } catch (NumberFormatException ex) {
                        // do nothing... continue
                    }
                } else if (msgType.equals("GDL90")) {
                    // Some datagrams contain 2 messages
                    if (!data.contains("7e14"))
                        continue;
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
                        t.point.setTime(clock.millis());
                        t.upsert(vehicleList);
                    }
                }
            }

            var now = Instant.now().toEpochMilli();
            var timestamp = clock.millis();
            long delay = prevTimestamp == 0 ? 0 : Math.min(2000, (timestamp - prevTimestamp) - (now - prevRealtime));
            prevTimestamp = timestamp;
            prevRealtime = now;
            delay /= replaySpeedFactor;

            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.i("Replay finished");
    }
}

