package com.quackers29.businesscraft.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TouristDisplayFormatTest {

    @Test
    void formatDistance_metersBelow1000() {
        assertEquals("42 m", TouristDisplayFormat.formatDistance(41.6));
    }

    @Test
    void formatDistance_kilometersAt1000() {
        assertEquals("1.2 km", TouristDisplayFormat.formatDistance(1200));
    }

    @Test
    void sanitize_truncatesLongNames() {
        String longName = "abcdefghijklmnopqrstuvwxyz";
        assertTrue(TouristDisplayFormat.sanitize(longName, "?").endsWith("..."));
    }

    @Test
    void withinNameTagRange_respects48Blocks() {
        assertTrue(TouristDisplayFormat.withinNameTagRange(47 * 47));
        assertFalse(TouristDisplayFormat.withinNameTagRange(49 * 49));
    }

    @Test
    void touristType_roundTripIndex() {
        for (TouristType type : TouristType.values()) {
            assertEquals(type, TouristType.fromIndex(type.index()));
        }
    }
}
