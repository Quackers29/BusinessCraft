package com.quackers29.businesscraft.ui.templates;

import com.quackers29.businesscraft.ui.theme.UiTheme;

/**
 * Town Interface colour names. Values come from {@link UiTheme} so town screens
 * share the platform and tourist palette.
 */
public final class TownInterfaceTheme {

    public static final int PRIMARY_COLOR = UiTheme.BUTTON;
    public static final int SECONDARY_COLOR = UiTheme.BUTTON;
    public static final int BACKGROUND_COLOR = UiTheme.PANEL;
    public static final int BORDER_COLOR = UiTheme.BORDER;

    public static final int ACTIVE_TAB_COLOR = UiTheme.BUTTON;
    public static final int INACTIVE_TAB_COLOR = UiTheme.SECTION;

    public static final int TEXT_COLOR = UiTheme.TEXT;
    public static final int TEXT_HIGHLIGHT = UiTheme.AQUA;

    public static final int SUCCESS_COLOR = UiTheme.ON_BG;
    public static final int DANGER_COLOR = UiTheme.OFF_BG;
    public static final int ACCENT_COLOR = UiTheme.GOLD;

    // Prevent instantiation
    private TownInterfaceTheme() {
        throw new UnsupportedOperationException("Theme class cannot be instantiated");
    }

    /**
     * Creates a BCTheme instance with the Town Interface color scheme.
     * 
     * @return A configured BCTheme for the town interface
     */
    public static com.quackers29.businesscraft.ui.templates.BCTheme createBCTheme() {
        return com.quackers29.businesscraft.ui.templates.BCTheme.builder()
                .primaryColor(PRIMARY_COLOR)
                .secondaryColor(SECONDARY_COLOR)
                .successColor(SUCCESS_COLOR)
                .dangerColor(DANGER_COLOR)
                .textLight(TEXT_COLOR)
                .textDark(UiTheme.ON_FG)
                .panelBackground(BACKGROUND_COLOR)
                .panelBorder(BORDER_COLOR)
                .roundedCorners(true)
                .build();
    }
}
