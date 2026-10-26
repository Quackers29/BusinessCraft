package com.quackers29.businesscraft.ui.screens;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TouristScreenLayoutTest {

    @Test
    void wideScreen_fitsEightRoomyRowsWithoutScrolling() {
        TouristScreenLayout layout = TouristScreenLayout.choose(960, 540, 8);
        assertEquals(0, layout.maxScroll);
        assertTrue(layout.roomy);
        assertTrue(layout.height <= 540);
        assertTrue(layout.width <= 960);
    }

    @Test
    void smallWindow_guiScale2_scrollsAndStaysInside() {
        TouristScreenLayout layout = TouristScreenLayout.choose(427, 240, 8);
        assertTrue(layout.maxScroll > 0);
        assertTrue(layout.listHeight < layout.contentHeight);
        assertTrue(layout.height <= 240);
        assertTrue(layout.width <= 427);
        assertTrue(layout.rowsTop + layout.listHeight <= layout.height);
    }

    @Test
    void tinyWindow_guiScale3_keepsAShortListOnScreen() {
        TouristScreenLayout layout = TouristScreenLayout.choose(284, 160, 8);
        assertTrue(layout.maxScroll > 0);
        assertTrue(layout.listHeight >= layout.rowHeight);
        assertTrue(layout.listHeight < layout.rowHeight * 4);
        assertTrue(layout.height <= 160);
        assertTrue(layout.width <= 284);
    }
}
