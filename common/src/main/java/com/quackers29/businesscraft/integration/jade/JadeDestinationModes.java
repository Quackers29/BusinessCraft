package com.quackers29.businesscraft.integration.jade;

import com.quackers29.businesscraft.platform.DestinationMode;

public final class JadeDestinationModes {

    private JadeDestinationModes() {
    }

    public static String configValue(DestinationMode mode) {
        if (mode == null) {
            return DestinationMode.AUTO.toConfigValue();
        }
        return mode.toConfigValue();
    }

    public static String translationKey(DestinationMode mode) {
        if (mode == null) {
            mode = DestinationMode.AUTO;
        }
        return switch (mode) {
            case ANY -> "businesscraft.jade.destination_mode.any";
            case AUTO -> "businesscraft.jade.destination_mode.auto";
            case SPECIFIC -> "businesscraft.jade.destination_mode.specific";
        };
    }
}
