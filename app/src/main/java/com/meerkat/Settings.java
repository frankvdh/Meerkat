package com.meerkat;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

import static com.meerkat.log.Log.I;

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
    public static int logLevel;
    public static boolean logRawMessages;
    public static boolean logDecodedMessages;
    public static boolean showLinearPredictionTrack;
    public static boolean showPolynomialPredictionTrack;
    public static int historySeconds;
    public static int purgeSeconds;
    public static int predictionSeconds;
    public static int polynomialPredictionStepSeconds;
    public static int gradientMaximumFeet;
    public static int gradientMinimumFeet;
    public static int screenYPosPercent;
    public static float screenWidth;
    public static float circleRadiusStep;
    public static Distance.Units distanceUnits;
    public static Height.Units altUnits;
    public static Speed.Units speedUnits;
    public static boolean simulate;
    public static String countryCode;

    public static void load(Context context) {
        prefs = context.getSharedPreferences("com.meerkat_preferences", MODE_PRIVATE);
        wifiName = prefs.getString("wifiName", "Ping-6C7A");
        port = prefs.getInt("port", 4000);
        showLog = prefs.getBoolean("showLog", true);
        fileLog = prefs.getBoolean("fileLog", true);
        logLevel = prefs.getInt("logLevel", I);
        logRawMessages = prefs.getBoolean("logRawMessages", false);
        logDecodedMessages = false;//prefs.getBoolean("logDecodedMessages", false);
        showLinearPredictionTrack = prefs.getBoolean("showLinearPredictionTrack", true);
        showPolynomialPredictionTrack = prefs.getBoolean("showPolynomialPredictionTrack", false);
        historySeconds = prefs.getInt("historySeconds", 60);
        purgeSeconds = prefs.getInt("historySeconds", 60);
        predictionSeconds = prefs.getInt("predictionSeconds", 60);
        polynomialPredictionStepSeconds = prefs.getInt("polynomialPredictionStepSeconds", 10);
        gradientMaximumFeet = prefs.getInt("gradientMaximumFeet", 5000);
        gradientMinimumFeet = prefs.getInt("gradientMinimumFeet", 1000);
        screenYPosPercent = prefs.getInt("screenYPosPercent", 25);
        screenWidth = prefs.getFloat("screenWidth", 10);
        circleRadiusStep = prefs.getFloat("circleRadiusStep", 5);
        try {
            distanceUnits = Distance.Units.valueOf(prefs.getString("distanceUnits", "NM").toUpperCase().trim());
        } catch(Exception e) {
            distanceUnits = Distance.Units.NM;
        }
        try {
            altUnits = Height.Units.valueOf(prefs.getString("altUnits", "FT").toUpperCase().trim());
        } catch(Exception e) {
            altUnits = Height.Units.FT;
        }
        try {
            speedUnits = Speed.Units.valueOf(prefs.getString("speedUnits", "KNOTS").toUpperCase().trim());
        } catch(Exception e) {
            speedUnits = Speed.Units.KNOTS;
        }
        simulate = prefs.getBoolean("simulate", true);
        countryCode = prefs.getString("countryCode", "ZK").toUpperCase();
        Log.i("Settings loaded");
    }

    public static void save() {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("wifiName", wifiName);
        edit.putInt("port", port);
        edit.putBoolean("showLog", showLog);
        edit.putBoolean("fileLog", fileLog);
        edit.putInt("logLevel", logLevel);
        edit.putBoolean("logRawMessages", logRawMessages);
        edit.putBoolean("logDecodedMessages", logDecodedMessages);
        edit.putBoolean("showLinearPredictionTrack", showLinearPredictionTrack);
        edit.putBoolean("showPolynomialPredictionTrack", showPolynomialPredictionTrack);
        edit.putInt("historySeconds", historySeconds);
        edit.putInt("purgeSeconds", purgeSeconds);
        edit.putInt("predictionSeconds", predictionSeconds);
        edit.putInt("polynomialPredictionStepSeconds", polynomialPredictionStepSeconds);
        edit.putInt("gradientMaximumFeet", gradientMaximumFeet);
        edit.putInt("gradientMinimumFeet", gradientMinimumFeet);
        edit.putInt("screenYPosPercent", screenYPosPercent);
        edit.putFloat("screenWidth", screenWidth);
        edit.putFloat("circleRadiusStep", circleRadiusStep);
        edit.putString("distanceUnits", String.valueOf(distanceUnits));
        edit.putString("altUnits", String.valueOf(altUnits));
        edit.putString("speedUnits", String.valueOf(speedUnits));
        edit.putBoolean("simulate", simulate);
        edit.putString("countryCode", countryCode);
        edit.apply();
        Log.i("Settings saved");
    }
}
