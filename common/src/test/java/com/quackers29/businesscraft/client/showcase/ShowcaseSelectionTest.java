package com.quackers29.businesscraft.client.showcase;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowcaseSelectionTest {

    @Test
    void absentProperty_selectsNothing() {
        assertFalse(ShowcaseSelection.enabled(null));
        assertEquals(List.of(), ShowcaseSelection.shotsFor(null));
    }

    @Test
    void allViewsMode_usesRouteRegistryNotShots() {
        assertTrue(ShowcaseSelection.allViewsMode("all-views"));
        assertEquals(List.of(), ShowcaseSelection.shotsFor("all-views"));
        assertTrue(ShowcaseSelection.twoTownsMode("two-towns"));
        assertEquals(List.of(), ShowcaseSelection.shotsFor("two-towns"));
        assertTrue(ShowcaseSelection.testbedMode("testbed"));
        assertEquals(List.of(), ShowcaseSelection.shotsFor("testbed"));
        assertEquals(16, ShowcaseRouteRegistry.allRoutes().size());
    }

    @Test
    void allOrEmpty_selectsEveryShot() {
        assertEquals(ShowcaseSelection.ALL_SHOTS, ShowcaseSelection.shotsFor(""));
        assertEquals(ShowcaseSelection.ALL_SHOTS, ShowcaseSelection.shotsFor("all"));
        assertEquals(ShowcaseSelection.ALL_SHOTS, ShowcaseSelection.shotsFor(" ALL "));
    }

    @Test
    void townScene_isTheFourTabsPlusWarmup() {
        assertEquals(
                List.of("warmup", "town_overview", "town_resources", "town_production", "town_settings"),
                ShowcaseSelection.shotsFor("town"));
        assertFalse(ShowcaseSelection.includes("town", "town_interface"));
    }

    @Test
    void blockScene_isFrontAndBackPlusWarmup() {
        assertEquals(
                List.of("warmup", "town_interface", "town_interface_back", "platform_interface",
                        "platform_interface_ui"),
                ShowcaseSelection.shotsFor("block"));
        assertFalse(ShowcaseSelection.includes("block", "town_overview"));
    }

    @Test
    void exactShot_doesNotPullTheOtherSide() {
        assertEquals(List.of("warmup", "town_interface"), ShowcaseSelection.shotsFor("town_interface"));
        assertEquals(
                List.of("warmup", "town_interface", "town_overview"),
                ShowcaseSelection.shotsFor("town_interface, town_overview"));
    }

    @Test
    void facing_matchesMinecraftAxes() {
        float[] south = ShowcaseSelection.facing(0, 0, 0, 0, 0, 5);
        assertEquals(0f, south[0], 0.01f);
        assertEquals(0f, south[1], 0.01f);

        float[] north = ShowcaseSelection.facing(0, 0, 0, 0, 0, -5);
        assertEquals(180f, Math.abs(north[0]), 0.01f);

        float[] up = ShowcaseSelection.facing(0, 0, 0, 0, 5, 5);
        assertTrue(up[1] < 0f, "looking up is a negative pitch");
    }
}
