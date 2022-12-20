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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import com.meerkat.log.Log;
import com.meerkat.map.MapView;
import com.meerkat.measure.Units;

public class SettingsActivity extends AppCompatActivity {
    private static SharedPreferences prefs;
    public static String wifiName;
    public static int port;
    public static boolean showLog;
    public static boolean fileLog;
    public static boolean appendLogFile;
    public static com.meerkat.log.Log.Level logLevel;
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
    public static int predictionMilliS;
    public static int polynomialPredictionStepMilliS, polynomialHistoryMilliS;
    public static int gradientMaximumDiff;
    public static int gradientMinimumDiff;
    public static int screenYPosPercent;
    public static float sensorSmoothingConstant;
    public static int screenWidthMetres;
    public static int circleRadiusStepMetres, dangerRadiusMetres;
    public static Units.Distance distanceUnits;
    public static Units.Height altUnits;
    public static Units.Speed speedUnits;
    public static Units.VertSpeed vertSpeedUnits;
    public static boolean simulate;
    public static String countryCode;
    public static MapView.DisplayOrientation displayOrientation;
    public static boolean keepScreenOn;
    public static boolean autoZoom;
    /**
     * The number of milliseconds to wait after user interaction before hiding the system UI.
     */
    public static int toolbarDelayMilliS, initToolbarDelayMilliS;

    public static void load(Context context) {
        String currentVersionName = BuildConfig.VERSION_NAME;
        prefs = context.getSharedPreferences("com.meerkat_preferences", MODE_PRIVATE);
        String settingsVersionName = prefs.getString("version", null);
        Log.d("Current version = %s, Settings version = %s", currentVersionName, settingsVersionName);
        boolean saveNeeded = settingsVersionName == null || !settingsVersionName.equals(currentVersionName);

        wifiName = prefs.getString("wifiName", "Ping-6C7A");
        try {
            port = Integer.parseInt(prefs.getString("port", "4000"));
        } catch (Exception e) {
            Log.e("Invalid port %s", prefs.getString("port", null));
            saveNeeded = true;
        }
        showLog = prefs.getBoolean("showLog", true);
        fileLog = prefs.getBoolean("fileLog", true);
        appendLogFile = prefs.getBoolean("appendLogFile", false);
        try {
            logLevel = com.meerkat.log.Log.Level.valueOf(prefs.getString("logLevel", "I").toUpperCase().trim().substring(0, 1));
        } catch (Exception e) {
            Log.e("Invalid logLevel %s", prefs.getString("logLevel", null));
            logLevel = com.meerkat.log.Log.Level.I;
            saveNeeded = true;
        }
        logRawMessages = prefs.getBoolean("logRawMessages", false);
        logDecodedMessages = prefs.getBoolean("logDecodedMessages", false);
        showLinearPredictionTrack = prefs.getBoolean("showLinearPredictionTrack", true);
        showPolynomialPredictionTrack = prefs.getBoolean("showPolynomialPredictionTrack", true);
        historySeconds = Math.max(0, Math.min(300, prefs.getInt("historySeconds", 60)));
        purgeSeconds = Math.max(1, Math.min(300, prefs.getInt("purgeSeconds", 60)));
        predictionMilliS = Math.max(0, Math.min(300, prefs.getInt("predictionSeconds", 60))) * 1000;
        polynomialPredictionStepMilliS = Math.max(1, Math.min(60, prefs.getInt("polynomialPredictionStepSeconds", 10))) * 1000;
        polynomialHistoryMilliS = Math.max(1, Math.min(60, prefs.getInt("polynomialHistorySeconds", 10))) * 1000;
        gradientMaximumDiff = Math.max(1000, Math.min(5000, prefs.getInt("gradientMaximumDiff", 2500)));
        gradientMinimumDiff = Math.max(100, Math.min(gradientMaximumDiff, prefs.getInt("gradientMinimumDiff", 1000)));
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
        screenWidthMetres = (int) (Math.max(Units.Distance.NM.toM(2), Math.min(Units.Distance.NM.toM(50), prefs.getInt("screenWidth", (int) (Units.Distance.NM.toM(10))))));
        circleRadiusStepMetres = (int) (Math.max(Units.Distance.NM.toM(1), Math.min(screenWidthMetres, prefs.getInt("circleRadiusStep", (int) (Units.Distance.NM.toM(5))))));
        dangerRadiusMetres = (int) (Math.max(Units.Distance.NM.toM(1), Math.min(screenWidthMetres, prefs.getInt("dangerRadius", (int) (Units.Distance.NM.toM(1))))));
        countryCode = prefs.getString("countryCode", "").toUpperCase();
        try {
            displayOrientation = MapView.DisplayOrientation.valueOf(prefs.getString("displayOrientation", "HeadingUp").trim());
        } catch (Exception e) {
            displayOrientation = MapView.DisplayOrientation.HeadingUp;
            saveNeeded = true;
        }
        keepScreenOn = prefs.getBoolean("keepScreenOn", true);
        autoZoom = prefs.getBoolean("autoZoom", false);
        minGpsDistanceChangeMetres = prefs.getInt("minGpsDistanceChangeMetres", 10);
        minGpsUpdateIntervalSeconds = prefs.getInt("minGpsUpdateIntervalSeconds", 10);
        toolbarDelayMilliS = Math.max(1, Math.min(20, prefs.getInt("toolbarDelaySecs", 3)))*1000;
        initToolbarDelayMilliS = Math.max(1, Math.min(20, prefs.getInt("initToolbarDelaySecs", 10)))*1000;
        simulate = prefs.getBoolean("simulate", false);
        if (simulate)
            displayOrientation = MapView.DisplayOrientation.NorthUp;
        if (saveNeeded) save();
        Log.log(logLevel, "Settings loaded");
    }

    public static void save() {
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
        edit.putInt("polynomialHistorySeconds", polynomialHistoryMilliS / 1000);
        edit.putInt("gradientMaximumDiff", gradientMaximumDiff);
        edit.putInt("gradientMinimumDiff", gradientMinimumDiff);
        edit.putInt("screenYPosPercent", screenYPosPercent);
        edit.putInt("sensorSmoothingConstant", (int) (sensorSmoothingConstant * 100));
        edit.putInt("screenWidth", screenWidthMetres);
        edit.putInt("circleRadiusStep", circleRadiusStepMetres);
        edit.putInt("dangerRadius", dangerRadiusMetres);
        edit.putString("distanceUnits", String.valueOf(distanceUnits));
        edit.putString("altUnits", String.valueOf(altUnits));
        edit.putString("speedUnits", String.valueOf(speedUnits));
        edit.putString("vertSpeedUnits", String.valueOf(vertSpeedUnits));
        edit.putString("countryCode", countryCode);
        edit.putString("displayOrientation", String.valueOf(displayOrientation));
        edit.putBoolean("keepScreenOn", keepScreenOn);
        edit.putBoolean("autoZoom", autoZoom);
        edit.putInt("toolbarDelaySecs", toolbarDelayMilliS / 1000);
        edit.putInt("initToolbarDelaySecs", initToolbarDelayMilliS / 1000);
        edit.putInt("minGpsDistanceChangeMetres", minGpsDistanceChangeMetres);
        edit.putInt("minGpsUpdateIntervalSeconds", minGpsUpdateIntervalSeconds);
        edit.putBoolean("simulate", simulate);
        edit.apply();
        Log.log(logLevel, "Settings saved");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Settings");
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
    }

    // The "Home" button is clicked
    @Override
    public boolean onSupportNavigateUp() {

        load(getApplicationContext());
        // Close Settings activity which returns to the Map activity
        finish();
        return super.onSupportNavigateUp();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
            makeNumber("port");
            setRange("scrYPos", 5, 25, 95, 5);
            setRange("scrWidth", (int) Units.Distance.NM.toM(1), (int) Units.Distance.NM.toM(1), (int) Units.Distance.NM.toM(50), (int) Units.Distance.NM.toM(1));
            setRange("circleStep", (int) Units.Distance.NM.toM(1), (int) Units.Distance.NM.toM(1), (int) Units.Distance.NM.toM(25), (int) Units.Distance.NM.toM(1));
            setRange("dangerRadius", (int) Units.Distance.NM.toM(1), (int) Units.Distance.NM.toM(1), (int) Units.Distance.NM.toM(5), (int) Units.Distance.NM.toM(1));
            setRange("gradMaxDiff", 2100, 5000, 10000, 100);
            setRange("gradMinDiff", 100, 1000, 2000, 100);
            setRange("minGpsDistMetres", 1, 10, 50, 1);
            setRange("minGpsIntervalSeconds", 1, 5, 10, 1);
            setRange("historySecs", 0, 60, 300, 5);
            setRange("purgeSecs", 5, 60, 300, 5);
            setRange("predictionSecs", 5, 60, 300, 5);
            setRange("polynomialPredictionStepSecs", 1, 6, 60, 1);
            setRange("polynomialHistorySecs", 1, 10, 20, 1);
            setRange("sensorSmoothingConstant", 1, 20, 99, 5);
        }

        private void setRange(String key, int min, int defaultValue, int max, int inc) {
            SeekBarPreference seekBarPreference = findPreference(key);

            if (seekBarPreference == null) {
                Log.e("Unknown Seekbar preference: %s", key);
                return;
            }
            seekBarPreference.setSeekBarIncrement(inc);
            seekBarPreference.setMin(min);
            seekBarPreference.setMax(max);
            seekBarPreference.setValue(defaultValue);
        }

        private void makeNumber(@SuppressWarnings("SameParameterValue") String key) {
            EditTextPreference numberPreference = findPreference(key);

            if (numberPreference == null) {
                android.util.Log.e("Unknown number preference: %s", key);
                return;
            }
            numberPreference.setOnBindEditTextListener(eT -> eT.setInputType(InputType.TYPE_CLASS_NUMBER));
        }
    }
}
