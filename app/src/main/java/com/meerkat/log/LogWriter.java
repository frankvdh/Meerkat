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

interface LogWriter {
    void append(int level, String tag, String msg);
}

class SystemOutLogWriter implements LogWriter {
    private final static String[] LEVELS = new String[]{" ", "V", "D", "I", "W", "E", "A"};

    public void append(int level, String tag, String msg) {
        System.out.println(LEVELS[level] + "/" + tag + ": " + msg);
    }
}

class FileLogWriter implements LogWriter {
    private final static String[] METHOD_NAMES = new String[]{" ", "v", "d", "i", "w", "e", "a"};
    BufferedWriter bw;

    public FileLogWriter(File file) throws IOException {
        this.bw = new BufferedWriter(new FileWriter(file, true));
    }

    public void append(int level, String tag, String msg) {
        try {
            bw.write(METHOD_NAMES[level] + "/" + tag + ": " + msg + "\r\n");
        } catch (IOException e) {
            // Ignore
        }
    }
}

class ViewLogWriter implements LogWriter {
    private final LogFragment logFragment;

    public ViewLogWriter(LogFragment logFragment) {
        this.logFragment = logFragment;
    }

    public void append(int level, String tag, String message) {
        if (logFragment != null && !message.isEmpty()) {
                 logFragment.append(message);
        }
    }

}

class AndroidLogWriter implements LogWriter {

    private final static String[] METHOD_NAMES = new String[]{" ", "v", "d", "i", "w", "e", "a"};

    private final Method[] mLogMethods;

    public AndroidLogWriter() throws ClassNotFoundException {
        mLogMethods = new Method[METHOD_NAMES.length];
        Class<?> logClass = Class.forName("android.util.Log");
        for (int i = 0; i < METHOD_NAMES.length; i++) {
            try {
                mLogMethods[i] = logClass.getMethod(METHOD_NAMES[i], String.class, String.class);
            } catch (NoSuchMethodException | SecurityException e) {
                // ignore
            }
        }
    }

    public void append(int level, String tag, String msg) {
        try {
            mLogMethods[level].invoke(null, tag, msg);
        } catch (InvocationTargetException | IllegalAccessException ex) {
            // ignore
        }
    }
}


