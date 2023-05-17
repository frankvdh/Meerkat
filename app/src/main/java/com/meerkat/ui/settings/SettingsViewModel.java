package com.meerkat.ui.settings;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.ViewModel;

import com.meerkat.BuildConfig;
import com.meerkat.log.Log;
import com.meerkat.map.Cup;
import com.meerkat.map.MapView;
import com.meerkat.measure.Units;

import java.util.Locale;

public class SettingsViewModel extends ViewModel {

    public SettingsViewModel() {
    }

    public void init() {
    }

    static SharedPreferences prefs;
    public static volatile String wifiName;
    public static volatile int port;
    public static volatile boolean showLog;
    public static volatile boolean fileLog;
    public static volatile boolean appendLogFile;
    public static volatile com.meerkat.log.Log.Level logLevel;
    public static volatile boolean logRawMessages;
    public static volatile boolean logDecodedMessages;
    public static volatile boolean showLinearPredictionTrack;
    public static volatile boolean showPolynomialPredictionTrack;
    public static volatile int historySeconds;
    public static volatile int purgeSeconds;
    // The minimum distance to change Updates in meters
    public static volatile int minGpsDistanceChangeMetres; // 10 meters

    // The minimum time between updates in milliseconds
    public static volatile int minGpsUpdateIntervalSeconds; // 10 seconds
    public static volatile boolean preferAdsbPosition;
    /*
     * Time smoothing constant for low-pass filter 0 ≤ α ≤ 1 ; a smaller value basically means more smoothing
     * See: http://en.wikipedia.org/wiki/Low-pass_filter#Discrete-time_realization
     */
    public static volatile int predictionMilliS;
    public static volatile int polynomialPredictionStepMilliS, polynomialHistoryMilliS;
    public static volatile int gradientMaximumDiff;
    public static volatile int gradientMinimumDiff;
    public static volatile int screenYPosPercent;
    public static volatile float sensorSmoothingConstant;
    public static volatile int screenWidthMetres;
    public static volatile int circleRadiusStepMetres, dangerRadiusMetres;
    public static volatile Units.Distance distanceUnits;
    public static volatile Units.Height altUnits;
    public static volatile Units.Speed speedUnits;
    public static volatile Units.VertSpeed vertSpeedUnits;
    public static volatile boolean logReplay;
    public static volatile boolean simulate;
    public static volatile float replaySpeedFactor;
    public static volatile String replaySpeedFactorString;
    public static volatile String countryCode;
    public static volatile String ownCallsign;
    public static volatile int ownId;
    public static volatile MapView.DisplayOrientation displayOrientation;
    public static volatile boolean keepScreenOn;
    public static volatile boolean autoZoom;
    public static volatile int minZoom, maxZoom;
    /**
     * The number of milliseconds to wait after user interaction before hiding the system UI.
     */
    public static volatile int toolbarDelayMilliS, initToolbarDelayMilliS;
    public static volatile boolean useCupFile;
    public static volatile Cup.Label labelText;
    public static volatile boolean showFrequency;
    public static volatile boolean showRunway;

    public static volatile int magFieldUpdateDistance;

    public static void loadPrefs(Context context) {
        String currentVersionName = BuildConfig.VERSION_NAME;
        prefs = context.getSharedPreferences("com.meerkat_preferences", MODE_PRIVATE);
        String settingsVersionName = prefs.getString("version", null);
        Log.d("Current version = %s, Settings version = %s", currentVersionName, settingsVersionName);
        boolean saveNeeded = settingsVersionName == null || !settingsVersionName.equals(currentVersionName);

        wifiName = prefs.getString("wifiName", null);
        try {
            port = Integer.parseInt(prefs.getString("port", "4000"));
        } catch (Exception e) {
            Log.e("Invalid port %s", prefs.getString("port", null));
            saveNeeded = true;
        }
        showLog = prefs.getBoolean("showLog", true);
        fileLog = prefs.getBoolean("fileLog", true);
        appendLogFile = prefs.getBoolean("appendLogFile", true);
        try {
            logLevel = com.meerkat.log.Log.Level.valueOf(prefs.getString("logLevel", "I").toUpperCase().trim().substring(0, 1));
        } catch (Exception e) {
            Log.e("Invalid logLevel %s", prefs.getString("logLevel", null));
            logLevel = com.meerkat.log.Log.Level.I;
            saveNeeded = true;
        }
        logRawMessages = prefs.getBoolean("logRawMessages", true);
        logDecodedMessages = prefs.getBoolean("logDecodedMessages", false);
        showLinearPredictionTrack = prefs.getBoolean("showLinearPredictionTrack", true);
        showPolynomialPredictionTrack = prefs.getBoolean("showPolynomialPredictionTrack", true);
        historySeconds = Math.max(0, Math.min(300, prefs.getInt("historySeconds", 60)));
        purgeSeconds = Math.max(1, Math.min(300, prefs.getInt("purgeSeconds", 30)));
        predictionMilliS = Math.max(0, Math.min(300, prefs.getInt("predictionSeconds", 60))) * 1000;
        polynomialPredictionStepMilliS = Math.max(1, Math.min(60, prefs.getInt("polynomialPredictionStepSeconds", 10))) * 1000;
        polynomialHistoryMilliS = Math.max(1000, Math.min(10000, prefs.getInt("polynomialHistoryMillis", 2000)));
        screenYPosPercent = Math.max(0, Math.min(100, prefs.getInt("screenYPosPercent", 25)));
        sensorSmoothingConstant = Math.max(0, Math.min(100, prefs.getInt("sensorSmoothingConstant", 20))) / 100f;
        try {
            distanceUnits = Units.Distance.valueOf(prefs.getString("distanceUnits", "NM").toUpperCase().trim());
        } catch (Exception e) {
            distanceUnits = Units.Distance.NM;
            saveNeeded = true;
        }
        try {
            altUnits = Units.Height.valueOf(prefs.getString("altUnits", "FT").toUpperCase().trim());
        } catch (Exception e) {
            altUnits = Units.Height.FT;
            saveNeeded = true;
        }
        try {
            speedUnits = Units.Speed.valueOf(prefs.getString("speedUnits", "KNOTS").toUpperCase().trim());
        } catch (Exception e) {
            speedUnits = Units.Speed.KNOTS;
            saveNeeded = true;
        }
        try {
            vertSpeedUnits = Units.VertSpeed.valueOf(prefs.getString("vertSpeedUnits", "FPM").toUpperCase().trim());
        } catch (Exception e) {
            vertSpeedUnits = Units.VertSpeed.FPM;
            saveNeeded = true;
        }
        gradientMaximumDiff = (int) altUnits.toM(Math.max(500, Math.min(5000, prefs.getInt("gradientMaximumDiff", 1000))));
        gradientMinimumDiff = (int) altUnits.toM(Math.max(100, Math.min(gradientMaximumDiff, prefs.getInt("gradientMinimumDiff", 500))));
        var screenWidthNm = Math.max(2, Math.min(50, prefs.getInt("screenWidth", 10)));
        screenWidthMetres = (int) distanceUnits.toM(Math.max(2, Math.min(50, prefs.getInt("screenWidth", 10))));
        circleRadiusStepMetres = (int) distanceUnits.toM(Math.max(1, Math.min(screenWidthNm, prefs.getInt("circleRadiusStep", 5))));
        var dangerRadiusString = prefs.getString("dangerRadius", "0.5");
        try {
            dangerRadiusMetres = (int) distanceUnits.toM(Math.max(0.1f, Math.min(screenWidthNm, Float.parseFloat(dangerRadiusString))));
        } catch (NumberFormatException ex) {
            Log.e("NumberFormatException: %s (%s)", ex.getMessage(), dangerRadiusString);
            dangerRadiusMetres = (int) distanceUnits.toM(0.5F);
        }
        minZoom = (int) (Math.max(dangerRadiusMetres, distanceUnits.toM(Math.min(screenWidthMetres, prefs.getInt("minZoom", 10)))));
        maxZoom = (int) (Math.max(screenWidthMetres, distanceUnits.toM(Math.min(50, prefs.getInt("maxZoom", 50)))));
        countryCode = prefs.getString("countryCode", "ZK").toUpperCase();
        ownCallsign = prefs.getString("ownCallsign", "ZKTHK").toUpperCase();
        try {
            ownId = Integer.parseInt(prefs.getString("ownId", "0"), 16);
        } catch (Exception e) {
            ownId = 0;
            saveNeeded = true;
        }

        try {
            displayOrientation = MapView.DisplayOrientation.valueOf(prefs.getString("displayOrientation", "TrackUp").trim());
        } catch (Exception e) {
            displayOrientation = MapView.DisplayOrientation.TrackUp;
            saveNeeded = true;
        }
        keepScreenOn = prefs.getBoolean("keepScreenOn", true);
        autoZoom = prefs.getBoolean("autoZoom", false);
        minGpsDistanceChangeMetres = prefs.getInt("minGpsDistanceChangeMetres", 10);
        minGpsUpdateIntervalSeconds = prefs.getInt("minGpsUpdateIntervalSeconds", 1);
        preferAdsbPosition = prefs.getBoolean("preferAdsbPosition", true);
        toolbarDelayMilliS = Math.max(1, Math.min(20, prefs.getInt("toolbarDelaySecs", 3))) * 1000;
        initToolbarDelayMilliS = Math.max(1, Math.min(20, prefs.getInt("initToolbarDelaySecs", 10))) * 1000;
        logReplay = prefs.getBoolean("logreplay", false);
        simulate = prefs.getBoolean("simulate", false);
        replaySpeedFactorString = prefs.getString("replaySpeedFactor", "10");
        try {
            replaySpeedFactor = Math.max(0.1f, Math.min(100f, Float.parseFloat(replaySpeedFactorString)));
        } catch (NumberFormatException ex) {
            Log.e("NumberFormatException: %s (%s)", ex.getMessage(), replaySpeedFactorString);
            replaySpeedFactor = 10;
        }
        useCupFile = prefs.getBoolean("useCupFile", true);
        try {
            labelText = Cup.Label.valueOf(prefs.getString("labelText", "Code").trim());
        } catch (Exception e) {
            labelText = Cup.Label.Code;
            saveNeeded = true;
        }
        showFrequency = prefs.getBoolean("showFrequency", true);
        showRunway = prefs.getBoolean("showRunway", true);
        magFieldUpdateDistance = (int) distanceUnits.toM(prefs.getInt("magFieldUpdateDistance", 30));

/*
        logReplay = true;
        saveNeeded = true;
*/
        if (saveNeeded) savePrefs();
        Log.log(logLevel, "Settings loaded");
    }

    public static void savePrefs() {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("version", BuildConfig.VERSION_NAME);
        edit.putString("wifiName", wifiName);
        edit.putString("port", "" + port);
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
        edit.putInt("predictionSeconds", predictionMilliS / 1000);
        edit.putInt("polynomialPredictionStepSeconds", polynomialPredictionStepMilliS / 1000);
        edit.putInt("polynomialHistoryMillis", polynomialHistoryMilliS);
        edit.putInt("gradientMaximumDiff", (int) altUnits.fromM(gradientMaximumDiff));
        edit.putInt("gradientMinimumDiff", (int) altUnits.fromM(gradientMinimumDiff));
        edit.putInt("screenYPosPercent", screenYPosPercent);
        edit.putInt("sensorSmoothingConstant", (int) (sensorSmoothingConstant * 100));
        edit.putInt("screenWidth", (int) distanceUnits.fromM(screenWidthMetres));
        edit.putInt("minZoom", (int) distanceUnits.fromM(minZoom));
        edit.putInt("maxZoom", (int) distanceUnits.fromM(maxZoom));
        edit.putInt("circleRadiusStep", (int) distanceUnits.fromM(circleRadiusStepMetres));
        edit.putString("dangerRadius", String.format(Locale.ENGLISH, "%.2f", distanceUnits.fromM(dangerRadiusMetres)));
        edit.putString("distanceUnits", String.valueOf(distanceUnits));
        edit.putString("altUnits", String.valueOf(altUnits));
        edit.putString("speedUnits", String.valueOf(speedUnits));
        edit.putString("vertSpeedUnits", String.valueOf(vertSpeedUnits));
        edit.putString("countryCode", countryCode);
        edit.putString("ownCallsign", ownCallsign);
        edit.putString("ownId", Integer.toHexString(ownId));
        edit.putString("displayOrientation", String.valueOf(displayOrientation));
        edit.putBoolean("keepScreenOn", keepScreenOn);
        edit.putBoolean("autoZoom", autoZoom);
        edit.putInt("toolbarDelaySecs", toolbarDelayMilliS / 1000);
        edit.putInt("initToolbarDelaySecs", initToolbarDelayMilliS / 1000);
        edit.putInt("minGpsDistanceChangeMetres", minGpsDistanceChangeMetres);
        edit.putInt("minGpsUpdateIntervalSeconds", minGpsUpdateIntervalSeconds);
        edit.putBoolean("preferAdsbPosition", preferAdsbPosition);
        edit.putBoolean("logreplay", logReplay);
        edit.putBoolean("simulate", simulate);
        edit.putString("replaySpeedFactor", String.format(Locale.ENGLISH, "%.2f", replaySpeedFactor));
        edit.putBoolean("useCupFile", useCupFile);
        edit.putString("labelText", labelText.toString());
        edit.putBoolean("showFrequency", showFrequency);
        edit.putBoolean("showRunway", showRunway);
        edit.putInt("magFieldUpdateDistance", (int) distanceUnits.fromM(magFieldUpdateDistance));
        edit.apply();
        Log.log(logLevel, "Settings saved");
    }
}