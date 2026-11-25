package com.quackers29.businesscraft.entity.tourist;

/**
 * Stable indices for tourist geometry. Textures can grow without code
 * ({@code skin_N.png}, {@code outfit_N.png}). A new hat or held-item shape needs a
 * model part plus a new index here, because the mesh lives in {@code TouristModel}.
 *
 * <p>Headwear {@code 0} and accessory {@code 0} draw nothing. Later indices that
 * share a mesh with an earlier one are recolours: the texture changes and the
 * silhouette does not. The constants stay in lockstep with the part names in
 * {@code TouristModel}. Counts come from those arrays, not from a fixed catalog size.
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
    public static final int HEADWEAR_VISOR = 16;
    public static final int HEADWEAR_BUCKET = 17;
    public static final int HEADWEAR_TURBAN = 18;
    public static final int HEADWEAR_HELMET = 19;
    public static final int HEADWEAR_FEDORA = 20;
    /** Cap mesh, scarlet texture. */
    public static final int HEADWEAR_CAP_SCARLET = 21;
    /** Sun-hat mesh, sand texture. */
    public static final int HEADWEAR_SUNHAT_SAND = 22;
    /** Beanie mesh, plum texture. */
    public static final int HEADWEAR_BEANIE_PLUM = 23;
    /** Beret mesh, ink texture. */
    public static final int HEADWEAR_BERET_INK = 24;
    /** Boater mesh, cream texture. */
    public static final int HEADWEAR_BOATER_CREAM = 25;
    /** Hood mesh, slate texture. */
    public static final int HEADWEAR_HOOD_SLATE = 26;
    /** Ushanka mesh, grey texture. */
    public static final int HEADWEAR_USHANKA_GREY = 27;
    /** Headband mesh, rose texture. */
    public static final int HEADWEAR_HEADBAND_ROSE = 28;
    /** Pith mesh, olive texture. */
    public static final int HEADWEAR_PITH_OLIVE = 29;
    /** Cap mesh, olive texture. */
    public static final int HEADWEAR_CAP_OLIVE = 30;
    /** Beret mesh, crimson texture. */
    public static final int HEADWEAR_BERET_CRIMSON = 31;
    public static final int HEADWEAR_MAX = HEADWEAR_BERET_CRIMSON;

    public static final int ACCESSORY_NONE = 0;
    public static final int ACCESSORY_CAMERA = 1;
    public static final int ACCESSORY_SUITCASE = 2;
    public static final int ACCESSORY_BACKPACK = 3;
    public static final int ACCESSORY_MAP = 4;
    /** Retired index (was the umbrella). Never valid; kept so other accessory indices stay stable. */
    private static final int ACCESSORY_RETIRED_5 = 5;
    public static final int ACCESSORY_BOOK = 6;
    public static final int ACCESSORY_BASKET = 7;
    public static final int ACCESSORY_LANTERN = 8;
    /** Backpack mesh, red texture. */
    public static final int ACCESSORY_BACKPACK_RED = 9;
    /** Suitcase mesh, brown texture. */
    public static final int ACCESSORY_SUITCASE_BROWN = 10;
    /** Camera mesh, brass texture. */
    public static final int ACCESSORY_CAMERA_BRASS = 11;
    public static final int ACCESSORY_BINOCULARS = 12;
    public static final int ACCESSORY_BRIEFCASE = 13;
    public static final int ACCESSORY_CANTEEN = 14;
    public static final int ACCESSORY_NEWSPAPER = 15;
    public static final int ACCESSORY_TOTE = 16;
    /** Book mesh, navy texture. */
    public static final int ACCESSORY_BOOK_NAVY = 17;
    /** Backpack mesh, olive texture. */
    public static final int ACCESSORY_BACKPACK_OLIVE = 18;
    /** Basket mesh, apple-red texture. */
    public static final int ACCESSORY_BASKET_APPLE = 19;
    /** Lantern mesh, copper texture. */
    public static final int ACCESSORY_LANTERN_COPPER = 20;
    public static final int ACCESSORY_MAX = ACCESSORY_LANTERN_COPPER;

    private TouristParts() {
    }

    public static boolean knownHeadwear(int index) {
        return index >= HEADWEAR_NONE && index <= HEADWEAR_MAX;
    }

    public static boolean knownAccessory(int index) {
        return index >= ACCESSORY_NONE && index <= ACCESSORY_MAX && index != ACCESSORY_RETIRED_5;
    }
}
