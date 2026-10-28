package com.quackers29.businesscraft.platform;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlatformLearnedRoutesTest {

    private static final UUID DEST_NEAR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DEST_FAR = UUID.fromString("22222222-2222-2222-2222-222222222222");

    /** 5 in-game minutes at 20 tps */
    private static final long SHORT_TRIP = 5L * 60L * 20L;
    /** 2 in-game hours at 20 tps */
    private static final long LONG_TRIP = 2L * 60L * 60L * 20L;

    @Test
    void autoStartsLikeAny_whenNoLearnedRoutes_effectiveShareZero() {
        PlatformLearnedRoutes routes = new PlatformLearnedRoutes();
        assertEquals(0, routes.effectiveLearnedShare(0.7, 2.0, null, UUID.randomUUID()));
    }

    @Test
    void nbtRoundTrip_preservesScoreAndAvgTripDuration() {
        PlatformLearnedRoutes routes = new PlatformLearnedRoutes();
        routes.recordSuccess(DEST_NEAR, 1000, 1.0, SHORT_TRIP, 12, 0.05);
        routes.recordSuccess(DEST_NEAR, 2000, 1.0, LONG_TRIP, 12, 0.05);

        CompoundTag tag = routes.save();
        PlatformLearnedRoutes loaded = new PlatformLearnedRoutes();
        loaded.load(tag);

        assertEquals(routes, loaded);
        PlatformLearnedRoutes.Entry entry = loaded.snapshotEntries().get(DEST_NEAR);
        assertNotNull(entry);
        assertTrue(entry.avgTripDurationTicks > SHORT_TRIP);
        assertTrue(entry.avgTripDurationTicks < LONG_TRIP);
    }

    @Test
    void longTripRoute_decaysSlowerThanShortTrip_afterSameElapsedTime() {
        double baseHalfLife = 1000.0;
        double multiplier = 2.0;
        double pruneFloor = 0.01;

        PlatformLearnedRoutes routes = new PlatformLearnedRoutes();
        routes.recordSuccess(DEST_NEAR, 0, 10.0, SHORT_TRIP, 12, pruneFloor);
        routes.recordSuccess(DEST_FAR, 0, 10.0, LONG_TRIP, 12, pruneFloor);

        long elapsed = SHORT_TRIP;
        routes.applyDecay(elapsed, baseHalfLife, multiplier, pruneFloor);

        double nearScore = routes.snapshotEntries().get(DEST_NEAR).score;
        double farScore = routes.snapshotEntries().get(DEST_FAR).score;

        assertTrue(farScore > nearScore,
                "long-trip route should retain more score (near=" + nearScore + ", far=" + farScore + ")");
    }

    @Test
    void entryHalfLife_isMaxOfBaseAndTripTimesMultiplier() {
        assertEquals(1000, PlatformLearnedRoutes.entryHalfLifeTicks(1000, 0, 2.0));
        assertEquals(2400, PlatformLearnedRoutes.entryHalfLifeTicks(1000, 1200, 2.0));
    }

    @Test
    void expiryPenalty_reducesScore() {
        PlatformLearnedRoutes routes = new PlatformLearnedRoutes();
        routes.recordSuccess(DEST_NEAR, 0, 5.0, SHORT_TRIP, 12, 0.05);
        routes.recordFailure(DEST_NEAR, 100, 2.0, 12, 0.05);
        assertEquals(3.0, routes.snapshotEntries().get(DEST_NEAR).score, 0.001);
    }

    @Test
    void pruneAndCap_dropWeakEntriesAndLimitCount() {
        PlatformLearnedRoutes routes = new PlatformLearnedRoutes();
        for (int i = 0; i < 20; i++) {
            routes.recordSuccess(UUID.randomUUID(), i, 1.0 + i * 0.01, SHORT_TRIP, 3, 0.05);
        }
        assertTrue(routes.snapshotEntries().size() <= 3);
    }
}
