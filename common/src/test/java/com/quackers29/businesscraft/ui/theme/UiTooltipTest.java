package com.quackers29.businesscraft.ui.theme;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiTooltipTest {

    @Test
    void place_matchesTheScreenInset() {
        assertEquals(new UiTooltip.Box(158, 118), UiTooltip.place(200, 150, 190, 140, 40, 30));
    }

    @Test
    void place_keepsABoxThatFits() {
        assertEquals(new UiTooltip.Box(20, 30), UiTooltip.place(200, 150, 20, 30, 40, 20));
    }
}
