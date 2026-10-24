package com.quackers29.businesscraft.entity.tourist;

/**
 * Stable indices for tourist geometry. Textures can grow without code
 * ({@code skin_N.png}, {@code outfit_N.png}). A new hat or held-item shape needs a
 * model part plus a new index here, because the mesh lives in {@code TouristModel}.
 *
 * <p>Headwear {@code 0} and accessory {@code 0} draw nothing.
 */
public final class TouristParts {
    public static final int HEADWEAR_NONE = 0;
    public static final int HEADWEAR_PITH = 1;
    public static final int HEADWEAR_CAP = 2;
    public static final int HEADWEAR_BERET = 3;
    public static final int HEADWEAR_SUNHAT = 4;
    public static final int HEADWEAR_FEZ = 5;
    public static final int HEADWEAR_HEADBAND = 6;
    public static final int HEADWEAR_TOPHAT = 7;

    public static final int ACCESSORY_NONE = 0;
    public static final int ACCESSORY_CAMERA = 1;
    public static final int ACCESSORY_SUITCASE = 2;
    public static final int ACCESSORY_BACKPACK = 3;
    public static final int ACCESSORY_MAP = 4;

    private TouristParts() {
    }

    public static boolean knownHeadwear(int index) {
        return index >= HEADWEAR_NONE && index <= HEADWEAR_TOPHAT;
    }

    public static boolean knownAccessory(int index) {
        return index >= ACCESSORY_NONE && index <= ACCESSORY_MAP;
    }
}
