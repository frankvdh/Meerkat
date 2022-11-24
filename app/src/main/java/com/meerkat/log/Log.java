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
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Log {

    public final static int V = 1;
    public final static int D = 2;
    public final static int I = 3;
    public final static int W = 4;
    public final static int E = 5;
    public final static int A = 6;

    private Log() {
    }


    private static final Set<LogWriter> mPrinters = new HashSet<>();
    public static final AndroidLogWriter ANDROID;
    public static ViewLogWriter viewLogWriter;

    private final static Map<String, String> mTags = new HashMap<>();

    private static String[] mUseTags = new String[]{"tag", "TAG"};
    private static boolean mUseFormat = false;
    private static int mMinLevel = V;

    static {
        try {
            ANDROID = new AndroidLogWriter();
            useLogWriter(ANDROID, true);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("java.util.log class not found");
        }
    }

    public static synchronized void useTags(String[] tags) {
        mUseTags = tags;
    }

    public static synchronized void level(int minLevel) {
        mMinLevel = minLevel;
    }

    public static synchronized void useFormat(boolean yes) {
        mUseFormat = yes;
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

    public static synchronized void v(Object msg, Object... args) {
        log(V, mUseFormat, msg, args);
    }

    public static synchronized void d(Object msg, Object... args) {
        log(D, mUseFormat, msg, args);
    }

    public static synchronized void i(Object msg, Object... args) {
        log(I, mUseFormat, msg, args);
    }

    public static synchronized void w(Object msg, Object... args) {
        log(W, mUseFormat, msg, args);
    }

    public static synchronized void e(Object msg, Object... args) {
        log(E, mUseFormat, msg, args);
    }

    public static synchronized void a(Object msg, Object... args) {
        log(A, mUseFormat, msg, args);
    }

    private static void log(int level, boolean fmt, Object msg, Object... args) {
        if (level < mMinLevel) {
            return;
        }
        String tag = tag();
        if (mUseTags.length > 0 && tag.equals(msg)) {
            if (args.length > 1) {
                print(level, tag, format(fmt, args[0], Arrays.copyOfRange(args, 1, args.length)));
            } else {
                print(level, tag, format(fmt, (args.length > 0 ? args[0] : "")));
            }
        } else {
            print(level, tag, format(fmt, msg, args));
        }
    }

    private static String format(boolean fmt, Object msg, Object... args) {
        Throwable t = null;
        if (args == null) {
            // Null array is not supposed to be passed into this method, so it must
            // be a single null argument
            args = new Object[]{null};
        }
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            t = (Throwable) args[args.length - 1];
            args = Arrays.copyOfRange(args, 0, args.length - 1);
        }
        if (fmt && msg instanceof String) {
            String head = (String) msg;
            if (head.indexOf('%') != -1) {
                return String.format(head, args);
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(msg == null ? "null" : msg.toString());
        for (Object arg : args) {
            sb.append("\t");
            sb.append(arg == null ? "null" : arg.toString());
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

    private static void print(int level, String tag, String msg) {
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

    private final static Pattern ANONYMOUS_CLASS = Pattern.compile("\\$\\d+$");
    private final static int STACK_DEPTH = 4;

    private static String tag() {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        if (stackTrace.length < STACK_DEPTH) {
            throw new IllegalStateException
                    ("Synthetic stacktrace didn't have enough elements: are you using proguard?");
        }
        String className = stackTrace[STACK_DEPTH - 1].getClassName();
        String tag = mTags.get(className);
        if (tag != null) {
            return tag;
        }

        try {
            Class<?> c = Class.forName(className);
            for (String f : mUseTags) {
                try {
                    Field field = c.getDeclaredField(f);

                    field.setAccessible(true);
                    Object value = field.get(null);
                    if (value instanceof String) {
                        mTags.put(className, (String) value);
                        return (String) value;
                    }
                } catch (NoSuchFieldException | IllegalAccessException |
                        IllegalStateException | NullPointerException e) {
                    //Ignore
                }
            }
        } catch (ClassNotFoundException e) { /* Ignore */ }

        // Check class field useTag, if exists - return it, otherwise - return the generated tag
        Matcher m = ANONYMOUS_CLASS.matcher(className);
        if (m.find()) {
            className = m.replaceAll("");
        }
        return className.substring(className.lastIndexOf('.') + 1);
    }
}
