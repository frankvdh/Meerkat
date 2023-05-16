package com.meerkat.ui.map;

import androidx.lifecycle.ViewModel;

import com.meerkat.MainActivity;
import com.meerkat.databinding.FragmentMapBinding;
import com.meerkat.map.Background;

public class MapViewModel extends ViewModel {

    public MapViewModel() {
    }

    public void init(FragmentMapBinding binding) {
        Background background = new Background(binding.mapView, binding.compassView, binding.compassText, binding.scaleText);
        binding.mapView.layers.addLayer(background);
        MainActivity.mapView = binding.mapView;
    }
}