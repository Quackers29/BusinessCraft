package com.quackers29.businesscraft.ui.modal.core;

import com.quackers29.businesscraft.ui.builders.UIGridBuilder;
import com.quackers29.businesscraft.ui.builders.BCComponentFactory;
import com.quackers29.businesscraft.ui.components.basic.BCLabel;

import com.quackers29.businesscraft.ui.components.basic.BCPanel;
import com.quackers29.businesscraft.ui.components.basic.BCButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A popup dialog component that can be used to collect user input.
 * Supports different types of inputs (string, numeric) with customizable title and buttons.
 */
public class BCPopupScreen extends BCPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(BCPopupScreen.class);
    
    // Popup types
    public enum PopupType {
        STRING_INPUT,
        NUMERIC_INPUT,
        CONFIRMATION
    }
    
    // Result data returned by the popup
    public static class PopupResult {
        private final boolean confirmed;
        private final String stringValue;
        private final int numericValue;
        
        private PopupResult(boolean confirmed, String stringValue, int numericValue) {
            this.confirmed = confirmed;
            this.stringValue = stringValue;
            this.numericValue = numericValue;
        }
        
        public boolean isConfirmed() {
            return confirmed;
        }
        
        public String getStringValue() {
            return stringValue;
        }
        
        public int getNumericValue() {
            return numericValue;
        }
        
        public static PopupResult confirmed(String value) {
            return new PopupResult(true, value, 0);
        }
        
        public static PopupResult confirmed(int value) {
            return new PopupResult(true, String.valueOf(value), value);
        }
        
        public static PopupResult cancelled() {
            return new PopupResult(false, "", 0);
        }
    }
    
    private final PopupType type;
    private final String title;
    private final Consumer<PopupResult> resultCallback;
    
    private EditBox vanillaEditBox;
    private BCButton backButton;
    private BCButton okButton;
    private Consumer<Button> closePopupHandler;
    private String currentInputValue = "";
    
    // For positioning relative to parent screen
    private int parentX;
    private int parentY;
    private int parentWidth;
    private int parentHeight;
    
    /**
     * Create a new popup dialog
     * 
     * @param type The type of input to collect
     * @param title The title of the popup
     * @param resultCallback Callback that receives the result when the popup is closed
     * @param width The width of the popup
     * @param height The height of the popup
     */
    public BCPopupScreen(PopupType type, String title, Consumer<PopupResult> resultCallback, int width, int height) {
        super(width, height);
        this.type = type;
        this.title = title;
        this.resultCallback = resultCallback;
        
        // Style the popup with fully opaque background
        this.withBackgroundColor(0xFF222222) // Fully opaque dark background
            .withBorderColor(0xFFAAAAAA)     // Light gray border
            .withCornerRadius(5);            // Slightly rounded corners
        
        // Create popup content
        createContent();
        
        // Center in screen by default
        Minecraft minecraft = Minecraft.getInstance();
        setParentBounds(0, 0, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
    }
    
    /**
     * Set the parent screen dimensions for centering purposes
     */
    public void setParentBounds(int x, int y, int width, int height) {
        this.parentX = x;
        this.parentY = y;
        this.parentWidth = width;
        this.parentHeight = height;
        
        // Position popup exactly in the center of the parent screen
        // This calculation gives us the center position
        int centerX = parentX + (parentWidth / 2);
        int centerY = parentY + (parentHeight / 2);
        
        // Adjust to top-left corner based on popup dimensions
        int posX = centerX - (this.width / 2);
        int posY = centerY - (this.height / 2);
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Positioning popup at ({}, {}) - center of parent at ({}, {})", 
                         posX, posY, centerX, centerY);
        }
        
        // Use absolute positioning
        this.position(posX, posY);
    }
    
    /**
     * Create the content of the popup based on its type
     */
    private void createContent() {
        int contentPadding = 20;
        int titleHeight = 30;
        int buttonHeight = 20;
        int buttonWidth = 100;
        int inputFieldHeight = 20;
        int spacing = 10;
        
        // Calculate positions and dimensions
        int contentWidth = this.width - (contentPadding * 2);
        
        // Create the input field for input popups
        if (type == PopupType.STRING_INPUT || type == PopupType.NUMERIC_INPUT) {
            // Create vanilla EditBox with proper dimensions
            vanillaEditBox = new EditBox(
                Minecraft.getInstance().font,
                contentPadding,  // initial x position 
                contentPadding + titleHeight + 40, // initial y position
                contentWidth, // full content width
                inputFieldHeight, // standard height
                Component.empty() // no label
            );
            
            // Configure the vanilla EditBox
            vanillaEditBox.setMaxLength(100);
            vanillaEditBox.setBordered(true);
            vanillaEditBox.setTextColor(0xFFFFFFFF); // White text
            vanillaEditBox.setValue(currentInputValue);
            vanillaEditBox.setTextColorUneditable(0xFFAAAAAA); // Gray when not editable
            
            // Set responder to update cached value
            vanillaEditBox.setResponder(text -> {
                if (type == PopupType.NUMERIC_INPUT) {
                    // For numeric input, validate that it contains only digits
                    if (text.isEmpty() || text.matches("^\\d+$")) {
                        currentInputValue = text;
                    } else {
                        // Reject invalid input
                        vanillaEditBox.setValue(currentInputValue);
                    }
                } else {
                    // For text input, accept any string
                    currentInputValue = text;
                }
            });
        }
        
        // Title label - centered
        BCLabel titleLabel = BCComponentFactory.createHeaderLabel(this.title, this.width - (contentPadding * 2));
        titleLabel.position((this.width - titleLabel.getWidth()) / 2, contentPadding);
        this.addChild(titleLabel);
        
        // Buttons - position at bottom with fixed spacing
        int totalButtonWidth = (buttonWidth * 2) + spacing;
        int buttonY = this.height - buttonHeight - contentPadding;
        int startX = (this.width - totalButtonWidth) / 2; // Center the buttons horizontally
        
        // Back button with brighter colors
        backButton = BCComponentFactory.createSecondaryButton(
            "Back", 
            button -> handleBackButton(), 
            buttonWidth
        );
        backButton.position(startX, buttonY);
        backButton.size(buttonWidth, buttonHeight); // Explicit sizing
        this.addChild(backButton);
        
        // OK button with brighter colors
        okButton = BCComponentFactory.createPrimaryButton(
            "OK", 
            button -> handleOkButton(), 
            buttonWidth
        );
        okButton.position(startX + buttonWidth + spacing, buttonY);
        okButton.size(buttonWidth, buttonHeight); // Explicit sizing
        this.addChild(okButton);
    }
    
    /**
     * Handle the back button click
     */
    private void handleBackButton() {
        if (resultCallback != null) {
            resultCallback.accept(PopupResult.cancelled());
        }
        
        if (closePopupHandler != null) {
            closePopupHandler.accept(null); 
        }
    }
    
    /**
     * Handle the OK button click
     */
    private void handleOkButton() {
        if (resultCallback != null) {
            switch (type) {
                case STRING_INPUT:
                    resultCallback.accept(PopupResult.confirmed(currentInputValue));
                    break;
                case NUMERIC_INPUT:
                    int numericValue = currentInputValue.isEmpty() ? 0 : Integer.parseInt(currentInputValue);
                    resultCallback.accept(PopupResult.confirmed(numericValue));
                    break;
                case CONFIRMATION:
                    resultCallback.accept(PopupResult.confirmed(""));
                    break;
            }
        }
        
        if (closePopupHandler != null) {
            closePopupHandler.accept(null);
        }
    }
    
    /**
     * Set the handler for closing the popup
     */
    public void setClosePopupHandler(Consumer<Button> closePopupHandler) {
        this.closePopupHandler = closePopupHandler;
    }
    
    /**
     * Set the initial value for the input field
     */
    public void setInitialValue(String value) {
        // Update our cached value
        currentInputValue = value != null ? value : "";
        
        // Update the EditBox directly
        if (vanillaEditBox != null) {
            vanillaEditBox.setValue(currentInputValue);
        }
    }
    
    /**
     * Focus the input field for immediate keyboard input
     */
    public void focusInput() {
        if (vanillaEditBox != null) {
            vanillaEditBox.setFocused(true);
        }
    }
    
    /**
     * Static method to easily create a string input popup
     */
    public static BCPopupScreen createStringInputPopup(String title, Consumer<PopupResult> resultCallback) {
        return new BCPopupScreen(PopupType.STRING_INPUT, title, resultCallback, 300, 150);
    }
    
    /**
     * Static method to easily create a numeric input popup
     */
    public static BCPopupScreen createNumericInputPopup(String title, Consumer<PopupResult> resultCallback) {
        return new BCPopupScreen(PopupType.NUMERIC_INPUT, title, resultCallback, 300, 150);
    }
    
    /**
     * Static method to easily create a confirmation popup
     */
    public static BCPopupScreen createConfirmationPopup(String title, Consumer<PopupResult> resultCallback) {
        return new BCPopupScreen(PopupType.CONFIRMATION, title, resultCallback, 300, 150);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        // Calculate screen and popup dimensions
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int popupX = (screenWidth - this.width) / 2;
        int popupY = (screenHeight - this.height) / 2;
        
        // Push pose matrix for proper Z-ordering
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 100);
        
        // Draw semi-transparent overlay behind popup
        guiGraphics.fill(0, 0, screenWidth, screenHeight, 0xA0000000);
        
        // Draw popup background (dark gray)
        guiGraphics.fill(popupX, popupY, popupX + this.width, popupY + this.height, 0xFF333333);
        
        // Draw border around popup
        guiGraphics.hLine(popupX, popupX + this.width - 1, popupY, 0xFFFFFFFF);
        guiGraphics.hLine(popupX, popupX + this.width - 1, popupY + this.height - 1, 0xFFFFFFFF);
        guiGraphics.vLine(popupX, popupY, popupY + this.height - 1, 0xFFFFFFFF);
        guiGraphics.vLine(popupX + this.width - 1, popupY, popupY + this.height - 1, 0xFFFFFFFF);
        
        // Define layout constants
        final int padding = 20;
        final int titleHeight = 30;
        
        // Draw title - centered at top
        guiGraphics.drawCenteredString(
            Minecraft.getInstance().font,
            Component.literal(this.title),
            popupX + this.width / 2,
            popupY + padding,
            0xFFFFFFFF
        );
        
        // Draw instruction text if this is an input popup
        if (type == PopupType.STRING_INPUT || type == PopupType.NUMERIC_INPUT) {
            String instruction = "Enter " + (type == PopupType.NUMERIC_INPUT ? "value:" : "text:");
            guiGraphics.drawString(
                Minecraft.getInstance().font,
                instruction,
                popupX + padding,
                popupY + padding + titleHeight,
                0xFFFFFFFF
            );
        }
        
        // Render EditBox with proper positioning
        if (vanillaEditBox != null) {
            // Position the EditBox properly
            int editBoxY = popupY + padding + titleHeight + 20;
            vanillaEditBox.setX(popupX + padding);
            vanillaEditBox.setY(editBoxY);
            vanillaEditBox.setWidth(this.width - (padding * 2));
            
            // Draw white border around EditBox for visibility
            int editBoxHeight = 20;
            guiGraphics.fill(
                popupX + padding - 1, 
                editBoxY - 1, 
                popupX + padding + this.width - (padding * 2) + 1, 
                editBoxY + editBoxHeight + 1, 
                0xFFFFFFFF
            );
            
            // Render the EditBox
            vanillaEditBox.render(guiGraphics, mouseX, mouseY, 0);
        }
        
        // Render buttons
        int buttonY = popupY + this.height - padding - 20;
        int buttonSpacing = 10;
        int buttonWidth = 100;
        int totalButtonWidth = (buttonWidth * 2) + buttonSpacing;
        int buttonStartX = popupX + (this.width - totalButtonWidth) / 2;
        
        // Cancel button (blue)
        guiGraphics.fill(
            buttonStartX, 
            buttonY, 
            buttonStartX + buttonWidth, 
            buttonY + 20, 
            0xFF5555AA // Blue shade matching your UI
        );
        guiGraphics.drawCenteredString(
            Minecraft.getInstance().font,
            Component.literal("Cancel"),
            buttonStartX + buttonWidth / 2,
            buttonY + 6,
            0xFFFFFFFF
        );
        
        // OK button (green)
        guiGraphics.fill(
            buttonStartX + buttonWidth + buttonSpacing, 
            buttonY, 
            buttonStartX + buttonWidth * 2 + buttonSpacing, 
            buttonY + 20, 
            0xFF55AA55 // Green shade matching your UI
        );
        guiGraphics.drawCenteredString(
            Minecraft.getInstance().font,
            Component.literal("OK"),
            buttonStartX + buttonWidth + buttonSpacing + buttonWidth / 2,
            buttonY + 6,
            0xFFFFFFFF
        );
        
        // Restore the pose
        guiGraphics.pose().popPose();
    }
    
    /**
     * Draw the popup's background with the original BusinessCraft style
     */
    @Override
    protected void renderBackground(GuiGraphics guiGraphics) {
        // Main background fill - fully opaque
        guiGraphics.fill(x, y, x + width, y + height, 0xFF222222);
        
        // Original border style - fully opaque
        int borderColor = 0xFFAAAAAA;
        guiGraphics.hLine(x, x + width - 1, y, borderColor);
        guiGraphics.hLine(x, x + width - 1, y + height - 1, borderColor);
        guiGraphics.vLine(x, y, y + height - 1, borderColor);
        guiGraphics.vLine(x + width - 1, y, y + height - 1, borderColor);
        
        // Header gradient in original blue colors - fully opaque
        guiGraphics.fillGradient(x + 1, y + 1, x + width - 1, y + 30, 0xFF335599, 0xFF223366);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Only handle left clicks
        if (button != 0) {
            return false;
        }
        
        // Calculate positions
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int popupX = (screenWidth - this.width) / 2;
        int popupY = (screenHeight - this.height) / 2;
        
        // Button dimensions and positions
        int padding = 20;
        int buttonY = popupY + this.height - padding - 20;
        int buttonSpacing = 10;
        int buttonWidth = 100;
        int buttonHeight = 20;
        int totalButtonWidth = (buttonWidth * 2) + buttonSpacing;
        int buttonStartX = popupX + (this.width - totalButtonWidth) / 2;
        
        // Check EditBox click first
        if (vanillaEditBox != null && vanillaEditBox.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        // Check Cancel button click
        if (isPointInRegion(
                buttonStartX, buttonY, 
                buttonWidth, buttonHeight, 
                mouseX, mouseY)) {
            handleBackButton();
            return true;
        }
        
        // Check OK button click
        if (isPointInRegion(
                buttonStartX + buttonWidth + buttonSpacing, buttonY, 
                buttonWidth, buttonHeight, 
                mouseX, mouseY)) {
            handleOkButton();
            return true;
        }
        
        // If the click is inside the popup, consume the event
        if (isPointInRegion(
                popupX, popupY, 
                this.width, this.height, 
                mouseX, mouseY)) {
            return true;
        }
        
        // Click outside the popup - treat as cancel
        handleBackButton();
        return true;
    }
    
    /**
     * Helper method to check if a point is within a rectangular region
     */
    private boolean isPointInRegion(int x, int y, int width, int height, double pointX, double pointY) {
        return pointX >= x && pointX < x + width && 
               pointY >= y && pointY < y + height;
    }
    
    /**
     * Handle key press events for this popup
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // First check if the EditBox wants to handle the key
        if (vanillaEditBox != null && vanillaEditBox.isFocused() && 
            vanillaEditBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        
        // Handle special keys
        switch (keyCode) {
            case 256: // ESC
                handleBackButton();
                return true;
            case 257: // ENTER
            case 335: // NUMPAD ENTER
                handleOkButton();
                return true;
            case 69:  // 'E' key - prevent inventory screen from opening
                if (isInputPopup()) {
                    return true;
                }
                break;
        }
        
        // Consume all keypresses for input popups
        return isInputPopup();
    }
    
    /**
     * Handle character type events for this popup
     */
    @Override
    public boolean charTyped(char c, int modifiers) {
        // Let the EditBox handle character input if it's focused
        if (vanillaEditBox != null && vanillaEditBox.charTyped(c, modifiers)) {
            return true;
        }
        
        // Consume all character events in input popups
        return isInputPopup();
    }
    
    /**
     * Check if this popup is used for text input
     * 
     * @return true if this is a STRING_INPUT or NUMERIC_INPUT popup
     */
    public boolean isInputPopup() {
        return type == PopupType.STRING_INPUT || type == PopupType.NUMERIC_INPUT;
    }
    
} 