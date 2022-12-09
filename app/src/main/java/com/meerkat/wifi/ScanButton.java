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
package com.meerkat.wifi;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.meerkat.log.Log;

public class ScanButton extends androidx.appcompat.widget.AppCompatButton {
    public ScanButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOnTouchListener(touchListener);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @SuppressLint("ClickableViewAccessibility")
    View.OnTouchListener touchListener = (view, motionEvent) -> {
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            Log.i("Click Scan");
            Intent taskIntent = new Intent(getContext(), WifiScanActivity.class);
            getContext().startActivity(taskIntent);
            return true;
        }
        return false;
    };

}
