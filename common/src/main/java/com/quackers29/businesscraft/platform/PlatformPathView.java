package com.quackers29.businesscraft.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Which platform paths a visualization should draw. A null id keeps every platform
 * (the Town Centre view). A specific id keeps only that platform.
 */
public final class PlatformPathView {
    private PlatformPathView() {
    }

    public static List<Platform> select(List<Platform> platforms, UUID onlyPlatformId) {
        if (platforms == null || platforms.isEmpty()) {
            return List.of();
        }
        if (onlyPlatformId == null) {
            return List.copyOf(platforms);
        }
        List<Platform> matched = new ArrayList<>();
        for (Platform platform : platforms) {
            if (onlyPlatformId.equals(platform.getId())) {
                matched.add(platform);
            }
        }
        return List.copyOf(matched);
    }
}
