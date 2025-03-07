package com.yourdomain.businesscraft.screen.components;

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
    
    private BCEditBoxComponent inputField;
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
        int contentPadding = 15;
        int titleHeight = 24;
        int buttonHeight = 25;  // Taller buttons for visibility
        int buttonWidth = 100;
        int inputFieldHeight = 20;
        int spacing = 20;
        
        // Title label - centered
        BCLabel titleLabel = BCComponentFactory.createHeaderLabel(this.title, this.width - (contentPadding * 2));
        titleLabel.position((this.width - titleLabel.getWidth()) / 2, contentPadding);
        this.addChild(titleLabel);
        
        // Input field
        if (type == PopupType.STRING_INPUT || type == PopupType.NUMERIC_INPUT) {
            // Input field label - explicit instruction
            BCLabel instructionLabel = BCComponentFactory.createBodyLabel("Enter new name:", this.width - (contentPadding * 2));
            instructionLabel.position(contentPadding, contentPadding + titleHeight + 10);
            this.addChild(instructionLabel);
            
            // Create a text supplier and change handler
            Supplier<String> textSupplier = () -> currentInputValue;
            Consumer<String> textChangeHandler = text -> {
                // For numeric input, validate the text
                if (type == PopupType.NUMERIC_INPUT) {
                    if (text.isEmpty() || text.matches("^\\d+$")) {
                        currentInputValue = text;
                    }
                } else {
                    currentInputValue = text;
                }
            };
            
            // Create the edit box with the supplier and handler
            inputField = new BCEditBoxComponent(
                this.width - (contentPadding * 2), 
                inputFieldHeight + 8,
                textSupplier,
                textChangeHandler,
                100 // Max length
            );
            // Position below the instruction label
            inputField.position(contentPadding, contentPadding + titleHeight + 35);
            
            // Style the input field for better visibility
            inputField.withBackgroundColor(0xFF555555);
            inputField.withTextColor(0xFFFFFFFF);
            inputField.withFocusedBorderColor(0xFFAAAAAA);
            
            this.addChild(inputField);
        }
        
        // Buttons - position at bottom with fixed spacing
        int totalButtonWidth = (buttonWidth * 2) + spacing;
        int buttonY = this.height - buttonHeight - contentPadding;
        
        // Back button with brighter colors
        backButton = BCComponentFactory.createSecondaryButton(
            "Back", 
            button -> handleBackButton(), 
            buttonWidth
        );
        backButton.position((this.width - totalButtonWidth) / 2, buttonY);
        backButton.size(buttonWidth, buttonHeight); // Explicit sizing
        this.addChild(backButton);
        
        // OK button with brighter colors
        okButton = BCComponentFactory.createPrimaryButton(
            "OK", 
            button -> handleOkButton(), 
            buttonWidth
        );
        okButton.position(backButton.getX() + buttonWidth + spacing, buttonY);
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
        currentInputValue = value;
        if (inputField != null) {
            inputField.setText(value);
        }
    }
    
    /**
     * Focus the input field for immediate keyboard input
     */
    public void focusInput() {
        if (inputField != null) {
            inputField.setFocused(true);
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
        // Draw background at our stored position - this is the first layer
        renderBackground(guiGraphics);
        
        // Render title - second layer
        if (title != null) {
            guiGraphics.drawCenteredString(
                Minecraft.getInstance().font,
                Component.literal(title),
                this.x + this.width / 2,
                this.y + 15,
                0xFFFFFFFF
            );
        }
        
        // Render instruction text - third layer
        if (type == PopupType.STRING_INPUT || type == PopupType.NUMERIC_INPUT) {
            guiGraphics.drawString(
                Minecraft.getInstance().font,
                "Enter new name:",
                this.x + 15,
                this.y + 45,
                0xFFE0E0E0
            );
        }
        
        // Render input field - fourth layer
        if (type == PopupType.STRING_INPUT || type == PopupType.NUMERIC_INPUT) {
            int textFieldX = this.x + 15;
            int textFieldY = this.y + 60;
            int textFieldWidth = this.width - 30;
            int textFieldHeight = 20;
            
            // Use our new utility to render the input field
            UIDirectRenderer.renderInputField(
                guiGraphics,
                currentInputValue,
                textFieldX,
                textFieldY,
                textFieldWidth,
                textFieldHeight,
                true // Always focused in a popup
            );
        }
        
        // Render Back button - fifth layer
        int buttonY = this.y + this.height - 35;
        UIDirectRenderer.renderButton(
            guiGraphics,
            "Back",
            this.x + (this.width / 2) - 110,
            buttonY,
            100,
            20,
            false, // Secondary style
            mouseX,
            mouseY
        );
        
        // Render OK button - sixth layer
        UIDirectRenderer.renderButton(
            guiGraphics,
            "OK",
            this.x + (this.width / 2) + 10,
            buttonY,
            100,
            20,
            true, // Primary style
            mouseX,
            mouseY
        );
    }
    
    /**
     * Draw the popup's background with the original BusinessCraft style
     */
    @Override
    protected void renderBackground(GuiGraphics guiGraphics) {
        // Use our new utility for panel background rendering
        UIDirectRenderer.renderPanelBackground(
            guiGraphics,
            x, y, width, height,
            true, // With header
            true  // Is dialog
        );
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            // Check if clicked on input field
            if (type == PopupType.STRING_INPUT || type == PopupType.NUMERIC_INPUT) {
                if (mouseX >= this.x + 15 && mouseX < this.x + this.width - 15 &&
                    mouseY >= this.y + 60 && mouseY < this.y + 80) {
                    // Input field clicked - already focused in a popup
                    return true;
                }
            }
            
            // Check if clicked on Back button using utility
            if (UIDirectRenderer.handleButtonClick(
                    mouseX, mouseY,
                    this.x + (this.width / 2) - 110,
                    this.y + this.height - 35,
                    100, 20,
                    btn -> handleBackButton()
                )) {
                return true;
            }
            
            // Check if clicked on OK button using utility
            if (UIDirectRenderer.handleButtonClick(
                    mouseX, mouseY,
                    this.x + (this.width / 2) + 10,
                    this.y + this.height - 35,
                    100, 20,
                    btn -> handleOkButton()
                )) {
                return true;
            }
        }
        
        // If click is inside popup but not on a control, consume the event
        if (mouseX >= this.x && mouseX < this.x + this.width &&
            mouseY >= this.y && mouseY < this.y + this.height) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Handle key presses
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Check for escape key to cancel
        if (keyCode == 256) { // ESCAPE
            handleBackButton();
            return true;
        }
        
        // Check for enter key to confirm
        if (keyCode == 257 || keyCode == 335) { // ENTER or NUMPAD ENTER
            handleOkButton();
            return true;
        }
        
        // Handle text input for the edit box
        if (type == PopupType.STRING_INPUT || type == PopupType.NUMERIC_INPUT) {
            // Only handle letter/number keys and backspace
            if (keyCode == 259) { // BACKSPACE
                if (!currentInputValue.isEmpty()) {
                    currentInputValue = currentInputValue.substring(0, currentInputValue.length() - 1);
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Handle character input
     */
    @Override
    public boolean charTyped(char c, int modifiers) {
        // Forward to input field
        if (type == PopupType.STRING_INPUT || type == PopupType.NUMERIC_INPUT) {
            // For numeric input, only accept digits
            if (type == PopupType.NUMERIC_INPUT) {
                if (Character.isDigit(c)) {
                    currentInputValue += c;
                    return true;
                }
            } else {
                // For string input, accept any printable character
                if (c >= 32 && c <= 126) { // ASCII printable characters
                    currentInputValue += c;
                    return true;
                }
            }
        }
        
        return false;
    }
} 