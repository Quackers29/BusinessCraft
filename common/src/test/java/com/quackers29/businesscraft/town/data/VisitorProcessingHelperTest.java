package com.quackers29.businesscraft.town.data;

import com.quackers29.businesscraft.api.ITownDataProvider.VisitHistoryRecord;
import com.quackers29.businesscraft.config.ConfigLoader;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * T-001: Distance Payment Calculation (Test + Docs Loop).
 *
 * Covers the core economy formula in VisitorProcessingHelper.calculatePayment():
 *   payment = (int) Math.max(1, (averageDistance / metersPerEmerald) * touristCount)
 *
 * The method is private, so it is invoked via reflection rather than modifying
 * production code (loop rule). Documentation:
 * vault/Economy/Tourist Payments/Distance Payment Calculation.md
 */
class VisitorProcessingHelperTest {

    private static final UUID ORIGIN_TOWN = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final BlockPos ORIGIN_POS = new BlockPos(0, 64, 0);

    private VisitorProcessingHelper helper;
    private VisitBuffer buffer;
    private int savedRate;

    @BeforeEach
    void setUp() {
        helper = new VisitorProcessingHelper();
        buffer = new VisitBuffer();
        savedRate = ConfigLoader.metersPerEmerald;
        ConfigLoader.metersPerEmerald = 50; // default rate, fixed for determinism
    }

    @AfterEach
    void tearDown() {
        ConfigLoader.metersPerEmerald = savedRate;
    }

    // --- helpers ---

    private int calculatePayment(VisitHistoryRecord record) throws Exception {
        Method m = VisitorProcessingHelper.class.getDeclaredMethod(
                "calculatePayment", VisitBuffer.class, VisitHistoryRecord.class);
        m.setAccessible(true);
        return (int) m.invoke(helper, buffer, record);
    }

    /** Adds n tourists from ORIGIN_TOWN with the given traveled distances. */
    private void arriveTourists(double... distances) {
        for (double d : distances) {
            buffer.addVisitor(ORIGIN_TOWN, ORIGIN_POS);
            buffer.updateVisitorDistance(ORIGIN_TOWN, d);
        }
    }

    private VisitHistoryRecord record(int count) {
        return new VisitHistoryRecord(System.currentTimeMillis(), ORIGIN_TOWN, count, ORIGIN_POS);
    }

    // --- formula tests ---

    @Test
    void calculatePayment_singleTourist_basicRate() throws Exception {
        arriveTourists(500.0);
        // 500 / 50 * 1 = 10
        assertEquals(10, calculatePayment(record(1)));
    }

    @Test
    void calculatePayment_multipleTourists_averagesDistanceAndMultipliesByCount() throws Exception {
        arriveTourists(400.0, 600.0);
        // avg = (400+600)/2 = 500; 500 / 50 * 2 = 20
        assertEquals(20, calculatePayment(record(2)));
    }

    @Test
    void calculatePayment_fractionalResult_truncatesTowardZero() throws Exception {
        arriveTourists(99.0);
        // 99 / 50 * 1 = 1.98 -> max(1, 1.98) = 1.98 -> (int) = 1
        assertEquals(1, calculatePayment(record(1)));
    }

    @Test
    void calculatePayment_shortTrip_paysMinimumOneEmeraldPerBatch() throws Exception {
        arriveTourists(10.0);
        // 10 / 50 * 1 = 0.2 -> max(1, 0.2) = 1
        assertEquals(1, calculatePayment(record(1)));
    }

    @Test
    void calculatePayment_shortTripManyTourists_minimumIsPerBatchNotPerTourist() throws Exception {
        arriveTourists(10.0, 10.0, 10.0, 10.0, 10.0);
        // avg = 10; 10 / 50 * 5 = 1.0 -> max(1, 1.0) = 1 (NOT 5)
        assertEquals(1, calculatePayment(record(5)));
    }

    @Test
    void calculatePayment_noDistanceRecorded_paysZero() throws Exception {
        buffer.addVisitor(ORIGIN_TOWN, ORIGIN_POS); // arrival but no distance update
        // averageDistance == 0 -> returns 0, minimum floor does NOT apply
        assertEquals(0, calculatePayment(record(1)));
    }

    @Test
    void calculatePayment_unknownOriginTown_paysZero() throws Exception {
        // Nothing in the buffer at all for this town
        assertEquals(0, calculatePayment(record(1)));
    }

    @Test
    void calculatePayment_respectsConfiguredRate() throws Exception {
        ConfigLoader.metersPerEmerald = 100;
        arriveTourists(500.0);
        // 500 / 100 * 1 = 5
        assertEquals(5, calculatePayment(record(1)));
    }

    @Test
    void calculatePayment_clearsDistanceAfterPayment_preventsDoublePay() throws Exception {
        arriveTourists(500.0);
        // Mirror production flow: processVisits() moves avg distance to the
        // persistent map and clears the live buffer before payment runs.
        buffer.processVisits();

        assertEquals(10, calculatePayment(record(1)));
        // Saved distance was cleared by the first payment -> second attempt pays 0
        assertEquals(0, calculatePayment(record(1)));
    }

    @Test
    void calculatePayment_zeroCountWithDistance_currentBehaviorPaysOne() throws Exception {
        // QUIRK (documented in vault note, Open questions): count=0 with positive
        // distance yields max(1, 0) = 1 emerald. Unreachable via normal buffering
        // today (records always have count >= 1), but the formula has no guard.
        // This test pins CURRENT behavior; if it starts failing, the formula
        // changed and the vault note must be updated.
        arriveTourists(500.0);
        assertEquals(1, calculatePayment(record(0)));
    }
}
