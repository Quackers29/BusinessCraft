package com.quackers29.businesscraft.town.data;

import com.quackers29.businesscraft.config.ConfigLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Models linear and loop rail topologies where open-ticket tourists used to alight at the first stop only.
 */
class TouristRouteDiscoverySimulatorTest {

    private static final UUID A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID C = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID D = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    private double savedBase;
    private double savedBonus;
    private double savedSkipPenalty;
    private int savedMaxSkips;
    private double savedPassWeight;

    @BeforeEach
    void saveConfig() {
        savedBase = ConfigLoader.touristAutoRideOnBaseChance;
        savedBonus = ConfigLoader.touristAutoRideOnDistanceBonusScale;
        savedSkipPenalty = ConfigLoader.touristAutoRideOnSkipPenalty;
        savedMaxSkips = ConfigLoader.touristAutoRideOnMaxSkips;
        savedPassWeight = ConfigLoader.touristAutoPassReachWeight;
        ConfigLoader.touristAutoRideOnBaseChance = 0.45;
        ConfigLoader.touristAutoRideOnDistanceBonusScale = 0.35;
        ConfigLoader.touristAutoRideOnSkipPenalty = 0.12;
        ConfigLoader.touristAutoRideOnMaxSkips = 4;
        ConfigLoader.touristAutoPassReachWeight = 0.35;
        ConfigLoader.touristAutoRideOnDistanceReferenceBlocks = 500.0;
    }

    @AfterEach
    void restoreConfig() {
        ConfigLoader.touristAutoRideOnBaseChance = savedBase;
        ConfigLoader.touristAutoRideOnDistanceBonusScale = savedBonus;
        ConfigLoader.touristAutoRideOnSkipPenalty = savedSkipPenalty;
        ConfigLoader.touristAutoRideOnMaxSkips = savedMaxSkips;
        ConfigLoader.touristAutoPassReachWeight = savedPassWeight;
    }

    @Test
    void linearAbc_firstStopPolicy_onlyAccumulatesScoreForB() {
        Map<UUID, Double> scores = TouristRouteDiscoverySimulator.learnedScoresAfterTrips(
                A, List.of(B, C), 50,
                TouristRouteDiscoverySimulator.ArrivalPolicy.FIRST_REACHABLE_STOP,
                1L, 2.0, 0.5);
        assertTrue(scores.getOrDefault(B, 0.0) > 0);
        assertEquals(0.0, scores.getOrDefault(C, 0.0), 0.001,
                "C is never reached when every Any-Town tourist alights at B");
    }

    @Test
    void loopAbcd_firstStopPolicy_neverLearnsCOrD() {
        Map<UUID, Double> scores = TouristRouteDiscoverySimulator.learnedScoresAfterTrips(
                A, List.of(B, C, D), 80,
                TouristRouteDiscoverySimulator.ArrivalPolicy.FIRST_REACHABLE_STOP,
                2L, 2.0, 0.5);
        assertTrue(scores.getOrDefault(B, 0.0) > 0);
        assertEquals(0.0, scores.getOrDefault(C, 0.0), 0.001);
        assertEquals(0.0, scores.getOrDefault(D, 0.0), 0.001);
    }

    @Test
    void loopAbcd_rideOnPolicy_eventuallyLearnsFartherStops() {
        Map<UUID, Double> scores = TouristRouteDiscoverySimulator.learnedScoresAfterTrips(
                A, List.of(B, C, D), 400,
                TouristRouteDiscoverySimulator.ArrivalPolicy.RIDE_ON,
                99L, 2.0, 0.35);
        assertTrue(scores.getOrDefault(B, 0.0) > 0);
        assertTrue(scores.getOrDefault(C, 0.0) > 0, "ride-on should allow learning C");
        assertTrue(scores.getOrDefault(D, 0.0) > 0, "ride-on should allow learning D");
    }
}
