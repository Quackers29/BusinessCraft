package com.quackers29.businesscraft.ui.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownScreenLayoutTest {

    @Test
    void effectiveGuiScale_capsAtVanillaMinimum() {
        assertEquals(2, TownScreenLayout.effectiveGuiScale(854, 480, 4));
        assertEquals(3, TownScreenLayout.effectiveGuiScale(1280, 720, 4));
        assertEquals(2, TownScreenLayout.effectiveGuiScale(1920, 1080, 2));
    }

    @Test
    void fitChrome_fitsMinimum240ScaledHeight() {
        int scaledHeight = 240;
        TownScreenLayout.Chrome chrome = TownScreenLayout.fitChrome(scaledHeight, TownScreenLayout.PANEL_HEIGHT);
        assertTrue(chrome.totalHeight(TownScreenLayout.PANEL_HEIGHT) <= scaledHeight - (TownScreenLayout.MIN_EDGE_MARGIN * 2));
        assertTrue(TownScreenLayout.topMargin(scaledHeight, TownScreenLayout.PANEL_HEIGHT, chrome)
                >= TownScreenLayout.MIN_EDGE_MARGIN);
        assertTrue(TownScreenLayout.bottomMargin(scaledHeight, TownScreenLayout.PANEL_HEIGHT, chrome)
                >= TownScreenLayout.MIN_EDGE_MARGIN);
    }
}
