package com.meerkat;

import static com.meerkat.Settings.purgeSeconds;
import static java.util.concurrent.TimeUnit.MINUTES;

import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.log.Log;
import com.meerkat.measure.Position;
import com.meerkat.ui.map.MapFragment;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Executors;

public class VehicleList extends HashMap<Integer, Vehicle> {
    static public VehicleList vehicleList;

    private void purge() {
        long now = new Date().getTime();
        synchronized (this) {
            Iterator<HashMap.Entry<Integer, Vehicle> > iterator = this.entrySet().iterator();
            while (iterator.hasNext()) {
                HashMap.Entry<Integer, Vehicle> entry = iterator.next();
                if (now - entry.getValue().current.getTime() > purgeSeconds * 1000L) {
                    Log.i("Purge: " + entry.getValue().callsign + ", " + (now - entry.getValue().current.getTime()) + ", " + new Date(now) + ", " + new Date(entry.getValue().current.getTime()));
                    entry.getValue().layer.setVisible(false, false);
                    MapFragment.layers.invalidateDrawable(entry.getValue().layer);
                    iterator.remove();
                }
            }
        }
     }

    public VehicleList() {
        super();
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::purge, 1, 1, MINUTES);
    }

    public void upsert(String callsign, int participantAddr, Position point, Gdl90Message.Emitter emitterType) {
        Vehicle v = get(participantAddr);
        if (v != null) {
            v.update(point, callsign, emitterType);
        } else {
            v = new Vehicle(participantAddr, callsign, point, emitterType);
            put(participantAddr, v);
        }
        MapFragment.refresh(v.layer);
    }

    public Collection<Vehicle> getVehicles() {
        synchronized (this) {
            return values();
        }
    }

}
