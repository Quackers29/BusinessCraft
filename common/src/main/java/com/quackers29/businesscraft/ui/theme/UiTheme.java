package com.quackers29.businesscraft.ui.theme;

/**
 * Single palette, spacing scale, and control sizes for BusinessCraft screens.
 * Screens read these constants. They do not keep a second copy of the hex values.
 */
public final class UiTheme {
    private UiTheme() {
    }

    public static final int PANEL = 0xF014161C;
    public static final int BORDER = 0xFFD7B56D;
    public static final int SECTION = 0xFF1A1F2A;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int MUTED = 0xFFC2C6CE;
    public static final int GOLD = 0xFFFFD56A;
    public static final int AQUA = 0xFF8FDBFF;
    public static final int ON_BG = 0xFF8FDB9A;
    public static final int ON_FG = 0xFF102816;
    public static final int OFF_BG = 0xFFF0A3A3;
    public static final int OFF_FG = 0xFF3A1216;
    public static final int BUTTON = 0xFF2C3342;
    public static final int BUTTON_HOVER = 0xFF3C4A62;
    public static final int BUTTON_DISABLED = 0xFF242830;
    public static final int BUTTON_DISABLED_TEXT = 0xFF8A909A;
    public static final int LINE = 0xFF3A404C;
    public static final int OVERLAY = 0xB0000000;
    public static final int ROW = 0xFF181C24;
    public static final int ROW_ALT = 0xFF1E2430;
    public static final int TRACK = 0xFF2A3140;

    public static final int SPACE_1 = 4;
    public static final int SPACE_2 = 8;
    public static final int SPACE_3 = 12;
    public static final int SPACE_4 = 16;

    public static final int BUTTON_HEIGHT = 20;
    public static final int BUTTON_HEIGHT_COMPACT = 16;
    public static final int BUTTON_PAD_X = 16;
    public static final int BACK_SIZE = 20;
    public static final int TOGGLE_WIDTH = 64;
    public static final int TOGGLE_HEIGHT = 18;
    public static final int SCROLLBAR = 3;

    public static int buttonWidth(int textWidth) {
        return Math.max(BACK_SIZE, textWidth + BUTTON_PAD_X);
    }

    public static int buttonHeight(int panelHeight) {
        return panelHeight > 0 && panelHeight < 250 ? BUTTON_HEIGHT_COMPACT : BUTTON_HEIGHT;
    }
}
