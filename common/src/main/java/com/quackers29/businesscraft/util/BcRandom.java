package com.quackers29.businesscraft.util;

import java.util.Random;
import java.util.UUID;

/**
 * Shared random stream for research choice and contract pricing.
 * Unseeded, each draw is still unpredictable. {@link #reseed(long)} is for the
 * headless production simulation, which must repeat a run exactly.
 */
public final class BcRandom {
    private static Random random = new Random();
    private static boolean seeded;

    private BcRandom() {
    }

    public static double nextDouble() {
        return random.nextDouble();
    }

    public static void reseed(long seed) {
        random = new Random(seed);
        seeded = true;
    }

    /**
     * Seeded runs get repeatable ids. A live server keeps {@link UUID#randomUUID()}.
     */
    public static UUID nextUuid() {
        if (!seeded) {
            return UUID.randomUUID();
        }
        return new UUID(random.nextLong(), random.nextLong());
    }

    public static void reset() {
        random = new Random();
        seeded = false;
    }
}
