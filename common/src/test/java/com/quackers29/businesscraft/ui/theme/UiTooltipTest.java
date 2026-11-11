package com.quackers29.businesscraft.ui.theme;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiTooltipTest {

    @Test
    void place_matchesTheScreenInset() {
        UiTooltip.Box box = UiTooltip.place(200, 150, 190, 140, 40, 30);
        assertEquals(new UiTooltip.Box(158, 118), box);
        assertEquals(UiTooltip.placeIn(2, 2, 198, 148, 190, 140, 40, 30), box);
    }

    @Test
    void place_keepsABoxThatFits() {
        assertEquals(new UiTooltip.Box(20, 30), UiTooltip.place(200, 150, 20, 30, 40, 20));
    }

    @Test
    void placeIn_staysInsideThePanel() {
        UiTooltip.Box box = UiTooltip.placeIn(100, 40, 180, 90, 170, 20, 50, 30);
        assertEquals(new UiTooltip.Box(130, 40), box);
    }

    @Test
    void placeIn_pinsAnOversizedBoxToTheTopLeft() {
        assertEquals(new UiTooltip.Box(10, 8), UiTooltip.placeIn(10, 8, 40, 20, 0, 0, 80, 40));
    }
}
