package com.quackers29.businesscraft.client.showcase;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Canonical list of BusinessCraft UI routes for the all-views showcase harness.
 */
public final class ShowcaseRouteRegistry {
    private static final List<ShowcaseRoute> ROUTES = List.of(
            townTab("town/overview", "overview"),
            townTab("town/resources", "resources"),
            townTab("town/production", "production"),
            townTab("town/settings", "settings"),
            new ShowcaseRoute("town/map", List.of(
                    "open-town",
                    "wait-for-screen:TownInterfaceScreen:120",
                    "set-tab:overview",
                    "wait-tab:overview",
                    "press-button:id:" + ShowcaseButtonIds.MAP_VIEW,
                    "wait-for-screen:TownMapModal:120")),
            new ShowcaseRoute("town/ranking", List.of(
                    "open-town",
                    "wait-for-screen:TownInterfaceScreen:120",
                    "set-tab:overview",
                    "wait-tab:overview",
                    "press-button:id:" + ShowcaseButtonIds.RANKING,
                    "wait-for-screen:TownLeaderboardScreen:120")),
            new ShowcaseRoute("town/trade", List.of(
                    "open-town",
                    "wait-for-screen:TownInterfaceScreen:120",
                    "set-tab:resources",
                    "wait-tab:resources",
                    "press-button:id:" + ShowcaseButtonIds.TRADE,
                    "wait-for-screen:BCModalInventoryScreen:120")),
            new ShowcaseRoute("town/payment", List.of(
                    "open-town",
                    "wait-for-screen:TownInterfaceScreen:120",
                    "set-tab:resources",
                    "wait-tab:resources",
                    "press-button:id:" + ShowcaseButtonIds.PAYMENT,
                    "wait-for-screen:PaymentBoardScreen:160")),
            new ShowcaseRoute("town/contracts", List.of(
                    "open-town",
                    "wait-for-screen:TownInterfaceScreen:120",
                    "set-tab:resources",
                    "wait-tab:resources",
                    "press-button:id:" + ShowcaseButtonIds.CONTRACTS,
                    "wait-for-screen:ContractBoardScreen:160")),
            new ShowcaseRoute("town/edit-name", List.of(
                    "open-town",
                    "wait-for-screen:TownInterfaceScreen:120",
                    "set-tab:settings",
                    "wait-tab:settings",
                    "press-button:id:" + ShowcaseButtonIds.EDIT_NAME,
                    "wait-for-popup:80")));

    private ShowcaseRouteRegistry() {
    }

    public static List<ShowcaseRoute> allRoutes() {
        return ROUTES;
    }

    /**
     * Screen simple class names that at least one route waits for (including town tabs).
     */
    public static Set<String> coveredScreenClassNames() {
        Set<String> covered = new LinkedHashSet<>();
        covered.add("TownInterfaceScreen");
        for (ShowcaseRoute route : ROUTES) {
            for (String step : route.steps()) {
                if (step.startsWith("wait-for-screen:")) {
                    String[] parts = step.split(":");
                    if (parts.length >= 2) {
                        covered.add(parts[1]);
                    }
                }
                if (step.startsWith("wait-for-popup:")) {
                    covered.add("BCPopupScreen");
                }
            }
        }
        return covered;
    }

    private static ShowcaseRoute townTab(String routeName, String tabId) {
        List<String> steps = new ArrayList<>();
        steps.add("open-town");
        steps.add("wait-for-screen:TownInterfaceScreen:120");
        steps.add("set-tab:" + tabId);
        steps.add("wait-tab:" + tabId);
        return new ShowcaseRoute(routeName, List.copyOf(steps));
    }
}
