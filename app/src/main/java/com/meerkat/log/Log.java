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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class Log {

    public enum Level {
        V(1), D(2), I(3), W(4), E(5), A(6);

        final public int value;

        Level(int value) {
            this.value = value;
        }
    }

    private Log() {
    }

    private static final Set<LogWriter> mPrinters = new HashSet<>();
    public static final AndroidLogWriter ANDROID;
    public static ViewLogWriter viewLogWriter;

    private static Level mMinLevel = Level.V;

    static {
        try {
            ANDROID = new AndroidLogWriter();
            useLogWriter(ANDROID, true);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("java.util.log class not found");
        }
    }

    public static synchronized void level(Level minLevel) {
        mMinLevel = minLevel;
    }

    public static synchronized void useLogWriter(LogWriter p, boolean on) {
        if (on) {
            mPrinters.add(p);
        } else {
            mPrinters.remove(p);
        }
    }

    public static synchronized void useFilePrinter(File file) throws IOException {
        FileLogWriter fp = new FileLogWriter(file);
        mPrinters.add(fp);
    }

    public static synchronized void useViewLogWriter(LogFragment logFragment) {
        viewLogWriter = new ViewLogWriter(logFragment);
        mPrinters.add(viewLogWriter);
    }

    public static synchronized void v(String msg, Object... args) {
        log(Level.V, msg, args);
    }

    public static synchronized void d(String msg, Object... args) {
        log(Level.D, msg, args);
    }

    public static synchronized void i(String msg, Object... args) {
        log(Level.I, msg, args);
    }

    public static synchronized void w(String msg, Object... args) {
        log(Level.W, msg, args);
    }

    public static synchronized void e(String msg, Object... args) {
        log(Level.E, msg, args);
    }

    public static synchronized void a(String msg, Object... args) {
        log(Level.A, msg, args);
    }

    public static void close() {
        for (LogWriter p : mPrinters) {
            if (p instanceof FileLogWriter)
                ((FileLogWriter) p).close();
        }

    }

    private static void log(Level level, String msg, Object... args) {
        if (level.value < mMinLevel.value) {
            return;
        }
        print(level, tag(), format(msg, args));
    }

    private static String format(String head, Object... args) {
        Throwable t = null;
        if (args != null && args.length > 0 && args[args.length - 1] instanceof Throwable) {
            t = (Throwable) args[args.length - 1];
            args = Arrays.copyOfRange(args, 0, args.length - 1);
        }
        StringBuilder sb = new StringBuilder();
        if (args == null || args.length == 0) {
            sb.append(head == null ? "null" : head);
        } else if (head != null && head.indexOf('%') != -1) {
            sb.append(String.format(head, args));
        } else {
            sb.append(head == null ? "null" : head);
            for (Object arg : args) {
                sb.append("\t");
                sb.append(arg == null ? "null" : arg.toString());
            }
        }
        if (t != null) {
            sb.append("\n");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            sb.append(sw);
        }
        return sb.toString();
    }

    final static int MAX_LOG_LINE_LENGTH = 4000;

    private static void print(Level level, String tag, String msg) {
        for (String line : msg.split("\\n")) {
            do {
                int splitPos = Math.min(MAX_LOG_LINE_LENGTH, line.length());
                for (int i = splitPos - 1; line.length() > MAX_LOG_LINE_LENGTH && i >= 0; i--) {
                    if (" \t,.;:?!{}()[]/\\".indexOf(line.charAt(i)) != -1) {
                        splitPos = i;
                        break;
                    }
                }
                splitPos = Math.min(splitPos + 1, line.length());
                String part = line.substring(0, splitPos);
                line = line.substring(splitPos);

                for (LogWriter p : mPrinters) {
                    p.append(level, tag, part);
                }
            } while (line.length() > 0);
        }
    }

    private final static int STACK_DEPTH = 4;
    private final static int MAX_FILENAME_LENGTH = 30;

    private static String tag() {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        if (stackTrace.length < STACK_DEPTH) {
            throw new IllegalStateException
                    ("Synthetic stacktrace didn't have enough elements: are you using proguard?");
        }
        String fileName = stackTrace[STACK_DEPTH - 1].getFileName();
        fileName = fileName.substring(0, fileName.lastIndexOf('.')) + '/' + stackTrace[STACK_DEPTH - 1].getLineNumber();
        return fileName.length() < MAX_FILENAME_LENGTH ? fileName : fileName.substring(fileName.length() - MAX_FILENAME_LENGTH);
    }
}
