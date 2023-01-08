package com.meerkat;

import static com.meerkat.SettingsActivity.simulate;
import static java.util.Calendar.HOUR;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;

import com.meerkat.gdl90.Traffic;
import com.meerkat.log.Log;
import com.meerkat.measure.Position;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogReplay extends Thread {
    private final VehicleList vehicleList;
    private final File logFile;
    static final Pattern rawPattern = Pattern.compile("(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d\\d)\\s(.*?)/[AEWIVD]\\s(.*)");
    static final DateFormat df = new SimpleDateFormat("EEE MMM dd kk:mm:ss z yyyy", Locale.getDefault());

    public LogReplay(VehicleList v, File logFile) throws IOException {
        this.vehicleList = v;
        this.logFile = logFile;
    }

    public void run() {
        Log.level(Log.Level.D);
        Log.i("new LogReplay");
        BufferedReader logReader;
        try {
            logReader = new BufferedReader(new FileReader(logFile));
        } catch (FileNotFoundException e) {
            Log.e("Log replay file not found: %s", logFile.getAbsolutePath());
            return;
        }
        Calendar date = new GregorianCalendar();
        long prev = 0;
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
            if (Objects.requireNonNull(m.group(5)).startsWith("Log restarted")) {
                try {
                    Date d = df.parse(Objects.requireNonNull(m.group(6)));
                    if (d == null) continue;
                    date.setTime(d);
                    date.set(SECOND, Integer.parseInt(Objects.requireNonNull(m.group(3))));
                    date.set(MILLISECOND, Integer.parseInt(Objects.requireNonNull(m.group(4))));
                } catch (ParseException e) {
                    Log.e("Restart date parse error: %s", e.getMessage());
                }
                continue;
            }
            if (!Objects.requireNonNull(m.group(5)).startsWith("PingComms") || !Objects.requireNonNull(m.group(6)).startsWith("7e14"))
                continue;
            date.set(HOUR, Integer.parseInt(Objects.requireNonNull(m.group(1))));
            date.set(MINUTE, Integer.parseInt(Objects.requireNonNull(m.group(2))));
            date.set(SECOND, Integer.parseInt(Objects.requireNonNull(m.group(3))));
            date.set(MILLISECOND, Integer.parseInt(Objects.requireNonNull(m.group(4))));

            var data = m.group(6);
            if (data == null) continue;
            byte[] raw = new byte[data.length() / 2];
            for (int j = 0; j < data.length(); j += 2) {
                raw[j / 2] = (byte) Integer.parseInt(data.substring(j, j + 2), 16);
            }

            ByteArrayInputStream is = new ByteArrayInputStream(raw);
            while (is.available() > 0) {
                if ((byte) is.read() != 0x7e) continue;
                byte messageId = (byte) is.read();
                var p1 = new Position("ADSB");
                Traffic t = new Traffic(messageId, date.getTimeInMillis(), p1, is);
                Log.d("%d %s", date.getTimeInMillis() - prev, t.toString());
                long delay = 0;
                if (simulate == SettingsActivity.SimType.LogRealTime) {
                   if (prev != 0)
                       if (t.point.getTime() > prev)
                        delay = Math.min(1000, (t.point.getTime() - prev));
                    prev = t.point.getTime();
                } else if (simulate == SettingsActivity.SimType.LogSlow)
                    delay = 1000;

            Log.i("%s: %s", df.format(t.point.getTime()), t.toString());
                if (delay > 0) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                 t.upsert(vehicleList);
            }
        }
    }
}

