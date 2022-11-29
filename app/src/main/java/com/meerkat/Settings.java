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
package com.meerkat;

import static android.content.Context.MODE_PRIVATE;
import static com.meerkat.log.Log.Level;

import android.content.Context;
import android.content.SharedPreferences;

import com.meerkat.log.Log;
import com.meerkat.measure.Distance;
import com.meerkat.measure.Height;
import com.meerkat.measure.Speed;

public class Settings {
    private static SharedPreferences prefs;
    public static String wifiName;
    public static int port;
    public static boolean showLog;
    public static boolean fileLog;
    public static Level logLevel;
    public static boolean logRawMessages;
    public static boolean logDecodedMessages;
    public static boolean showLinearPredictionTrack;
    public static boolean showPolynomialPredictionTrack;
    public static int historySeconds;
    public static int purgeSeconds;
    public static int predictionSeconds;
    public static int polynomialPredictionStepSeconds;
    public static int gradientMaximumDiff;
    public static int gradientMinimumDiff;
    public static int screenYPosPercent;
    public static float screenWidth;
    public static float circleRadiusStep;
    public static Distance.Units distanceUnits;
    public static Height.Units altUnits;
    public static Speed.Units speedUnits;
    public static boolean simulate;
    public static String countryCode;
    public static boolean trackUp, headingUp;

    public static void load(Context context) {
        prefs = context.getSharedPreferences("com.meerkat_preferences", MODE_PRIVATE);
        boolean saveNeeded = prefs.getString("wifiName", null) == null;
        wifiName = prefs.getString("wifiName", "Ping-6C7A");
        port = prefs.getInt("port", 4000);
        showLog = prefs.getBoolean("showLog", true);
        fileLog = prefs.getBoolean("fileLog", true);
        try {
        logLevel = Level.valueOf(prefs.getString("logLevel", "I").toUpperCase().trim().substring(0, 1));
        } catch(Exception e) {
            logLevel = Level.I;
            saveNeeded = true;
        }
        logRawMessages = prefs.getBoolean("logRawMessages", false);
        logDecodedMessages = prefs.getBoolean("logDecodedMessages", false);
        showLinearPredictionTrack = prefs.getBoolean("showLinearPredictionTrack", true);
        showPolynomialPredictionTrack = prefs.getBoolean("showPolynomialPredictionTrack", true);
        historySeconds = prefs.getInt("historySeconds", 60);
        purgeSeconds = prefs.getInt("historySeconds", 60);
        predictionSeconds = prefs.getInt("predictionSeconds", 60);
        polynomialPredictionStepSeconds = prefs.getInt("polynomialPredictionStepSeconds", 10);
        gradientMaximumDiff = prefs.getInt("gradientMaximumDiff", 5000);
        gradientMinimumDiff = prefs.getInt("gradientMinimumDiff", 1000);
        screenYPosPercent = prefs.getInt("screenYPosPercent", 25);
        screenWidth = prefs.getFloat("screenWidth", 10);
        circleRadiusStep = prefs.getFloat("circleRadiusStep", 5);
        try {
            distanceUnits = Distance.Units.valueOf(prefs.getString("distanceUnits", "NM").toUpperCase().trim());
        } catch(Exception e) {
            distanceUnits = Distance.Units.NM;
            saveNeeded = true;
        }
        try {
            altUnits = Height.Units.valueOf(prefs.getString("altUnits", "FT").toUpperCase().trim());
        } catch(Exception e) {
            altUnits = Height.Units.FT;
            saveNeeded = true;
        }
        try {
            speedUnits = Speed.Units.valueOf(prefs.getString("speedUnits", "KNOTS").toUpperCase().trim());
        } catch(Exception e) {
            speedUnits = Speed.Units.KNOTS;
            saveNeeded = true;
        }
        simulate = prefs.getBoolean("simulate", false);
        countryCode = prefs.getString("countryCode", "ZK").toUpperCase();
        headingUp = prefs.getBoolean("headingUp", true);
        trackUp = !headingUp && prefs.getBoolean("trackUp", true);
        if (saveNeeded)
            save();
        Log.i("Settings loaded");
    }

    public static void save() {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("wifiName", wifiName);
        edit.putInt("port", port);
        edit.putBoolean("showLog", showLog);
        edit.putBoolean("fileLog", fileLog);
        edit.putString("logLevel", String.valueOf(logLevel));
        edit.putBoolean("logRawMessages", logRawMessages);
        edit.putBoolean("logDecodedMessages", logDecodedMessages);
        edit.putBoolean("showLinearPredictionTrack", showLinearPredictionTrack);
        edit.putBoolean("showPolynomialPredictionTrack", showPolynomialPredictionTrack);
        edit.putInt("historySeconds", historySeconds);
        edit.putInt("purgeSeconds", purgeSeconds);
        edit.putInt("predictionSeconds", predictionSeconds);
        edit.putInt("polynomialPredictionStepSeconds", polynomialPredictionStepSeconds);
        edit.putInt("gradientMaximumDiff", gradientMaximumDiff);
        edit.putInt("gradientMinimumDiff", gradientMinimumDiff);
        edit.putInt("screenYPosPercent", screenYPosPercent);
        edit.putFloat("screenWidth", screenWidth);
        edit.putFloat("circleRadiusStep", circleRadiusStep);
        edit.putString("distanceUnits", String.valueOf(distanceUnits));
        edit.putString("altUnits", String.valueOf(altUnits));
        edit.putString("speedUnits", String.valueOf(speedUnits));
        edit.putBoolean("simulate", simulate);
        edit.putString("countryCode", countryCode);
        edit.putBoolean("trackUp", trackUp);
        edit.putBoolean("headingUp", headingUp);
        edit.apply();
        Log.i("Settings saved");
    }
}
