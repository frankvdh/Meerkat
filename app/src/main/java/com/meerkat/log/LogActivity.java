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

import static com.meerkat.SettingsActivity.keepScreenOn;
import static com.meerkat.SettingsActivity.showLog;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.meerkat.databinding.ActivityLogBinding;
import com.meerkat.log.Log;

import java.util.concurrent.ScheduledFuture;

public class LogActivity extends AppCompatActivity {

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.meerkat.databinding.ActivityLogBinding binding = ActivityLogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        textView = binding.textMessages;
        int maxTextViewWidth = textView.getMaxWidth();
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(maxTextViewWidth, View.MeasureSpec.AT_MOST);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        textView.measure(widthMeasureSpec, heightMeasureSpec);
        textView.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        textView.setGravity(Gravity.START);
        if (showLog) Log.useViewLogWriter(this);
        textView.setKeepScreenOn(keepScreenOn);
    }

    public void append(String str) {
        if (textView == null) return;
        synchronized (textView) {
            if (textView.getLineCount() > 100) {
                String text = textView.getText().toString();
                String finalText = text.substring(text.indexOf('\n') + 1);
                runOnUiThread(() -> {
                    if (textView != null) textView.setText(finalText);
                });
            }
            runOnUiThread(() -> {
                if (textView != null) textView.append(str + "\r\n");
            });
        }
    }

    public void clear() {
        runOnUiThread(() -> {
            if (textView != null) textView.setText("");
        });
    }

    private TextView view(String text) {
        TextView tv = new TextView(getApplicationContext());
        tv.setText(text);
        tv.setTextColor(Color.BLACK);
        tv.setGravity(Gravity.CENTER);
        return tv;
    }

    // The "Home" button is clicked
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}
