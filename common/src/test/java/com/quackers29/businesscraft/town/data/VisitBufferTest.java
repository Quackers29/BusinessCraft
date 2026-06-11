package com.quackers29.businesscraft.town.data;

import com.quackers29.businesscraft.api.ITownDataProvider.VisitHistoryRecord;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-010: Visit Buffer (Test + Docs Loop).
 *
 * Covers the buffering, deduplication, distance accumulation, flush, and
 * two-map distance survival rules in VisitBuffer.
 *
 * The buffer groups near-simultaneous arrivals per origin town (~1 s global
 * quiet window) so that payment (T-001) and milestones see one aggregated
 * VisitHistoryRecord + average distance per origin instead of per-tourist spam.
 *
 * Documentation: vault/Town/Visits/Visit Buffer.md
 */
class VisitBufferTest {

    private static final UUID TOWN_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TOWN_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final BlockPos POS_A = new BlockPos(100, 64, 200);
    private static final BlockPos POS_B = new BlockPos(300, 64, 400);

    private VisitBuffer buffer;

    @BeforeEach
    void setUp() {
        buffer = new VisitBuffer();
    }

    // --- helpers ---

    private VisitHistoryRecord findRecord(List<VisitHistoryRecord> records, UUID origin) {
        return records.stream()
                .filter(r -> TOWN_A.equals(origin) ? r.getOriginTownId().equals(TOWN_A) : r.getOriginTownId().equals(origin))
                .findFirst()
                .orElse(null);
    }

    // --- add / accumulate / dedup ---

    @Test
    void addVisitor_firstArrival_createsEntryWithCountOne() {
        buffer.addVisitor(TOWN_A, POS_A);
        assertEquals(1, buffer.getVisitorCount());
        // distance still 0 until update
        assertEquals(0.0, buffer.getAverageDistance(TOWN_A), 0.0001);
    }

    @Test
    void addVisitor_sameOriginWithinWindow_incrementsCountAndReusesOriginPos() {
        buffer.addVisitor(TOWN_A, POS_A);
        buffer.updateVisitorDistance(TOWN_A, 100.0);

        buffer.addVisitor(TOWN_A, new BlockPos(999, 999, 999)); // different pos should be ignored for this cycle
        buffer.updateVisitorDistance(TOWN_A, 300.0);

        // Still one origin in buffer
        assertEquals(1, buffer.getVisitorCount());
        // count = 2, total dist = 400, avg = 200
        assertEquals(200.0, buffer.getAverageDistance(TOWN_A), 0.0001);
    }

    @Test
    void addVisitor_twoDifferentOrigins_bothTracked() {
        buffer.addVisitor(TOWN_A, POS_A);
        buffer.updateVisitorDistance(TOWN_A, 500.0);
        buffer.addVisitor(TOWN_B, POS_B);
        buffer.updateVisitorDistance(TOWN_B, 120.0);

        assertEquals(2, buffer.getVisitorCount());
        assertEquals(500.0, buffer.getAverageDistance(TOWN_A), 0.0001);
        assertEquals(120.0, buffer.getAverageDistance(TOWN_B), 0.0001);
    }

    // --- distance update rules ---

    @Test
    void updateVisitorDistance_onAbsentTown_ignored() {
        // Never added TOWN_A
        buffer.updateVisitorDistance(TOWN_A, 999.0);
        assertEquals(0, buffer.getVisitorCount());
        assertEquals(0.0, buffer.getAverageDistance(TOWN_A), 0.0001);
    }

    @Test
    void updateVisitorDistance_sumsAcrossMultipleCalls() {
        buffer.addVisitor(TOWN_A, POS_A);
        buffer.updateVisitorDistance(TOWN_A, 10.0);
        buffer.updateVisitorDistance(TOWN_A, 20.0);
        buffer.updateVisitorDistance(TOWN_A, 30.0);
        // total 60 / 1 = 60
        assertEquals(60.0, buffer.getAverageDistance(TOWN_A), 0.0001);
    }

    // --- getAverageDistance two-tier lookup ---

    @Test
    void getAverageDistance_livePositivePrefersVisitorsMap() {
        buffer.addVisitor(TOWN_A, POS_A);
        buffer.updateVisitorDistance(TOWN_A, 250.0);
        // Even if we manually had something in distanceMap (we don't), live wins while >0
        assertEquals(250.0, buffer.getAverageDistance(TOWN_A), 0.0001);
    }

    @Test
    void getAverageDistance_fallsBackToDistanceMapAfterProcess() throws Exception {
        buffer.addVisitor(TOWN_A, POS_A);
        buffer.updateVisitorDistance(TOWN_A, 400.0);

        List<VisitHistoryRecord> records = buffer.processVisits();
        assertEquals(1, records.size());
        // visitors cleared, but distance survived
        assertEquals(0, buffer.getVisitorCount());
        assertEquals(400.0, buffer.getAverageDistance(TOWN_A), 0.0001);
    }

    @Test
    void getAverageDistance_zeroWhenNeitherMapHasData() {
        assertEquals(0.0, buffer.getAverageDistance(TOWN_A), 0.0001);
    }

    // --- shouldProcess timing (global lastVisitTime) ---

    @Test
    void shouldProcess_falseImmediatelyAfterAdd() {
        buffer.addVisitor(TOWN_A, POS_A);
        buffer.updateVisitorDistance(TOWN_A, 100.0);
        // lastVisitTime just set; delta ~0 < 1000
        assertFalse(buffer.shouldProcess());
    }

    @Test
    void shouldProcess_trueAfterTimeoutWithVisitors() throws Exception {
        buffer.addVisitor(TOWN_A, POS_A);
        buffer.updateVisitorDistance(TOWN_A, 100.0);
        // Wait for the real 1000 ms quiet window
        Thread.sleep(1100);
        assertTrue(buffer.shouldProcess());
    }

    @Test
    void shouldProcess_falseAfterProcessVisitsEvenIfTimePassed() throws Exception {
        buffer.addVisitor(TOWN_A, POS_A);
        buffer.updateVisitorDistance(TOWN_A, 100.0);
        buffer.processVisits(); // clears visitors
        Thread.sleep(1100);
        // visitors empty → shouldProcess false regardless of time
        assertFalse(buffer.shouldProcess());
    }

    // --- processVisits flush rules ---

    @Test
    void processVisits_emptyBuffer_returnsEmptyAndNoSideEffects() {
        List<VisitHistoryRecord> records = buffer.processVisits();
        assertTrue(records.isEmpty());
        assertEquals(0, buffer.getVisitorCount());
        assertEquals(0.0, buffer.getAverageDistance(TOWN_A), 0.0001);
    }

    @Test
    void processVisits_emitsOneRecordPerOriginWithAggregatedCountAndFlushTimestamp() {
        long before = System.currentTimeMillis();

        buffer.addVisitor(TOWN_A, POS_A);
        buffer.updateVisitorDistance(TOWN_A, 480.0);
        buffer.addVisitor(TOWN_A, POS_A);
        buffer.updateVisitorDistance(TOWN_A, 520.0); // now count 2, avg 500
        buffer.addVisitor(TOWN_B, POS_B);
        buffer.updateVisitorDistance(TOWN_B, 310.0);

        List<VisitHistoryRecord> records = buffer.processVisits();
        long after = System.currentTimeMillis();

        assertEquals(2, records.size());

        VisitHistoryRecord recA = records.stream().filter(r -> r.getOriginTownId().equals(TOWN_A)).findFirst().orElseThrow();
        VisitHistoryRecord recB = records.stream().filter(r -> r.getOriginTownId().equals(TOWN_B)).findFirst().orElseThrow();

        assertEquals(2, recA.getCount());
        assertEquals(POS_A, recA.getOriginPos());
        assertTrue(recA.getTimestamp() >= before && recA.getTimestamp() <= after);

        assertEquals(1, recB.getCount());
        assertEquals(POS_B, recB.getOriginPos());
    }

    @Test
    void processVisits_snapshotsOnlyPositiveAvgsToDistanceMap_andClearsVisitors() {
        buffer.addVisitor(TOWN_A, POS_A);
        buffer.updateVisitorDistance(TOWN_A, 50.0);   // positive
        buffer.addVisitor(TOWN_B, POS_B);
        // TOWN_B has count=1 but distance never updated → avg 0

        List<VisitHistoryRecord> records = buffer.processVisits();

        assertEquals(2, records.size()); // both origins still emitted
        assertEquals(0, buffer.getVisitorCount());

        // Only positive avg was stored
        assertEquals(50.0, buffer.getAverageDistance(TOWN_A), 0.0001);
        assertEquals(0.0, buffer.getAverageDistance(TOWN_B), 0.0001);
    }

    @Test
    void processVisits_zeroDistanceOriginStillEmitsRecordButDoesNotStoreDistance() {
        buffer.addVisitor(TOWN_A, POS_A);
        // no updateVisitorDistance → avg remains 0

        List<VisitHistoryRecord> records = buffer.processVisits();
        assertEquals(1, records.size());
        assertEquals(1, records.get(0).getCount());
        // distanceMap should not contain it (or contains 0, but get returns 0 either way)
        assertEquals(0.0, buffer.getAverageDistance(TOWN_A), 0.0001);
    }

    // --- clearSavedDistance + post-payment behavior ---

    @Test
    void clearSavedDistance_removesEntryFromDistanceMap() {
        buffer.addVisitor(TOWN_A, POS_A);
        buffer.updateVisitorDistance(TOWN_A, 777.0);
        buffer.processVisits();

        assertEquals(777.0, buffer.getAverageDistance(TOWN_A), 0.0001);

        buffer.clearSavedDistance(TOWN_A);
        assertEquals(0.0, buffer.getAverageDistance(TOWN_A), 0.0001);
    }

    @Test
    void clearSavedDistance_onUnknownId_isSafe() {
        buffer.clearSavedDistance(TOWN_A); // no crash
        assertEquals(0.0, buffer.getAverageDistance(TOWN_A), 0.0001);
    }

    // --- global timer quirk / coalescing behavior (pinned) ---

    @Test
    void globalLastVisitTime_resetsOnAnyOrigin_arrivalsCoalesceAcrossTowns() throws Exception {
        // Add A, wait a little but <1s total
        buffer.addVisitor(TOWN_A, POS_A);
        buffer.updateVisitorDistance(TOWN_A, 100.0);
        Thread.sleep(400);

        // Add B — this resets the shared lastVisitTime
        buffer.addVisitor(TOWN_B, POS_B);
        buffer.updateVisitorDistance(TOWN_B, 200.0);
        Thread.sleep(400); // still <1s since B's add

        // Not yet ready
        assertFalse(buffer.shouldProcess());

        // Now wait full quiet from the LAST add (B)
        Thread.sleep(800); // total >1s since B
        assertTrue(buffer.shouldProcess());

        List<VisitHistoryRecord> records = buffer.processVisits();
        // Both A and B are still in the same flush batch because B's arrival kept the window open
        assertEquals(2, records.size());
        assertTrue(records.stream().anyMatch(r -> r.getOriginTownId().equals(TOWN_A)));
        assertTrue(records.stream().anyMatch(r -> r.getOriginTownId().equals(TOWN_B)));
    }

    // --- update after flush is ignored (distance must be supplied before process) ---

    @Test
    void updateVisitorDistance_afterProcessVisits_isIgnored() {
        buffer.addVisitor(TOWN_A, POS_A);
        buffer.updateVisitorDistance(TOWN_A, 123.0);
        buffer.processVisits();

        // Too late — visitors map is empty
        buffer.updateVisitorDistance(TOWN_A, 999.0);
        // Still the value from the previous cycle (or 0 after clear)
        assertEquals(123.0, buffer.getAverageDistance(TOWN_A), 0.0001);

        buffer.clearSavedDistance(TOWN_A);
        // Now a late update cannot resurrect it for this origin until a new addVisitor cycle
        buffer.updateVisitorDistance(TOWN_A, 999.0);
        assertEquals(0.0, buffer.getAverageDistance(TOWN_A), 0.0001);
    }
}
