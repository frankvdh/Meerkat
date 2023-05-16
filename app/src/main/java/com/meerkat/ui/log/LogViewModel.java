package com.meerkat.ui.log;

import static com.meerkat.ui.settings.SettingsViewModel.keepScreenOn;
import static com.meerkat.ui.settings.SettingsViewModel.showLog;

import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import androidx.lifecycle.ViewModel;

import com.meerkat.databinding.FragmentLogBinding;
import com.meerkat.log.Log;

public class LogViewModel extends ViewModel {

    private TextView textView;
    private int numLines;

    public LogViewModel() {
    }

    public void init(FragmentLogBinding binding) {
        textView = binding.textMessages;
        numLines = textView.getContext().getResources().getDisplayMetrics().heightPixels / textView.getLineHeight();
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
        // Because this always runs on the UI thread, there's no need to synchronize
//        runOnUiThread(() -> {
        if (textView.getLineCount() >= numLines) {
            String text = textView.getText().toString();
            textView.setText(text.substring(text.indexOf('\n') + 1));
        }
        textView.append(str + "\r\n");
        //        });
    }

    @SuppressWarnings("unused")
    public void clear() {
//        runOnUiThread(() -> {
        if (textView != null) textView.setText("");
//        });
    }
}