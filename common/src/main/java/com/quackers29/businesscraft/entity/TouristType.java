package com.quackers29.businesscraft.entity;

import net.minecraft.util.RandomSource;

/**
 * Lightweight tourist flavour labels — display-only, no gameplay effect.
 */
public enum TouristType {
    SIGHTSEER("Sightseer"),
    SHOPPER("Shopper"),
    SCHOLAR("Scholar"),
    PILGRIM("Pilgrim"),
    ADVENTURER("Adventurer");

    private final String displayName;

    TouristType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static TouristType fromIndex(int index) {
        TouristType[] values = values();
        if (index < 0 || index >= values.length) {
            return SIGHTSEER;
        }
        return values[index];
    }

    public static TouristType random(RandomSource random) {
        return values()[random.nextInt(values().length)];
    }

    public int index() {
        return ordinal();
    }
}
