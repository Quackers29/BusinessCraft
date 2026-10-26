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
    void withinNameTagRange_respectsFiveBlocks() {
        assertEquals(5.0, TouristDisplayFormat.NAME_TAG_MAX_DISTANCE);
        assertTrue(TouristDisplayFormat.withinNameTagRange(4.9 * 4.9));
        assertTrue(TouristDisplayFormat.withinNameTagRange(5.0 * 5.0));
        assertFalse(TouristDisplayFormat.withinNameTagRange(5.1 * 5.1));
    }

    @Test
    void touristType_roundTripIndex() {
        for (TouristType type : TouristType.values()) {
            assertEquals(type, TouristType.fromIndex(type.index()));
        }
    }
}
