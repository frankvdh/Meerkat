package com.meerkat;

public class LowPassFilter {
    /*
     * time smoothing constant for low-pass filter 0 ≤ alpha ≤ 1 ; a smaller
     * value basically means more smoothing See: http://en.wikipedia.org/wiki
     * /Low-pass_filter#Discrete-time_realization
     */
    float ALPHA;
    float lastOutput;

    public LowPassFilter(float ALPHA) {
        this.ALPHA = ALPHA;
    }

    public float lowPass(float input) {
        lastOutput = (Math.abs(input - lastOutput) > 170) ? input : lastOutput + ALPHA * (input - lastOutput);
        return lastOutput;
    }
}