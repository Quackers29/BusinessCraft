package com.quackers29.businesscraft.client.showcase;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Canonical list of BusinessCraft UI routes for the all-views showcase harness.
 */
public final class ShowcaseRouteRegistry {
    private static final List<ShowcaseCoverageExclusion> EXCLUSIONS = List.of(
            new ShowcaseCoverageExclusion("TradeScreen",
                    "Legacy MenuScreens handler; trade UI opens only via BCModalInventoryScreen (route town/trade)."),
            new ShowcaseCoverageExclusion("StorageScreen",
                    "Legacy MenuScreens handler; communal/personal storage opens only via BCModalInventoryScreen."));

    private static final List<ShowcaseRoute> ROUTES = buildRoutes();

    private ShowcaseRouteRegistry() {
    }

    public static List<ShowcaseRoute> allRoutes() {
        return ROUTES;
    }

    /**
     * Before (vanilla merchant window) and after (tourist screen) shots, plus a
     * scrolled trade list. The scrolled route is not part of {@link #allRoutes()}
     * so the full UI sweep stays the same length.
     */
    public static List<ShowcaseRoute> touristScreenRoutes() {
        List<ShowcaseRoute> routes = new ArrayList<>();
        for (ShowcaseRoute route : ROUTES) {
            if (route.name().startsWith("tourist/")) {
                routes.add(route);
            }
        }
        routes.add(new ShowcaseRoute("tourist/scrolled", List.of(
                "setup:world",
                "open-tourist:voyager",
                "wait-for-screen:TouristScreen:160",
                "scroll-tourist:4",
                "hover-tourist-trade:5")));
        return List.copyOf(routes);
    }

    public static List<ShowcaseCoverageExclusion> coverageExclusions() {
        return EXCLUSIONS;
    }

    public static Map<String, String> exclusionReasonsByClass() {
        return EXCLUSIONS.stream()
                .collect(java.util.stream.Collectors.toMap(
                        ShowcaseCoverageExclusion::screenClassName,
                        ShowcaseCoverageExclusion::reason,
                        (a, b) -> a,
                        java.util.LinkedHashMap::new));
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

    private static List<ShowcaseRoute> buildRoutes() {
        List<ShowcaseRoute> routes = new ArrayList<>();
        routes.add(townTab("town/overview", "overview"));
        routes.add(townTab("town/resources", "resources"));
        routes.add(townTab("town/production", "production"));
        routes.add(townTab("town/settings", "settings"));
        routes.add(new ShowcaseRoute("town/map", List.of(
                "setup:world",
                "open-town",
                "wait-for-screen:TownInterfaceScreen:120",
                "set-tab:overview",
                "wait-tab:overview",
                "press-button:id:" + ShowcaseButtonIds.MAP_VIEW,
                "wait-for-screen:TownMapModal:120")));
        routes.add(new ShowcaseRoute("town/ranking", List.of(
                "setup:world",
                "open-town",
                "wait-for-screen:TownInterfaceScreen:120",
                "set-tab:overview",
                "wait-tab:overview",
                "press-button:id:" + ShowcaseButtonIds.RANKING,
                "wait-for-screen:TownLeaderboardScreen:120")));
        routes.add(new ShowcaseRoute("town/town-detail", List.of(
                "setup:world",
                "open-town",
                "wait-for-screen:TownInterfaceScreen:120",
                "set-tab:overview",
                "wait-tab:overview",
                "press-button:id:" + ShowcaseButtonIds.RANKING,
                "wait-for-screen:TownLeaderboardScreen:120",
                "open-leaderboard-detail:0",
                "wait-for-screen:TownDetailScreen:80")));
        routes.add(new ShowcaseRoute("town/trade", List.of(
                "setup:world",
                "open-town",
                "wait-for-screen:TownInterfaceScreen:120",
                "set-tab:resources",
                "wait-tab:resources",
                "press-button:id:" + ShowcaseButtonIds.TRADE,
                "wait-for-screen:BCModalInventoryScreen:120")));
        routes.add(new ShowcaseRoute("town/payment", List.of(
                "setup:world",
                "open-town",
                "wait-for-screen:TownInterfaceScreen:120",
                "set-tab:resources",
                "wait-tab:resources",
                "press-button:id:" + ShowcaseButtonIds.PAYMENT,
                "wait-for-screen:PaymentBoardScreen:160")));
        routes.add(new ShowcaseRoute("town/contracts", List.of(
                "setup:world",
                "open-town",
                "wait-for-screen:TownInterfaceScreen:120",
                "set-tab:resources",
                "wait-tab:resources",
                "press-button:id:" + ShowcaseButtonIds.CONTRACTS,
                "wait-for-screen:ContractBoardScreen:160")));
        routes.add(new ShowcaseRoute("town/contract-detail", List.of(
                "setup:world",
                "open-town",
                "wait-for-screen:TownInterfaceScreen:120",
                "set-tab:resources",
                "wait-tab:resources",
                "press-button:id:" + ShowcaseButtonIds.CONTRACTS,
                "wait-for-screen:ContractBoardScreen:160",
                "wait-for-contracts:200",
                "press-button:id:" + ShowcaseButtonIds.CONTRACT_VIEW,
                "wait-for-screen:ContractDetailScreen:120")));
        routes.add(new ShowcaseRoute("town/edit-name", List.of(
                "setup:world",
                "open-town",
                "wait-for-screen:TownInterfaceScreen:120",
                "set-tab:settings",
                "wait-tab:settings",
                "press-button:id:" + ShowcaseButtonIds.EDIT_NAME,
                "wait-for-popup:80")));
        routes.add(new ShowcaseRoute("town/visitor-history", List.of(
                "setup:world",
                "open-town",
                "wait-for-screen:TownInterfaceScreen:120",
                "set-tab:settings",
                "wait-tab:settings",
                "press-button:id:" + ShowcaseButtonIds.VIEW_VISITORS,
                "wait-for-screen:BCModalGridScreen:120")));
        routes.add(new ShowcaseRoute("platform/management", List.of(
                "setup:world",
                "open-town",
                "wait-for-screen:TownInterfaceScreen:120",
                "set-tab:settings",
                "wait-tab:settings",
                "press-button:id:" + ShowcaseButtonIds.SETTINGS_SET_PLATFORMS,
                "wait-for-screen:PlatformManagementScreenV2:160",
                "wait-for-platforms:120")));
        routes.add(new ShowcaseRoute("platform/destinations", List.of(
                "setup:world",
                "open-town",
                "wait-for-screen:TownInterfaceScreen:120",
                "set-tab:settings",
                "wait-tab:settings",
                "press-button:id:" + ShowcaseButtonIds.SETTINGS_SET_PLATFORMS,
                "wait-for-screen:PlatformManagementScreenV2:160",
                "wait-for-platforms:120",
                "press-button:id:" + ShowcaseButtonIds.platformDestinations(0),
                "wait-for-screen:DestinationsScreenV2:160")));
        routes.add(tourist("tourist/before", "legacy"));
        routes.add(tourist("tourist/wanderer", "wanderer"));
        routes.add(tourist("tourist/voyager", "voyager"));
        routes.add(new ShowcaseRoute("tourist/ticket", List.of(
                "setup:world",
                "open-tourist:wanderer",
                "wait-for-screen:TouristScreen:160",
                "hover-tourist-trade:0")));
        return List.copyOf(routes);
    }

    private static ShowcaseRoute tourist(String routeName, String which) {
        String screen = "legacy".equals(which) ? "MerchantScreen" : "TouristScreen";
        return new ShowcaseRoute(routeName, List.of(
                "setup:world",
                "open-tourist:" + which,
                "wait-for-screen:" + screen + ":160"));
    }

    private static ShowcaseRoute townTab(String routeName, String tabId) {
        List<String> steps = new ArrayList<>();
        steps.add("setup:world");
        steps.add("open-town");
        steps.add("wait-for-screen:TownInterfaceScreen:120");
        steps.add("set-tab:" + tabId);
        steps.add("wait-tab:" + tabId);
        return new ShowcaseRoute(routeName, List.copyOf(steps));
    }
}
