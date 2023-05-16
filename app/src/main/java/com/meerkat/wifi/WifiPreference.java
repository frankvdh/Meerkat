package com.meerkat.wifi;

import static com.meerkat.ui.settings.SettingsViewModel.wifiName;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.meerkat.R;

public class WifiPreference extends Preference {
    public WifiPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.setLayoutResource(R.layout.wifi_preference_layout);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.itemView.setClickable(false); // disable parent click
        EditText editTextWifiName = (EditText) holder.findViewById(R.id.editTextWifiName);
        editTextWifiName.setText(wifiName);
    }
}
