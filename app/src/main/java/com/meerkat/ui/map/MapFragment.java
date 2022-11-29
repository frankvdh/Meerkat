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
package com.meerkat.ui.map;

import static com.meerkat.Settings.screenWidth;
import static com.meerkat.Settings.trackUp;
import static com.meerkat.ui.map.AircraftLayer.loadIcon;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.meerkat.Gps;
import com.meerkat.databinding.FragmentMapBinding;
import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.log.Log;

public class MapFragment extends Fragment {

    private FragmentMapBinding binding;
    static Background background;
    public static LayerDrawable layers;
    static float scaleFactor;
    // Used to detect pinch zoom gesture.
    private ScaleGestureDetector scaleGestureDetector = null;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("createView");
        binding = FragmentMapBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        ImageView mapView = binding.mapview;
        background = new Background(getContext(), binding.compassView, binding.compassText, getWidth(getContext()));
        layers = new LayerDrawable(new Drawable[]{background});
        mapView.setImageDrawable(layers);

        for (var emitterType : Gdl90Message.Emitter.values()) {
            emitterType.bitmap = loadIcon(getContext(), emitterType.iconId);
        }

        // Attach a pinch zoom listener to the map view
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new PinchListener(mapView));
        mapView.setOnTouchListener(handleTouch);
        Log.d("finished creating");
        return root;
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        scaleFactor = getWidth(getContext()) / screenWidth / 2;
    }

    //get width screen
    public static int getWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    static Matrix positionMatrix(int centreX, int centreY, float x, float y, float angle) {
        Matrix matrix = new Matrix();
        // Rotate about centre of icon & translate to bitmap position
        matrix.setRotate(trackUp && Gps.location.hasBearing() ? angle - Gps.location.getBearing() : angle, centreX, centreY);
        matrix.postTranslate(x - centreX, y - centreY);
        return matrix;
    }

    private final View.OnTouchListener handleTouch = (view, event) -> {
//        getView().performClick();
        // Dispatch activity on touch event to the scale gesture detector.
        return scaleGestureDetector.onTouchEvent(event);
    };

    public static void refresh(AircraftLayer layer) {
        if (layer == null)
            MapFragment.layers.invalidateSelf();
        else
            MapFragment.layers.invalidateDrawable(layer);
    }

    /* This listener is used to listen pinch zoom gesture. */
    private static class PinchListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        private final ImageView mapView;

        // The default constructor pass context and imageview object.
        public PinchListener(ImageView mapView) {
            this.mapView = mapView;
        }

        // When pinch zoom gesture occurred.
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            // Scale the image with pinch zoom value.
            scaleFactor *= detector.getScaleFactor() * mapView.getScaleX();
            refresh(null);
            return true;
        }
    }

}