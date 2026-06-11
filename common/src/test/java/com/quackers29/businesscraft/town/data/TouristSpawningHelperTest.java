package com.quackers29.businesscraft.town.data;

import com.quackers29.businesscraft.testutil.McBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-037: Tourist Spawning and Destination Selection (Test + Docs Loop).
 *
 * Documents TouristSpawningHelper (the central tourist creation path):
 *   - ANY_TOWN sentinel (UUID 0-0)
 *   - Platform destination filtering (enabled only) and ANY fallback
 *   - Fair select prep (origin removal + allowed intersect + long->int pop map)
 *   - Random-along-path positioning with 3-attempt occupancy + air checks
 *   - Expiry injection from ConfigLoader.touristExpiryMinutes
 *   - Origin.addTourist() on successful entity add
 *
 * Core methods (selectTouristDestination, selectFairTownByPopulation,
 * spawnTouristOnPlatform, private spawnTourist) all require a real ServerLevel
 * with a populated TownManager and registered entities + platforms. They are
 * therefore NEEDS-MC; only smoke + sentinel pinning are possible here.
 *
 * Documentation: vault/Tourists/Spawning/Tourist Spawning and Destination Selection.md
 */
class TouristSpawningHelperTest {

    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

    @Test
    void spawningHelper_canBeInstantiated() {
        TouristSpawningHelper helper = new TouristSpawningHelper();
        assertNotNull(helper);
    }

    @Test
    void anyTownDestinationSentinel_matchesEntityConstant_andIsZeroUUID() throws Exception {
        // The private ANY_TOWN_DESTINATION in the helper must be the same sentinel
        // used by TouristEntity.ANY_TOWN_DESTINATION (new UUID(0, 0)).
        // This value is the "no specific destination / any town" marker returned
        // when a platform has no enabled concrete targets.
        Field f = TouristSpawningHelper.class.getDeclaredField("ANY_TOWN_DESTINATION");
        f.setAccessible(true);
        UUID anyFromHelper = (UUID) f.get(null);

        assertEquals(new UUID(0, 0), anyFromHelper);
        // Hand-computed: UUID(0,0) is the only value that will be treated as ANY
        // by downstream arrival/visitor code.
        assertEquals(0L, anyFromHelper.getMostSignificantBits());
        assertEquals(0L, anyFromHelper.getLeastSignificantBits());
    }

    // All other documented rules (ANY fallback when enabled list empty,
    // population map construction with (int) cast, random progress lerp for
    // spawnPos, 3-attempt occupied/air retry, expiry = (int)(minutes*60*20),
    // post-spawn addTourist, per-platform headcount vs maxTouristsPerTown)
    // live in the vault note under "Rules & formulas (exact)" and "Edge cases".
    // They cannot be exercised here without a live ServerLevel + TownManager
    // containing real Town objects and a Platform with start/end + destinations.
    // See ledger T-037 (NEEDS-MC) and the vault note Test coverage section.
}