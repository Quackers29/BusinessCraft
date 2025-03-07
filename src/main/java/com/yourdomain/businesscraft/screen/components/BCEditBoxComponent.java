package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Enhanced edit box component for BusinessCraft UI system.
 * Provides text input functionality with styling and validation.
 */
public class BCEditBoxComponent extends BCComponent {
    private final EditBox editBox;
    private final Supplier<String> textSupplier;
    private final Consumer<String> onTextChanged;
    private final int maxLength;
    
    private int textColor = 0xFFFFFF;
    private int backgroundColor = 0x80000000;
    private int focusedBorderColor = 0xFFFFFFFF;
    private int unfocusedBorderColor = 0x80FFFFFF;
    
    /**
     * Create a new edit box component
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
            width, height,
            Component.empty() // No label
        );
        
        // Configure the edit box
        this.editBox.setMaxLength(maxLength);
        this.editBox.setValue(textSupplier.get());
        this.editBox.setResponder(this::handleTextChange);
        this.editBox.setTextColor(textColor);
        this.editBox.setBordered(true);
        this.editBox.setCanLoseFocus(true);
    }
    
    /**
     * Handle text changes
     */
    private void handleTextChange(String text) {
        if (onTextChanged != null) {
            onTextChanged.accept(text);
        }
        triggerEvent("textChanged");
    }
    
    /**
     * Set the text color
     */
    public BCEditBoxComponent withTextColor(int color) {
        this.textColor = color;
        this.editBox.setTextColor(color);
        return this;
    }
    
    /**
     * Set the background color
     */
    @Override
    public BCEditBoxComponent withBackgroundColor(int color) {
        this.backgroundColor = color;
        return this;
    }
    
    /**
     * Set the border color when focused
     */
    public BCEditBoxComponent withFocusedBorderColor(int color) {
        this.focusedBorderColor = color;
        return this;
    }
    
    /**
     * Set the border color when not focused
     */
    public BCEditBoxComponent withUnfocusedBorderColor(int color) {
        this.unfocusedBorderColor = color;
        return this;
    }
    
    /**
     * Get the current text
     */
    public String getText() {
        return this.editBox.getValue();
    }
    
    /**
     * Set the text
     */
    public void setText(String text) {
        this.editBox.setValue(text);
    }
    
    /**
     * Set focus state for this component
     */
    public void setFocused(boolean focused) {
        this.editBox.setFocused(focused);
        // When focusing, move cursor to end of text for easier editing
        if (focused) {
            this.editBox.moveCursorToEnd();
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver((int)mouseX, (int)mouseY) || !enabled) {
            return false;
        }
        
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
        
        // Update text from supplier if not focused
        if (!this.editBox.isFocused()) {
            String currentText = this.editBox.getValue();
            String suppliedText = textSupplier.get();
            if (!currentText.equals(suppliedText)) {
                this.editBox.setValue(suppliedText);
            }
        }
        
        // Draw a more prominent background
        int bgColor = backgroundColor;
        guiGraphics.fill(x, y, x + width, y + height, bgColor);
        
        // Draw a more visible border
        int borderColor = this.editBox.isFocused() ? 0xFFFFFFFF : 0xFFAAAAAA; // White when focused, light gray otherwise
        
        guiGraphics.hLine(x, x + width - 1, y, borderColor);
        guiGraphics.hLine(x, x + width - 1, y + height - 1, borderColor);
        guiGraphics.vLine(x, y, y + height - 1, borderColor);
        guiGraphics.vLine(x + width - 1, y, y + height - 1, borderColor);
        
        // Ensure the edit box is visible with proper settings
        this.editBox.setBordered(false); // We're drawing our own border
        this.editBox.setEditable(true); // Always editable in a popup
        this.editBox.setTextColor(0xFFFFFFFF); // White text for better contrast
        
        // Render the edit box (without its background)
        this.editBox.render(guiGraphics, mouseX, mouseY, 0);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.editBox.isFocused()) {
            return this.editBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.editBox.isFocused()) {
            return this.editBox.charTyped(codePoint, modifiers);
        }
        return false;
    }
    
    @Override
    public void tick() {
        super.tick();
        this.editBox.tick();
    }
} 