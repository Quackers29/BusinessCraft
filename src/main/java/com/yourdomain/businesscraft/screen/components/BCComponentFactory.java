package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Factory class for creating common BusinessCraft UI components with consistent styling.
 * This factory ensures all components follow the same design language and color scheme.
 */
public class BCComponentFactory {
    // Color constants for consistent styling
    private static final int PRIMARY_COLOR = 0xA0335599;       // Semi-transparent blue
    private static final int SECONDARY_COLOR = 0xA0884466;     // Semi-transparent purple
    private static final int SUCCESS_COLOR = 0xA0339944;       // Semi-transparent green
    private static final int DANGER_COLOR = 0xA0993333;        // Semi-transparent red
    private static final int BACKGROUND_COLOR = 0x80222222;    // Semi-transparent dark gray
    private static final int BORDER_COLOR = 0xA0AAAAAA;        // Light gray
    private static final int TEXT_COLOR = 0xFFFFFFFF;          // White text
    private static final int TEXT_HIGHLIGHT = 0xFFDDFFFF;      // Light cyan highlight text
    
    // Standard dimension constants
    private static final int STANDARD_BUTTON_HEIGHT = 20;
    private static final int SMALL_BUTTON_HEIGHT = 16;
    private static final int LARGE_BUTTON_HEIGHT = 24;
    private static final int STANDARD_PADDING = 8;
    private static final int SMALL_PADDING = 4;
    private static final int LARGE_PADDING = 12;
    
    // Deprecated constants kept for backwards compatibility
    @Deprecated
    public static final int TEXT_LIGHT = 0xFFFFFF;
    
    @Deprecated
    public static final int TEXT_DARK = 0x333333;
    
    @Deprecated
    public static final int TEXT_MUTED = 0x999999;
    
    @Deprecated
    public static final int BUTTON_HEIGHT = STANDARD_BUTTON_HEIGHT;
    
    /**
     * Creates a primary button with consistent styling.
     * 
     * @param translationKey The translation key for the button text
     * @param onPress The action to perform when pressed
     * @param width The width of the button
     * @return A styled primary button
     */
    public static BCButton createPrimaryButton(String translationKey, Consumer<Button> onPress, int width) {
        BCButton button = new BCButton(translationKey, onPress, width, STANDARD_BUTTON_HEIGHT);
        button.withType(BCButton.ButtonType.PRIMARY);
        return button;
    }
    
    /**
     * Creates a secondary button with consistent styling.
     * 
     * @param translationKey The translation key for the button text
     * @param onPress The action to perform when pressed
     * @param width The width of the button
     * @return A styled secondary button
     */
    public static BCButton createSecondaryButton(String translationKey, Consumer<Button> onPress, int width) {
        BCButton button = new BCButton(translationKey, onPress, width, STANDARD_BUTTON_HEIGHT);
        button.withType(BCButton.ButtonType.SECONDARY);
        return button;
    }
    
    /**
     * Creates a danger button with consistent styling.
     * 
     * @param translationKey The translation key for the button text
     * @param onPress The action to perform when pressed
     * @param width The width of the button
     * @return A styled danger button
     */
    public static BCButton createDangerButton(String translationKey, Consumer<Button> onPress, int width) {
        BCButton button = new BCButton(translationKey, onPress, width, STANDARD_BUTTON_HEIGHT);
        button.withType(BCButton.ButtonType.DANGER);
        return button;
    }
    
    /**
     * Creates a success button with consistent styling.
     * 
     * @param translationKey The translation key for the button text
     * @param onPress The action to perform when pressed
     * @param width The width of the button
     * @return A styled success button
     */
    public static BCButton createSuccessButton(String translationKey, Consumer<Button> onPress, int width) {
        BCButton button = new BCButton(translationKey, onPress, width, STANDARD_BUTTON_HEIGHT);
        button.withType(BCButton.ButtonType.SUCCESS);
        return button;
    }
    
    /**
     * Creates an icon button with consistent styling.
     * 
     * @param tooltip The tooltip to display when hovered
     * @param onPress The action to perform when pressed
     * @return A styled icon button
     */
    public static BCButton createIconButton(Component tooltip, Consumer<Button> onPress) {
        BCButton button = new BCButton("", onPress, SMALL_BUTTON_HEIGHT, SMALL_BUTTON_HEIGHT);
        button.withTooltip(tooltip);
        return button;
    }
    
    @Deprecated
    public static BCLabel createHeaderLabel(String translationKey, int width) {
        BCLabel label = new BCLabel(translationKey, width, 12);
        label.withTextColor(TEXT_HIGHLIGHT)
              .withAlignment(BCLabel.TextAlignment.CENTER)
              .withShadow(true);
        return label;
    }
    
    @Deprecated
    public static BCLabel createBodyLabel(String translationKey, int width) {
        BCLabel label = new BCLabel(translationKey, width, 10);
        label.withTextColor(TEXT_COLOR)
              .withAlignment(BCLabel.TextAlignment.LEFT);
        return label;
    }
    
    @Deprecated
    public static BCLabel createDynamicLabel(Supplier<Component> textSupplier, int width) {
        BCLabel label = new BCLabel(textSupplier, width, 10);
        label.withTextColor(TEXT_COLOR)
              .withAlignment(BCLabel.TextAlignment.LEFT);
        return label;
    }
    
    /**
     * Creates a panel with a flow layout.
     */
    public static BCPanel createFlowPanel(int width, int height, BCFlowLayout.Direction direction, int spacing) {
        BCPanel panel = new BCPanel(width, height);
        panel.withLayout(new BCFlowLayout(direction, spacing));
        return panel;
    }
    
    /**
     * Creates a panel with a grid layout.
     */
    public static BCPanel createGridPanel(int width, int height, int columns, int hSpacing, int vSpacing) {
        BCPanel panel = new BCPanel(width, height);
        panel.withLayout(new BCGridLayout(columns, hSpacing, vSpacing));
        return panel;
    }
    
    /**
     * Creates a container panel with consistent styling.
     */
    public static BCPanel createContainerPanel(int width, int height) {
        BCPanel panel = new BCPanel(width, height);
        panel.withBackgroundColor(BACKGROUND_COLOR)
             .withBorderColor(BORDER_COLOR)
             .withCornerRadius(3);
        
        return panel;
    }
    
    /**
     * Creates a tooltip panel with consistent styling.
     */
    public static BCPanel createTooltipPanel(String translationKey, int width) {
        BCPanel panel = new BCPanel(width, 24);
        panel.withBackgroundColor(0xF0100010);
        panel.withBorderColor(0xFF5000FF);
        panel.withPadding(SMALL_PADDING);
        
        BCLabel label = createBodyLabel(translationKey, width - 2 * SMALL_PADDING);
        panel.addChild(label);
        
        return panel;
    }
    
    /**
     * Creates an edit box with consistent styling.
     */
    public static EditBoxComponent createEditBox(int width, Supplier<String> initialText, Consumer<String> onTextChanged, int maxLength) {
        return new EditBoxComponent(width, STANDARD_BUTTON_HEIGHT, initialText, onTextChanged, maxLength);
    }
    
    /**
     * Creates a toggle button with consistent styling.
     */
    public static ToggleButtonComponent createToggleButton(Component text, Consumer<Button> onPress, int width) {
        return new ToggleButtonComponent(0, 0, width, STANDARD_BUTTON_HEIGHT, text, onPress);
    }
    
    /**
     * Creates a data-bound button with consistent styling.
     */
    public static DataBoundButtonComponent createDataBoundButton(Supplier<Component> textSupplier, Consumer<Button> onPress, int width) {
        return new DataBoundButtonComponent(textSupplier, onPress, width, STANDARD_BUTTON_HEIGHT);
    }
    
    /**
     * Creates a data label with consistent styling.
     */
    public static DataLabelComponent createDataLabel(Supplier<String> textSupplier, int color, int width) {
        return new DataLabelComponent(textSupplier, color, width);
    }
    
    /**
     * Creates a slot component.
     */
    public static SlotComponent createSlot() {
        return new SlotComponent();
    }
} 