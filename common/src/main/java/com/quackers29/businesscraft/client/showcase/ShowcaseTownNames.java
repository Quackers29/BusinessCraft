package com.quackers29.businesscraft.client.showcase;

import java.util.List;

/**
 * Town names used while a showcase or multiplayer-smoke run is active.
 * A showcase run names the first town Brookside and the second Hillcrest.
 * A multiplayer smoke run names the line Oakvale, Hillcrest, Rivertown.
 * Gameplay outside those runs still draws from {@code ConfigLoader.townNames}.
 */
public final class ShowcaseTownNames {
    public static final String PRIMARY = "Brookside";
    public static final String SECONDARY = "Hillcrest";
    public static final List<String> MULTIPLAYER_LINE = List.of("Oakvale", "Hillcrest", "Rivertown");

    private static int index;

    private ShowcaseTownNames() {
    }

    public static boolean isActive() {
        return ShowcaseSelection.enabled(System.getProperty("businesscraft.showcase")) || multiplayerSetup();
    }

    /** Next pinned name. Only call when {@link #isActive()} is true. */
    public static String next() {
        int n = index++;
        if (multiplayerSetup()) {
            if (n < MULTIPLAYER_LINE.size()) {
                return MULTIPLAYER_LINE.get(n);
            }
            return MULTIPLAYER_LINE.get(0) + "-" + (n + 1);
        }
        if (n == 0) {
            return PRIMARY;
        }
        if (n == 1) {
            return SECONDARY;
        }
        return PRIMARY + "-" + (n + 1);
    }

    private static boolean multiplayerSetup() {
        return "setup".equals(System.getProperty("businesscraft.multiplayerSmoke"));
    }

    public static void reset() {
        index = 0;
    }
}
