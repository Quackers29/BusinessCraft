package com.quackers29.businesscraft.entity.tourist;

/**
 * Packed visual choices for one tourist. One synced int: skin, outfit, headwear, accessory.
 * Each component is a byte in the range 0–254. {@code -1} means the look has not been chosen yet.
 * 255 is reserved so the unset sentinel cannot collide with a real combination.
 */
public final class TouristAppearance {
    public static final int UNSET = -1;
    public static final int MAX_COMPONENT = 254;

    private TouristAppearance() {
    }

    public static int pack(int skin, int outfit, int headwear, int accessory) {
        check(skin, "skin");
        check(outfit, "outfit");
        check(headwear, "headwear");
        check(accessory, "accessory");
        return (skin & 0xFF)
                | ((outfit & 0xFF) << 8)
                | ((headwear & 0xFF) << 16)
                | ((accessory & 0xFF) << 24);
    }

    public static int skin(int packed) {
        return packed & 0xFF;
    }

    public static int outfit(int packed) {
        return (packed >>> 8) & 0xFF;
    }

    public static int headwear(int packed) {
        return (packed >>> 16) & 0xFF;
    }

    public static int accessory(int packed) {
        return (packed >>> 24) & 0xFF;
    }

    private static void check(int value, String name) {
        if (value < 0 || value > MAX_COMPONENT) {
            throw new IllegalArgumentException(name + " must be 0.." + MAX_COMPONENT + ", was " + value);
        }
    }
}
