package com.quackers29.businesscraft.town;

import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.testutil.McBootstrap;
import com.quackers29.businesscraft.town.components.TownUpgradeComponent;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-035: Work Unit Accounting (Test + Docs Loop).
 *
 * Covers the WU counter math in Town:
 *   setWorkUnits(amount) = max(0, amount)
 *   addWorkUnits(amount) = addExact(current, amount) with overflow rewrite,
 *                          then floor at 0, then (if cap > 0) floor at cap
 *   getWorkUnitCap() = (long) wu_cap modifier (0 when absent)
 *
 * Town ctor is now usable thanks to McBootstrap (see T-008 pattern).
 * Private maps in TownUpgradeComponent are poked via reflection only to
 * inject a deterministic "wu_cap" for cap-clamp tests — no production code changed.
 * Documentation: vault/Town/Resources/Work Unit Accounting.md
 */
class TownTest {

    private static final UUID TOWN_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final BlockPos TOWN_POS = new BlockPos(0, 64, 0);

    private Town town;
    private int savedDefaultPop;

    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

    @BeforeEach
    void setUp() throws Exception {
        savedDefaultPop = ConfigLoader.defaultStartingPopulation;
        ConfigLoader.defaultStartingPopulation = 5; // match other Town-using tests for ctor determinism

        town = new Town(TOWN_ID, TOWN_POS, "TestTown");
        // Fresh town starts at 0 WU (field init) and wu_cap modifier absent (0)
    }

    @AfterEach
    void tearDown() {
        ConfigLoader.defaultStartingPopulation = savedDefaultPop;
    }

    // --- reflection helper to inject wu_cap without going through full upgrade research ---
    @SuppressWarnings("unchecked")
    private void setWuCap(float capValue) throws Exception {
        Field upgradesField = Town.class.getDeclaredField("upgrades");
        upgradesField.setAccessible(true);
        TownUpgradeComponent upgrades = (TownUpgradeComponent) upgradesField.get(town);

        Field modsField = TownUpgradeComponent.class.getDeclaredField("activeModifiers");
        modsField.setAccessible(true);
        Map<String, Float> mods = (Map<String, Float>) modsField.get(upgrades);
        mods.put("wu_cap", capValue);
    }

    private long getWuCapViaReflection() throws Exception {
        // Direct read of the map for verification in one test; normally use the public getter
        Field upgradesField = Town.class.getDeclaredField("upgrades");
        upgradesField.setAccessible(true);
        TownUpgradeComponent upgrades = (TownUpgradeComponent) upgradesField.get(town);

        Field modsField = TownUpgradeComponent.class.getDeclaredField("activeModifiers");
        modsField.setAccessible(true);
        Map<String, Float> mods = (Map<String, Float>) modsField.get(upgrades);
        Float v = mods.get("wu_cap");
        return v == null ? 0L : v.longValue();
    }

    // --- getWorkUnits / basic set ---

    @Test
    void getWorkUnits_freshTown_isZero() {
        assertEquals(0L, town.getWorkUnits());
    }

    @Test
    void setWorkUnits_positive_storesExactValue() {
        town.setWorkUnits(123L);
        // 123 > 0 → stored 123
        assertEquals(123L, town.getWorkUnits());
    }

    @Test
    void setWorkUnits_zero_storesZero() {
        town.setWorkUnits(0L);
        assertEquals(0L, town.getWorkUnits());
    }

    @Test
    void setWorkUnits_negative_clampsToZero() {
        town.setWorkUnits(-50L);
        // Math.max(0, -50) = 0
        assertEquals(0L, town.getWorkUnits());
    }

    // --- addWorkUnits happy paths ---

    @Test
    void addWorkUnits_positive_increases() {
        town.addWorkUnits(40L);
        // 0 + 40 = 40
        assertEquals(40L, town.getWorkUnits());
    }

    @Test
    void addWorkUnits_zero_noChange() {
        town.addWorkUnits(0L);
        assertEquals(0L, town.getWorkUnits());
    }

    @Test
    void addWorkUnits_negative_decreases() {
        town.setWorkUnits(30L);
        town.addWorkUnits(-10L);
        // 30 + (-10) = 20
        assertEquals(20L, town.getWorkUnits());
    }

    // --- zero and floor behavior ---

    @Test
    void addWorkUnits_negativeBelowZero_floorsAtZero() {
        town.setWorkUnits(5L);
        town.addWorkUnits(-100L);
        // 5 + (-100) underflow path or post-clamp → 0
        assertEquals(0L, town.getWorkUnits());
    }

    @Test
    void addWorkUnits_fromZero_negative_floorsAtZero() {
        town.addWorkUnits(-1L);
        // 0 + (-1) → rewrite or post → 0
        assertEquals(0L, town.getWorkUnits());
    }

    // --- cap behavior (cap=0 means no upper clamp) ---

    @Test
    void getWorkUnitCap_defaultZero() {
        assertEquals(0L, town.getWorkUnitCap());
    }

    @Test
    void addWorkUnits_withCapZero_allowsGrowthPastWhatWouldBeCapped() throws Exception {
        // cap remains 0 → no upper clamp
        town.addWorkUnits(999_999L);
        assertEquals(999_999L, town.getWorkUnits());
    }

    // --- overflow paths ---

    @Test
    void addWorkUnits_positiveOverflow_rewritesToMaxValue() {
        town.setWorkUnits(Long.MAX_VALUE - 5);
        town.addWorkUnits(100L);
        // addExact overflows → amount>0 ? Long.MAX_VALUE : 0
        assertEquals(Long.MAX_VALUE, town.getWorkUnits());
    }

    @Test
    void addWorkUnits_negativeUnderflow_rewritesToZero() {
        town.setWorkUnits(0);
        // Forcing the negative overflow path: current=0, large negative amount
        // (addExact(0, Long.MIN_VALUE) would overflow for most impls, but even without,
        // the post-clamp if ( < 0 ) catches it. We exercise the rewrite by a huge negative.)
        town.addWorkUnits(Long.MIN_VALUE);
        // either rewrite sets 0 or post-clamp sets 0
        assertEquals(0L, town.getWorkUnits());
    }

    // --- cap clamp after add (only add respects cap; set does not) ---

    @Test
    void addWorkUnits_whenAboveCap_clampsToCap() throws Exception {
        setWuCap(1000f);
        town.setWorkUnits(900L);
        town.addWorkUnits(200L);
        // 900 + 200 = 1100 (no overflow) → cap>0 && 1100>1000 → 1000
        assertEquals(1000L, town.getWorkUnits());
    }

    @Test
    void addWorkUnits_smallAddThatStaysUnderCap_noClamp() throws Exception {
        setWuCap(1000f);
        town.setWorkUnits(900L);
        town.addWorkUnits(50L);
        // 900 + 50 = 950 < 1000 → stays 950
        assertEquals(950L, town.getWorkUnits());
    }

    @Test
    void addWorkUnits_overflowThenCap_stillClampsToCap() throws Exception {
        setWuCap(5000L); // small cap
        town.setWorkUnits(Long.MAX_VALUE - 100);
        town.addWorkUnits(1000L);
        // overflow rewrite → Long.MAX_VALUE → cap>0 && MAX>5000 → 5000
        assertEquals(5000L, town.getWorkUnits());
    }

    @Test
    void setWorkUnits_bypassesCap_canSetAboveCap() throws Exception {
        setWuCap(100f);
        town.setWorkUnits(999L);
        // set only does max(0, amount); ignores cap
        assertEquals(999L, town.getWorkUnits());
        // subsequent add will clamp
        town.addWorkUnits(1L);
        assertEquals(100L, town.getWorkUnits());
    }

    // --- cap value round-trip via public getter after reflection injection ---

    @Test
    void getWorkUnitCap_afterInjectingModifier_returnsLongTruncatedValue() throws Exception {
        setWuCap(12345.9f);
        // getter casts inside getWorkUnitCap
        assertEquals(12345L, town.getWorkUnitCap());
        // verify the map actually holds the float (for precision awareness)
        assertEquals(12345L, getWuCapViaReflection());
    }

    // ============================================================
    // T-036: Boundary Geometry Queries (getBoundaryRadius + spatial)
    // Extends the same TownTest (big-class split per protocol).
    // Reflection injects "border" modifier exactly like wu_cap above.
    // All formulas have hand-computed expectations in comments.
    // ============================================================

    @SuppressWarnings("unchecked")
    private void setBorder(float borderValue) throws Exception {
        Field upgradesField = Town.class.getDeclaredField("upgrades");
        upgradesField.setAccessible(true);
        TownUpgradeComponent upgrades = (TownUpgradeComponent) upgradesField.get(town);

        Field modsField = TownUpgradeComponent.class.getDeclaredField("activeModifiers");
        modsField.setAccessible(true);
        Map<String, Float> mods = (Map<String, Float>) modsField.get(upgrades);
        mods.put("border", borderValue);
    }

    private float getBorderViaReflection() throws Exception {
        Field upgradesField = Town.class.getDeclaredField("upgrades");
        upgradesField.setAccessible(true);
        TownUpgradeComponent upgrades = (TownUpgradeComponent) upgradesField.get(town);

        Field modsField = TownUpgradeComponent.class.getDeclaredField("activeModifiers");
        modsField.setAccessible(true);
        Map<String, Float> mods = (Map<String, Float>) modsField.get(upgrades);
        Float v = mods.get("border");
        return v == null ? 0f : v;
    }

    // --- getBoundaryRadius (fallback 50 + explicit modifier) ---

    @Test
    void getBoundaryRadius_freshTown_noModifier_returnsFallback50() {
        // No "border" entry → <=0 branch → 50
        assertEquals(50, town.getBoundaryRadius());
    }

    @Test
    void getBoundaryRadius_explicitBorder30_returns30() throws Exception {
        setBorder(30.0f);
        // (int)30.0f = 30
        assertEquals(30, town.getBoundaryRadius());
    }

    @Test
    void getBoundaryRadius_borderWithFraction_truncatesTowardZero() throws Exception {
        setBorder(45.9f);
        // (int)45.9f = 45 (truncation toward zero)
        assertEquals(45, town.getBoundaryRadius());
    }

    @Test
    void getBoundaryRadius_borderZeroOrNegative_fallsBackTo50() throws Exception {
        setBorder(0.0f);
        assertEquals(50, town.getBoundaryRadius());
        setBorder(-7.5f);
        assertEquals(50, town.getBoundaryRadius());
    }

    // --- isPositionInside ---

    @Test
    void isPositionInside_nullPos_returnsFalse() {
        assertEquals(false, town.isPositionInside(null));
    }

    @Test
    void isPositionInside_centerPos_returnsTrue() {
        // distSqr( same pos ) = 0 <= (50*50) = 2500
        assertEquals(true, town.isPositionInside(TOWN_POS));
    }

    @Test
    void isPositionInside_exactBoundaryInclusive_returnsTrue() throws Exception {
        // r=50, place a pos exactly 50 blocks away on X (distSqr = 2500)
        BlockPos onBoundary = new BlockPos(TOWN_POS.getX() + 50, TOWN_POS.getY(), TOWN_POS.getZ());
        // 50*50 = 2500; 2500 <= 2500 → true (inclusive)
        assertEquals(true, town.isPositionInside(onBoundary));
    }

    @Test
    void isPositionInside_justOutside_returnsFalse() throws Exception {
        // r=50, 51 blocks away → 51*51=2601 > 2500
        BlockPos justOut = new BlockPos(TOWN_POS.getX() + 51, TOWN_POS.getY(), TOWN_POS.getZ());
        assertEquals(false, town.isPositionInside(justOut));
    }

    @Test
    void isPositionInside_withCustomRadius_affectsResult() throws Exception {
        setBorder(10.0f);
        BlockPos inside = new BlockPos(TOWN_POS.getX() + 5, TOWN_POS.getY(), TOWN_POS.getZ());
        BlockPos outside = new BlockPos(TOWN_POS.getX() + 11, TOWN_POS.getY(), TOWN_POS.getZ());
        // 5^2=25 <= 100 → true; 11^2=121 > 100 → false
        assertEquals(true, town.isPositionInside(inside));
        assertEquals(false, town.isPositionInside(outside));
    }

    // --- wouldOverlapWith ---

    @Test
    void wouldOverlapWith_nullOther_returnsFalse() {
        assertEquals(false, town.wouldOverlapWith(null));
    }

    @Test
    void wouldOverlapWith_self_returnsTrue_forNonZeroRadius() {
        // d=0 < (50+50) = 100 → true
        assertEquals(true, town.wouldOverlapWith(town));
    }

    @Test
    void wouldOverlapWith_centersExactlyAtSum_returnsFalse() throws Exception {
        // Two towns r=50 each, centers 100 blocks apart on X
        // d=100 == 100, 100 < 100 is false (strict <)
        Town other = new Town(UUID.randomUUID(), new BlockPos(TOWN_POS.getX() + 100, TOWN_POS.getY(), TOWN_POS.getZ()), "Other");
        assertEquals(false, town.wouldOverlapWith(other));
    }

    @Test
    void wouldOverlapWith_strictlyLessThanSum_returnsTrue() throws Exception {
        // d=99 < 100 → overlap
        Town other = new Town(UUID.randomUUID(), new BlockPos(TOWN_POS.getX() + 99, TOWN_POS.getY(), TOWN_POS.getZ()), "Other");
        assertEquals(true, town.wouldOverlapWith(other));
    }

    @Test
    void wouldOverlapWith_differentRadii_usesSum() throws Exception {
        setBorder(30.0f); // this r=30
        Town other = new Town(UUID.randomUUID(), new BlockPos(TOWN_POS.getX() + 70, TOWN_POS.getY(), TOWN_POS.getZ()), "Other");
        // Force other to r=50 via its own upgrades (fresh other starts at 50)
        // 70 < (30+50)=80 → true
        assertEquals(true, town.wouldOverlapWith(other));
    }

    // --- getMinimumDistanceRequired ---

    @Test
    void getMinimumDistanceRequired_nullOther_returnsOwnRadius() {
        // r=50 (fresh)
        assertEquals(50.0, town.getMinimumDistanceRequired(null), 0.0001);
    }

    @Test
    void getMinimumDistanceRequired_twoTowns_returnsSum() throws Exception {
        setBorder(40.0f); // this=40
        Town other = new Town(UUID.randomUUID(), new BlockPos(TOWN_POS.getX() + 123, TOWN_POS.getY(), TOWN_POS.getZ()), "Other");
        // other fresh r=50; 40+50=90
        assertEquals(90.0, town.getMinimumDistanceRequired(other), 0.0001);
    }

    @Test
    void getMinimumDistanceRequired_afterBorderChange_affectsResult() throws Exception {
        setBorder(25.0f);
        assertEquals(25.0, town.getMinimumDistanceRequired(null), 0.0001);
    }

    // ============================================================
    // T-039: Visit History Recording (recordVisit + totals + trim)
    // Extends the same TownTest (big-class split per protocol).
    // Pure logic using only BlockPos/UUID (no McBootstrap/ItemStack needed).
    // All formulas have hand-computed expectations in comments.
    // Documentation: vault/Town/Visits/Visit History Recording.md
    // ============================================================

    private static final BlockPos ORIGIN_POS_A = new BlockPos(30, 64, 40); // distSqr=900+1600=2500, dist=50.0 exact
    private static final BlockPos ORIGIN_POS_B = new BlockPos(0, 64, 200);  // dist=200.0 exact
    private static final UUID ORIGIN_U = UUID.fromString("11111111-1111-1111-1111-111111111111");

    // --- happy path + math ---

    @Test
    void recordVisit_happyPath_insertsNewestFirst_updatesTotals() {
        // dist = sqrt(30^2 + 40^2) = sqrt(900+1600)=sqrt(2500)=50.0
        // 50.0 * 2 = 100.0
        town.recordVisit(ORIGIN_U, 2, ORIGIN_POS_A);

        assertEquals(2L, town.getTotalTouristsArrived());
        assertEquals(100.0, town.getTotalTouristDistance(), 1e-9);

        List<ITownDataProvider.VisitHistoryRecord> hist = town.getVisitHistory();
        assertEquals(1, hist.size());
        ITownDataProvider.VisitHistoryRecord rec = hist.get(0);
        assertEquals(ORIGIN_U, rec.getOriginTownId());
        assertEquals(2, rec.getCount());
        assertEquals(ORIGIN_POS_A, rec.getOriginPos());
        assertTrue(rec.getTimestamp() > 0);
    }

    @Test
    void recordVisit_multipleRecords_newestIsFirst() {
        town.recordVisit(ORIGIN_U, 1, ORIGIN_POS_A);
        UUID v = UUID.randomUUID();
        town.recordVisit(v, 7, ORIGIN_POS_B);

        List<ITownDataProvider.VisitHistoryRecord> hist = town.getVisitHistory();
        assertEquals(2, hist.size());
        // newest first
        assertEquals(v, hist.get(0).getOriginTownId());
        assertEquals(7, hist.get(0).getCount());
        assertEquals(ORIGIN_U, hist.get(1).getOriginTownId());
        assertEquals(1, hist.get(1).getCount());
    }

    // --- count edges (including quirks) ---

    @Test
    void recordVisit_countZero_createsRecord_noTotalChange() {
        // Hand-computed: +=0 leaves totals at 0; record still takes a history slot
        town.recordVisit(ORIGIN_U, 0, ORIGIN_POS_A);

        assertEquals(0L, town.getTotalTouristsArrived());
        assertEquals(0.0, town.getTotalTouristDistance(), 1e-9);

        List<ITownDataProvider.VisitHistoryRecord> hist = town.getVisitHistory();
        assertEquals(1, hist.size());
        assertEquals(0, hist.get(0).getCount());
    }

    @Test
    void recordVisit_negativeCount_decrementsTotal_currentBehavior() {
        // Hand-computed: first 50.0*5=250, arrived=5; then 50.0*(-2)=-100, arrived=3; dist=150.0
        // negative counts are accepted by += with no guard (pinned current behavior)
        town.recordVisit(ORIGIN_U, 5, ORIGIN_POS_A);
        town.recordVisit(ORIGIN_U, -2, ORIGIN_POS_A);

        assertEquals(3L, town.getTotalTouristsArrived());
        assertEquals(150.0, town.getTotalTouristDistance(), 1e-9);
    }

    // --- position guard edges ---

    @Test
    void recordVisit_nullOriginPos_skipsDistance_butStillRecords() {
        town.recordVisit(ORIGIN_U, 4, null);

        assertEquals(4L, town.getTotalTouristsArrived());
        assertEquals(0.0, town.getTotalTouristDistance(), 1e-9); // no addition
        assertEquals(1, town.getVisitHistory().size());
    }

    @Test
    void recordVisit_blockPosZero_skipsDistance_currentBehavior() {
        // The guard is "!= BlockPos.ZERO" (identity against the constant) + null check
        town.recordVisit(ORIGIN_U, 3, BlockPos.ZERO);

        assertEquals(3L, town.getTotalTouristsArrived());
        assertEquals(0.0, town.getTotalTouristDistance(), 1e-9);
    }

    // --- trim behavior ---

    @Test
    void recordVisit_exactly50_keepsAll() {
        for (int i = 0; i < 50; i++) {
            town.recordVisit(UUID.randomUUID(), 1, ORIGIN_POS_A);
        }
        assertEquals(50, town.getVisitHistory().size());
    }

    @Test
    void recordVisit_51st_trimsOldest() {
        // Insert 50, then one more; oldest (first inserted) must be dropped
        UUID first = UUID.randomUUID();
        town.recordVisit(first, 1, ORIGIN_POS_A);
        for (int i = 1; i < 50; i++) {
            town.recordVisit(UUID.randomUUID(), 1, ORIGIN_POS_A);
        }
        // size is 50, first is still at end
        assertEquals(50, town.getVisitHistory().size());
        assertEquals(first, town.getVisitHistory().get(49).getOriginTownId());

        // 51st
        UUID fiftyFirst = UUID.randomUUID();
        town.recordVisit(fiftyFirst, 1, ORIGIN_POS_A);

        List<ITownDataProvider.VisitHistoryRecord> hist = town.getVisitHistory();
        assertEquals(50, hist.size());
        // newest is at 0
        assertEquals(fiftyFirst, hist.get(0).getOriginTownId());
        // the original first has been trimmed (no longer in list)
        for (ITownDataProvider.VisitHistoryRecord r : hist) {
            assertNotEquals(first, r.getOriginTownId());
        }
    }

    // --- unmodifiable view ---

    @Test
    void getVisitHistory_returnsUnmodifiableView() {
        town.recordVisit(ORIGIN_U, 1, ORIGIN_POS_A);
        List<ITownDataProvider.VisitHistoryRecord> hist = town.getVisitHistory();
        assertThrows(UnsupportedOperationException.class, () -> hist.add(null));
        assertThrows(UnsupportedOperationException.class, () -> hist.remove(0));
        assertThrows(UnsupportedOperationException.class, hist::clear);
    }
}
