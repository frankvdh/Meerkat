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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.meerkat.databinding.FragmentMapBinding;
import com.meerkat.log.Log;

public class MapFragment extends Fragment {

    private FragmentMapBinding binding;
    Background background;
    public static LayerDrawable layers;
    static float scaleFactor;
    // Used to detect pinch zoom gesture.
    private ScaleGestureDetector scaleGestureDetector = null;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("createView");

        binding = FragmentMapBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        ImageView mapView = binding.mapview;

        // Attach a pinch zoom listener to the map view
        if (scaleGestureDetector == null) {
            PinchListener pinchListener = new PinchListener(mapView);
            scaleGestureDetector = new ScaleGestureDetector(getContext(), pinchListener);
        }

        background = new Background();
        layers = new LayerDrawable(new Drawable[]{background});
         mapView.setImageDrawable(layers);
        Log.d("finished creating");
        return root;
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        scaleFactor = getWidth(getContext())/ screenWidth/2;
    }

    //get width screen
    public static int getWidth(Context context){
        return context.getResources().getDisplayMetrics().widthPixels;
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public boolean onTouchEvent(MotionEvent event) {
        // Dispatch activity on touch event to the scale gesture detector.
        scaleGestureDetector.onTouchEvent(event);
        return true;
    }

//    @Override
//    public boolean performClick() {
//        super.performClick();
//        return false;
//    }
//
//    @Override public boolean dispatchTouchEvent(MotionEvent event) {
//        mLastTouchPoint = new Point((int) event.getX(), (int) event.getY());
//        postInvalidate();
//        return super.dispatchTouchEvent(event);
//    }

    public static void refresh(AircraftLayer layer) {
        if (layer == null)
            MapFragment.layers.invalidateSelf();
        else
            MapFragment.layers.invalidateDrawable(layer);
    }

    /* This listener is used to listen pinch zoom gesture. */
    private static class PinchListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        private final static String TAG_PINCH_LISTENER = "PINCH_LISTENER";
        private final ImageView mapView;

        // The default constructor pass context and imageview object.
        public PinchListener(ImageView mapView) {
            this.mapView = mapView;
        }

        // When pinch zoom gesture occurred.
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (detector == null) {
                Log.e(TAG_PINCH_LISTENER, "Pinch listener onScale detector parameter is null.");
                return false;
            }
            // Scale the image with pinch zoom value.
            scaleFactor *= detector.getScaleFactor() * mapView.getScaleX();
            refresh(null);
            return true;
        }
    }

}