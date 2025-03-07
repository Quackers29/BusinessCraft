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
 * Enhanced edit box component for BusinessCraft UI system.
 * Provides text input functionality with styling and validation.
 * 
 * Features:
 * - Custom styling for focused and unfocused states
 * - Data binding with a text supplier and change handler
 * - Automatic text synchronization when not focused
 * - Proper focus and keyboard event handling
 */
public class BCEditBoxComponent extends BCComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(BCEditBoxComponent.class);
    
    private final EditBox editBox;
    private final Supplier<String> textSupplier;
    private final Consumer<String> onTextChanged;
    private final int maxLength;
    
    private int textColor = 0xFFFFFFFF; // White text for better visibility
    private int backgroundColor = 0x80000000; // Semi-transparent black background
    private int focusedBorderColor = 0xFFFFFFFF; // White border when focused
    private int unfocusedBorderColor = 0xFFAAAAAA; // Gray border when not focused
    
    /**
     * Create a new edit box component with data binding
     * 
     * @param width Width of the edit box
     * @param height Height of the edit box
     * @param textSupplier Supplier that provides the current text value
     * @param onTextChanged Consumer that receives text changes
     * @param maxLength Maximum allowed text length
     */
    public BCEditBoxComponent(int width, int height, Supplier<String> textSupplier, Consumer<String> onTextChanged, int maxLength) {
        super(width, height);
        this.textSupplier = textSupplier;
        this.onTextChanged = onTextChanged;
        this.maxLength = maxLength;
        
        // Create the vanilla edit box
        this.editBox = new EditBox(
            Minecraft.getInstance().font,
            0, 0, // Position will be set in render
            width - 8, // Account for padding
            height - 4, // Account for vertical centering
            Component.empty() // No label
        );
        
        // Configure the edit box
        this.editBox.setMaxLength(maxLength);
        this.editBox.setValue(textSupplier != null ? textSupplier.get() : "");
        this.editBox.setResponder(this::handleTextChange);
        this.editBox.setTextColor(textColor);
        this.editBox.setBordered(false); // We'll draw our own border
        this.editBox.setCanLoseFocus(true);
    }
    
    /**
     * Handle text changes and propagate to the registered handler
     */
    private void handleTextChange(String text) {
        if (onTextChanged != null) {
            try {
                onTextChanged.accept(text);
            } catch (Exception e) {
                LOGGER.error("Error in text change handler: {}", e.getMessage());
            }
        }
        triggerEvent("textChanged");
    }
    
    /**
     * Set the text color
     * 
     * @param color Text color (ARGB format)
     * @return This component for method chaining
     */
    public BCEditBoxComponent withTextColor(int color) {
        this.textColor = color;
        this.editBox.setTextColor(color);
        return this;
    }
    
    /**
     * Set the background color
     * 
     * @param color Background color (ARGB format)
     * @return This component for method chaining
     */
    @Override
    public BCEditBoxComponent withBackgroundColor(int color) {
        this.backgroundColor = color;
        return this;
    }
    
    /**
     * Set the border color when focused
     * 
     * @param color Border color when focused (ARGB format)
     * @return This component for method chaining
     */
    public BCEditBoxComponent withFocusedBorderColor(int color) {
        this.focusedBorderColor = color;
        return this;
    }
    
    /**
     * Set the border color when not focused
     * 
     * @param color Border color when not focused (ARGB format)
     * @return This component for method chaining
     */
    public BCEditBoxComponent withUnfocusedBorderColor(int color) {
        this.unfocusedBorderColor = color;
        return this;
    }
    
    /**
     * Get the current text value
     * 
     * @return Current text in the edit box
     */
    public String getText() {
        return this.editBox.getValue();
    }
    
    /**
     * Set the text value
     * 
     * @param text Text to set
     */
    public void setText(String text) {
        this.editBox.setValue(text != null ? text : "");
    }
    
    /**
     * Check if the edit box is currently focused
     * 
     * @return true if focused, false otherwise
     */
    @Override
    public boolean isFocused() {
        return this.editBox.isFocused();
    }
    
    /**
     * Set focus state for this component
     * 
     * @param focused Whether this component should be focused
     */
    @Override
    public void setFocused(boolean focused) {
        this.editBox.setFocused(focused);
        super.setFocused(focused);
        
        // When focusing, move cursor to end of text for easier editing
        if (focused) {
            this.editBox.moveCursorToEnd();
        }
        
        // Trigger focus events
        if (focused) {
            triggerEvent("focus");
        } else {
            triggerEvent("blur");
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Only handle clicks if enabled and visible
        if (!isMouseOver((int)mouseX, (int)mouseY) || !enabled || !visible) {
            return false;
        }
        
        // Let the edit box handle the click first
        boolean handled = this.editBox.mouseClicked(mouseX, mouseY, button);
        if (handled) {
            setFocused(true);
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Update the editBox position to match our position - adjust for better visibility
        this.editBox.setX(x + 4);
        this.editBox.setY(y + (height - 10) / 2); // Center vertically
        this.editBox.setWidth(width - 8);
        
        // Update text from supplier if not focused and supplier exists
        if (!this.editBox.isFocused() && textSupplier != null) {
            String currentText = this.editBox.getValue();
            String suppliedText = textSupplier.get();
            if (suppliedText != null && !currentText.equals(suppliedText)) {
                this.editBox.setValue(suppliedText);
            }
        }
        
        // Draw a more prominent background
        guiGraphics.fill(x, y, x + width, y + height, backgroundColor);
        
        // Draw a more visible border with appropriate color based on focus state
        int borderColor = this.editBox.isFocused() ? focusedBorderColor : unfocusedBorderColor;
        
        guiGraphics.hLine(x, x + width - 1, y, borderColor);
        guiGraphics.hLine(x, x + width - 1, y + height - 1, borderColor);
        guiGraphics.vLine(x, y, y + height - 1, borderColor);
        guiGraphics.vLine(x + width - 1, y, y + height - 1, borderColor);
        
        // Ensure the edit box is visible with proper settings
        this.editBox.setBordered(false); // We're drawing our own border
        this.editBox.setEditable(enabled); // Only editable if component is enabled
        this.editBox.setTextColor(textColor);
        
        // Render the edit box (without its background)
        this.editBox.render(guiGraphics, mouseX, mouseY, 0);
    }
    
    /**
     * Handle key press events when the edit box is focused
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.editBox.isFocused() && enabled && visible) {
            return this.editBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }
    
    /**
     * Handle character input events when the edit box is focused
     */
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.editBox.isFocused() && enabled && visible) {
            return this.editBox.charTyped(codePoint, modifiers);
        }
        return false;
    }
    
    /**
     * Update animation and cursor blink state
     */
    @Override
    public void tick() {
        super.tick();
        if (visible) {
            this.editBox.tick();
        }
    }
} 