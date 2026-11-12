package com.quackers29.businesscraft.client.showcase;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowcaseRouteSelectionTest {
    private static List<String> names(List<ShowcaseRoute> routes) {
        return routes.stream().map(ShowcaseRoute::name).toList();
    }

    @Test
    void blankSpecSelectsEveryRoute() {
        List<ShowcaseRoute> all = ShowcaseRouteRegistry.allRoutes();
        assertEquals(all, ShowcaseRouteSelection.select(all, null));
        assertEquals(all, ShowcaseRouteSelection.select(all, "  "));
    }

    @Test
    void exactNamesAndGroupsSelectInRegistryOrder() {
        List<ShowcaseRoute> all = ShowcaseRouteRegistry.allRoutes();
        assertEquals(List.of("town/map"), names(ShowcaseRouteSelection.select(all, "town/map")));
        assertEquals(List.of("town/map", "platform/management", "platform/interface"),
                names(ShowcaseRouteSelection.select(all, "platform,town/map")));
        List<String> tourist = names(ShowcaseRouteSelection.select(all, "Tourist"));
        assertTrue(tourist.size() >= 3 && tourist.stream().allMatch(n -> n.startsWith("tourist/")));
    }

    @Test
    void aGroupNeedsTheSlashBoundary() {
        List<ShowcaseRoute> all = ShowcaseRouteRegistry.allRoutes();
        assertThrows(IllegalArgumentException.class, () -> ShowcaseRouteSelection.select(all, "plat"));
        assertThrows(IllegalArgumentException.class, () -> ShowcaseRouteSelection.select(all, "town/nope"));
    }

    /** tools/showcase-targets.json must only name routes and modes that exist. */
    @Test
    void targetMapNamesRealRoutes() throws IOException {
        Path file = Path.of("..", "tools", "showcase-targets.json");
        if (!Files.isRegularFile(file)) {
            file = Path.of("tools", "showcase-targets.json");
        }
        JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        List<ShowcaseRoute> all = ShowcaseRouteRegistry.allRoutes();
        JsonArray rules = root.getAsJsonArray("rules");
        for (JsonElement rule : rules) {
            for (JsonElement target : rule.getAsJsonObject().getAsJsonArray("targets")) {
                String text = target.getAsString();
                if (text.startsWith("route:")) {
                    ShowcaseRouteSelection.select(all, text.substring("route:".length()));
                }
            }
        }
    }
}
