package com.meerkat.wifi;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.meerkat.R;

public class WifiPreference extends Preference {
    public WifiPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.setLayoutResource(R.layout.wifi_preference_layout);
    }
}
