package com.quackers29.businesscraft.town.data;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * T-011: Town Leaderboard Ranking Calculation (Test + Docs Loop).
 *
 * Covers the pure DTO logic in TownLeaderboardData:
 *   - distanceTo(BlockPos): Euclidean via Math.sqrt(position.distSqr(other))
 *   - formatDistance(double): m/km threshold at 1000 with (int) truncation under, %.1f over
 *
 * The richer "ranking" (comparators, dynamic columns, SortMode cycling) lives in
 * TownLeaderboardScreen and is UI-bound (not tested here). Tourism metric source
 * (Town.getTotalTouristsArrived via recordVisit) is documented in the vault note.
 *
 * Documentation: vault/Town/Leaderboard/Ranking Calculation.md
 */
class TownLeaderboardDataTest {

    private static final UUID SAMPLE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final BlockPos ORIGIN = new BlockPos(0, 64, 0);
    private static final BlockPos FAR = new BlockPos(1200, 64, 0); // exactly 1.2 km example

    // --- record construction & accessors (baseline) ---

    @Test
    void recordConstruction_storesAllFields() {
        TownLeaderboardData data = new TownLeaderboardData(
                SAMPLE_ID, "TestTown", ORIGIN, 42L, 123L, 67.5f, 19L);

        assertEquals(SAMPLE_ID, data.townId());
        assertEquals("TestTown", data.name());
        assertEquals(ORIGIN, data.position());
        assertEquals(42L, data.population());
        assertEquals(123L, data.money());
        assertEquals(67.5f, data.happiness());
        assertEquals(19L, data.tourism());
    }

    // --- distanceTo ---

    @Test
    void distanceTo_samePosition_returnsZero() {
        TownLeaderboardData data = new TownLeaderboardData(SAMPLE_ID, "Home", ORIGIN, 10, 0, 50f, 0);
        // distSqr(0,0,0) = 0; sqrt = 0.0
        assertEquals(0.0, data.distanceTo(ORIGIN), 1e-9);
    }

    @Test
    void distanceTo_threeBlocksEast_exactInteger() {
        BlockPos east3 = new BlockPos(3, 64, 0);
        TownLeaderboardData data = new TownLeaderboardData(SAMPLE_ID, "East", ORIGIN, 1, 0, 50f, 0);
        // 3^2 = 9, sqrt(9) = 3.0 exact
        assertEquals(3.0, data.distanceTo(east3), 1e-9);
    }

    @Test
    void distanceTo_6_8_10_triangle_exactTen() {
        // Classic 6-8-10 right triangle (scaled 3-4-5)
        BlockPos corner = new BlockPos(6, 64, 8);
        TownLeaderboardData data = new TownLeaderboardData(SAMPLE_ID, "Corner", ORIGIN, 1, 0, 50f, 0);
        // 6^2 + 8^2 = 36 + 64 = 100; sqrt(100) = 10.0
        assertEquals(10.0, data.distanceTo(corner), 1e-9);
    }

    @Test
    void distanceTo_oneBlockX_oneBlockZ_sqrtTwo() {
        BlockPos diag = new BlockPos(1, 64, 1);
        TownLeaderboardData data = new TownLeaderboardData(SAMPLE_ID, "Diag", ORIGIN, 1, 0, 50f, 0);
        // distSqr = 1 + 0 + 1 = 2; sqrt(2) ≈ 1.41421356237
        assertEquals(Math.sqrt(2), data.distanceTo(diag), 1e-9);
    }

    @Test
    void distanceTo_largeDistance_1200Blocks() {
        TownLeaderboardData data = new TownLeaderboardData(SAMPLE_ID, "Far", ORIGIN, 1, 0, 50f, 0);
        // 1200^2 = 1_440_000; sqrt = 1200.0 exact
        assertEquals(1200.0, data.distanceTo(FAR), 1e-9);
    }

    // --- formatDistance ---

    @Test
    void formatDistance_zero_blocksAsMeters() {
        // 0 < 1000 → (int)0 → "0m"
        assertEquals("0m", TownLeaderboardData.formatDistance(0.0));
    }

    @Test
    void formatDistance_500_underThresholdMeters() {
        // 500 < 1000 → "500m"
        assertEquals("500m", TownLeaderboardData.formatDistance(500.0));
    }

    @Test
    void formatDistance_999_truncatesTowardZero() {
        // 999.9 < 1000 → (int)999.9 = 999 → "999m"
        assertEquals("999m", TownLeaderboardData.formatDistance(999.9));
    }

    @Test
    void formatDistance_exactly1000_oneDecimalKm() {
        // 1000 >= 1000 → 1000/1000.0 = 1.0 → "1.0km"
        assertEquals("1.0km", TownLeaderboardData.formatDistance(1000.0));
    }

    @Test
    void formatDistance_1234_onePointTwoKm() {
        // 1234 / 1000.0 = 1.234 → %.1f → "1.2km"
        assertEquals("1.2km", TownLeaderboardData.formatDistance(1234.0));
    }

    @Test
    void formatDistance_10000_tenPointZeroKm() {
        // 10000 / 1000 = 10.0 → "10.0km"
        assertEquals("10.0km", TownLeaderboardData.formatDistance(10000.0));
    }

    @Test
    void formatDistance_123456_oneHundredTwentyThreePointFiveKm() {
        // 123456 / 1000.0 = 123.456 → %.1f rounds to "123.5km"
        assertEquals("123.5km", TownLeaderboardData.formatDistance(123456.0));
    }

    @Test
    void formatDistance_negativeMeters_currentBehaviorPinsNegativeString() {
        // QUIRK (documented in vault note): negative input <1000 takes the m branch
        // and produces a leading-minus string. Unreachable via distanceTo (always >=0)
        // but formatDistance is public static. This test pins the current behavior.
        assertEquals("-42m", TownLeaderboardData.formatDistance(-42.0));
    }

    @Test
    void formatDistance_largeMagnitudeNegative_stillUsesMetersBranch() {
        // QUIRK (documented): the condition is `distance < 1000` (signed), not abs.
        // Any negative (e.g. -1500) is < 1000, so (int) cast + "m" branch always.
        // km formatting is unreachable for negative inputs. Pins current behavior.
        assertEquals("-1500m", TownLeaderboardData.formatDistance(-1500.0));
    }
}
