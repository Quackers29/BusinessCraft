package com.quackers29.businesscraft.town.data;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.platform.PlatformLearnedRoutes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.util.RandomSource;

/**
 * Simplified model of repeated Any-Town trips along a fixed stop order from one origin platform.
 * Mirrors open-ticket arrival: each stop is a chance to alight or ride on (no payment simulation).
 */
public final class TouristRouteDiscoverySimulator {

    public enum ArrivalPolicy {
        /** Pre-ride-on behaviour: always alight at the first reachable stop after origin. */
        FIRST_REACHABLE_STOP,
        /** Config-driven ride-on rolls at each stop until the tourist alights. */
        RIDE_ON
    }

    private TouristRouteDiscoverySimulator() {
    }

    public static Map<UUID, Double> learnedScoresAfterTrips(UUID originTownId, List<UUID> stopsAfterOrigin,
            int tripCount, ArrivalPolicy policy, long seed, double arrivalWeight, double passWeight) {
        PlatformLearnedRoutes routes = new PlatformLearnedRoutes();
        RandomSource random = RandomSource.create(seed);
        long tick = 0;
        double blockStep = Math.max(1.0, ConfigLoader.touristAutoRideOnDistanceReferenceBlocks);

        for (int trip = 0; trip < tripCount; trip++) {
            tick += 100;
            double traveled = 0;
            int skips = 0;
            for (UUID stop : stopsAfterOrigin) {
                if (stop.equals(originTownId)) {
                    continue;
                }
                traveled += blockStep;
                if (policy == ArrivalPolicy.FIRST_REACHABLE_STOP) {
                    routes.recordSuccess(stop, tick, arrivalWeight, 100, 12, 0.01);
                    break;
                }
                if (TouristAlightDecision.shouldAlightAtOpenStop(stop, originTownId, traveled, skips, random)) {
                    routes.recordSuccess(stop, tick, arrivalWeight, 100, 12, 0.01);
                    break;
                }
                routes.recordSuccess(stop, tick, passWeight, 0, 12, 0.01);
                skips++;
            }
        }

        Map<UUID, Double> scores = new HashMap<>();
        for (Map.Entry<UUID, PlatformLearnedRoutes.Entry> e : routes.snapshotEntries().entrySet()) {
            scores.put(e.getKey(), e.getValue().score);
        }
        return scores;
    }
}
