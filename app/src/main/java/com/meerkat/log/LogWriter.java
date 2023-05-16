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
package com.meerkat.log;

import com.meerkat.ui.log.LogViewModel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

interface LogWriter {
    void append(Log.Level level, String tag, String msg);
}

@SuppressWarnings("unused")
class SystemOutLogWriter implements LogWriter {
    public void append(Log.Level level, String tag, String msg) {
        System.out.println(level + "/" + tag + ": " + msg);
    }
}

class FileLogWriter implements LogWriter {
    BufferedWriter bw;
    final File file;
    final boolean append;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    public FileLogWriter(File file, boolean append) {
        this.file = file;
        this.append = append;
        try {
            bw = new BufferedWriter(new FileWriter(file, append));
            bw.write(String.format("\r\n\r\n%s %s/%s %s\r\n", Instant.now().toString(), "FileLogWriter", Log.Level.A, "Log restarted"));
        } catch (Exception e) {
            bw = null;
            android.util.Log.e("FileLogWriter", "Log File create failed: " + e.getMessage());
        }
    }

    public void append(Log.Level level, String tag, String msg) {
        try {
            if (bw == null)
                bw = new BufferedWriter(new FileWriter(file, append));
            synchronized (this) {
                bw.write(String.format("%s %s/%s %s\r\n", formatter.format(Instant.now()), tag, level, msg));
            }
        } catch (Exception e) {
            bw = null;
            android.util.Log.e(tag, "File Log write (" + level + ", " + msg + ") failed: " + e.getMessage());
        }
    }

    public void close() {
        append(Log.Level.A, "FileLogWriter", "Log closed");
        try {
            bw.flush();
            bw.close();
        } catch (Exception e) {
            android.util.Log.e("FileLogWriter", "File Log close failed: " + e.getMessage());
        }
        bw = null;
    }
}

class ViewLogWriter implements LogWriter {
    private final LogViewModel logViewModel;

    public ViewLogWriter(LogViewModel logViewModel) {
        this.logViewModel = logViewModel;
    }

    public void append(Log.Level level, String tag, String message) {
        if (logViewModel != null && !message.isEmpty()) {
            logViewModel.append(message);
        }
    }
}

class AndroidLogWriter implements LogWriter {

    private final Method[] mLogMethods;

    public AndroidLogWriter() throws ClassNotFoundException {
        mLogMethods = new Method[Log.Level.A.value + 1];
        Class<?> logClass = Class.forName("android.util.Log");
        for (Log.Level l : Log.Level.values()) {
            try {
                // "A" level is translated to wtf because android.util.log doesn't have "A"
                mLogMethods[l.value] = logClass.getMethod(l.value == Log.Level.A.value ? "wtf" : l.name().toLowerCase(), String.class, String.class);
            } catch (NoSuchMethodException | SecurityException e) {
                android.util.Log.wtf(this.getClass().getCanonicalName(), "Log constructor failed: " + l + " -- " + e.getMessage());
            }
        }
    }

    public void append(Log.Level level, String tag, String msg) {
        try {
            mLogMethods[level.value].invoke(null, tag, msg);
        } catch (Exception ex) {
            android.util.Log.wtf(tag, "Log failed: " + level + " -- " + msg);
        }
    }
}


