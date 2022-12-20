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
package com.meerkat.test;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static java.lang.Math.PI;

import com.meerkat.measure.Position;
import com.meerkat.measure.Units;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PositionTest extends TestCase {
    @Test
    public void testBearingToRad() {
        Assert.assertEquals(PI / 2, Position.bearingToRad(0), 0.1);
        Assert.assertEquals(PI / 4, Position.bearingToRad(45), 0.1);
        Assert.assertEquals(0, Position.bearingToRad(90), 0.1);
        Assert.assertEquals(-PI / 4, Position.bearingToRad(135), 0.1);
        Assert.assertEquals(-PI / 2, Position.bearingToRad(180), 0.1);
        Assert.assertEquals(-3 * PI / 4, Position.bearingToRad(225), 0.1);
        Assert.assertEquals(-PI, Position.bearingToRad(270), 0.1);
        Assert.assertEquals(3 * PI / 4, Position.bearingToRad(315), 0.1);
        Assert.assertEquals(PI / 2, Position.bearingToRad(360), 0.1);
    }

    @Test
    public void testRadToBearing() {
        Assert.assertEquals(0, Position.radToBearing(PI / 2), 0.1);
        Assert.assertEquals(45, Position.radToBearing(PI / 4), 0.1);
        Assert.assertEquals(90, Position.radToBearing(0), 0.1);
        Assert.assertEquals(135, Position.radToBearing(-PI / 4), 0.1);
        Assert.assertEquals(180, Position.radToBearing(-PI / 2), 0.1);
        Assert.assertEquals(225, Position.radToBearing(-3 * PI / 4), 0.1);
        Assert.assertEquals(270, Position.radToBearing(-PI), 0.1);
        Assert.assertEquals(270, Position.radToBearing(PI), 0.1);
        Assert.assertEquals(315, Position.radToBearing(3 * PI / 4), 0.1);
        Assert.assertEquals(0, Position.radToBearing(PI / 2), 0.1);
    }

    @Mock
   static Position marton;
    static {
        marton  = new Position("TEST", -(40 + 4 / 60.0 + 9 / 3600.0), 175 + 22 / 60.0 + 42 / 3600.0, Units.Height.FT.toM(5000), 0L);
        when(marton.getTime()).thenReturn(0L);
        doNothing().when(marton).setTime(isA(Long.class));
    }
    @Mock
    Position p1;

    @Test
    public void testLinearPredict() {
        long now = System.currentTimeMillis();
        when(marton.getTime()).thenReturn(now);
        doNothing().when(marton).setTime(isA(Long.class));
        when(marton.getLatitude()).thenReturn(-(40 + 4 / 60.0 + 9 / 3600.0));
        when(marton.getLongitude()).thenReturn(175 + 22 / 60.0 + 42 / 3600.0);
        when(marton.getAltitude()).thenReturn(Units.Height.FT.toM(5000));
        when(marton.getSpeed()).thenReturn(123f);
        when(marton.hasSpeed()).thenReturn(true);
        when(marton.getBearing()).thenReturn(0f);
        when(marton.hasBearing()).thenReturn(true);
        when(marton.getVVel()).thenReturn(0d);
        Position p = new Position("test");
        marton.linearPredict(3600000, p);
        Assert.assertEquals(40.96848700215959, p.getLatitude(), .0001);
        Assert.assertEquals( 175.37833333333333, p.getLongitude(), .0001);
        Assert.assertEquals(5000d/3.28084, p.getAltitude(), 0.1);
        Assert.assertEquals(123d, p.getSpeed(), 0.1);
    }

    @Test
    public void testMovePoint() {
        var p = new Position("test", -(40 + 4 / 60.0 + 9 / 3600.0), 175 + 22 / 60.0 + 42 / 3600.0, Units.Height.FT.toM(5000f),
                Units.Speed.KNOTS.toMps(123f), 0, 0, true, true, 0);
        p.moveBy(3600000);
        Assert.assertNotNull(p);
        Assert.assertEquals(-39.1698463311738, p.getLatitude(), .00001);
        Assert.assertEquals(175.37833333333333, p.getLongitude(), .00001);
   }

    @Test
    public void testHeightAbove() {
        when(p1.getAltitude()).thenReturn(Units.Height.FT.toM(4000d));
        when(marton.getAltitude()).thenReturn(Units.Height.FT.toM(5000f));
        var diff = Position.heightAbove(p1, marton);
        Assert.assertEquals(Units.Height.FT.toM(-1000d), diff , 0.1);
    }

    @Test
    public void testDMS() {
        Assert.assertEquals("-40:03:08.0", Position.DMS(-(40 + 3 / 60.0 + 8 / 3600.0)));
        Assert.assertEquals("175:22:42.0", Position.DMS(175 + 22 / 60.0 + 42 / 3600.0));
    }
}