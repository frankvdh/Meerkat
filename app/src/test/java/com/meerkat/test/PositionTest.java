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

import static com.meerkat.measure.Height.Units.FT;
import static org.mockito.Mockito.when;
import static java.lang.Math.PI;

import com.meerkat.measure.Height;
import com.meerkat.measure.Position;
import com.meerkat.measure.Speed;

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
    Position marton;
    @Mock
    Position p1;

    @Test
    public void testLinearPredict() {
        long now = System.currentTimeMillis();
        when(marton.getTime()).thenReturn(now);
        when(marton.getLatitude()).thenReturn(-(40 + 4 / 60.0 + 9 / 3600.0));
        when(marton.getLongitude()).thenReturn(175 + 22 / 60.0 + 42 / 3600.0);
        when(marton.getAltitude()).thenReturn(5000d/3.28084);
        when(marton.getSpeedMps()).thenReturn(0f);
        when(marton.getSpeedUnits()).thenReturn(new Speed(100f, Speed.Units.KPH));
        when(marton.hasSpeed()).thenReturn(true);
        when(marton.getBearing()).thenReturn(0f);
        when(marton.hasBearing()).thenReturn(true);
        when(marton.getVVel()).thenReturn(0f);
        Position p = marton.linearPredict(3600000);
        Assert.assertEquals(40.96848700215959, p.getLatitude(), .0001);
        Assert.assertEquals( 175.37833333333333, p.getLongitude(), .0001);
        Assert.assertEquals(5000d/3.28084, p.getAltitude(), 0.1);
        Assert.assertEquals(123d, p.getSpeedUnits().value, 0.1);
    }

    @Test
    public void testMovePoint() {
        Position p = new Position("test", -(40 + 4 / 60.0 + 9 / 3600.0), 175 + 22 / 60.0 + 42 / 3600.0, new Height(5000f, FT),
                new Speed(123f, Speed.Units.KNOTS), 0, 0, true, true, 0).linearPredict(3600000);
        Assert.assertNotNull(p);
        Assert.assertEquals(-39.1698463311738, p.getLatitude(), .00001);
        Assert.assertEquals(175.37833333333333, p.getLongitude(), .00001);
   }

    @Test
    public void testHeightAbove() {
        when(p1.getAlt()).thenReturn(new Height(4000f, FT));
        when(marton.getAlt()).thenReturn(new Height(5000f, FT));
        Height diff = Position.heightAbove(p1, marton);
        Assert.assertEquals(-1000.0, diff.value , 0.1);
        Assert.assertEquals("ft", diff.units.label);
    }

    @Test
    public void testDMS() {
        Assert.assertEquals("-40:03:08.0", Position.DMS(-(40 + 3 / 60.0 + 8 / 3600.0)));
        Assert.assertEquals("175:22:42.0", Position.DMS(175 + 22 / 60.0 + 42 / 3600.0));
    }
}