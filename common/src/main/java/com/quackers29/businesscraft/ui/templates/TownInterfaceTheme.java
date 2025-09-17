package com.quackers29.businesscraft.ui.templates;

/**
 * Centralized theme constants for the Town Interface Screen and related components.
 * This class provides consistent color definitions across all town interface elements.
 */
public final class TownInterfaceTheme {
    
    // Primary UI colors - lighter and more visible
    public static final int PRIMARY_COLOR = 0xA0335599;       // Semi-transparent blue
    public static final int SECONDARY_COLOR = 0xA0884466;     // Semi-transparent purple
    public static final int BACKGROUND_COLOR = 0x80222222;    // Semi-transparent dark gray
    public static final int BORDER_COLOR = 0xA0AAAAAA;        // Light gray
    
    // Tab-specific colors
    public static final int ACTIVE_TAB_COLOR = 0xA0CCDDFF;    // Light blue for active tab
    public static final int INACTIVE_TAB_COLOR = 0x80555555;  // Medium gray for inactive tabs
    
    // Text colors
    public static final int TEXT_COLOR = 0xFFFFFFFF;          // White text
    public static final int TEXT_HIGHLIGHT = 0xFFDDFFFF;      // Light cyan highlight text
    
    // Status colors
    public static final int SUCCESS_COLOR = 0xA0339944;       // Green
    public static final int DANGER_COLOR = 0xA0993333;        // Red
    
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
            .textDark(0xFF202020)
            .panelBackground(BACKGROUND_COLOR)
            .panelBorder(BORDER_COLOR)
            .roundedCorners(true)
            .build();
    }
} 
