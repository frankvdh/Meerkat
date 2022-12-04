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
import com.meerkat.measure.VertSpeed;
import com.meerkat.ui.map.MapFragment;

public class Settings {
    private static SharedPreferences prefs;
    public static String wifiName;
    public static int port;
    public static boolean showLog;
    public static boolean fileLog;
    public static boolean appendLogFile;
    public static Level logLevel;
    public static boolean logRawMessages;
    public static boolean logDecodedMessages;
    public static boolean showLinearPredictionTrack;
    public static boolean showPolynomialPredictionTrack;
    public static int historySeconds;
    public static int purgeSeconds;
    // The minimum distance to change Updates in meters
    public static int minGpsDistanceChangeMetres; // 10 meters

    // The minimum time between updates in milliseconds
    public static int minGpsUpdateIntervalSeconds; // 10 seconds

    /*
     * Time smoothing constant for low-pass filter 0 ≤ α ≤ 1 ; a smaller value basically means more smoothing
     * See: http://en.wikipedia.org/wiki/Low-pass_filter#Discrete-time_realization
     */
    public static int predictionSeconds;
    public static int polynomialPredictionStepSeconds, polynomialHistorySeconds;
    public static int gradientMaximumDiff;
    public static int gradientMinimumDiff;
    public static int screenYPosPercent;
    public static float sensorSmoothingConstant;
    public static float screenWidth;
    public static float circleRadiusStep;
    public static Distance.Units distanceUnits;
    public static Height.Units altUnits;
    public static Speed.Units speedUnits;
    public static VertSpeed.Units vertSpeedUnits;
    public static boolean simulate;
    public static String countryCode;
    public static MapFragment.DisplayOrientation displayOrientation;
    public static boolean keepScreenOn;

    public static void load(Context context) {
        String currentVersionName = BuildConfig.VERSION_NAME;
        prefs = context.getSharedPreferences("com.meerkat_preferences", MODE_PRIVATE);
        String settingsVersionName = prefs.getString("version", null);
        Log.d("Current version = %s, Settings version = %s", currentVersionName, settingsVersionName);
        boolean saveNeeded = settingsVersionName == null || !settingsVersionName.equals(currentVersionName);

        wifiName = prefs.getString("wifiName", "Ping-6C7A");
        port = prefs.getInt("port", 4000);
        showLog = prefs.getBoolean("showLog", true);
        fileLog = prefs.getBoolean("fileLog", true);
        appendLogFile = prefs.getBoolean("appendLogFile", false);
        try {
            logLevel = Level.valueOf(prefs.getString("logLevel", "I").toUpperCase().trim().substring(0, 1));
        } catch (Exception e) {
            Log.e("Invalid logLevel %s", prefs.getString("logLevel", null));
            logLevel = Level.I;
            saveNeeded = true;
        }
        logRawMessages = prefs.getBoolean("logRawMessages", false);
        logDecodedMessages = prefs.getBoolean("logDecodedMessages", false);
        showLinearPredictionTrack = prefs.getBoolean("showLinearPredictionTrack", true);
        showPolynomialPredictionTrack = prefs.getBoolean("showPolynomialPredictionTrack", true);
        historySeconds = Math.max(0, Math.min(300, prefs.getInt("historySeconds", 60)));
        purgeSeconds = Math.max(1, Math.min(300, prefs.getInt("purgeSeconds", 60)));
        predictionSeconds = Math.max(0, Math.min(300, prefs.getInt("predictionSeconds", 60)));
        polynomialPredictionStepSeconds = Math.max(1, Math.min(60, prefs.getInt("polynomialPredictionStepSeconds", 10)));
        polynomialHistorySeconds = Math.max(1, Math.min(60, prefs.getInt("polynomialHistorySeconds", 10)));
        gradientMaximumDiff = Math.max(1000, Math.min(5000, prefs.getInt("gradientMaximumDiff", 2500)));
        gradientMinimumDiff = Math.max(100, Math.min(gradientMaximumDiff, prefs.getInt("gradientMinimumDiff", 1000)));
        screenYPosPercent = Math.max(0, Math.min(100, prefs.getInt("screenYPosPercent", 25)));
        sensorSmoothingConstant = Math.max(0, Math.min(1, prefs.getFloat("sensorSmoothingConstant", 0.2f)));
        screenWidth = Math.max(0.5f, Math.min(50, prefs.getFloat("screenWidth", 10)));
        circleRadiusStep = Math.max(0.1f, Math.min(screenWidth, prefs.getFloat("circleRadiusStep", 5)));
        try {
            distanceUnits = Distance.Units.valueOf(prefs.getString("distanceUnits", "NM").toUpperCase().trim());
        } catch (Exception e) {
            distanceUnits = Distance.Units.NM;
            saveNeeded = true;
        }
        try {
            altUnits = Height.Units.valueOf(prefs.getString("altUnits", "FT").toUpperCase().trim());
        } catch (Exception e) {
            altUnits = Height.Units.FT;
            saveNeeded = true;
        }
        try {
            speedUnits = Speed.Units.valueOf(prefs.getString("speedUnits", "KNOTS").toUpperCase().trim());
        } catch (Exception e) {
            speedUnits = Speed.Units.KNOTS;
            saveNeeded = true;
        }
        try {
            vertSpeedUnits = VertSpeed.Units.valueOf(prefs.getString("vertSpeedUnits", "FPM").toUpperCase().trim());
        } catch (Exception e) {
            vertSpeedUnits = VertSpeed.Units.FPM;
            saveNeeded = true;
        }
        countryCode = prefs.getString("countryCode", "").toUpperCase();
        try {
            displayOrientation = MapFragment.DisplayOrientation.valueOf(prefs.getString("displayOrientation", "HeadingUp").trim());
        } catch (Exception e) {
            displayOrientation = MapFragment.DisplayOrientation.HeadingUp;
            saveNeeded = true;
        }
        keepScreenOn = prefs.getBoolean("keepScreenOn", true);
        minGpsDistanceChangeMetres = prefs.getInt("minGpsDistanceChangeMetres", 10);
        minGpsUpdateIntervalSeconds = prefs.getInt("minGpsUpdateIntervalSeconds", 10);
        simulate = prefs.getBoolean("simulate", false);
        if (simulate)
            displayOrientation = MapFragment.DisplayOrientation.NorthUp;
        if (saveNeeded)
            save();
        Log.i("Settings loaded");
    }

    public static void save() {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("version", BuildConfig.VERSION_NAME);
        edit.putString("wifiName", wifiName);
        edit.putInt("port", port);
        edit.putBoolean("showLog", showLog);
        edit.putBoolean("fileLog", fileLog);
        edit.putBoolean("appendLogFile", appendLogFile);
        edit.putString("logLevel", String.valueOf(logLevel));
        edit.putBoolean("logRawMessages", logRawMessages);
        edit.putBoolean("logDecodedMessages", logDecodedMessages);
        edit.putBoolean("showLinearPredictionTrack", showLinearPredictionTrack);
        edit.putBoolean("showPolynomialPredictionTrack", showPolynomialPredictionTrack);
        edit.putInt("historySeconds", historySeconds);
        edit.putInt("purgeSeconds", purgeSeconds);
        edit.putInt("predictionSeconds", predictionSeconds);
        edit.putInt("polynomialPredictionStepSeconds", polynomialPredictionStepSeconds);
        edit.putInt("polynomialHistorySeconds", polynomialHistorySeconds);
        edit.putInt("gradientMaximumDiff", gradientMaximumDiff);
        edit.putInt("gradientMinimumDiff", gradientMinimumDiff);
        edit.putInt("screenYPosPercent", screenYPosPercent);
        edit.putFloat("sensorSmoothingConstant", sensorSmoothingConstant);
        edit.putFloat("screenWidth", screenWidth);
        edit.putFloat("circleRadiusStep", circleRadiusStep);
        edit.putString("distanceUnits", String.valueOf(distanceUnits));
        edit.putString("altUnits", String.valueOf(altUnits));
        edit.putString("speedUnits", String.valueOf(speedUnits));
        edit.putString("vertSpeedUnits", String.valueOf(vertSpeedUnits));
        edit.putString("countryCode", countryCode);
        edit.putString("displayOrientation", String.valueOf(displayOrientation));
        edit.putBoolean("keepScreenOn", keepScreenOn);
        edit.putInt("minGpsDistanceChangeMetres", minGpsDistanceChangeMetres);
        edit.putInt("minGpsUpdateIntervalSeconds", minGpsUpdateIntervalSeconds);
        edit.putBoolean("simulate", simulate);
        edit.apply();
        Log.i("Settings saved");
    }
}
