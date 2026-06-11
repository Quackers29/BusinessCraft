package com.quackers29.businesscraft.town.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-009: Tourist Allocation (Test + Docs Loop).
 *
 * Covers the population-proportional fairness logic in TouristAllocationTracker:
 *   - Guards, single-option fast path, zero-pop random fallback.
 *   - Fairness gap = current - (pop / totalPop * totalTourists)
 *   - Selection prefers most negative gap (most under-allocated), 10% random among unders.
 *   - recordSpawn / recordRemoval mutate the per-origin counters (used by select).
 *
 * recordTouristSpawn is private to the logic (never called from prod today — see vault note);
 * the static originTrackers map is cleared via reflection in @BeforeEach (no prod changes).
 * Documentation: vault/Tourists/Capacity/Tourist Allocation.md
 */
class TouristAllocationTrackerTest {

    private static final UUID ORIGIN = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID DEST_A  = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DEST_B  = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DEST_C  = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void setUp() throws Exception {
        clearOriginTrackers();
    }

    @AfterEach
    void tearDown() throws Exception {
        clearOriginTrackers();
    }

    // --- reflection helpers (state hygiene + private method access) ---

    private void clearOriginTrackers() throws Exception {
        Field f = TouristAllocationTracker.class.getDeclaredField("originTrackers");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, ?> map = (Map<UUID, ?>) f.get(null);
        map.clear();
    }

    /** Invokes the private calculateFairnessGap on the per-origin DestinationTracker after ensuring state. */
    private double calculateFairnessGap(UUID origin, UUID townId, int population, int totalPopulation) throws Exception {
        // Ensure a tracker exists for this origin (recordSpawn creates it without needing a prior select)
        TouristAllocationTracker.recordTouristSpawn(origin, townId);

        Field trackersF = TouristAllocationTracker.class.getDeclaredField("originTrackers");
        trackersF.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Object> trackers = (Map<UUID, Object>) trackersF.get(null);
        Object destTracker = trackers.get(origin);
        assertNotNull(destTracker, "DestinationTracker should have been created by recordSpawn");

        Class<?> dtClass = destTracker.getClass();
        Method m = dtClass.getDeclaredMethod("calculateFairnessGap", UUID.class, int.class, int.class);
        m.setAccessible(true);
        return (double) m.invoke(destTracker, townId, population, totalPopulation);
    }

    // --- guard tests ---

    @Test
    void selectFairDestination_nullOrigin_returnsNull() {
        Map<UUID, Integer> opts = new HashMap<>();
        opts.put(DEST_A, 100);
        assertNull(TouristAllocationTracker.selectFairDestination(null, opts));
    }

    @Test
    void selectFairDestination_nullOptions_returnsNull() {
        assertNull(TouristAllocationTracker.selectFairDestination(ORIGIN, null));
    }

    @Test
    void selectFairDestination_emptyOptions_returnsNull() {
        assertNull(TouristAllocationTracker.selectFairDestination(ORIGIN, Collections.emptyMap()));
    }

    // --- simple selection paths ---

    @Test
    void selectFairDestination_singleOption_returnsTheOnlyOne() {
        Map<UUID, Integer> opts = new HashMap<>();
        opts.put(DEST_A, 42);
        UUID chosen = TouristAllocationTracker.selectFairDestination(ORIGIN, opts);
        assertEquals(DEST_A, chosen);
    }

    @Test
    void selectFairDestination_zeroTotalPopulation_returnsOneOfTheOptions() {
        // When all reported pops are 0 we fall back to random among keys (new Random each time).
        // This pins current behavior; order is not guaranteed.
        Map<UUID, Integer> opts = new LinkedHashMap<>();
        opts.put(DEST_A, 0);
        opts.put(DEST_B, 0);
        UUID chosen = TouristAllocationTracker.selectFairDestination(ORIGIN, opts);
        assertTrue(chosen.equals(DEST_A) || chosen.equals(DEST_B),
                "zero-pop path must return one of the provided keys");
    }

    // --- fairness gap formula (via reflection, hand-computed) ---

    @Test
    void calculateFairnessGap_zeroTotalTourists_returnsZero() throws Exception {
        // Even with positive pops, if the tracker has seen 0 spawns yet, gap is defined as 0.
        // We force a tracker via record but then the *internal* totalTourists for gap calc in this call is still 0
        // until we record more. Actually record once first, then the *next* gap calc sees total=1.
        // Simpler: directly after first record, a gap calc with totalPop>0 but we rely on internal total=1.
        // So test the early-return path by computing before any record for that origin.
        // Because helper always does a recordSpawn, we test the formula path that hits the if by using a fresh origin
        // and calling the gap helper which records once (total becomes 1 inside tracker). Use a direct select path
        // to create tracker with total=0 and then reflect? For clarity we test the >0 math below and note the guard.
        UUID fresh = UUID.randomUUID();
        // To hit the (totalTourists == 0) return we would need a tracker instance whose total is still 0.
        // selectFair with positive pop creates tracker but total remains 0 (no spawn recorded).
        TouristAllocationTracker.selectFairDestination(fresh, Map.of(DEST_A, 50));
        // Now invoke gap via same reflection pattern but we know it will hit early return.
        // We duplicate minimal logic here for the guard test (or accept that our helper always seeds).
        // Instead assert via observable: first selection with pops >0 when no prior spawns yields first-in-order.
        Map<UUID, Integer> opts = new LinkedHashMap<>();
        opts.put(DEST_A, 10);
        opts.put(DEST_B, 90);
        UUID first = TouristAllocationTracker.selectFairDestination(fresh, opts);
        assertEquals(DEST_A, first, "with totalTourists=0 all gaps=0, first in order wins");
    }

    @Test
    void calculateFairnessGap_afterSpawn_currentMinusTarget() throws Exception {
        // recordSpawn makes totalTourists=1 and current[DEST_A]=1
        // target for pop=25 / totalPop=100 is 0.25; gap = 1 - 0.25 = 0.75
        double gap = calculateFairnessGap(ORIGIN, DEST_A, 25, 100);
        // targetProportion = 25/100 = 0.25; targetCount=0.25*1; current=1 (from the record inside helper); gap=1-0.25=0.75
        assertEquals(0.75, gap, 1e-9);
    }

    // --- deterministic under-allocation preference (stable even with 10% random-under) ---

    @Test
    void selectFairDestination_afterSpawnToLowPopPrefersHighPopUnderAllocated() {
        // Seed: one tourist recorded to low-pop A (pop 10). Total tourists from origin now 1.
        // Options: A=10, B=90. targetA=0.1, currA=1 → gapA=+0.9 (over)
        //          targetB=0.9, currB=0 → gapB=-0.9 (under)
        // Lowest gap is B. Even if 10% random-under branch hits, under list contains only B → B is returned.
        TouristAllocationTracker.recordTouristSpawn(ORIGIN, DEST_A);

        Map<UUID, Integer> opts = new LinkedHashMap<>();
        opts.put(DEST_A, 10);
        opts.put(DEST_B, 90);

        UUID chosen = TouristAllocationTracker.selectFairDestination(ORIGIN, opts);
        assertEquals(DEST_B, chosen, "B is the only under-allocated (most negative gap)");
    }

    @Test
    void selectFairDestination_equalPopsNoAllocs_picksFirstInOrder() {
        // No prior spawns (totalT=0 → gaps all 0). Tie on lowest gap → first in iteration order wins.
        // We pass LinkedHashMap to make test order deterministic (prod callers often pass HashMap).
        Map<UUID, Integer> opts = new LinkedHashMap<>();
        opts.put(DEST_A, 50);
        opts.put(DEST_B, 50);
        opts.put(DEST_C, 50);

        UUID chosen = TouristAllocationTracker.selectFairDestination(ORIGIN, opts);
        assertEquals(DEST_A, chosen, "all gaps==0, first entry wins via strict < in min scan");
    }

    // --- removal and state mutation ---

    @Test
    void recordTouristRemoval_decrementsAndChangesSubsequentSelection() {
        // Two spawns to A, then remove one from A → A now has 1, total=1 after remove? Wait simulate.
        TouristAllocationTracker.recordTouristSpawn(ORIGIN, DEST_A);
        TouristAllocationTracker.recordTouristSpawn(ORIGIN, DEST_A); // total=2, currA=2
        TouristAllocationTracker.recordTouristRemoval(ORIGIN, DEST_A); // total=1, currA=1

        // Now with options A=10, B=90 the gap calc will see currA=1, totalT=1 → same as single spawn case
        // gapA = 1 - 0.1 = +0.9; gapB = 0-0.9=-0.9 → B
        Map<UUID, Integer> opts = new LinkedHashMap<>();
        opts.put(DEST_A, 10);
        opts.put(DEST_B, 90);

        UUID chosen = TouristAllocationTracker.selectFairDestination(ORIGIN, opts);
        assertEquals(DEST_B, chosen);
    }

    @Test
    void recordTouristRemoval_whenCountZeroOrAbsent_isNoOp() {
        // Should not underflow or create negative entries
        TouristAllocationTracker.recordTouristRemoval(ORIGIN, DEST_A); // absent
        TouristAllocationTracker.recordTouristSpawn(ORIGIN, DEST_A);
        TouristAllocationTracker.recordTouristRemoval(ORIGIN, DEST_A);
        TouristAllocationTracker.recordTouristRemoval(ORIGIN, DEST_A); // now zero, second should no-op

        // State should be as if one spawn + one removal happened (total=0, no entry or 0)
        // Select with two equal options should still give first (no active tourists → gap 0 path)
        Map<UUID, Integer> opts = new LinkedHashMap<>();
        opts.put(DEST_A, 40);
        opts.put(DEST_B, 60);
        UUID chosen = TouristAllocationTracker.selectFairDestination(ORIGIN, opts);
        assertEquals(DEST_A, chosen);
    }

    // --- isolation ---

    @Test
    void selectFairDestination_originIsolation() {
        UUID o1 = UUID.randomUUID();
        UUID o2 = UUID.randomUUID();

        TouristAllocationTracker.recordTouristSpawn(o1, DEST_A); // o1 has bias toward B
        // o2 has no records

        Map<UUID, Integer> opts = new LinkedHashMap<>();
        opts.put(DEST_A, 10);
        opts.put(DEST_B, 90);

        // o1 should prefer B
        assertEquals(DEST_B, TouristAllocationTracker.selectFairDestination(o1, opts));
        // o2 (totalT=0 for it) should give first in order
        assertEquals(DEST_A, TouristAllocationTracker.selectFairDestination(o2, opts));
    }

    // --- record null safety (no exceptions, no state pollution) ---

    @Test
    void recordSpawn_andRemoval_withNulls_areNoOps() throws Exception {
        // Should not throw and should leave the map empty for a fresh origin
        TouristAllocationTracker.recordTouristSpawn(null, DEST_A);
        TouristAllocationTracker.recordTouristSpawn(ORIGIN, null);
        TouristAllocationTracker.recordTouristRemoval(null, DEST_A);
        TouristAllocationTracker.recordTouristRemoval(ORIGIN, null);

        // Verify no tracker leaked for ORIGIN
        Field f = TouristAllocationTracker.class.getDeclaredField("originTrackers");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, ?> map = (Map<UUID, ?>) f.get(null);
        assertFalse(map.containsKey(ORIGIN));
    }
}
