package assignment3.solutions;

import assignment3.Alloy;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public abstract class Solution {
    public final int width;
    public final int height;
    public final int maxIterations;
    protected final float s;
    protected final float t;
    protected final float[] thermalConstants;
    protected final int threshold;

    public Solution(int width, int height, int maxIterations, float s, float t, float[] thermalConstants, int threshold) {
        this.width = width;
        this.height = height;
        this.maxIterations = maxIterations;
        this.s = s;
        this.t = t;
        this.thermalConstants = thermalConstants;
        this.threshold = threshold;
    }

    // Get a random value to apply as noise.
    public static float randomNoise() {
        return ThreadLocalRandom.current().nextFloat(0.75f, 1.25f);
    }

    public static Alloy randomAlloy() {
        var random = ThreadLocalRandom.current();

        // Variation code from Victor Lockwood.
        int m1Variation = random.nextInt(25);
        int variationBound = 25 - m1Variation;
        int m2Variation = random.nextInt(variationBound);

        int m1 = 33 + m1Variation;
        int m2 = 33 + m2Variation;
        int m3 = 100 - (m1 + m2);

        int[] values = {m1, m2, m3};
        return new Alloy(values);
    }

    protected abstract void compute();

    public abstract void run();
}
