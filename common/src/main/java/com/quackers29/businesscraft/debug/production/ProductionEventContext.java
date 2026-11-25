package com.quackers29.businesscraft.debug.production;

/**
 * Game tick stamped onto production events. The server sets it from the level
 * clock; the headless simulation sets it from the loop counter.
 */
public final class ProductionEventContext {
    private static long gameTick;

    private ProductionEventContext() {
    }

    public static long gameTick() {
        return gameTick;
    }

    public static void setGameTick(long tick) {
        gameTick = tick;
    }
}
