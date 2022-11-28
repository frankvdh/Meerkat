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

import com.meerkat.ui.log.LogFragment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

interface LogWriter {
    void append(Log.Level level, String tag, String msg);
}

class SystemOutLogWriter implements LogWriter {
    public void append(Log.Level level, String tag, String msg) {
        System.out.println(level + "/" + tag + ": " + msg);
    }
}

class FileLogWriter implements LogWriter {
    static final DateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

    BufferedWriter bw;
final File file;
    public FileLogWriter(File file) {
        this.file = file;
        append(Log.Level.A, "Log restarted", new Date().toString());
    }

    public void append(Log.Level level, String tag, String msg)  {
        try {
            if (bw == null)
                bw = new BufferedWriter(new FileWriter(file, true));
            bw.write(String.format("%s %s/%s %s\r\n",  sdf.format(new Date()), tag, level, msg));
        } catch (IOException e) {
            bw = null;
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
    private final LogFragment logFragment;

    public ViewLogWriter(LogFragment logFragment) {
        this.logFragment = logFragment;
    }

    public void append(Log.Level level, String tag, String message) {
        if (logFragment != null && !message.isEmpty()) {
            logFragment.append(message);
        }
    }

}

class AndroidLogWriter implements LogWriter {

    private final Method[] mLogMethods;

    public AndroidLogWriter() throws ClassNotFoundException {
        mLogMethods = new Method[Log.Level.A.value +1];
        Class<?> logClass = Class.forName("android.util.Log");
        for (Log.Level l: Log.Level.values()) {
            try {
                mLogMethods[l.value] = logClass.getMethod(l.name().toLowerCase(), String.class, String.class);
            } catch (NoSuchMethodException | SecurityException e) {
                // ignore
            }
        }
    }

    public void append(Log.Level level, String tag, String msg) {
        try {
            mLogMethods[level.value].invoke(null, tag, msg);
        } catch (InvocationTargetException | IllegalAccessException ex) {
            // ignore
        }
    }
}


