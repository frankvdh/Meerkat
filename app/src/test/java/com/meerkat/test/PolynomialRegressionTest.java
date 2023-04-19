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

import com.meerkat.PolynomialRegression;

import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;

public class PolynomialRegressionTest {
    @Test
    public void test() {
        var y = new int[]{1, 6, 17, 34, 57, 86, 121, 162, 209, 262, 321};
        var now = Instant.now().toEpochMilli();
        PolynomialRegression pr = new PolynomialRegression(now, 1);
        for (int i = 0; i < y.length; i++)
            pr.add(now + i, y[i]);

        double[] coeff = pr.getCoefficients()[0];
        Assert.assertArrayEquals(new double[]{1, 2, 3, now}, coeff, 0.000001);
    }

    @Test
    public void test1() {
        var data = new Data[]{
                new Data(-31065, 147.1311f), new Data(-30269, 147.1311f), new Data(-29149, 147.1311f), new Data(-28227, 147.1311f),
                new Data(-27304, 147.1311f), new Data(-26307, 147.1311f), new Data(-25153, 147.1311f), new Data(-24249, 147.1311f),
                new Data(-23308, 147.1311f), new Data(-22291, 147.1311f), new Data(-21161, 147.1311f)};

        var now = Instant.now().toEpochMilli();
        PolynomialRegression pr = new PolynomialRegression(now + data[0].l, 1);
        for (Data d : data)
            pr.add(now + d.l, d.f);

        double[] coeff = pr.getCoefficients()[0];
        Assert.assertArrayEquals(new double[]{147.1, 0, 0, now + data[0].l}, coeff, 0.1);
    }
    @Test
    public void test2() {
        var data = new Data[]{
                new Data(-47816, 18.28125f), new Data(-47020, 18.28125f), new Data(-45900, 18.28125f), new Data(-44978, 18.28125f),
                new Data(-44055, 18.28125f), new Data(-43058, 18.28125f), new Data(-41904, 18.28125f), new Data(-41000, 18.28125f),
                new Data(-40059, 18.28125f), new Data(-39042, 18.28125f), new Data(-37912, 18.28125f),
        };

        var now = Instant.now().toEpochMilli();
        PolynomialRegression pr = new PolynomialRegression(now + data[0].l, 1);
        for (Data d : data)
            pr.add(now + d.l, d.f);

        double[] coeff = pr.getCoefficients()[0];
        Assert.assertArrayEquals(new double[]{18.2, 0, 0, now + data[0].l}, coeff, 0.1);
    }

    @Test
    public void test3() {
        var data = new Data[]{
                new Data(1000, 100), new Data(1010, 100), new Data(1020, 100)
        };

        var now = Instant.now().toEpochMilli();
        PolynomialRegression pr = new PolynomialRegression(now + data[0].l, 1);
        for (Data d : data)
            pr.add(now + d.l, d.f);

        double[] coeff = pr.getCoefficients()[0];
        Assert.assertArrayEquals(new double[]{100, 0, 0, now + data[0].l}, coeff, 0.1);
    }

    @Test
    public void test4() {
        var data = new Data[]{
                new Data(1000000, 0), new Data(1005000, 10), new Data(1010000, 20), new Data(1015000, 30)
        };

        var now = Instant.now().toEpochMilli();
        PolynomialRegression pr = new PolynomialRegression(now + data[0].l, 1);
        for (Data d : data)
            pr.add(now + d.l, d.f);

        double[] coeff = pr.getCoefficients()[0];
        Assert.assertArrayEquals(new double[]{0, .002, 0, now + data[0].l}, coeff, 0.1);
    }

    @Test
    public void testDifferential() {
        var data = new Data[]{
                new Data(1000000, 10), new Data(1010000, 20), new Data(1015000, 10)
        };

        var now = Instant.now().toEpochMilli();
        PolynomialRegression pr = new PolynomialRegression(now + data[0].l, 1);
        for (Data d : data)
            pr.add(now + d.l, d.f);

        var coeff = pr.getCoefficients()[0];
        Assert.assertArrayEquals(new double[]{10, 0, 0, now + data[0].l}, coeff, 0.1);
    }

    @Test
    public void testClock() {
        var data = new Data[]{
                new Data(0, 18), new Data(10, 17), new Data(20, 16), new Data(30, 15),
                new Data(40, 14), new Data(50, 13), new Data(60, 12), new Data(70, 11),
                new Data(80, 10), new Data(90, 9), new Data(100, 8),
        };

        var now = Instant.now().toEpochMilli();
        PolynomialRegression pr = new PolynomialRegression(now + data[0].l, 1);
        for (Data d : data)
            pr.add(now + d.l, d.f);

        double[] coeff = pr.getCoefficients()[0];
        Assert.assertArrayEquals(new double[]{18, -0.1, 0, now}, coeff, 0.01);
    }

    @Test
    public void testClock2() {
        var data = new Data[]{
                new Data(400000, 18), new Data(400010, 17), new Data(400020, 16), new Data(400030, 15),
                new Data(400040, 14), new Data(400050, 13), new Data(400060, 12), new Data(400070, 11),
                new Data(400080, 10), new Data(400090,  9), new Data(400100, 8),
        };

        var now = Instant.now().toEpochMilli();
        PolynomialRegression pr = new PolynomialRegression(now + data[0].l, 1);
        for (Data d : data)
            pr.add(now + d.l, d.f);

        var coeff = pr.getCoefficients()[0];
        Assert.assertArrayEquals(new double[]{18, -0.1, 0, now + data[0].l}, coeff, 0.01);
    }
    public static class Data {
       public final long l;
      public  final float f;

        Data(long l, float f) {
            this.l = l;
            this.f = f;
        }
    }

}
