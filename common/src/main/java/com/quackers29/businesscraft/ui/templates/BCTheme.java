package com.quackers29.businesscraft.ui.templates;

import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;

/**
 * Theme class for BusinessCraft UI components.
 * Provides consistent styling across all UI elements.
 */
public class BCTheme {
    // Default colors with improved visibility
    private static final int DEFAULT_PRIMARY_COLOR = 0xA0335599;       // Semi-transparent blue
    private static final int DEFAULT_SECONDARY_COLOR = 0xA0884466;     // Semi-transparent purple
    private static final int DEFAULT_SUCCESS_COLOR = 0xA0339944;       // Semi-transparent green
    private static final int DEFAULT_DANGER_COLOR = 0xA0993333;        // Semi-transparent red
    private static final int DEFAULT_WARNING_COLOR = 0xA0999933;       // Semi-transparent yellow
    private static final int DEFAULT_INFO_COLOR = 0xA0339999;          // Semi-transparent teal
    private static final int DEFAULT_TEXT_LIGHT = 0xFFFFFFFF;          // White text
    private static final int DEFAULT_TEXT_DARK = 0xFF202020;           // Dark gray text
    private static final int DEFAULT_PANEL_BACKGROUND = 0x80222222;    // Semi-transparent dark gray
    private static final int DEFAULT_PANEL_BORDER = 0xA0AAAAAA;        // Light gray border
    
    // Default theme instance
    private static final BCTheme DEFAULT = createDefaultTheme();
    
    // Active theme
    private static BCTheme activeTheme = DEFAULT;
    
    // Color palette
    private int primaryColor;
    private int secondaryColor;
    private int successColor;
    private int dangerColor;
    private int warningColor;
    private int infoColor;
    
    // Text colors
    private int textLight;
    private int textDark;
    private int textMuted = 0x999999;
    
    // Panel colors
    private int panelBackground;
    private int panelBorder;
    
    // Component sizing
    private int padding = 5;
    private int margin = 5;
    private int cornerRadius = 3;
    private int borderWidth = 1;
    private boolean roundedCorners = true;
    
    /**
     * Private constructor for the theme.
     * Use the builder to create a new theme.
     */
    private BCTheme() {
        // Private constructor
    }
    
    /**
     * Creates the default theme with improved visibility colors.
     * 
     * @return The default theme
     */
    private static BCTheme createDefaultTheme() {
        BCTheme theme = new BCTheme();
        theme.primaryColor = DEFAULT_PRIMARY_COLOR;
        theme.secondaryColor = DEFAULT_SECONDARY_COLOR;
        theme.successColor = DEFAULT_SUCCESS_COLOR;
        theme.dangerColor = DEFAULT_DANGER_COLOR;
        theme.warningColor = DEFAULT_WARNING_COLOR;
        theme.infoColor = DEFAULT_INFO_COLOR;
        theme.textLight = DEFAULT_TEXT_LIGHT;
        theme.textDark = DEFAULT_TEXT_DARK;
        theme.panelBackground = DEFAULT_PANEL_BACKGROUND;
        theme.panelBorder = DEFAULT_PANEL_BORDER;
        theme.roundedCorners = true;
        return theme;
    }
    
    /**
     * Get the active theme.
     * 
     * @return The active theme
     */
    public static BCTheme get() {
        return activeTheme;
    }
    
    /**
     * Set the active theme.
     * 
     * @param theme The theme to set as active
     */
    public static void setActiveTheme(BCTheme theme) {
        if (theme != null) {
            activeTheme = theme;
        } else {
            activeTheme = DEFAULT;
        }
    }
    
    /**
     * Reset to the default theme.
     */
    public static void resetToDefault() {
        activeTheme = DEFAULT;
    }
    
    /**
     * Get the default theme.
     * 
     * @return The default theme
     */
    public static BCTheme getDefaultTheme() {
        return DEFAULT;
    }
    
    /**
     * Create a new theme builder.
     * 
     * @return A new theme builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    // Color getters
    public int getPrimaryColor() { return primaryColor; }
    public int getSecondaryColor() { return secondaryColor; }
    public int getSuccessColor() { return successColor; }
    public int getDangerColor() { return dangerColor; }
    public int getWarningColor() { return warningColor; }
    public int getInfoColor() { return infoColor; }
    
    // Text color getters
    public int getTextLight() { return textLight; }
    public int getTextDark() { return textDark; }
    public int getTextMuted() { return textMuted; }
    
    // Panel color getters
    public int getPanelBackground() { return panelBackground; }
    public int getPanelBorder() { return panelBorder; }
    
    // Component sizing getters
    public int getPadding() { return padding; }
    public int getMargin() { return margin; }
    public int getCornerRadius() { return cornerRadius; }
    public int getBorderWidth() { return borderWidth; }
    public boolean hasRoundedCorners() { return roundedCorners; }
    
    // Font helpers
    public Font getFont() {
        return Minecraft.getInstance().font;
    }
    
    public int getLineHeight() {
        return getFont().lineHeight;
    }
    
    /**
     * Builder class for creating themes.
     */
    public static class Builder {
        private BCTheme theme;
        
        public Builder() {
            theme = new BCTheme();
        }
        
        public Builder primaryColor(int color) {
            theme.primaryColor = color;
            return this;
        }
        
        public Builder secondaryColor(int color) {
            theme.secondaryColor = color;
            return this;
        }
        
        public Builder successColor(int color) {
            theme.successColor = color;
            return this;
        }
        
        public Builder dangerColor(int color) {
            theme.dangerColor = color;
            return this;
        }
        
        public Builder warningColor(int color) {
            theme.warningColor = color;
            return this;
        }
        
        public Builder infoColor(int color) {
            theme.infoColor = color;
            return this;
        }
        
        public Builder textLight(int color) {
            theme.textLight = color;
            return this;
        }
        
        public Builder textDark(int color) {
            theme.textDark = color;
            return this;
        }
        
        public Builder textMuted(int color) {
            theme.textMuted = color;
            return this;
        }
        
        public Builder panelBackground(int color) {
            theme.panelBackground = color;
            return this;
        }
        
        public Builder panelBorder(int color) {
            theme.panelBorder = color;
            return this;
        }
        
        public Builder padding(int padding) {
            theme.padding = padding;
            return this;
        }
        
        public Builder margin(int margin) {
            theme.margin = margin;
            return this;
        }
        
        public Builder cornerRadius(int radius) {
            theme.cornerRadius = radius;
            return this;
        }
        
        public Builder borderWidth(int width) {
            theme.borderWidth = width;
            return this;
        }
        
        public Builder roundedCorners(boolean rounded) {
            theme.roundedCorners = rounded;
            return this;
        }
        
        public BCTheme build() {
            return theme;
        }
    }
} 
