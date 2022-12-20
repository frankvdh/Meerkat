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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
    static final DateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.ENGLISH);

    BufferedWriter bw;
    final File file;
    final boolean append;

    public FileLogWriter(File file, boolean append) {
        this.file = file;
        this.append = append;
        append(Log.Level.A, "Log restarted", new Date().toString());
    }

    public void append(Log.Level level, String tag, String msg) {
        try {
            if (bw == null)
                bw = new BufferedWriter(new FileWriter(file, append));
            bw.write(String.format("%s %s/%s %s\r\n", sdf.format(new Date()), tag, level, msg));
        } catch (IOException e) {
            bw = null;
            android.util.Log.e(tag, "File Log write (" + level + ", " + msg + ") failed: " + e.getMessage());
        }
    }

    public void close() {
        append(Log.Level.A, "Log closed", new Date().toString());
        try {
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        bw = null;
    }
}

class ViewLogWriter implements LogWriter {
    private final LogActivity logActivity;

    public ViewLogWriter(LogActivity logActivity) {
        this.logActivity = logActivity;
    }

    public void append(Log.Level level, String tag, String message) {
        if (logActivity != null && !message.isEmpty()) {
            logActivity.append(message);
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


