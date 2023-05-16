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

import static org.mockito.Mockito.when;
import static java.lang.Math.PI;
import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

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
    static Position marton = new Position("TEST");
    @Mock
    Position p1;

    @Test
    public void testLinearPredict() {
        when(marton.getLatitude()).thenReturn(-(40 + 4 / 60.0 + 9 / 3600.0));
        when(marton.getLongitude()).thenReturn(175 + 22 / 60.0 + 42 / 3600.0);
        when(marton.getAltitude()).thenReturn(Units.Height.FT.toM(5000));
        when(marton.getSpeed()).thenReturn(120f);
        when(marton.getTrack()).thenReturn(0f);

        var trackRad = Position.latLonDegToRad(marton.getTrack());
        var latRad = Position.latLonDegToRad(marton.getLatitude());
        var lonRad = Position.latLonDegToRad(marton.getLongitude());
        var distFraction = marton.getSpeed() * 3600 / 6371000d; // earth Radius In Metres
        var cosLat = cos(latRad);
        var sinLat = sin(latRad);
        var sinDist = sin(distFraction);
        var cosDist = cos(distFraction);

        var latitudeResult = asin(sinLat * cosDist + cosLat * sinDist * cos(trackRad));
        var a = atan2(sin(trackRad) * sinDist * cosLat, cosDist - sinLat * sin(latitudeResult));
        var longitudeResult = (lonRad + a + 3 * PI) % (2 * PI) - PI;
        var lat = Position.rad2latLonDeg(latitudeResult);
        var lon = Position.rad2latLonDeg(longitudeResult);
        var alt = marton.getAltitude() + 0;
        Assert.assertEquals(-36.184097, lat, .0001);
        Assert.assertEquals(marton.getLongitude(), lon, .0001);
        Assert.assertEquals(marton.getAltitude(), alt, 0.1);
    }

    @Test
    public void testHeightAbove() {
        when(p1.getAltitude()).thenReturn(Units.Height.FT.toM(4000d));
        when(marton.getAltitude()).thenReturn(Units.Height.FT.toM(5000f));
        var diff = p1.heightAboveOwnship();
        Assert.assertEquals(Units.Height.FT.toM(-1000d), diff, 0.1);
    }

    @Test
    public void testDMS() {
        Assert.assertEquals("-40:03:08.0", Position.DMS(-(40 + 3 / 60.0 + 8 / 3600.0)));
        Assert.assertEquals("175:22:42.0", Position.DMS(175 + 22 / 60.0 + 42 / 3600.0));
    }
}