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
        TownScreenLayout.Chrome chrome = TownScreenLayout.fitChrome(240, TownScreenLayout.PANEL_HEIGHT);
        assertTrue(chrome.totalHeight(TownScreenLayout.PANEL_HEIGHT) <= 240);
        int top = TownScreenLayout.panelTopY(240, TownScreenLayout.PANEL_HEIGHT, chrome);
        int bottom = top + TownScreenLayout.PANEL_HEIGHT + chrome.bottomGap() + chrome.bottomRowHeight();
        assertTrue(top - chrome.titleMargin() >= 0);
        assertTrue(bottom <= 240);
    }
}
