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

    package com.meerkat.ui.settings;


    import static com.meerkat.ui.settings.SettingsViewModel.dangerRadiusMetres;
    import static com.meerkat.ui.settings.SettingsViewModel.distanceUnits;
    import static com.meerkat.ui.settings.SettingsViewModel.loadPrefs;
    import static com.meerkat.ui.settings.SettingsViewModel.port;
    import static com.meerkat.ui.settings.SettingsViewModel.prefs;
    import static com.meerkat.ui.settings.SettingsViewModel.replaySpeedFactor;
    import static com.meerkat.ui.settings.SettingsViewModel.replaySpeedFactorString;
    import static com.meerkat.ui.settings.SettingsViewModel.screenWidthMetres;

    import android.content.SharedPreferences;
    import android.os.Bundle;
    import android.text.InputType;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;

    import androidx.annotation.NonNull;
    import androidx.fragment.app.Fragment;
    import androidx.lifecycle.ViewModelProvider;
    import androidx.preference.EditTextPreference;
    import androidx.preference.PreferenceFragmentCompat;
    import androidx.preference.SeekBarPreference;

    import com.meerkat.R;
    import com.meerkat.databinding.FragmentSettingsBinding;
    import com.meerkat.log.Log;

    import java.util.Locale;

    public class SettingsFragment extends Fragment {

        private FragmentSettingsBinding binding;

        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            SettingsViewModel settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

            binding = FragmentSettingsBinding.inflate(inflater, container, false);
            View root = binding.getRoot();
            Log.d("Create SettingsView");
            super.onCreate(savedInstanceState);
            settingsViewModel.init();
            if (savedInstanceState == null) {
                this.getChildFragmentManager().beginTransaction().replace(R.id.settings, new PreferenceFragment()).commit();
            }
            replaySpeedFactorString = String.format(Locale.ENGLISH, "%.2f", replaySpeedFactor);
            return root;
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            binding = null;
        }


        public void onResume() {
            Log.i("SettingsActivity Resume");
            super.onResume();
            //Setup a shared preference listener for hpwAddress and restart transport
            SharedPreferences.OnSharedPreferenceChangeListener listener = (prefs, key) -> {
                if (key.equals("wifiName")) {
                    Log.i("Wifi Name changed");
//                        EditText editTextWifiName = getContext().getApplicationContext().findViewById(R.id.editTextWifiName);
//                        editTextWifiName.setText(wifiName);
                }
            };
            prefs.registerOnSharedPreferenceChangeListener(listener);
        }

        // The "Return" button is clicked...
        // Reload from storage to get changes into public static variables
        // Strings, booleans, and ints can be edited directly, and are saved automatically
        // Floats cannot be edited, so they are edited as strings, which are saved automatically.
        // Preferences are then reloaded, which recalculates the float values
        @Override
        public void onDestroy() {
            loadPrefs(requireContext());
            super.onDestroy();
        }

        public static class PreferenceFragment extends PreferenceFragmentCompat {

            @Override
            public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
                setPreferencesFromResource(R.xml.preferences, rootKey);
                makeNumber("port", port);
                makeNumber("replaySpeedFactor", replaySpeedFactor);
                makeNumber("dangerRadius", (float) distanceUnits.fromM(dangerRadiusMetres));
                setRange("scrYPos", 5, 25, 95, 5);
                setRange("scrWidth", 1, 1, 50, 1);
                setRange("minZoom", 1, (int) distanceUnits.fromM(screenWidthMetres), 10, 1);
                setRange("maxZoom", (int) distanceUnits.fromM(screenWidthMetres), 50, 50, 1);
                setRange("circleRadiusStep", 1, 1, 25, 1);
                setRange("gradMaxDiff", 1000, 1000, 5000, 100);
                setRange("gradMinDiff", 100, 500, 2000, 100);
                setRange("minGpsDistMetres", 1, 10, 50, 1);
                setRange("minGpsIntervalSeconds", 1, 5, 10, 1);
                setRange("historySecs", 0, 60, 300, 5);
                setRange("purgeSecs", 5, 60, 300, 5);
                setRange("predictionSecs", 5, 60, 300, 5);
                setRange("polynomialPredictionStepSecs", 1, 6, 60, 1);
                setRange("polynomialHistoryMillis", 1000, 2500, 10000, 100);
                setRange("sensorSmoothingConstant", 1, 20, 99, 5);
                setRange("magFieldUpdateKm", 1, 30, 99, 1);
            }


            /**
             * Set a SeekBar with the given values for the given setting
             * All paramaters are in the user's units
             *
             * @param key          Preference string
             * @param min          minimum value for seekBar
             * @param defaultValue default value
             * @param max          maximum value
             * @param inc          increment
             */
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
                var summary = seekBarPreference.getSummary();
                seekBarPreference.setSummary(summary == null ? "" : summary.toString().replace("[DISTANCE_UNITS]", distanceUnits.units.label));
                var title = seekBarPreference.getTitle();
                seekBarPreference.setTitle(title == null ? "" : title.toString().replace("[DISTANCE_UNITS]", distanceUnits.units.label));
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

            private void makeNumber(String key, float value) {
                EditTextPreference numberPreference = findPreference(key);

                if (numberPreference == null) {
                    android.util.Log.e("Unknown float number preference: %s", key);
                    return;
                }
                numberPreference.setText(String.format(Locale.ENGLISH, "%.2f", value));
                numberPreference.setOnBindEditTextListener(eT -> eT.setInputType(InputType.TYPE_CLASS_NUMBER));
            }
        }
    }
