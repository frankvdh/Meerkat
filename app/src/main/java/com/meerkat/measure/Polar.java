package com.meerkat.measure;

import static com.meerkat.Settings.altUnits;
import static com.meerkat.Settings.distanceUnits;

import android.location.Location;

public class Polar {
    public double bearing;
    public Distance distance;
    public Height altDifference;

    public Polar() {
        distance = new Distance();
        altDifference = new Height();
    }


    public void set(Location base, Position point) {
        distance.set(base.distanceTo(point) / distanceUnits.factor, distanceUnits);
        bearing = base.bearingTo(point);
        altDifference.set(point.isAirborne() ?(float) (point.getAltitude()-base.getAltitude()) / altUnits.factor : Float.NaN, altUnits);
    }


    public Polar(Distance d, double b, Height a) {
        distance = d;
        bearing = b;
        altDifference = a;
    }
}

