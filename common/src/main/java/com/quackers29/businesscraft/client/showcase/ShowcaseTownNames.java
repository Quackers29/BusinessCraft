package com.quackers29.businesscraft.client.showcase;

/**
 * Town names used while {@code businesscraft.showcase} is set.
 * The first placed town is Brookside and the second is Hillcrest, matching
 * the tourist-screen destination. Later towns are {@code Brookside-N}.
 * Gameplay outside the harness still draws from {@code ConfigLoader.townNames}.
 */
public final class ShowcaseTownNames {
    public static final String PRIMARY = "Brookside";
    public static final String SECONDARY = "Hillcrest";

    private static int index;

    private ShowcaseTownNames() {
    }

    public static boolean isActive() {
        return ShowcaseSelection.enabled(System.getProperty("businesscraft.showcase"));
    }

    /** Next pinned name. Only call when {@link #isActive()} is true. */
    public static String next() {
        int n = index++;
        if (n == 0) {
            return PRIMARY;
        }
        if (n == 1) {
            return SECONDARY;
        }
        return PRIMARY + "-" + (n + 1);
    }

    public static void reset() {
        index = 0;
    }
}
