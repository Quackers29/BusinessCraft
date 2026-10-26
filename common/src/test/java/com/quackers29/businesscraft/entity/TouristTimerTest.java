package com.quackers29.businesscraft.entity;

import com.quackers29.businesscraft.testutil.McBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TouristTimerTest {

    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

    @Test
    void shouldSync_onlyOnOpenOrWhenMovementChanges() {
        assertTrue(TouristTimer.shouldSync(false, true, true));
        assertFalse(TouristTimer.shouldSync(true, true, true));
        assertTrue(TouristTimer.shouldSync(true, true, false));
        assertFalse(TouristTimer.shouldSync(true, false, false));
    }

    @Test
    void clock_advancesAMinuteWithoutANewSlotValue() {
        TouristTimer.Clock clock = new TouristTimer.Clock();
        int journey = 17 * 60 + 31;
        int remaining = 60 * 60 + 51 * 60 + 25;
        String first = clock.text(1_000L, journey, remaining, remaining, true);
        assertEquals("Out for 17m · 1h 51m left while they wait", first);
        String still = clock.text(1_000L + 20L * 20L, journey, remaining, remaining, true);
        assertEquals(first, still);
        String next = clock.text(1_000L + 30L * 20L, journey, remaining, remaining, true);
        assertEquals("Out for 18m · 1h 50m left while they wait", next);
        assertEquals(journey + 30, clock.shownJourney());
    }

    @Test
    void clock_pausesTheCountdownWhileTheyRide() {
        TouristTimer.Clock clock = new TouristTimer.Clock();
        clock.text(0L, 10, 30, 30, false);
        String paused = clock.text(100L * 20L, 10, 30, 30, false);
        assertEquals("Out for 1m · timer paused (<1m left)", paused);
        assertEquals(30, clock.shownRemaining());
    }
}
