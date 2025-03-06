package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

/**
 * A text input component that allows the user to enter and edit text.
 */
public class BCEditBoxComponent implements UIComponent {
    private final EditBox editBox;
    private final Consumer<String> onTextChanged;
    private int width;
    private int height;
    private int x;
    private int y;
    private boolean visible = true;

    /**
     * Creates a new edit box component.
     *
     * @param width The width of the edit box
     * @param height The height of the edit box
     * @param placeholder Placeholder text to display when empty
     * @param onTextChanged Callback for when the text changes
     */
    public BCEditBoxComponent(int width, int height, Component placeholder, Consumer<String> onTextChanged) {
        this.width = width;
        this.height = height;
        this.onTextChanged = onTextChanged;
        
        this.editBox = new EditBox(
            Minecraft.getInstance().font,
            0, 0, width, height,
            placeholder
        );
        
        this.editBox.setMaxLength(128);
        this.editBox.setBordered(true);
        this.editBox.setVisible(true);
        this.editBox.setTextColor(0xFFFFFFFF);
        this.editBox.setTextColorUneditable(0xFF888888);
        
        // Set up the change listener
        this.editBox.setResponder(text -> {
            if (this.onTextChanged != null) {
                this.onTextChanged.accept(text);
            }
        });
    }

    /**
     * Renders the edit box with x and y offsets.
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, int offsetX, int offsetY) {
        if (!visible) {
            return;
        }
        
        this.x = offsetX;
        this.y = offsetY;
        
        this.editBox.setX(offsetX);
        this.editBox.setY(offsetY);
        this.editBox.render(graphics, mouseX, mouseY, 0); // partialTick is not used in EditBox
    }
    
    /**
     * Implementation of mouse click handling for internal use
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) {
            return false;
        }
        
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            return this.editBox.mouseClicked(mouseX, mouseY, button);
        }
        
        return false;
    }
    
    /**
     * Handles key press events.
     *
     * @param keyCode The key code
     * @param scanCode The scan code
     * @param modifiers The modifier keys
     * @return True if the key press was handled
     */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible) {
            return false;
        }
        
        return this.editBox.keyPressed(keyCode, scanCode, modifiers);
    }
    
    /**
     * Handles character input events.
     *
     * @param codePoint The character code point
     * @param modifiers The modifier keys
     * @return True if the character input was handled
     */
    public boolean charTyped(char codePoint, int modifiers) {
        if (!visible) {
            return false;
        }
        
        return this.editBox.charTyped(codePoint, modifiers);
    }
    
    /**
     * Gets the current text value.
     *
     * @return The current text value
     */
    public String getValue() {
        return this.editBox.getValue();
    }
    
    /**
     * Sets the text value.
     *
     * @param text The new text value
     */
    public void setValue(String text) {
        this.editBox.setValue(text);
    }
    
    /**
     * Sets whether this edit box is focused.
     *
     * @param focused True to focus this edit box
     */
    public void setFocused(boolean focused) {
        this.editBox.setFocused(focused);
    }
    
    /**
     * Gets whether this edit box is focused.
     *
     * @return True if this edit box is focused
     */
    public boolean isFocused() {
        return this.editBox.isFocused();
    }
    
    /**
     * Sets whether the text can be edited.
     *
     * @param editable True if the text can be edited
     */
    public void setEditable(boolean editable) {
        this.editBox.setEditable(editable);
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        this.editBox.setVisible(visible);
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }
    
    @Override
    public int getWidth() {
        return width;
    }
    
    @Override
    public int getHeight() {
        return height;
    }
    
    @Override
    public void tick() {
        this.editBox.tick();
    }
    
    @Override
    public void init(Consumer<Button> buttonConsumer) {
        // No regular buttons to register
    }
} 