package com.quackers29.businesscraft.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the distance ranks that replaced the old 20 m / level-3 cap.
 *
 * <p>Before (hardcoded): level 2 at 20 m, level 3 at 40 m, stuck at 3 forever.
 * A 100 m hop (the first trip that pays 1 emerald at metersPerEmerald=100)
 * was already max rank. After, with the default lists:
 * Wanderer 0, Traveler 500, Voyager 1500, Veteran 3000, Renowned 6000.
 * Town tiers: Wayside 0, Halt 2000, Station 10000, Destination 40000, Landmark 120000.
 */
class JourneyProgressTest {
    private static final int[] TOURIST = JourneyProgress.DEFAULT_TOURIST_LEVELS;
    private static final int[] TOWN = JourneyProgress.DEFAULT_TOWN_TIERS;

    @Test
    void touristRank_beforeFirstEmerald_staysWanderer() {
        // 100 m is minPayoutDistance and pays 1 emerald, but is still rank 1.
        assertEquals(1, JourneyProgress.levelForDistance(0, TOURIST));
        assertEquals(1, JourneyProgress.levelForDistance(100, TOURIST));
        assertEquals(1, JourneyProgress.levelForDistance(499, TOURIST));
        assertEquals("Wanderer", JourneyProgress.tierName(1));
    }

    @Test
    void touristRank_matchesFareMilestones() {
        // 500 m → 5 emeralds, rank 2. 1500 m → 15 emeralds, rank 3 (past the 1 km apple).
        // 3000 m → 30 emeralds, rank 4. 6000 m → 60 emeralds, rank 5 (past the 5 km bread).
        assertEquals(2, JourneyProgress.levelForDistance(500, TOURIST));
        assertEquals(3, JourneyProgress.levelForDistance(1500, TOURIST));
        assertEquals(4, JourneyProgress.levelForDistance(3000, TOURIST));
        assertEquals(5, JourneyProgress.levelForDistance(6000, TOURIST));
        assertEquals(5, JourneyProgress.levelForDistance(20000, TOURIST));
        assertEquals("Renowned", JourneyProgress.tierName(5));
    }

    @Test
    void oldTwentyMeterCap_wouldHaveMaxedBeforeAPaidTrip() {
        // Documents the previous formula so the change stays obvious in review.
        int oldMax = 3;
        int oldAt100 = Math.min(1 + (int) (100.0 / 20.0), oldMax);
        assertEquals(3, oldAt100);
        assertEquals(1, JourneyProgress.levelForDistance(100, TOURIST));
    }

    @Test
    void townTier_usesCumulativeVisitorDistance() {
        assertEquals(1, JourneyProgress.townLevel(600, TOWN));
        assertEquals(2, JourneyProgress.townLevel(2000, TOWN));
        assertEquals(3, JourneyProgress.townLevel(10000, TOWN));
        assertEquals(4, JourneyProgress.townLevel(40000, TOWN));
        assertEquals(5, JourneyProgress.townLevel(120000, TOWN));
        assertEquals("Wayside · 600 m", JourneyProgress.formatTownNotoriety(600, TOWN));
        assertEquals("Station · 10.0 km", JourneyProgress.formatTownNotoriety(10000, TOWN));
    }

    @Test
    void xpFillsAcrossTheCurrentSpan_andStaysFullAtMax() {
        // Rank 1 spans 0–500. Halfway (250 m) of the novice bar (10 XP) is 5.
        assertEquals(5, JourneyProgress.xpForDistance(250, 1, TOURIST));
        assertEquals(0, JourneyProgress.xpForDistance(0, 1, TOURIST));
        // Rank 5 is the cap: the bar reads full.
        assertEquals(250, JourneyProgress.xpForDistance(6000, 5, TOURIST));
        assertEquals(250, JourneyProgress.xpForDistance(9000, 5, TOURIST));
    }

    @Test
    void customThresholds_stayStrictlyConfigDriven() {
        int[] custom = {10, 20};
        assertEquals(1, JourneyProgress.levelForDistance(9, custom));
        assertEquals(2, JourneyProgress.levelForDistance(10, custom));
        assertEquals(3, JourneyProgress.levelForDistance(20, custom));
        assertEquals(3, JourneyProgress.maxLevel(custom));
    }
}
