package com.quackers29.businesscraft.client.showcase;

import java.util.List;

/**
 * Named navigation path ending on one UI view for a showcase screenshot.
 *
 * @param name  Route id (e.g. {@code town/map})
 * @param steps Ordered step strings interpreted by {@link ShowcaseNavigator}
 */
public record ShowcaseRoute(String name, List<String> steps) {
    public String fileBaseName() {
        return "showcase_view_" + name.replace('/', '_');
    }
}
