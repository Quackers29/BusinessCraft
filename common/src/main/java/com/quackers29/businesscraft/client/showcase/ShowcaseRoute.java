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

    /**
     * Simple class name of the last {@code wait-for-screen} step, or null when
     * the route never waits on a screen. Popups stay on the town screen, so
     * {@code wait-for-popup} does not change this.
     */
    public String expectedScreen() {
        String expected = null;
        for (String step : steps) {
            if (step == null || !step.startsWith("wait-for-screen:")) {
                continue;
            }
            String[] parts = step.split(":", 3);
            if (parts.length >= 2 && !parts[1].isEmpty()) {
                expected = parts[1];
            }
        }
        return expected;
    }
}
