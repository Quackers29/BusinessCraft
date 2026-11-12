package com.quackers29.businesscraft.network;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reach rules for packets that act on a Town Centre. The player-facing lookups need a
 * live level, so the two-client smoke covers them; this pins the distance maths.
 */
class LoadedTownAccessTest {
    private static final BlockPos TOWN = new BlockPos(0, 64, 0);
    private static final BlockPos KIOSK = new BlockPos(40, 64, 0);

    @Test
    void reachIsEightBlocksFromTheBlockCentre() {
        assertEquals(64.0D, LoadedTownAccess.REACH_DISTANCE_SQR);
        assertTrue(LoadedTownAccess.withinReach(0.5, 64.5, 0.5, TOWN));
        assertTrue(LoadedTownAccess.withinReach(8.5, 64.5, 0.5, TOWN));
        assertFalse(LoadedTownAccess.withinReach(8.51, 64.5, 0.5, TOWN));
    }

    @Test
    void reachCountsHeightAndDiagonals() {
        assertTrue(LoadedTownAccess.withinReach(0.5, 72.5, 0.5, TOWN));
        assertFalse(LoadedTownAccess.withinReach(0.5, 72.6, 0.5, TOWN));
        // 6 along each horizontal axis is 8.49 blocks away.
        assertFalse(LoadedTownAccess.withinReach(6.5, 64.5, 6.5, TOWN));
        assertTrue(LoadedTownAccess.withinReach(5.5, 64.5, 5.5, TOWN));
    }

    @Test
    void aTownOneHundredAndTwentyBlocksAwayIsOutOfReach() {
        assertFalse(LoadedTownAccess.withinReach(0.5, 64.0, 120.5, TOWN));
        assertEquals(120.0D * 120.0D + 0.25D,
                LoadedTownAccess.distanceSqrToCentre(0.5, 64.0, 120.5, TOWN), 1.0e-9);
    }

    @Test
    void missingPositionIsNeverInReach() {
        assertFalse(LoadedTownAccess.withinReach(0.5, 64.5, 0.5, null));
        assertFalse(LoadedTownAccess.withinPlatformReach(0.5, 64.5, 0.5, null, null));
    }

    @Test
    void platformActionsAreAllowedFromTheTownCentreOrThatKiosk() {
        // Beside the Town Centre, far from the kiosk.
        assertTrue(LoadedTownAccess.withinPlatformReach(2.5, 64.0, 0.5, TOWN, KIOSK));
        // Beside the kiosk, 40 blocks from the Town Centre.
        assertTrue(LoadedTownAccess.withinPlatformReach(41.5, 64.0, 2.5, TOWN, KIOSK));
        // Between the two, in reach of neither.
        assertFalse(LoadedTownAccess.withinPlatformReach(20.5, 64.0, 0.5, TOWN, KIOSK));
    }

    @Test
    void aPlatformWithoutAKioskNeedsTownCentreReach() {
        assertTrue(LoadedTownAccess.withinPlatformReach(2.5, 64.0, 0.5, TOWN, null));
        assertFalse(LoadedTownAccess.withinPlatformReach(41.5, 64.0, 2.5, TOWN, null));
    }
}
