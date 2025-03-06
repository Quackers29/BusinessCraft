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
    // These constants are kept for backward compatibility
    // New code should use BCTheme.get() methods instead
    @Deprecated
    public static final int PRIMARY_COLOR = 0x336699;
    @Deprecated
    public static final int SECONDARY_COLOR = 0x993366;
    @Deprecated
    public static final int SUCCESS_COLOR = 0x339933;
    @Deprecated
    public static final int DANGER_COLOR = 0x993333;
    @Deprecated
    public static final int WARNING_COLOR = 0x999933;
    @Deprecated
    public static final int INFO_COLOR = 0x339999;
    
    @Deprecated
    public static final int TEXT_LIGHT = 0xFFFFFF;
    @Deprecated
    public static final int TEXT_DARK = 0x333333;
    @Deprecated
    public static final int TEXT_MUTED = 0x999999;
    
    @Deprecated
    public static final int BUTTON_HEIGHT = 20;
    @Deprecated
    public static final int SMALL_BUTTON_HEIGHT = 16;
    @Deprecated
    public static final int LARGE_BUTTON_HEIGHT = 24;
    
    /**
     * Create a standard primary button
     */
    public static BCButton createPrimaryButton(String translationKey, Consumer<Button> onPress, int width) {
        BCButton button = new BCButton(translationKey, onPress, width, BCTheme.get().getStandardButtonHeight());
        button.withType(BCButton.ButtonType.PRIMARY);
        return button;
    }
    
    /**
     * Create a standard secondary button
     */
    public static BCButton createSecondaryButton(String translationKey, Consumer<Button> onPress, int width) {
        BCButton button = new BCButton(translationKey, onPress, width, BCTheme.get().getStandardButtonHeight());
        button.withType(BCButton.ButtonType.SECONDARY);
        return button;
    }
    
    /**
     * Create a danger button (for destructive actions)
     */
    public static BCButton createDangerButton(String translationKey, Consumer<Button> onPress, int width) {
        BCButton button = new BCButton(translationKey, onPress, width, BCTheme.get().getStandardButtonHeight());
        button.withType(BCButton.ButtonType.DANGER);
        return button;
    }
    
    /**
     * Create a success button (for confirmations)
     */
    public static BCButton createSuccessButton(String translationKey, Consumer<Button> onPress, int width) {
        BCButton button = new BCButton(translationKey, onPress, width, BCTheme.get().getStandardButtonHeight());
        button.withType(BCButton.ButtonType.SUCCESS);
        return button;
    }
    
    /**
     * Create a small icon button
     */
    public static BCButton createIconButton(Component tooltip, Consumer<Button> onPress) {
        BCButton button = new BCButton("", onPress, BCTheme.get().getSmallButtonHeight(), BCTheme.get().getSmallButtonHeight());
        button.withTooltip(tooltip);
        return button;
    }
    
    /**
     * Create a standard header label
     */
    public static BCLabel createHeaderLabel(String translationKey, int width) {
        BCLabel label = new BCLabel(translationKey, width, 12);
        label.withTextColor(BCTheme.get().getTextLight())
             .withAlignment(BCLabel.TextAlignment.CENTER)
             .withShadow(true);
        return label;
    }
    
    /**
     * Create a standard body text label
     */
    public static BCLabel createBodyLabel(String translationKey, int width) {
        BCLabel label = new BCLabel(translationKey, width, 10);
        label.withTextColor(BCTheme.get().getTextLight())
             .withAlignment(BCLabel.TextAlignment.LEFT);
        return label;
    }
    
    /**
     * Create a dynamic label that updates with a supplier
     */
    public static BCLabel createDynamicLabel(Supplier<Component> textSupplier, int width) {
        BCLabel label = new BCLabel(textSupplier, width, 10);
        label.withTextColor(BCTheme.get().getTextLight())
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
        panel.withBackgroundColor(BCTheme.get().getPanelBackground())
             .withBorderColor(BCTheme.get().getPanelBorder());
        return panel;
    }
    
    /**
     * Create a tooltip panel with text
     */
    public static BCPanel createTooltipPanel(String translationKey, int width) {
        BCPanel panel = new BCPanel(width, 0);
        panel.withBackgroundColor(0xF0100010);
        panel.withBorderColor(0xFF5000FF);
        panel.withPadding(BCTheme.get().getSmallPadding());
        
        BCLabel label = createBodyLabel(translationKey, width - 2 * BCTheme.get().getSmallPadding());
        panel.addChild(label);
        
        return panel;
    }
    
    /**
     * Create an edit box for text input
     */
    public static EditBoxComponent createEditBox(int width, Supplier<String> initialText, Consumer<String> onTextChanged, int maxLength) {
        return new EditBoxComponent(width, BCTheme.get().getStandardButtonHeight(), initialText, onTextChanged, maxLength);
    }
    
    /**
     * Create a toggle button
     */
    public static ToggleButtonComponent createToggleButton(Component text, Consumer<Button> onPress, int width) {
        return new ToggleButtonComponent(0, 0, width, BCTheme.get().getStandardButtonHeight(), text, onPress);
    }
    
    /**
     * Create a data-bound button that updates its text automatically
     */
    public static DataBoundButtonComponent createDataBoundButton(Supplier<Component> textSupplier, Consumer<Button> onPress, int width) {
        return new DataBoundButtonComponent(textSupplier, onPress, width, BCTheme.get().getStandardButtonHeight());
    }
    
    /**
     * Create a data-bound label that updates its text automatically
     */
    public static DataLabelComponent createDataLabel(Supplier<String> textSupplier, int color, int width) {
        return new DataLabelComponent(textSupplier, color, width);
    }
    
    /**
     * Create a slot component for item display
     */
    public static SlotComponent createSlot() {
        return new SlotComponent();
    }
} 