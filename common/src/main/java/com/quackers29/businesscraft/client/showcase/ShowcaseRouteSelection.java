package com.quackers29.businesscraft.client.showcase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Narrows the all-views walk to the routes a change touches. No Minecraft types.
 *
 * <p>The spec is a comma-separated list. A token is an exact route name ({@code town/map}) or a
 * group, which is the part before the slash ({@code platform} selects {@code platform/management}
 * and {@code platform/interface}). A blank spec selects every route. An unknown token is an error
 * so a typo cannot quietly shrink a run to nothing.
 */
public final class ShowcaseRouteSelection {
    public static final String PROPERTY = "businesscraft.showcase.routes";

    private ShowcaseRouteSelection() {
    }

    public static List<ShowcaseRoute> select(List<ShowcaseRoute> all, String spec) {
        if (spec == null || spec.isBlank()) {
            return all;
        }
        List<String> tokens = new ArrayList<>();
        for (String raw : spec.split(",")) {
            String token = raw.trim().toLowerCase(Locale.ROOT);
            if (!token.isEmpty() && !tokens.contains(token)) {
                tokens.add(token);
            }
        }
        List<ShowcaseRoute> picked = new ArrayList<>();
        List<String> unknown = new ArrayList<>();
        for (String token : tokens) {
            boolean any = false;
            for (ShowcaseRoute route : all) {
                if (matches(route.name(), token)) {
                    any = true;
                }
            }
            if (!any) {
                unknown.add(token);
            }
        }
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("unknown showcase route(s) " + unknown + "; routes are "
                    + all.stream().map(ShowcaseRoute::name).toList());
        }
        for (ShowcaseRoute route : all) {
            for (String token : tokens) {
                if (matches(route.name(), token)) {
                    picked.add(route);
                    break;
                }
            }
        }
        return List.copyOf(picked);
    }

    static boolean matches(String routeName, String token) {
        String name = routeName.toLowerCase(Locale.ROOT);
        return name.equals(token) || name.startsWith(token + "/");
    }
}
