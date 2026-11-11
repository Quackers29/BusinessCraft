package com.quackers29.businesscraft.town.data;

import com.quackers29.businesscraft.config.ConfigLoader;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TouristAlightDecisionTest {

    private static final UUID ORIGIN = UUID.randomUUID();
    private static final UUID STOP = UUID.randomUUID();

    private int savedMaxSkips;

    @BeforeEach
    void setUp() {
        savedMaxSkips = ConfigLoader.touristAutoRideOnMaxSkips;
        ConfigLoader.touristAutoRideOnBaseChance = 0.0;
        ConfigLoader.touristAutoRideOnDistanceBonusScale = 0.0;
        ConfigLoader.touristAutoRideOnSkipPenalty = 0.0;
        ConfigLoader.touristAutoRideOnMaxStayChance = 1.0;
    }

    @AfterEach
    void tearDown() {
        ConfigLoader.touristAutoRideOnMaxSkips = savedMaxSkips;
    }

    @Test
    void maxSkipsForcesAlightEvenWithHighStayChance() {
        ConfigLoader.touristAutoRideOnMaxSkips = 2;
        ConfigLoader.touristAutoRideOnBaseChance = 1.0;
        ConfigLoader.touristAutoRideOnMaxStayChance = 1.0;
        assertTrue(TouristAlightDecision.shouldAlightAtOpenStop(STOP, ORIGIN, 100, 2, RandomSource.create(0)));
        assertFalse(TouristAlightDecision.shouldAlightAtOpenStop(STOP, ORIGIN, 100, 1, RandomSource.create(0)));
    }

    @Test
    void zeroStayChanceAlwaysAlights() {
        ConfigLoader.touristAutoRideOnMaxSkips = 10;
        assertTrue(TouristAlightDecision.shouldAlightAtOpenStop(
                STOP, ORIGIN, 0, 0, RandomSource.create(1)));
    }
}
