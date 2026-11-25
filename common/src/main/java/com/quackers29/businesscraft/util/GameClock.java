package com.quackers29.businesscraft.util;

import java.util.function.LongSupplier;

/**
 * Wall-clock source for systems whose stages are measured in real milliseconds
 * (contract auctions and courier windows). Production uses {@link System#currentTimeMillis()}.
 * The headless production simulation installs a clock that advances with the simulated tick.
 */
public final class GameClock {
    private static LongSupplier source = System::currentTimeMillis;

    private GameClock() {
    }

    public static long millis() {
        return source.getAsLong();
    }

    public static void set(LongSupplier supplier) {
        source = supplier == null ? System::currentTimeMillis : supplier;
    }

    public static void reset() {
        source = System::currentTimeMillis;
    }
}
