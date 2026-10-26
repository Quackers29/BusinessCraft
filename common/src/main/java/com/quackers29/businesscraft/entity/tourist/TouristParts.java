package com.quackers29.businesscraft.entity.tourist;

/**
 * Stable indices for tourist geometry. Textures can grow without code
 * ({@code skin_N.png}, {@code outfit_N.png}). A new hat or held-item shape needs a
 * model part plus a new index here, because the mesh lives in {@code TouristModel}.
 *
 * <p>Headwear {@code 0} and accessory {@code 0} draw nothing. Indices {@code 13–15}
 * and {@code 9–11} reuse an earlier mesh (a recolor) so the texture can change
 * without a new silhouette. The constants stay in lockstep with the part names
 * in {@code TouristModel}.
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
    public static final int HEADWEAR_HOOD = 8;
    public static final int HEADWEAR_BOATER = 9;
    public static final int HEADWEAR_FLOWER = 10;
    public static final int HEADWEAR_USHANKA = 11;
    public static final int HEADWEAR_BEANIE = 12;
    /** Cap mesh, navy texture. */
    public static final int HEADWEAR_CAP_NAVY = 13;
    /** Cap mesh, dust-coloured texture. */
    public static final int HEADWEAR_CAP_DUST = 14;
    /** Fez mesh, plum texture. */
    public static final int HEADWEAR_FEZ_PLUM = 15;
    public static final int HEADWEAR_MAX = HEADWEAR_FEZ_PLUM;

    public static final int ACCESSORY_NONE = 0;
    public static final int ACCESSORY_CAMERA = 1;
    public static final int ACCESSORY_SUITCASE = 2;
    public static final int ACCESSORY_BACKPACK = 3;
    public static final int ACCESSORY_MAP = 4;
    public static final int ACCESSORY_UMBRELLA = 5;
    public static final int ACCESSORY_BOOK = 6;
    public static final int ACCESSORY_BASKET = 7;
    public static final int ACCESSORY_LANTERN = 8;
    /** Backpack mesh, red texture. */
    public static final int ACCESSORY_BACKPACK_RED = 9;
    /** Suitcase mesh, brown texture. */
    public static final int ACCESSORY_SUITCASE_BROWN = 10;
    /** Camera mesh, brass texture. */
    public static final int ACCESSORY_CAMERA_BRASS = 11;
    public static final int ACCESSORY_MAX = ACCESSORY_CAMERA_BRASS;

    private TouristParts() {
    }

    public static boolean knownHeadwear(int index) {
        return index >= HEADWEAR_NONE && index <= HEADWEAR_MAX;
    }

    public static boolean knownAccessory(int index) {
        return index >= ACCESSORY_NONE && index <= ACCESSORY_MAX;
    }
}
