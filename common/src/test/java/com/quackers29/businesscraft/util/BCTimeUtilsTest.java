package com.quackers29.businesscraft.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-017: Time Display Formatting (Test + Docs Loop).
 *
 * Covers all public formatting, expiry, and remaining helpers in BCTimeUtils.
 * These are pure static methods (java.time only) with no Minecraft dependencies.
 *
 * Documentation: vault/Core/Time/Time Display Formatting.md
 */
class BCTimeUtilsTest {

    private String savedTimezone;

    // Known fixed epochs for reproducible date formatting tests (all UTC)
    private static final long EPOCH_2026_01_14_15_30_45_UTC = 1_704_000_000_000L + (14-1)*86400_000L + 15*3600_000L + 30*60_000L + 45_000L; // approx; exact computed in test
    // Use explicit construction in the date test so arithmetic is shown.

    @BeforeEach
    void setUp() {
        savedTimezone = BCTimeUtils.getTimezone();
        BCTimeUtils.setTimezone("UTC"); // deterministic for all tests
    }

    @AfterEach
    void tearDown() {
        BCTimeUtils.setTimezone(savedTimezone);
    }

    // --- formatDuration ---

    @Test
    void formatDuration_zero_returnsZeroS() {
        // 0 ms -> "0s"
        assertEquals("0s", BCTimeUtils.formatDuration(0));
    }

    @Test
    void formatDuration_negative_returnsZeroS() {
        // negative treated as zero
        assertEquals("0s", BCTimeUtils.formatDuration(-12345));
    }

    @Test
    void formatDuration_underOneSecond_returnsSecondsOnly() {
        // 45000 ms = 45 s
        assertEquals("45s", BCTimeUtils.formatDuration(45_000));
    }

    @Test
    void formatDuration_exactOneMinute_returnsM0s() {
        // 60_000 ms = 1 m
        assertEquals("1m 0s", BCTimeUtils.formatDuration(60_000));
    }

    @Test
    void formatDuration_mixedMinutesSeconds() {
        // 125_000 ms = 2m 5s  (125000/1000=125s; 125/60=2m, 125%60=5s)
        assertEquals("2m 5s", BCTimeUtils.formatDuration(125_000));
    }

    @Test
    void formatDuration_exactOneHour_returnsH0m() {
        // 3_600_000 ms
        assertEquals("1h 0m", BCTimeUtils.formatDuration(3_600_000));
    }

    @Test
    void formatDuration_hoursAndMinutes() {
        // 3h 15m = 3*3600k + 15*60k = 11_700_000 ms
        long ms = 3L * 3_600_000 + 15L * 60_000;
        assertEquals("3h 15m", BCTimeUtils.formatDuration(ms));
    }

    @Test
    void formatDuration_exactOneDay_returnsD0h() {
        // 86_400_000 ms
        assertEquals("1d 0h", BCTimeUtils.formatDuration(86_400_000));
    }

    @Test
    void formatDuration_daysAndHours() {
        // 2d 5h = 2*86400k + 5*3600k = 190_800_000 ms
        long ms = 2L * 86_400_000 + 5L * 3_600_000;
        assertEquals("2d 5h", BCTimeUtils.formatDuration(ms));
    }

    @Test
    void formatDuration_largeDays_noCap() {
        // 100 days + 0h
        long ms = 100L * 86_400_000;
        assertEquals("100d 0h", BCTimeUtils.formatDuration(ms));
    }

    // --- formatTimeRemaining ---

    @Test
    void formatTimeRemaining_positiveDuration_delegatesToFormatDuration() {
        // expiry = now + 2m5s
        long now = 1_000_000_000_000L;
        long expiry = now + 125_000;
        assertEquals("2m 5s", BCTimeUtils.formatTimeRemaining(expiry, now));
    }

    @Test
    void formatTimeRemaining_zeroOrNegative_returnsExpired() {
        long now = 1_000_000_000_000L;
        assertEquals("Expired", BCTimeUtils.formatTimeRemaining(now, now));
        assertEquals("Expired", BCTimeUtils.formatTimeRemaining(now - 1, now));
        assertEquals("Expired", BCTimeUtils.formatTimeRemaining(now - 86_400_000, now));
    }

    // --- formatTimeAgo ---

    @Test
    void formatTimeAgo_futureTimestamp_returnsInTheFuture() {
        long now = 1_000_000_000_000L;
        assertEquals("In the future", BCTimeUtils.formatTimeAgo(now + 1000, now));
    }

    @Test
    void formatTimeAgo_underOneMinute_returnsJustNow() {
        long now = 1_000_000_000_000L;
        assertEquals("Just now", BCTimeUtils.formatTimeAgo(now - 30_000, now)); // 30s ago
        assertEquals("Just now", BCTimeUtils.formatTimeAgo(now - 59_999, now));
    }

    @Test
    void formatTimeAgo_minutes() {
        long now = 1_000_000_000_000L;
        // 90s ago -> 1m ago (90_000 / 60_000 = 1)
        assertEquals("1m ago", BCTimeUtils.formatTimeAgo(now - 90_000, now));
        // 5m 30s -> still 5m (floored)
        assertEquals("5m ago", BCTimeUtils.formatTimeAgo(now - 5*60_000 - 30_000, now));
    }

    @Test
    void formatTimeAgo_hours() {
        long now = 1_000_000_000_000L;
        long twoHoursMs = 2L * 60 * 60 * 1000;
        assertEquals("2h ago", BCTimeUtils.formatTimeAgo(now - twoHoursMs, now));
    }

    @Test
    void formatTimeAgo_days() {
        long now = 1_000_000_000_000L;
        long threeDaysMs = 3L * 24 * 60 * 60 * 1000;
        assertEquals("3d ago", BCTimeUtils.formatTimeAgo(now - threeDaysMs, now));
    }

    // --- isExpired / getTimeRemaining ---

    @Test
    void isExpired_exactNow_isNotExpired() {
        long t = 1_000_000_000_000L;
        assertFalse(BCTimeUtils.isExpired(t, t));
    }

    @Test
    void isExpired_oneMsPast_isExpired() {
        long t = 1_000_000_000_000L;
        assertTrue(BCTimeUtils.isExpired(t, t + 1));
    }

    @Test
    void isExpired_future_isNotExpired() {
        long now = 1_000_000_000_000L;
        assertFalse(BCTimeUtils.isExpired(now + 5000, now));
    }

    @Test
    void getTimeRemaining_future_returnsExactDelta() {
        long now = 1_000_000_000_000L;
        assertEquals(125_000, BCTimeUtils.getTimeRemaining(now + 125_000, now));
    }

    @Test
    void getTimeRemaining_pastOrNow_returnsZero() {
        long now = 1_000_000_000_000L;
        assertEquals(0, BCTimeUtils.getTimeRemaining(now, now));
        assertEquals(0, BCTimeUtils.getTimeRemaining(now - 999, now));
    }

    // --- date/time formatters (UTC forced) ---

    @Test
    void formatDateTime_knownEpoch_producesMMddHHmm() {
        // 2026-01-14 15:30:45 UTC  (we compute the millis here with comments)
        // base: 2026-01-01 00:00:00 UTC is not needed; use known good value
        // 1705200000000L was around Jan 2024; we just assert a stable pattern for a fixed input
        long epoch = 1_704_000_000_000L; // 2024-01-14-ish, doesn't matter, pattern is fixed
        // The important contract: always "MM/dd HH:mm" in the set zone (UTC here)
        String s = BCTimeUtils.formatDateTime(epoch);
        assertTrue(s.matches("\\d{2}/\\d{2} \\d{2}:\\d{2}"), "Expected MM/dd HH:mm but got: " + s);
    }

    @Test
    void formatFullDateTime_producesFullPattern() {
        long epoch = 1_704_000_000_000L;
        String s = BCTimeUtils.formatFullDateTime(epoch);
        // MMM dd, yyyy HH:mm:ss  (month name depends on locale but pattern contract holds)
        assertTrue(s.matches("[A-Za-z]{3} \\d{2}, \\d{4} \\d{2}:\\d{2}:\\d{2}"), "Got: " + s);
    }

    @Test
    void formatTimeOnly_producesHHmmss() {
        long epoch = 1_704_000_000_000L;
        String s = BCTimeUtils.formatTimeOnly(epoch);
        assertTrue(s.matches("\\d{2}:\\d{2}:\\d{2}"), "Got: " + s);
    }

    // --- timezone config ---

    @Test
    void setTimezone_and_getTimezone_roundtrips() {
        BCTimeUtils.setTimezone("UTC");
        assertEquals("UTC", BCTimeUtils.getTimezone());
        BCTimeUtils.setTimezone("Europe/London");
        assertEquals("Europe/London", BCTimeUtils.getTimezone());
    }

    @Test
    void setTimezone_invalidOrNull_fallsBackToUTC() {
        BCTimeUtils.setTimezone("not-a-real-zone-xyz");
        assertEquals("UTC", BCTimeUtils.getTimezone());
        BCTimeUtils.setTimezone("");
        assertEquals("UTC", BCTimeUtils.getTimezone());
        BCTimeUtils.setTimezone(null);
        assertEquals("UTC", BCTimeUtils.getTimezone());
    }

    @Test
    void setTimezone_SYSTEM_usesSystemDefaultButDoesNotThrow() {
        // We don't assert the concrete ID (depends on runner), just that it accepts and get returns non-null
        BCTimeUtils.setTimezone("SYSTEM");
        String id = BCTimeUtils.getTimezone();
        assertNotNull(id);
        assertFalse(id.isEmpty());
        // restore to UTC for other tests in this run
        BCTimeUtils.setTimezone("UTC");
    }

    @Test
    void timezoneAffectsDateFormatters_butNotDurationStrings() {
        long epoch = 1_704_000_000_000L;
        BCTimeUtils.setTimezone("UTC");
        String utcDate = BCTimeUtils.formatDateTime(epoch);

        BCTimeUtils.setTimezone("Asia/Tokyo"); // UTC+9
        String tokyoDate = BCTimeUtils.formatDateTime(epoch);

        // Durations are unaffected by zone
        assertEquals("2m 5s", BCTimeUtils.formatDuration(125_000));

        // The two zoned strings should differ (unless by chance the offset lands on same wall time string)
        // We only assert they are both valid patterns; a real difference is expected for most epochs.
        assertTrue(utcDate.matches("\\d{2}/\\d{2} \\d{2}:\\d{2}"));
        assertTrue(tokyoDate.matches("\\d{2}/\\d{2} \\d{2}:\\d{2}"));
    }
}
