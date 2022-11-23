package com.meerkat;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.log.Log;
import com.meerkat.measure.Distance;
import com.meerkat.measure.Height;
import com.meerkat.measure.Polar;
import com.meerkat.measure.Position;
import com.meerkat.measure.Speed;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;

public class Simulator {

    private int nextActionTime;
    private int actionIndex;
    private Action action;
    private Flight flight;
    ScheduledFuture thread;

    private static final Flight[] flights = {
            new Flight(1, "test", Gdl90Message.Emitter.Light, new Polar(new Distance(5f, Distance.Units.NM), 0, new Height(0f, Height.Units.FT)),
                    new Speed(100f, Speed.Units.KNOTS), 0, 0, false, 0,
                    new Action[]{
                            new Action(0, 0, 0, 6, false),
                            new Action(0, 0, 1000, 12, true),
                            new Action(0, 360, 5000, 120, true),
                            new Action(0, 0, 0, 5, true),
                            new Action(0, -360, -10000, 120, true),
                    }),

            new Flight(2, "ZK-HVY", Gdl90Message.Emitter.Heavy, new Polar(new Distance(20f, Distance.Units.NM), 225, new Height(20000f, Height.Units.FT)),
                    new Speed(500f, Speed.Units.KNOTS), 20, 0, true,5,
                    new Action[]{
                            new Action(0, 0, 0, 300, true),
                    }),

            new Flight(3, "ZK-GLI", Gdl90Message.Emitter.Glider, new Polar(new Distance(25f, Distance.Units.NM), 15, new Height(5000f, Height.Units.FT)),
                    new Speed(100f, Speed.Units.KNOTS), 180, 0, true, 0,
                    new Action[]{
                            new Action(0, -1080, -9000, 300, true),
                            new Action(-100, 0, 0, 120, true),
                            new Action(0, 0, 0, 60, false),
                    }),

            new Flight(4, "ZK-HEL", Gdl90Message.Emitter.Rotor, new Polar(new Distance(25f, Distance.Units.NM), -15, new Height(0f, Height.Units.FT)),
                    new Speed(0f, Speed.Units.KNOTS), 180, 0, true, 10,
                    new Action[]{
                            new Action(0, 0, 1500, 100, true),
                            new Action(150, 90, 0, 20, true),
                            new Action(0, 0, 0, 60, true),
                    }),

            new Flight(5, "UAV", Gdl90Message.Emitter.UAV, new Polar(new Distance(15f, Distance.Units.NM), -30, new Height(0f, Height.Units.FT)),
                    new Speed(0f, Speed.Units.KNOTS), 0, 0, true, 0,
                    new Action[]{
                            new Action(10, 0, 500, 10, true),
                            new Action(0, 0, -500, 10, true),
                    }),

            new Flight(5, "UAV", Gdl90Message.Emitter.UAV, new Polar(new Distance(15f, Distance.Units.NM), -30, new Height(0f, Height.Units.FT)),
                    new Speed(0f, Speed.Units.KNOTS), 0, 0, true, 120,
                    new Action[]{
                            new Action(10, 0, 500, 10, true),
                            new Action(0, 0, -500, 10, true),
                    }),

            new Flight(6, "UAW", Gdl90Message.Emitter.UAV, new Polar(new Distance(16f, Distance.Units.NM), -30, new Height(0f, Height.Units.FT)),
                    new Speed(0f, Speed.Units.KNOTS), 0, 0, true, 240,
                    new Action[]{
                            new Action(10, 0, 500, 100, true),
                            new Action(0, 0, -500, 60, true),
                    })
    };


    private Simulator(Flight f) {
        flight = f;
        actionIndex = 0;
        nextActionTime = 0;
        Log.i("new Sim: " + flight.position.toString());
        thread = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::act, flight.initialDelay, 1, SECONDS);
    }

    public static void start() {
        for (Flight f : flights)
            new Simulator(f);
    }

    private void act() {
        if (actionIndex >= flight.actions.length) {
            Log.i("cancel Sim");
            this.thread.cancel(false);
            return;
        }
        if (--nextActionTime <= 0) {
            Log.d("Next action: " + this.flight.callsign);
            action = flight.actions[++actionIndex];
            nextActionTime = action.duration;
            flight.position.setAirborne(action.airborne);
            flight.position.setVVel(0);
        }
        flight.position.setCrcValid(true);
        flight.position.setSpeed(new Speed(flight.position.getSpeedUnits().value + action.accel, Speed.Units.KNOTS));
        flight.position.setTrack((flight.position.getTrack() + action.turn) % 360);
        flight.position.setVVel(flight.position.getVVel() + action.climb);
        flight.position.setTime(new Date().getTime());
        Polar p = new Polar(new Distance(flight.position.getSpeedMps(), Distance.Units.M), flight.position.getTrack(), new Height(flight.position.getVVel() * 1/60, Height.Units.FT));
        flight.position.moveBy(p);
        VehicleList.vehicleList.upsert(flight.callsign, flight.id, flight.position.linearPredict(1), flight.emitterType);
    }

    private static class Flight {
        Position position;
        int id;
        String callsign;
        Gdl90Message.Emitter emitterType;
        Action[] actions;
        final int initialDelay;

        Flight(int id, String callsign, Gdl90Message.Emitter emitterType, Polar p, Speed speed, float track, float vVel, boolean airborne, int initialDelay, Action[] actions) {
            this.id = id;
            this.callsign = callsign;
            this.emitterType = emitterType;
            this.initialDelay = initialDelay;
            this.actions = actions;
            position = new Position(Gps.location, p);
            position.setSpeed(speed);
            position.setTrack(track);
            position.setVVel(vVel);
            position.setAirborne(airborne);
        }

    }

    private static class Action {
        float accel;
        float turn;
        float climb;
        int duration;
        boolean airborne;

        Action(float accel, float turn, float climb, int dur, boolean airborne) {
            this.accel = accel / dur;
            this.turn = turn / dur;
            this.climb = climb / dur;
            duration = dur;
            this.airborne = airborne;
        }
    }
}
