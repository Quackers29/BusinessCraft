package com.quackers29.businesscraft.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Centralized time formatting utilities for BusinessCraft.
 *
 * <p>All time display formatting should go through this class to ensure consistency.
 * Server-side code should pass server time to these methods; the timezone configuration
 * is used for date/time display formatting only.
 *
 * <p><b>Usage:</b>
 * <pre>
 * // Format remaining time (server-side, pass server time)
 * String remaining = BCTimeUtils.formatTimeRemaining(expiryEpoch, serverNow);
 *
 * // Format a timestamp as date/time
 * String dateTime = BCTimeUtils.formatDateTime(creationTime);
 *
 * // Format a duration in milliseconds
 * String duration = BCTimeUtils.formatDuration(millis);
 *
 * // Check if expired
 * boolean expired = BCTimeUtils.isExpired(expiryEpoch, serverNow);
 * </pre>
 *
 * @since Phase 4 - Time Utility Module
 */
public final class BCTimeUtils {

    /** Default timezone when none is configured */
    private static final String DEFAULT_TIMEZONE = "UTC";

    /** Date/time format pattern */
    private static final String DATETIME_PATTERN = "MM/dd HH:mm";

    /** Full date/time format pattern */
    private static final String FULL_DATETIME_PATTERN = "MMM dd, yyyy HH:mm:ss";

    /** Time only format pattern */
    private static final String TIME_ONLY_PATTERN = "HH:mm:ss";

    /** Configured timezone - loaded from config, defaults to UTC */
    private static ZoneId configuredZone = ZoneId.of(DEFAULT_TIMEZONE);

    /** Cached formatters for performance */
    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATETIME_PATTERN);
    private static DateTimeFormatter fullDateTimeFormatter = DateTimeFormatter.ofPattern(FULL_DATETIME_PATTERN);
    private static DateTimeFormatter timeOnlyFormatter = DateTimeFormatter.ofPattern(TIME_ONLY_PATTERN);

    private BCTimeUtils() {
        // Utility class - no instantiation
    }

    // =========================================================================
    // Configuration
    // =========================================================================

    /**
     * Sets the timezone used for date/time formatting.
     *
     * <p>Valid values:
     * <ul>
     *   <li>"UTC" - Coordinated Universal Time (default)</li>
     *   <li>"SYSTEM" - Use the system's default timezone</li>
     *   <li>Any valid timezone ID (e.g., "America/New_York", "Europe/London")</li>
     * </ul>
     *
     * @param timezone The timezone string to use
     */
    public static void setTimezone(String timezone) {
        if (timezone == null || timezone.isEmpty() || timezone.equalsIgnoreCase("UTC")) {
            configuredZone = ZoneId.of("UTC");
        } else if (timezone.equalsIgnoreCase("SYSTEM")) {
            configuredZone = ZoneId.systemDefault();
        } else {
            try {
                configuredZone = ZoneId.of(timezone);
            } catch (Exception e) {
                // Invalid timezone, fall back to UTC
                configuredZone = ZoneId.of("UTC");
            }
        }
    }

    /**
     * Gets the currently configured timezone ID.
     *
     * @return The timezone ID string
     */
    public static String getTimezone() {
        return configuredZone.getId();
    }

    // =========================================================================
    // Time Remaining / Duration Formatting
    // =========================================================================

    /**
     * Formats the time remaining until an expiry time as a human-readable string.
     *
     * <p>Examples:
     * <ul>
     *   <li>"2d 5h" (for 2 days, 5 hours)</li>
     *   <li>"3h 15m" (for 3 hours, 15 minutes)</li>
     *   <li>"5m 30s" (for 5 minutes, 30 seconds)</li>
     *   <li>"45s" (for 45 seconds)</li>
     *   <li>"Expired" (if already past expiry)</li>
     * </ul>
     *
     * @param expiryEpoch The expiry time in epoch milliseconds
     * @param serverNow The current server time in epoch milliseconds
     * @return A formatted time remaining string
     */
    public static String formatTimeRemaining(long expiryEpoch, long serverNow) {
        long remaining = expiryEpoch - serverNow;

        if (remaining <= 0) {
            return "Expired";
        }

        return formatDuration(remaining);
    }

    /**
     * Formats a duration in milliseconds as a human-readable string.
     *
     * <p>Examples:
     * <ul>
     *   <li>"2d 5h" (for 2 days, 5 hours)</li>
     *   <li>"3h 15m" (for 3 hours, 15 minutes)</li>
     *   <li>"5m 30s" (for 5 minutes, 30 seconds)</li>
     *   <li>"45s" (for 45 seconds)</li>
     *   <li>"0s" (for 0 or negative)</li>
     * </ul>
     *
     * @param millis The duration in milliseconds
     * @return A formatted duration string
     */
    public static String formatDuration(long millis) {
        if (millis <= 0) {
            return "0s";
        }

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }

    /**
     * Formats a "time ago" string for display.
     *
     * <p>Examples:
     * <ul>
     *   <li>"Just now" (for less than 1 minute)</li>
     *   <li>"5m ago" (for 5 minutes ago)</li>
     *   <li>"2h ago" (for 2 hours ago)</li>
     *   <li>"3d ago" (for 3 days ago)</li>
     * </ul>
     *
     * @param timestamp The timestamp in epoch milliseconds
     * @param now The current time in epoch milliseconds
     * @return A formatted "time ago" string
     */
    public static String formatTimeAgo(long timestamp, long now) {
        long diff = now - timestamp;

        if (diff < 0) {
            return "In the future";
        }

        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);

        if (days > 0) return days + "d ago";
        if (hours > 0) return hours + "h ago";
        if (minutes > 0) return minutes + "m ago";
        return "Just now";
    }

    // =========================================================================
    // Date/Time Formatting
    // =========================================================================

    /**
     * Formats an epoch timestamp as a short date/time string.
     *
     * <p>Format: "MM/dd HH:mm" (e.g., "01/14 15:30")
     *
     * @param epochMillis The timestamp in epoch milliseconds
     * @return A formatted date/time string in the configured timezone
     */
    public static String formatDateTime(long epochMillis) {
        ZonedDateTime zdt = Instant.ofEpochMilli(epochMillis).atZone(configuredZone);
        return zdt.format(dateTimeFormatter);
    }

    /**
     * Formats an epoch timestamp as a full date/time string.
     *
     * <p>Format: "MMM dd, yyyy HH:mm:ss" (e.g., "Jan 14, 2026 15:30:45")
     *
     * @param epochMillis The timestamp in epoch milliseconds
     * @return A formatted full date/time string in the configured timezone
     */
    public static String formatFullDateTime(long epochMillis) {
        ZonedDateTime zdt = Instant.ofEpochMilli(epochMillis).atZone(configuredZone);
        return zdt.format(fullDateTimeFormatter);
    }

    /**
     * Formats an epoch timestamp as a time-only string.
     *
     * <p>Format: "HH:mm:ss" (e.g., "15:30:45")
     *
     * @param epochMillis The timestamp in epoch milliseconds
     * @return A formatted time string in the configured timezone
     */
    public static String formatTimeOnly(long epochMillis) {
        ZonedDateTime zdt = Instant.ofEpochMilli(epochMillis).atZone(configuredZone);
        return zdt.format(timeOnlyFormatter);
    }

    // =========================================================================
    // Utility Methods
    // =========================================================================

    /**
     * Checks if a given expiry time has passed.
     *
     * @param expiryEpoch The expiry time in epoch milliseconds
     * @param serverNow The current server time in epoch milliseconds
     * @return true if the expiry time has passed, false otherwise
     */
    public static boolean isExpired(long expiryEpoch, long serverNow) {
        return serverNow > expiryEpoch;
    }

    /**
     * Calculates the remaining time until expiry.
     *
     * @param expiryEpoch The expiry time in epoch milliseconds
     * @param serverNow The current server time in epoch milliseconds
     * @return The remaining time in milliseconds, or 0 if already expired
     */
    public static long getTimeRemaining(long expiryEpoch, long serverNow) {
        long remaining = expiryEpoch - serverNow;
        return remaining > 0 ? remaining : 0;
    }
}
