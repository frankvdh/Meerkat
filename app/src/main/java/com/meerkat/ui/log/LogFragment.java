package com.meerkat.ui.log;

import static com.meerkat.Settings.showLog;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.meerkat.databinding.FragmentLogBinding;
import com.meerkat.log.Log;

public class LogFragment extends Fragment {

    private FragmentLogBinding binding;
    private TextView textView;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLogBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        textView = binding.textMessages;
        int maxTextViewWidth = textView.getMaxWidth();
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(maxTextViewWidth, View.MeasureSpec.AT_MOST);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        textView.measure(widthMeasureSpec, heightMeasureSpec);
        textView.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        textView.setGravity(Gravity.START);
        if (showLog) Log.useViewLogWriter(this);
        return root;
    }

    public void append(String str) {
        if (textView == null) return;
        Activity activity = getActivity();
        synchronized (textView) {
            if (textView.getLineCount() > 100) {
                String text = textView.getText().toString();
                String finalText = text.substring(text.indexOf('\n') + 1);
                activity.runOnUiThread(() -> {if (textView!=null) textView.setText(finalText);});
            }
            activity.runOnUiThread(() -> {if (textView!=null) textView.append(str + "\r\n");});
        }
    }

    public void clear() {
        getActivity().runOnUiThread(() -> {if (textView!=null) textView.setText("");});
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        textView = null;
    }
}