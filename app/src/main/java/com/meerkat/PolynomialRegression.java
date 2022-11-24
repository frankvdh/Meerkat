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

/**
 * https://rosettacode.org/wiki/Polynomial_regression#Java
 */
public class PolynomialRegression {
    private int N;
    private final int numSeries;
    private double xSum;
    private double x2Sum;
    private double x3Sum;
    private double x4Sum;
    private final double[] ySum;
    private final double[] xySum;
    private final double[] x2ySum;
    private final long initialX;

    public PolynomialRegression(long start, int numSeries) {
        initialX = start;
        this.numSeries = numSeries;
        N = 0;
        xSum = 0;
        x2Sum = 0;
        x3Sum = 0;
        x4Sum = 0;
        ySum = new double[numSeries];
        xySum = new double[numSeries];
        x2ySum = new double[numSeries];
    }

    public void add(long X, float... Y) {
        double x = X - initialX;
        double x2 = x * x;
        xSum += x;
        x2Sum += x2;
        x3Sum += x2 * x;
        x4Sum += x2 * x2;
        for (int i = 0; i < numSeries; i++) {
            double y = Y[i];
            ySum[i] += y;
            xySum[i] += x * y;
            x2ySum[i] += x2 * y;
        }
        N++;
    }

    public double[][] getCoefficients() {
        if (N < 1) return null;
        double xm = xSum / N;
        double x2m = x2Sum / N;
        double sxxN = (x2Sum - xSum * xm);
        double sxx2N = (x3Sum - xSum * x2m);
        double sx2x2N = (x4Sum - x2Sum * x2m);
        double div1 = sxxN * sx2x2N - sxx2N * sxx2N;
        double div2 = sxxN * sx2x2N - sxx2N * sxx2N;
        if (div1 == 0 || div2 == 0)
            return null;

        double[][] result = new double[numSeries][];
        for (int i = 0; i < numSeries; i++) {
            double ym = ySum[i] / N;
            double sxyN = (xySum[i] - xSum * ym);
            double sx2yN = (x2ySum[i] - x2m * ySum[i]);
            double coefficient1 = (sxyN * sx2x2N - sx2yN * sxx2N) / div1;
            double coefficient2 = (sx2yN * sxxN - sxyN * sxx2N) / div2;
            result[i] = new double[]{ym - coefficient1 * xm - coefficient2 * x2m, coefficient1, coefficient2, initialX};
        }
        return result;
    }
}