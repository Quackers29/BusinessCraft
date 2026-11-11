package com.quackers29.businesscraft.integration.jade;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TouristTripStatusTest {

    @Test
    void from_prefersRidingThenWaiting() {
        assertEquals(TouristTripStatus.RIDING, TouristTripStatus.from(true, true));
        assertEquals(TouristTripStatus.WAITING, TouristTripStatus.from(false, true));
        assertEquals(TouristTripStatus.TRAVELING, TouristTripStatus.from(false, false));
    }

    @Test
    void nbtRoundTrip() {
        for (TouristTripStatus status : TouristTripStatus.values()) {
            assertEquals(status, TouristTripStatus.parseNbt(status.toNbt()));
        }
    }
}
