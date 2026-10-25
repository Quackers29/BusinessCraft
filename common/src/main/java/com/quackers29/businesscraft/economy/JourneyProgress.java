package com.quackers29.businesscraft.economy;

import com.quackers29.businesscraft.entity.TouristDisplayFormat;

/**
 * Distance thresholds for tourist journey rank and town notoriety.
 * <p>
 * A threshold array lists the distance required to reach rank 2, 3, … .
 * Rank 1 starts at 0. Both curves are config lists so a pack can retune them
 * without changing the emerald fare ({@code metersPerEmerald}).
 */
public final class JourneyProgress {
    /** Blocks to reach tourist ranks 2–5. Rank 1 is 0. */
    public static final int[] DEFAULT_TOURIST_LEVELS = {500, 1500, 3000, 6000};
    /** Cumulative visitor-blocks to reach town tiers 2–5. Tier 1 is 0. */
    public static final int[] DEFAULT_TOWN_TIERS = {2000, 10000, 40000, 120000};

    private static final String[] TOURIST_NAMES = {"Wanderer", "Traveler", "Voyager", "Veteran", "Renowned"};
    private static final String[] TOWN_NAMES = {"Wayside", "Halt", "Station", "Destination", "Landmark"};

    private JourneyProgress() {
    }

    public static int levelForDistance(double distance, int[] thresholds) {
        int level = 1;
        if (thresholds == null || distance <= 0) {
            return level;
        }
        for (int threshold : thresholds) {
            if (threshold > 0 && distance + 1.0e-6 >= threshold) {
                level++;
            } else if (threshold > 0) {
                break;
            }
        }
        return level;
    }

    public static int maxLevel(int[] thresholds) {
        return 1 + (thresholds == null ? 0 : thresholds.length);
    }

    /** Distance required to be this rank. Rank 1 is 0. */
    public static int distanceToReach(int level, int[] thresholds) {
        if (level <= 1 || thresholds == null || thresholds.length == 0) {
            return 0;
        }
        int index = Math.min(level - 2, thresholds.length - 1);
        return Math.max(0, thresholds[index]);
    }

    /** Distance required to reach the next rank, or null at the last rank. */
    public static Integer distanceToNext(int level, int[] thresholds) {
        if (thresholds == null || thresholds.length == 0 || level >= maxLevel(thresholds) || level < 1) {
            return null;
        }
        int index = level - 1;
        if (index < 0 || index >= thresholds.length) {
            return null;
        }
        return thresholds[index];
    }

    /** 0 at the start of this rank, 1 at the next rank (or at the last rank). */
    public static double rankFraction(double distance, int level, int[] thresholds) {
        Integer next = distanceToNext(level, thresholds);
        if (next == null) {
            return 1.0;
        }
        double start = distanceToReach(level, thresholds);
        double span = Math.max(1.0, next - start);
        return Math.max(0.0, Math.min(1.0, (distance - start) / span));
    }

    public static int townLevel(double cumulativeDistance, int[] thresholds) {
        return levelForDistance(cumulativeDistance, thresholds);
    }

    public static String tierName(int level) {
        return nameAt(TOURIST_NAMES, level);
    }

    public static String townTierName(int level) {
        return nameAt(TOWN_NAMES, level);
    }

    /**
     * Trading-screen XP for the current rank. The bar fills across the distance
     * span of that rank and stays full at the last rank.
     */
    public static int xpForDistance(double distance, int level, int[] thresholds) {
        int safeLevel = Math.max(1, level);
        int cap = vanillaXpCap(safeLevel);
        if (thresholds == null || thresholds.length == 0 || safeLevel >= maxLevel(thresholds)) {
            return cap;
        }
        double start = safeLevel <= 1 ? 0.0 : thresholds[Math.min(safeLevel - 2, thresholds.length - 1)];
        double end = thresholds[Math.min(safeLevel - 1, thresholds.length - 1)];
        double span = Math.max(1.0, end - start);
        double into = Math.max(0.0, distance - start);
        int xp = (int) ((into / span) * cap);
        return Math.max(0, Math.min(cap, xp));
    }

    public static String formatTownNotoriety(double cumulativeDistance, int[] thresholds) {
        int level = townLevel(cumulativeDistance, thresholds);
        return townTierName(level) + " · " + TouristDisplayFormat.formatDistance(Math.max(0.0, cumulativeDistance));
    }

    /** Chat fragment: tourist rank, then the destination town's notoriety. */
    public static String arrivalDetail(double tripDistance, int[] touristThresholds,
            double townTotal, int[] townThresholds) {
        String rank = tierName(levelForDistance(tripDistance, touristThresholds));
        return rank + " | " + formatTownNotoriety(townTotal, townThresholds);
    }

    public static int vanillaXpCap(int level) {
        return switch (Math.max(1, level)) {
            case 1 -> 10;
            case 2 -> 70;
            case 3 -> 150;
            default -> 250;
        };
    }

    private static String nameAt(String[] names, int level) {
        int index = Math.max(0, Math.min(names.length - 1, level - 1));
        return names[index];
    }
}
