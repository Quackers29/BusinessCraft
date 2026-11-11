package com.quackers29.businesscraft.town.data;

import com.quackers29.businesscraft.config.ConfigLoader;

import java.util.UUID;

import net.minecraft.util.RandomSource;

/**
 * Whether an open-ticket tourist should get off at a platform stop or ride on toward a farther town.
 * Uses the same "stationary at platform" moment as arrival detection ({@code VisitorProcessingHelper}).
 */
public final class TouristAlightDecision {

    private TouristAlightDecision() {
    }

    /**
     * @return {@code true} if the tourist should alight (process arrival); {@code false} to stay on the train.
     */
    public static boolean shouldAlightAtOpenStop(UUID stopTownId, UUID originTownId, double traveledBlocks,
            int rideOnSkipsUsed, RandomSource random) {
        if (stopTownId == null || stopTownId.equals(originTownId)) {
            return false;
        }
        if (rideOnSkipsUsed >= ConfigLoader.touristAutoRideOnMaxSkips) {
            return true;
        }
        double stayProbability = rideOnStayProbability(traveledBlocks, rideOnSkipsUsed);
        return random.nextDouble() >= stayProbability;
    }

    /**
     * Probability the tourist stays on at this stop (does not alight).
     */
    public static double rideOnStayProbability(double traveledBlocks, int rideOnSkipsUsed) {
        double base = ConfigLoader.touristAutoRideOnBaseChance;
        double ref = Math.max(1.0, ConfigLoader.touristAutoRideOnDistanceReferenceBlocks);
        double distBonus = ConfigLoader.touristAutoRideOnDistanceBonusScale * (traveledBlocks / ref);
        double skipPenalty = ConfigLoader.touristAutoRideOnSkipPenalty * rideOnSkipsUsed;
        double raw = base + distBonus - skipPenalty;
        return Math.min(ConfigLoader.touristAutoRideOnMaxStayChance, Math.max(0.0, raw));
    }
}
