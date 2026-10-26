package com.quackers29.businesscraft.client.showcase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Decides which showcase shots a run will take. No Minecraft types, so unit
 * tests can cover the selection rules without starting the game.
 *
 * <p>The system property is absent when showcase is off. An empty value or
 * {@code all} selects every shot. Otherwise the value is a comma-separated
 * list of scene names ({@code block}, {@code town}) or exact shot names.
 */
public final class ShowcaseSelection {
    public static final List<String> ALL_SHOTS = List.of(
            "warmup",
            "town_interface",
            "town_interface_back",
            "town_overview",
            "town_resources",
            "town_production",
            "town_settings",
            "platform_interface",
            "platform_interface_screen",
            "platform_overview",
            "platform_front",
            "platform_side",
            "platform_rail",
            "platform_hand",
            "platform_path",
            "town_paths");

    private static final Map<String, List<String>> SCENES = Map.of(
            "block", List.of("town_interface", "town_interface_back"),
            "town", List.of("town_overview", "town_resources", "town_production", "town_settings"),
            "platform", List.of("town_overview", "platform_interface", "platform_interface_screen", "platform_overview"),
            "kiosk", List.of("platform_front", "platform_side", "platform_rail", "platform_hand", "platform_path", "town_paths"));

    private ShowcaseSelection() {
    }

    public static final String ALL_VIEWS = "all-views";
    public static final String TWO_TOWNS = "two-towns";
    public static final String TESTBED = "testbed";
    public static final String MAP_SWEEP = "map-sweep";
    public static final String TOURISTS = "tourists";
    public static final String TOURIST_SCREEN = "tourist-screen";

    public static boolean enabled(String property) {
        return property != null;
    }

    public static boolean allViewsMode(String property) {
        return property != null && ALL_VIEWS.equalsIgnoreCase(property.trim());
    }

    public static boolean twoTownsMode(String property) {
        return property != null && TWO_TOWNS.equalsIgnoreCase(property.trim());
    }

    public static boolean testbedMode(String property) {
        return property != null && TESTBED.equalsIgnoreCase(property.trim());
    }

    public static boolean mapSweepMode(String property) {
        return property != null && MAP_SWEEP.equalsIgnoreCase(property.trim());
    }

    public static boolean touristsMode(String property) {
        return property != null && TOURISTS.equalsIgnoreCase(property.trim());
    }

    public static boolean touristScreenMode(String property) {
        return property != null && TOURIST_SCREEN.equalsIgnoreCase(property.trim());
    }

    public static List<String> shotsFor(String property) {
        if (!enabled(property) || allViewsMode(property) || twoTownsMode(property) || testbedMode(property)
                || mapSweepMode(property) || touristsMode(property) || touristScreenMode(property)) {
            return List.of();
        }
        List<String> selected = new ArrayList<>();
        for (String shot : ALL_SHOTS) {
            if (includes(property, shot)) {
                selected.add(shot);
            }
        }
        if (!selected.isEmpty() && !selected.contains("warmup")) {
            selected.add(0, "warmup");
        }
        return List.copyOf(selected);
    }

    public static boolean includes(String property, String shotName) {
        if (!enabled(property) || shotName == null) {
            return false;
        }
        String value = property.trim();
        if (value.isEmpty() || value.equalsIgnoreCase("all")) {
            return true;
        }
        for (String raw : value.split(",")) {
            String token = raw.trim().toLowerCase(Locale.ROOT);
            if (token.isEmpty()) {
                continue;
            }
            if (token.equals(shotName.toLowerCase(Locale.ROOT))) {
                return true;
            }
            List<String> scene = SCENES.get(token);
            if (scene != null) {
                for (String sceneShot : scene) {
                    if (sceneShot.equalsIgnoreCase(shotName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Yaw and pitch, in degrees, a camera at {@code eye} uses to look at {@code target}.
     * Yaw {@code 0} looks south (+Z) and negative pitch looks up, matching Minecraft.
     */
    public static float[] facing(double eyeX, double eyeY, double eyeZ,
            double targetX, double targetY, double targetZ) {
        double dx = targetX - eyeX;
        double dy = targetY - eyeY;
        double dz = targetZ - eyeZ;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, horizontal));
        return new float[] { yaw, pitch };
    }
}
