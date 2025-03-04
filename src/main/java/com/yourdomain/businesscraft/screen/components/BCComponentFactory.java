package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Factory for creating standardized UI components for BusinessCraft.
 * Provides consistent styling and behavior across the mod.
 */
public class BCComponentFactory {
    // Standard colors
    public static final int PRIMARY_COLOR = 0x336699;
    public static final int SECONDARY_COLOR = 0x993366;
    public static final int SUCCESS_COLOR = 0x339933;
    public static final int DANGER_COLOR = 0x993333;
    public static final int WARNING_COLOR = 0x999933;
    public static final int INFO_COLOR = 0x339999;
    
    // Standard text colors
    public static final int TEXT_LIGHT = 0xFFFFFF;
    public static final int TEXT_DARK = 0x333333;
    public static final int TEXT_MUTED = 0x999999;
    
    // Standard sizes
    public static final int BUTTON_HEIGHT = 20;
    public static final int SMALL_BUTTON_HEIGHT = 16;
    public static final int LARGE_BUTTON_HEIGHT = 24;
    
    /**
     * Create a standard primary button
     */
    public static BCButton createPrimaryButton(String translationKey, Consumer<Button> onPress, int width) {
        BCButton button = new BCButton(translationKey, onPress, width, BUTTON_HEIGHT);
        button.withType(BCButton.ButtonType.PRIMARY);
        return button;
    }
    
    /**
     * Create a standard secondary button
     */
    public static BCButton createSecondaryButton(String translationKey, Consumer<Button> onPress, int width) {
        BCButton button = new BCButton(translationKey, onPress, width, BUTTON_HEIGHT);
        button.withType(BCButton.ButtonType.SECONDARY);
        return button;
    }
    
    /**
     * Create a danger button (for destructive actions)
     */
    public static BCButton createDangerButton(String translationKey, Consumer<Button> onPress, int width) {
        BCButton button = new BCButton(translationKey, onPress, width, BUTTON_HEIGHT);
        button.withType(BCButton.ButtonType.DANGER);
        return button;
    }
    
    /**
     * Create a success button (for confirmations)
     */
    public static BCButton createSuccessButton(String translationKey, Consumer<Button> onPress, int width) {
        BCButton button = new BCButton(translationKey, onPress, width, BUTTON_HEIGHT);
        button.withType(BCButton.ButtonType.SUCCESS);
        return button;
    }
    
    /**
     * Create a small icon button
     */
    public static BCButton createIconButton(Component tooltip, Consumer<Button> onPress) {
        BCButton button = new BCButton("", onPress, SMALL_BUTTON_HEIGHT, SMALL_BUTTON_HEIGHT);
        button.withTooltip(tooltip);
        return button;
    }
    
    /**
     * Create a standard header label
     */
    public static BCLabel createHeaderLabel(String translationKey, int width) {
        BCLabel label = new BCLabel(translationKey, width, 12);
        label.withTextColor(TEXT_LIGHT)
             .withAlignment(BCLabel.TextAlignment.CENTER)
             .withShadow(true);
        return label;
    }
    
    /**
     * Create a standard body text label
     */
    public static BCLabel createBodyLabel(String translationKey, int width) {
        BCLabel label = new BCLabel(translationKey, width, 10);
        label.withTextColor(TEXT_LIGHT)
             .withAlignment(BCLabel.TextAlignment.LEFT);
        return label;
    }
    
    /**
     * Create a dynamic label that updates with a supplier
     */
    public static BCLabel createDynamicLabel(Supplier<Component> textSupplier, int width) {
        BCLabel label = new BCLabel(textSupplier, width, 10);
        label.withTextColor(TEXT_LIGHT)
             .withAlignment(BCLabel.TextAlignment.LEFT);
        return label;
    }
    
    /**
     * Create a standard panel with flow layout
     */
    public static BCPanel createFlowPanel(int width, int height, BCFlowLayout.Direction direction, int spacing) {
        BCPanel panel = new BCPanel(width, height);
        panel.withLayout(new BCFlowLayout(direction, spacing));
        return panel;
    }
    
    /**
     * Create a standard panel with grid layout
     */
    public static BCPanel createGridPanel(int width, int height, int columns, int hSpacing, int vSpacing) {
        BCPanel panel = new BCPanel(width, height);
        panel.withLayout(new BCGridLayout(columns, hSpacing, vSpacing));
        return panel;
    }
    
    /**
     * Create a standard container panel with a border
     */
    public static BCPanel createContainerPanel(int width, int height) {
        BCPanel panel = new BCPanel(width, height);
        panel.withBackgroundColor(0x80000000)
             .withBorderColor(0xFFAAAAAA);
        return panel;
    }
} 