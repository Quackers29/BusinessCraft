package com.quackers29.businesscraft.platform;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * How a platform chooses tourist destinations at spawn time.
 */
public enum DestinationMode {
    ANY,
    AUTO,
    SPECIFIC;

    private static final Logger LOGGER = LogUtils.getLogger();

    public static DestinationMode fromConfig(String raw, DestinationMode fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().toLowerCase();
        return switch (normalized) {
            case "any" -> ANY;
            case "auto" -> AUTO;
            case "specific" -> SPECIFIC;
            default -> {
                LOGGER.warn("Unknown tourists.defaultDestinationMode '{}', using {}", raw, fallback);
                yield fallback;
            }
        };
    }

    public String toConfigValue() {
        return name().toLowerCase();
    }
}
