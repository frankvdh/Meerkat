package com.meerkat.ui.aircraftlist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.meerkat.databinding.FragmentAircraftlistBinding;

public class AircraftListFragment extends Fragment {

    private FragmentAircraftlistBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        AircraftListViewModel aircraftListViewModel = new ViewModelProvider(this).get(AircraftListViewModel.class);
        binding = FragmentAircraftlistBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        aircraftListViewModel.init(binding);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}