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
        assertTrue(ShowcaseSelection.touristsMode("tourists"));
        assertEquals(List.of(), ShowcaseSelection.shotsFor("tourists"));
        assertTrue(ShowcaseSelection.touristScreenMode("tourist-screen"));
        assertEquals(List.of(), ShowcaseSelection.shotsFor("tourist-screen"));
        assertTrue(ShowcaseSelection.productionTooltipMode("production-tooltip"));
        assertEquals(List.of(), ShowcaseSelection.shotsFor("production-tooltip"));
        assertEquals(1, ShowcaseRouteRegistry.productionTooltipRoutes().size());
        assertTrue(ShowcaseRouteRegistry.allRoutes().stream()
                .noneMatch(route -> "town/production-tooltip".equals(route.name())));
        assertTrue(ShowcaseSelection.jadeMode("jade"));
        assertEquals(List.of(), ShowcaseSelection.shotsFor("jade"));
        assertEquals(4, ShowcaseJade.EXPECTED_SHOTS.size());
        assertEquals(19, ShowcaseRouteRegistry.allRoutes().size());
        assertEquals(5, ShowcaseRouteRegistry.touristScreenRoutes().size());
        assertTrue(ShowcaseRouteRegistry.touristScreenRoutes().stream()
                .anyMatch(route -> "tourist/scrolled".equals(route.name())));
        assertTrue(ShowcaseRouteRegistry.allRoutes().stream()
                .noneMatch(route -> "tourist/scrolled".equals(route.name())));
    }

    @Test
    void allOrEmpty_selectsEveryShot() {
        assertEquals(ShowcaseSelection.ALL_SHOTS, ShowcaseSelection.shotsFor(""));
        assertEquals(ShowcaseSelection.ALL_SHOTS, ShowcaseSelection.shotsFor("all"));
        assertEquals(ShowcaseSelection.ALL_SHOTS, ShowcaseSelection.shotsFor(" ALL "));
    }

    @Test
    void writtenShots_omitWarmup() {
        assertEquals(ShowcaseSelection.ALL_SHOTS.size() - 1, ShowcaseSelection.writtenShots("all").size());
        assertEquals(19, ShowcaseSelection.writtenShots("all").size());
        assertFalse(ShowcaseSelection.writtenShots("all").contains("warmup"));
        assertEquals(List.of("town_interface", "town_interface_back"), ShowcaseSelection.writtenShots("block"));
        assertEquals(4, ShowcaseSelection.writtenShots("town").size());
        assertEquals(6, ShowcaseSelection.writtenShots("platform").size());
        assertEquals(6, ShowcaseSelection.writtenShots("kiosk").size());
        assertEquals(2, ShowcaseSelection.writtenShots("lamp").size());
        assertEquals(List.of(), ShowcaseSelection.writtenShots("all-views"));
        assertEquals(List.of(), ShowcaseSelection.writtenShots(null));
    }

    @Test
    void routes_recordTheScreenTheyWaitFor() {
        assertEquals("TownInterfaceScreen", route("town/overview").expectedScreen());
        assertEquals("TownInterfaceScreen", route("town/edit-name").expectedScreen());
        assertEquals("TownMapModal", route("town/map").expectedScreen());
        assertEquals("ContractDetailScreen", route("town/contract-detail").expectedScreen());
        assertEquals("MerchantScreen", route("tourist/before").expectedScreen());
        assertEquals("TouristScreen", route("tourist/voyager").expectedScreen());
        assertEquals("TouristScreen", touristRoute("tourist/scrolled").expectedScreen());
    }

    @Test
    void showcaseTownNames_arePinnedInOrder() {
        System.setProperty("businesscraft.showcase", "all");
        try {
            ShowcaseTownNames.reset();
            assertTrue(ShowcaseTownNames.isActive());
            assertEquals(ShowcaseTownNames.PRIMARY, ShowcaseTownNames.next());
            assertEquals(ShowcaseTownNames.SECONDARY, ShowcaseTownNames.next());
            assertEquals("Brookside-3", ShowcaseTownNames.next());
        } finally {
            ShowcaseTownNames.reset();
            System.clearProperty("businesscraft.showcase");
        }
        assertFalse(ShowcaseTownNames.isActive());
    }

    private static ShowcaseRoute route(String name) {
        return ShowcaseRouteRegistry.allRoutes().stream()
                .filter(candidate -> candidate.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static ShowcaseRoute touristRoute(String name) {
        return ShowcaseRouteRegistry.touristScreenRoutes().stream()
                .filter(candidate -> candidate.name().equals(name))
                .findFirst()
                .orElseThrow();
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
                List.of("warmup", "town_interface", "town_interface_back"),
                ShowcaseSelection.shotsFor("block"));
        assertFalse(ShowcaseSelection.includes("block", "town_overview"));
    }

    @Test
    void lampScene_isTheLitAndDarkSign() {
        assertEquals(
                List.of("warmup", "platform_lamp_on", "platform_lamp_off"),
                ShowcaseSelection.shotsFor("lamp"));
    }

    @Test
    void kioskScene_showsTheMachineAndItsPath() {
        assertEquals(
                List.of("warmup", "platform_front", "platform_side", "platform_rail", "platform_hand",
                        "platform_path", "town_paths"),
                ShowcaseSelection.shotsFor("kiosk"));
    }

    @Test
    void platformScene_isTownCentreBlockAndBothScreens() {
        assertEquals(
                List.of("warmup", "town_overview", "platform_interface",
                        "platform_interface_mode_any", "platform_interface_mode_auto",
                        "platform_interface_mode_specific", "platform_overview"),
                ShowcaseSelection.shotsFor("platform"));
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
