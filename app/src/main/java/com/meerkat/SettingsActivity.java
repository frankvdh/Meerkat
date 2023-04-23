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
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import com.meerkat.log.Log;
import com.meerkat.map.MapView;
import com.meerkat.measure.Units;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    private static SharedPreferences prefs;
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
    public static volatile boolean simulate;
    public static volatile float simulateSpeedFactor;
    public static volatile String simulateSpeedFactorString;
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
        gradientMinimumDiff = (int)altUnits.toM( Math.max(100, Math.min(gradientMaximumDiff, prefs.getInt("gradientMinimumDiff", 1000))));
        var screenWidthNm = (int) Math.max(2, Math.min(50, prefs.getInt("screenWidth", 10)));
        screenWidthMetres = (int) distanceUnits.toM(Math.max(2, Math.min(50, prefs.getInt("screenWidth", 10))));
        circleRadiusStepMetres = (int) distanceUnits.toM(Math.max(1, Math.min(screenWidthNm, prefs.getInt("circleRadiusStep", 5))));
        dangerRadiusMetres = (int) distanceUnits.toM(Math.max(1, Math.min(screenWidthNm, prefs.getInt("dangerRadius", 1))));
        minZoom = (int) (Math.max(dangerRadiusMetres, Math.min(screenWidthMetres, prefs.getInt("minZoom", (int) (distanceUnits.toM(10))))));
        maxZoom = (int) (Math.max(screenWidthMetres, Math.min(distanceUnits.toM(50), prefs.getInt("maxZoom", (int) (distanceUnits.toM(50))))));
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
        autoZoom = prefs.getBoolean("autoZoom", true);
        minGpsDistanceChangeMetres = prefs.getInt("minGpsDistanceChangeMetres", 10);
        minGpsUpdateIntervalSeconds = prefs.getInt("minGpsUpdateIntervalSeconds", 1);
        preferAdsbPosition = prefs.getBoolean("preferAdsbPosition", true);
        toolbarDelayMilliS = Math.max(1, Math.min(20, prefs.getInt("toolbarDelaySecs", 3))) * 1000;
        initToolbarDelayMilliS = Math.max(1, Math.min(20, prefs.getInt("initToolbarDelaySecs", 10))) * 1000;
        simulate = prefs.getBoolean("simulate", false);
        simulateSpeedFactorString = prefs.getString("simulateSpeedFactorString", "10");
        try {
            simulateSpeedFactor = Math.max(0.1f, Math.min(100f, Float.parseFloat(simulateSpeedFactorString)));
        } catch (NumberFormatException ex) {
            Log.e("NumberFormatException: %s (%s)", ex.getMessage(), simulateSpeedFactorString);
            simulateSpeedFactor = 10;
        }


        simulate = true;
        polynomialHistoryMilliS = 2000;
        simulateSpeedFactor = 10;
        maxZoom = (int) distanceUnits.toM(50);
        autoZoom = false;
        saveNeeded = true;

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
        edit.putInt("minZoom", minZoom);
        edit.putInt("maxZoom", maxZoom);
        edit.putInt("circleRadiusStep", (int) distanceUnits.fromM(circleRadiusStepMetres));
        edit.putInt("dangerRadius", (int) distanceUnits.fromM(dangerRadiusMetres));
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
        edit.putBoolean("simulate", simulate);
        edit.putString("simulateSpeedFactorString", String.format(Locale.ENGLISH, "%.2f", simulateSpeedFactor));
        edit.apply();
        Log.log(logLevel, "Settings saved");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        simulateSpeedFactorString = String.format(Locale.ENGLISH, "%.2f", simulateSpeedFactor);
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

    public void onResume() {
        Log.i("SettingsActivity Resume");
        super.onResume();
        //Setup a shared preference listener for hpwAddress and restart transport
        SharedPreferences.OnSharedPreferenceChangeListener listener = (prefs, key) -> {
            if (key.equals("wifiName")) {
                Log.i("Wifi Name changed");
                EditText editTextWifiName = findViewById(R.id.editTextWifiName);
                editTextWifiName.setText(wifiName);
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }


    // The "Return" button is clicked...
    // Reload from storage to get changes into public variables
    // Strings, booleans, and ints can be edited directly, and are saved automatically
    // Floats cannot be edited, so they are edited as strings, which are saved automatically.
    // Preferences are then reloaded, which recalculates the float values
    @Override
    public void onDestroy() {
        loadPrefs(getApplicationContext());
        super.onDestroy();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
            makeNumber("port", port);
            makeNumber("simulateSpeedFactorString", simulateSpeedFactor);
            setRange("scrYPos", 5, 25, 95, 5);
            setRange("scrWidth", 1, 1, 50, 1,  distanceUnits.units.factor);
            setRange("minZoom", 1, screenWidthMetres, 10, 1,  distanceUnits.units.factor);
            setRange("maxZoom", screenWidthMetres, 50, 50, 1,  distanceUnits.units.factor);
            setRange("circleStep", 1, 1, 25, 1,  distanceUnits.units.factor);
            setRange("dangerRadius", 1, 1, screenWidthMetres, 1,  distanceUnits.units.factor);
            setRange("gradMaxDiff", 1000, 1000, 5000, 100, Units.Height.FT.units.factor);
            setRange("gradMinDiff", 100, 500, 2000, 100, Units.Height.FT.units.factor);
            setRange("minGpsDistMetres", 1, 10, 50, 1);
            setRange("minGpsIntervalSeconds", 1, 5, 10, 1);
            setRange("historySecs", 0, 60, 300, 5);
            setRange("purgeSecs", 5, 60, 300, 5);
            setRange("predictionSecs", 5, 60, 300, 5);
            setRange("polynomialPredictionStepSecs", 1, 6, 60, 1);
            setRange("polynomialHistoryMillis", 1000, 2500, 10000, 100);
            setRange("sensorSmoothingConstant", 1, 20, 99, 5);
        }

        private void setRange(String key, int min, int defaultValue, int max, int inc) {
            setRange(key, min, defaultValue, max, inc, 1);
            }

            private void setRange(String key, int min, int defaultValue, int max, int inc, float factor) {
            SeekBarPreference seekBarPreference = findPreference(key);

            if (seekBarPreference == null) {
                Log.e("Unknown Seekbar preference: %s", key);
                return;
            }
            seekBarPreference.setSeekBarIncrement((int) (inc/factor));
            seekBarPreference.setMin((int) (min/factor));
            seekBarPreference.setMax((int) (max/factor));
            seekBarPreference.setValue((int) (defaultValue/factor));
        }

        private void makeNumber(@SuppressWarnings("SameParameterValue") String key, int value) {
            EditTextPreference numberPreference = findPreference(key);

            if (numberPreference == null) {
                android.util.Log.e("Unknown int number preference: %s", key);
                return;
            }
            numberPreference.setText(Integer.toString(value));
            numberPreference.setOnBindEditTextListener(eT -> eT.setInputType(InputType.TYPE_CLASS_NUMBER));
        }

        private void makeNumber(@SuppressWarnings("SameParameterValue") String key, float value) {
            EditTextPreference numberPreference = findPreference(key);

            if (numberPreference == null) {
                android.util.Log.e("Unknown float number preference: %s", key);
                return;
            }
            numberPreference.setText(String.format(Locale.ENGLISH, "%.02f", value));
            numberPreference.setOnBindEditTextListener(eT -> eT.setInputType(InputType.TYPE_CLASS_NUMBER));
        }
    }
}
