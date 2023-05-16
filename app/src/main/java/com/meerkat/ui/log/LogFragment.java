package com.meerkat.ui.log;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.meerkat.databinding.FragmentLogBinding;
import com.meerkat.log.Log;

public class LogFragment extends Fragment {

    private FragmentLogBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogViewModel logViewModel = new ViewModelProvider(this).get(LogViewModel.class);

        binding = FragmentLogBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        Log.d("Create LogView");
        super.onCreate(savedInstanceState);
        FragmentLogBinding binding = FragmentLogBinding.inflate(inflater, container, false);
        logViewModel.init(binding);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}