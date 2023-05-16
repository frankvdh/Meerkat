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
package com.meerkat.ui;

import static com.meerkat.ui.settings.SettingsViewModel.toolbarDelayMilliS;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.meerkat.R;
import com.meerkat.log.Log;

/**
 * Full-screen activity that shows and hides the status bar and navigation/system bar.
 */
public class HidingActionBar extends AppCompatActivity {

    private ActionBar actionBar;

    // If the back button is pressed (or swipe right->left), display the toolbar
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //       Log.i("key code %d, vis %d", keyCode, actionbarView.getVisibility());
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (actionBar.isShowing()) {
                actionBar.hide();
            } else {
                showActionbar(toolbarDelayMilliS);
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Schedules a call to hide() in milliseconds, canceling any previously scheduled calls.
     */
    @SuppressWarnings("SameParameterValue")
    void delayedHide(@SuppressWarnings("SameParameterValue") int delayMillis) {
        hideHandler.removeCallbacks(hideRunnable);
        hideHandler.postDelayed(hideRunnable, delayMillis);
    }

    Handler hideHandler = new Handler(Looper.myLooper());
    Runnable hideRunnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            actionBar.hide();
        }
    };

    void showActionbar(int visibleTime) {
        actionBar.show();
        delayedHide(visibleTime);
    }

    // Inflate the options menu when the user opens the menu for the first time
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Log.i("Click Settings");
//                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.action_aircraft_list:
                Log.i("Click Aircraft List");
//                startActivity(new Intent(this, AircraftListActivity.class));
                return true;

            case R.id.action_log:
                Log.i("Click Log");
                //               startActivity(new Intent(this, LogActivity.class));
                return true;

            case R.id.action_quit:
                Log.i("Click Quit");
                this.finish();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Let the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }
}