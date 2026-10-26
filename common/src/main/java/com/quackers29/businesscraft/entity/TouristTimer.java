package com.quackers29.businesscraft.entity;

/**
 * Minute labels for the tourist screen, advanced on the client from the snapshot
 * taken when the screen opened.
 *
 * <p>The server writes the journey and remaining seconds into the menu once, and
 * writes the remaining seconds again only when the tourist starts or stops moving
 * (the expiry countdown pauses while they ride). It does not refresh those slots
 * every second, so the menu sync does not emit a timer packet every tick. The
 * client adds the time since the screen opened and rebuilds the label only when
 * the displayed minute changes.
 */
public final class TouristTimer {
    private TouristTimer() {
    }

    /** True when the menu should copy fresh timer seconds into its data slots. */
    public static boolean shouldSync(boolean seeded, boolean wasStationary, boolean nowStationary) {
        return !seeded || wasStationary != nowStationary;
    }

    public static int plusElapsed(int anchorSeconds, long anchorGameTime, long nowGameTime) {
        long elapsedTicks = Math.max(0L, nowGameTime - anchorGameTime);
        long total = (long) Math.max(0, anchorSeconds) + elapsedTicks / 20L;
        if (total > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) total;
    }

    public static int minusElapsed(int anchorSeconds, long anchorGameTime, long nowGameTime) {
        long elapsedTicks = Math.max(0L, nowGameTime - anchorGameTime);
        long total = (long) Math.max(0, anchorSeconds) - elapsedTicks / 20L;
        return (int) Math.max(0L, total);
    }

    public static int minuteBucket(int seconds) {
        return Math.max(0, seconds) / 60;
    }

    /**
     * Display state for one open screen. Holds the last minute it rendered so a
     * frame that is still inside the same minute does not rebuild the string.
     */
    public static final class Clock {
        private boolean anchored;
        private long anchorTime;
        private int anchorJourney;
        private int anchorRemaining;
        private boolean anchorStationary;
        private int lastRemainingSlot = Integer.MIN_VALUE;
        private int cachedKey = Integer.MIN_VALUE;
        private String cached = "";
        private int shownJourney;
        private int shownRemaining;

        public String text(long now, int openedJourney, int openedRemaining, int remainingSlot, boolean stationary) {
            if (!anchored) {
                anchorTime = now;
                anchorJourney = Math.max(0, openedJourney);
                anchorRemaining = Math.max(0, openedRemaining);
                anchorStationary = stationary;
                lastRemainingSlot = remainingSlot;
                anchored = true;
            } else if (stationary != anchorStationary || remainingSlot != lastRemainingSlot) {
                anchorJourney = plusElapsed(anchorJourney, anchorTime, now);
                anchorTime = now;
                anchorRemaining = Math.max(0, remainingSlot);
                anchorStationary = stationary;
                lastRemainingSlot = remainingSlot;
                cachedKey = Integer.MIN_VALUE;
            }
            shownJourney = plusElapsed(anchorJourney, anchorTime, now);
            shownRemaining = anchorStationary
                    ? minusElapsed(anchorRemaining, anchorTime, now)
                    : anchorRemaining;
            int key = minuteBucket(shownJourney) * 8192 + minuteBucket(shownRemaining) * 2 + (stationary ? 1 : 0);
            if (key != cachedKey) {
                cachedKey = key;
                cached = TouristTradeCatalog.waitingLine(shownJourney, shownRemaining, stationary);
            }
            return cached;
        }

        public int shownJourney() {
            return shownJourney;
        }

        public int shownRemaining() {
            return shownRemaining;
        }
    }
}
