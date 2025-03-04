package com.yourdomain.businesscraft.screen;

/**
 * Theme system for BusinessCraft UI
 * Provides consistent colors, sizes, and styles
 */
public class BCTheme {
    // Singleton instance
    private static BCTheme instance;
    
    // Color scheme
    private int primaryColor = 0xFF2196F3;
    private int primaryVariantColor = 0xFF0D47A1;
    private int secondaryColor = 0xFF4CAF50;
    private int secondaryVariantColor = 0xFF1B5E20;
    private int backgroundColor = 0xE0101010;
    private int backgroundSecondaryColor = 0xE0202020;
    private int backgroundVariantColor = 0xE0161620;
    private int surfaceColor = 0xF0303030;
    private int surfaceVariantColor = 0xF0252525;
    private int borderColor = 0xFF555555;
    private int textColor = 0xFFFFFFFF;
    private int textSecondaryColor = 0xFFCCCCCC;
    private int accentColor = 0xFFFF9800;
    private int errorColor = 0xFFF44336;
    private int successColor = 0xFF4CAF50;
    private int warningColor = 0xFFFFEB3B;
    private int infoColor = 0xFF2196F3;
    private int dialogBackgroundColor = 0xF0252525;
    
    // Padding sizes
    private int smallPadding = 4;
    private int mediumPadding = 8;
    private int largePadding = 12;
    
    // Font sizes
    private int smallFontSize = 8;
    private int mediumFontSize = 10;
    private int largeFontSize = 12;
    
    /**
     * Get the singleton instance of the theme
     */
    public static BCTheme get() {
        if (instance == null) {
            instance = new BCTheme();
        }
        return instance;
    }
    
    /**
     * Set a new theme as the current one
     */
    public static void set(BCTheme theme) {
        instance = theme;
    }
    
    /**
     * Create a new theme with default values
     */
    public BCTheme() {
        // Default constructor with default values
    }
    
    /**
     * Create a new theme builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters for colors
    public int getPrimaryColor() { return primaryColor; }
    public int getPrimaryVariantColor() { return primaryVariantColor; }
    public int getSecondaryColor() { return secondaryColor; }
    public int getSecondaryVariantColor() { return secondaryVariantColor; }
    public int getBackgroundColor() { return backgroundColor; }
    public int getBackgroundSecondaryColor() { return backgroundSecondaryColor; }
    public int getBackgroundVariantColor() { return backgroundVariantColor; }
    public int getSurfaceColor() { return surfaceColor; }
    public int getSurfaceVariantColor() { return surfaceVariantColor; }
    public int getBorderColor() { return borderColor; }
    public int getTextColor() { return textColor; }
    public int getTextSecondaryColor() { return textSecondaryColor; }
    public int getAccentColor() { return accentColor; }
    public int getErrorColor() { return errorColor; }
    public int getSuccessColor() { return successColor; }
    public int getWarningColor() { return warningColor; }
    public int getInfoColor() { return infoColor; }
    public int getDialogBackgroundColor() { return dialogBackgroundColor; }
    
    // Getters for padding sizes
    public int getSmallPadding() { return smallPadding; }
    public int getMediumPadding() { return mediumPadding; }
    public int getLargePadding() { return largePadding; }
    
    // Getters for font sizes
    public int getSmallFontSize() { return smallFontSize; }
    public int getMediumFontSize() { return mediumFontSize; }
    public int getLargeFontSize() { return largeFontSize; }
    
    /**
     * Get a dark theme
     */
    public static BCTheme darkTheme() {
        return BCTheme.builder()
            .primaryColor(0xFF2196F3)
            .primaryVariantColor(0xFF0D47A1)
            .secondaryColor(0xFF4CAF50)
            .secondaryVariantColor(0xFF1B5E20)
            .backgroundColor(0xE0101010)
            .backgroundSecondaryColor(0xE0202020)
            .backgroundVariantColor(0xE0161620)
            .surfaceColor(0xF0303030)
            .surfaceVariantColor(0xF0252525)
            .borderColor(0xFF555555)
            .textColor(0xFFFFFFFF)
            .textSecondaryColor(0xFFCCCCCC)
            .accentColor(0xFFFF9800)
            .build();
    }
    
    /**
     * Get a light theme
     */
    public static BCTheme lightTheme() {
        return BCTheme.builder()
            .primaryColor(0xFF1976D2)
            .primaryVariantColor(0xFF0D47A1)
            .secondaryColor(0xFF388E3C)
            .secondaryVariantColor(0xFF1B5E20)
            .backgroundColor(0xE0F5F5F5)
            .backgroundSecondaryColor(0xE0E0E0E0)
            .backgroundVariantColor(0xE0EBEBF0)
            .surfaceColor(0xF0FFFFFF)
            .surfaceVariantColor(0xF0F5F5F5)
            .borderColor(0xFFBDBDBD)
            .textColor(0xFF212121)
            .textSecondaryColor(0xFF757575)
            .accentColor(0xFFF57C00)
            .build();
    }
    
    /**
     * Get a fantasy theme
     */
    public static BCTheme fantasyTheme() {
        return BCTheme.builder()
            .primaryColor(0xFF7E57C2)
            .primaryVariantColor(0xFF4527A0)
            .secondaryColor(0xFFFFB300)
            .secondaryVariantColor(0xFFC68400)
            .backgroundColor(0xE0251D29)
            .backgroundSecondaryColor(0xE0352838)
            .backgroundVariantColor(0xE0302640)
            .surfaceColor(0xF0453545)
            .surfaceVariantColor(0xF0553D55)
            .borderColor(0xFF7E57C2)
            .textColor(0xFFF5F5F5)
            .textSecondaryColor(0xFFBBBBBB)
            .accentColor(0xFFFFB300)
            .build();
    }
    
    /**
     * Builder for creating themes
     */
    public static class Builder {
        private BCTheme theme = new BCTheme();
        
        public Builder primaryColor(int color) {
            theme.primaryColor = color;
            return this;
        }
        
        public Builder primaryVariantColor(int color) {
            theme.primaryVariantColor = color;
            return this;
        }
        
        public Builder secondaryColor(int color) {
            theme.secondaryColor = color;
            return this;
        }
        
        public Builder secondaryVariantColor(int color) {
            theme.secondaryVariantColor = color;
            return this;
        }
        
        public Builder backgroundColor(int color) {
            theme.backgroundColor = color;
            return this;
        }
        
        public Builder backgroundSecondaryColor(int color) {
            theme.backgroundSecondaryColor = color;
            return this;
        }
        
        public Builder backgroundVariantColor(int color) {
            theme.backgroundVariantColor = color;
            return this;
        }
        
        public Builder surfaceColor(int color) {
            theme.surfaceColor = color;
            return this;
        }
        
        public Builder surfaceVariantColor(int color) {
            theme.surfaceVariantColor = color;
            return this;
        }
        
        public Builder borderColor(int color) {
            theme.borderColor = color;
            return this;
        }
        
        public Builder textColor(int color) {
            theme.textColor = color;
            return this;
        }
        
        public Builder textSecondaryColor(int color) {
            theme.textSecondaryColor = color;
            return this;
        }
        
        public Builder accentColor(int color) {
            theme.accentColor = color;
            return this;
        }
        
        public Builder errorColor(int color) {
            theme.errorColor = color;
            return this;
        }
        
        public Builder successColor(int color) {
            theme.successColor = color;
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
        
        public Builder dialogBackgroundColor(int color) {
            theme.dialogBackgroundColor = color;
            return this;
        }
        
        public Builder smallPadding(int padding) {
            theme.smallPadding = padding;
            return this;
        }
        
        public Builder mediumPadding(int padding) {
            theme.mediumPadding = padding;
            return this;
        }
        
        public Builder largePadding(int padding) {
            theme.largePadding = padding;
            return this;
        }
        
        public Builder smallFontSize(int size) {
            theme.smallFontSize = size;
            return this;
        }
        
        public Builder mediumFontSize(int size) {
            theme.mediumFontSize = size;
            return this;
        }
        
        public Builder largeFontSize(int size) {
            theme.largeFontSize = size;
            return this;
        }
        
        public BCTheme build() {
            return theme;
        }
    }
} 