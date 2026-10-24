package com.quackers29.businesscraft.economy;

import com.quackers29.businesscraft.config.ConfigLoader;

/**
 * Config-driven tourist fare curve (blocks traveled → emeralds per tourist).
 * <p>
 * Batch payout applies {@code max(1, emeraldsPerTourist × touristCount)} with
 * truncation toward zero on the combined value (legacy formula shape).
 */
public final class TouristPaymentCalculator {

    private TouristPaymentCalculator() {
    }

    /**
     * Emeralds for one arrival batch after {@code minPayoutDistance} has been satisfied.
     *
     * @param averageDistance mean clamped travel distance across the batch (blocks)
     * @param touristCount    tourists in the batch
     * @return emeralds to place on the payment board (0 only when distance is not positive)
     */
    public static int computeBatchPayment(double averageDistance, int touristCount) {
        if (averageDistance <= 0) {
            return 0;
        }
        double emeraldsPerTourist = emeraldsPerTouristUnrounded(averageDistance);
        double raw = emeraldsPerTourist * touristCount;
        return (int) Math.max(1, raw);
    }

    /**
     * Unrounded emeralds earned per tourist at the given travel distance.
     */
    public static double emeraldsPerTouristUnrounded(double distanceBlocks) {
        if (distanceBlocks <= 0) {
            return 0;
        }
        double exponent = ConfigLoader.payoutCurveExponent;
        if (exponent == 1.0) {
            return distanceBlocks / (double) ConfigLoader.metersPerEmerald;
        }
        double reference = ConfigLoader.payoutReferenceBlocks;
        if (reference <= 0) {
            return 0;
        }
        return Math.pow(distanceBlocks / reference, exponent)
                * ConfigLoader.payoutEmeraldsPerTouristAtReference;
    }
}
