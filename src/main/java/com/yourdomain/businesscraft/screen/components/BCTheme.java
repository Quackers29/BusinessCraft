package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;

/**
 * Theme system for BusinessCraft UI components.
 * Centralizes styling options and provides consistent theming across the mod.
 */
public class BCTheme {
    // Default theme instance
    private static final BCTheme DEFAULT = new BCTheme();
    
    // Active theme
    private static BCTheme activeTheme = DEFAULT;
    
    // Color palette
    private int primaryColor = 0x336699;
    private int secondaryColor = 0x993366;
    private int successColor = 0x339933;
    private int dangerColor = 0x993333;
    private int warningColor = 0x999933;
    private int infoColor = 0x339999;
    
    // Text colors
    private int textLight = 0xFFFFFF;
    private int textDark = 0x333333;
    private int textMuted = 0x999999;
    
    // Panel colors
    private int panelBackground = 0x80000000;
    private int panelBorder = 0x80FFFFFF;
    
    // Component sizing
    private int standardButtonHeight = 20;
    private int smallButtonHeight = 16;
    private int largeButtonHeight = 24;
    private int standardPadding = 8;
    private int smallPadding = 4;
    private int largePadding = 12;
    
    // Border styling
    private int borderWidth = 1;
    private boolean roundedCorners = true;
    
    /**
     * Get the active theme
     */
    public static BCTheme get() {
        return activeTheme;
    }
    
    /**
     * Set a custom theme as the active theme
     */
    public static void setActiveTheme(BCTheme theme) {
        if (theme != null) {
            activeTheme = theme;
        }
    }
    
    /**
     * Reset to the default theme
     */
    public static void resetToDefault() {
        activeTheme = DEFAULT;
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
    public int getStandardButtonHeight() { return standardButtonHeight; }
    public int getSmallButtonHeight() { return smallButtonHeight; }
    public int getLargeButtonHeight() { return largeButtonHeight; }
    public int getStandardPadding() { return standardPadding; }
    public int getSmallPadding() { return smallPadding; }
    public int getLargePadding() { return largePadding; }
    
    // Border styling getters
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
     * Create a builder for customizing a theme
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for creating custom themes
     */
    public static class Builder {
        private final BCTheme theme = new BCTheme();
        
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
        
        public Builder standardButtonHeight(int height) {
            theme.standardButtonHeight = height;
            return this;
        }
        
        public Builder smallButtonHeight(int height) {
            theme.smallButtonHeight = height;
            return this;
        }
        
        public Builder largeButtonHeight(int height) {
            theme.largeButtonHeight = height;
            return this;
        }
        
        public Builder standardPadding(int padding) {
            theme.standardPadding = padding;
            return this;
        }
        
        public Builder smallPadding(int padding) {
            theme.smallPadding = padding;
            return this;
        }
        
        public Builder largePadding(int padding) {
            theme.largePadding = padding;
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