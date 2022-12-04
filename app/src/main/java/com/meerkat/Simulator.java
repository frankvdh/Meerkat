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
package com.meerkat;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.meerkat.gdl90.Gdl90Message;
import com.meerkat.log.Log;
import com.meerkat.measure.Distance;
import com.meerkat.measure.Height;
import com.meerkat.measure.Polar;
import com.meerkat.measure.Position;
import com.meerkat.measure.Speed;
import com.meerkat.measure.VertSpeed;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;

public class Simulator {

    private static final Position initialPos = new Position("gps", -(40 + 4 / 60.0 + 9 / 3600.0), 175 + 22 / 60.0 + 42 / 3600.0,
            new Height(5000f, Height.Units.FT), new Speed(100f, Speed.Units.KNOTS), 350f, new VertSpeed(0f, VertSpeed.Units.FPM), true, true, System.currentTimeMillis());

    private int nextActionTime;
    private int actionIndex;
    private Action action;
    private final Flight flight;
    private final int initialDelay;

    private final boolean isGps;
    ScheduledFuture<?> thread;

    private static final Flight ownShip =
            new Flight("ownship", Gdl90Message.Emitter.Light, new Polar(new Distance(0f, Distance.Units.NM), 0, new Height(0f, Height.Units.FT)),
                    new Speed(100f, Speed.Units.KNOTS), 20, true,
                    new Action[]{
                            new Action(0, 0, 0, 6, false),
                            new Action(0, 0, 1000, 12, true),
                            new Action(0, 360, 5000, 120, true),
                            new Action(0, 0, 0, 5, true),
                            new Action(0, -360, -10000, 120, true),
                    });

    private static final Simulator[] traffic = {
            new Simulator(new Flight("test", Gdl90Message.Emitter.Light, new Polar(new Distance(5f, Distance.Units.NM), 0, new Height(0f, Height.Units.FT)),
                    new Speed(100f, Speed.Units.KNOTS), 0, false,
                    new Action[]{
                            new Action(0, 0, 0, 6, false),
                            new Action(0, 0, 1000, 12, true),
                            new Action(0, 360, 5000, 120, true),
                            new Action(0, 0, 0, 5, true),
                            new Action(0, -360, -10000, 120, true),
                    }), 5, false),

            new Simulator(new Flight("ZK-HVY", Gdl90Message.Emitter.Heavy, new Polar(new Distance(20f, Distance.Units.NM), 225, new Height(40000f, Height.Units.FT)),
                    new Speed(500f, Speed.Units.KNOTS), 20, true,
                    new Action[]{
                            new Action(0, 0, 0, 300, true),
                    }), 10, false),

            new Simulator(new Flight("ANZ123", Gdl90Message.Emitter.Small, new Polar(new Distance(10f, Distance.Units.NM), 215, new Height(20000f, Height.Units.FT)),
                    new Speed(250f, Speed.Units.KNOTS), 20, true,
                    new Action[]{
                            new Action(0, 0, 0, 300, true),
                    }), 10, false),

            new Simulator(new Flight("ZK-GLI", Gdl90Message.Emitter.Glider, new Polar(new Distance(15f, Distance.Units.NM), 15, new Height(5000f, Height.Units.FT)),
                    new Speed(100f, Speed.Units.KNOTS), 180, true,
                    new Action[]{
                            new Action(0, -1080, -9000, 300, true),
                            new Action(-100, 0, 0, 120, true),
                            new Action(0, 0, 0, 60, false),
                    }), 5, false),

            new Simulator(new Flight("ZK-HEL", Gdl90Message.Emitter.Rotor, new Polar(new Distance(15f, Distance.Units.NM), -15, new Height(0f, Height.Units.FT)),
                    new Speed(0f, Speed.Units.KNOTS), 180, true,
                    new Action[]{
                            new Action(0, 0, 1500, 100, true),
                            new Action(150, 90, 0, 20, true),
                            new Action(0, 0, 0, 60, true),
                    }), 15, false),

            new Simulator(new Flight("UAV", Gdl90Message.Emitter.UAV, new Polar(new Distance(15f, Distance.Units.NM), -30, new Height(0f, Height.Units.FT)),
                    new Speed(0f, Speed.Units.KNOTS), 0, true,
                    new Action[]{
                            new Action(10, 0, 500, 10, true),
                            new Action(0, 0, -500, 10, true),
                    }), 5, false),

            new Simulator(new Flight("UAV", Gdl90Message.Emitter.UAV, new Polar(new Distance(15f, Distance.Units.NM), -30, new Height(0f, Height.Units.FT)),
                    new Speed(0f, Speed.Units.KNOTS), 0, true,
                    new Action[]{
                            new Action(10, 0, 500, 10, true),
                            new Action(0, 0, -500, 10, true),
                    }), 120, false),

            new Simulator(new Flight("UAW", Gdl90Message.Emitter.UAV, new Polar(new Distance(16f, Distance.Units.NM), -30, new Height(0f, Height.Units.FT)),
                    new Speed(0f, Speed.Units.KNOTS), 0, true,
                    new Action[]{
                            new Action(10, 0, 500, 100, true),
                            new Action(0, 0, -500, 60, true),
                    }), 240, false)
    };

    private Simulator(Flight f, int initialDelay, boolean isGps) {
        flight = f;
        actionIndex = -1;
        nextActionTime = 0;
        this.isGps = isGps;
        this.initialDelay = initialDelay;
        Log.i("new Sim: %s %s", flight.callsign, flight.position.toString());
    }

    public static void startAll() {
        new Simulator(ownShip, 0, true).start();
        for (Simulator s : traffic)
            s.start();
    }

    private void start() {
        thread = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::act, initialDelay, 1, SECONDS);
    }

    private void act() {
         if (--nextActionTime <= 0) {
             if (++actionIndex >= flight.actions.length) {
                 Log.i("cancel Sim " + flight.callsign);
                 this.thread.cancel(false);
                 return;
             }
             Log.d("Next action: %s @ %s", flight.callsign, flight.position);
            action = flight.actions[actionIndex];
            nextActionTime = action.duration;
            flight.position.setProvider("Sim");
            flight.position.setAirborne(action.airborne);
            flight.position.setVVel(action.climb);
        }
        flight.position.setCrcValid(true);
        flight.position.setSpeed(new Speed(flight.position.getSpeedUnits().value + action.accel, Speed.Units.KNOTS));
        flight.position.setTrack((flight.position.getTrack() + action.turn) % 360);
        flight.position.setTime(System.currentTimeMillis());
        if (isGps)
            Gps.setLocation(flight.position);
        else {
            Polar p = new Polar(new Distance(flight.position.getSpeedMps(), Distance.Units.M), flight.position.getTrack(), new Height(flight.position.getVVel().value * flight.position.getVVel().units.factor, Height.Units.M));
            flight.position.moveBy(p);
            VehicleList.vehicleList.upsert(flight.callsign, flight.id, flight.position.linearPredict(1000), flight.emitterType);
        }
    }

    static class Flight {
        final Position position;
        final int id;
        final String callsign;
        final Gdl90Message.Emitter emitterType;
        final Action[] actions;
        static int idNum = 0;

        Flight(String callsign, Gdl90Message.Emitter emitterType, Polar p, Speed speed, float track, boolean airborne, Action[] actions) {
            this.id = idNum++;
            this.callsign = callsign;
            this.emitterType = emitterType;
            this.actions = actions;
            position = new Position(initialPos, p);
            position.setSpeed(speed);
            position.setTrack(track);
            position.setVVel(new VertSpeed(0f, VertSpeed.Units.FPM));
            position.setAirborne(airborne);
        }

    }

    static class Action {
        final float accel;
        final float turn;
        final VertSpeed climb;
        final int duration;
        final boolean airborne;

        Action(float accel, float turn, float climb, int dur, boolean airborne) {
            this.accel = accel / dur;
            this.turn = turn / dur;
            this.climb = new VertSpeed(climb * 60 / dur, VertSpeed.Units.FPM);
            duration = dur;
            this.airborne = airborne;
        }
    }
}
