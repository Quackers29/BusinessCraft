package com.quackers29.businesscraft.town.data;

import net.minecraft.core.BlockPos;
import java.util.UUID;

/**
 * Data transfer object for town leaderboard display.
 * Contains essential town information for client-side leaderboard rendering.
 */
public record TownLeaderboardData(
    UUID townId,
    String name,
    BlockPos position,
    long population,
    long money,
    float happiness
) {
    /**
     * Calculate distance from this town to another position.
     *
     * @param otherPos The position to calculate distance to
     * @return Distance in blocks
     */
    public double distanceTo(BlockPos otherPos) {
        return Math.sqrt(position.distSqr(otherPos));
    }

    /**
     * Format distance for display.
     *
     * @param distance Distance in blocks
     * @return Formatted string (e.g., "123m" or "1.2km")
     */
    public static String formatDistance(double distance) {
        if (distance < 1000) {
            return String.format("%dm", (int) distance);
        } else {
            return String.format("%.1fkm", distance / 1000.0);
        }
    }
}
