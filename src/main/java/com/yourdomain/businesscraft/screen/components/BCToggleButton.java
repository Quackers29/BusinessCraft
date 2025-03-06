package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

/**
 * A toggle button component that can be switched between an on and off state.
 */
public class BCToggleButton implements UIComponent {
    private int width;
    private int height;
    private Component enabledText;
    private Component disabledText;
    private boolean toggled;
    private Consumer<BCToggleButton> onClick;
    private int x;
    private int y;
    private boolean visible = true;

    /**
     * Creates a new toggle button.
     *
     * @param width The width of the button
     * @param height The height of the button
     * @param enabledText The text to display when toggled on
     * @param disabledText The text to display when toggled off
     * @param initialState The initial toggle state
     * @param onClick Callback for when the button is clicked
     */
    public BCToggleButton(int width, int height, Component enabledText, 
                         Component disabledText, boolean initialState, 
                         Consumer<BCToggleButton> onClick) {
        this.width = width;
        this.height = height;
        this.enabledText = enabledText;
        this.disabledText = disabledText;
        this.toggled = initialState;
        this.onClick = onClick;
    }

    /**
     * Renders the toggle button with x and y offsets.
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, int offsetX, int offsetY) {
        if (!visible) {
            return;
        }
        
        this.x = offsetX;
        this.y = offsetY;
        
        // Determine colors based on toggle state
        int bgColor = toggled ? 0xFF00AA00 : 0xFF555555; // Green when enabled, gray when disabled
        int textColor = 0xFFFFFFFF; // White text
        
        // Draw background
        graphics.fill(offsetX, offsetY, offsetX + width, offsetY + height, bgColor);
        
        // Draw border
        graphics.fill(offsetX, offsetY, offsetX + width, offsetY + 1, 0xFFFFFFFF); // Top
        graphics.fill(offsetX, offsetY + height - 1, offsetX + width, offsetY + height, 0xFFFFFFFF); // Bottom
        graphics.fill(offsetX, offsetY, offsetX + 1, offsetY + height, 0xFFFFFFFF); // Left
        graphics.fill(offsetX + width - 1, offsetY, offsetX + width, offsetY + height, 0xFFFFFFFF); // Right
        
        // Draw text
        Component textToRender = toggled ? enabledText : disabledText;
        int textWidth = Minecraft.getInstance().font.width(textToRender.getString());
        int textX = offsetX + (width - textWidth) / 2;
        int textY = offsetY + (height - 8) / 2;
        
        graphics.drawString(Minecraft.getInstance().font, textToRender, textX, textY, textColor);
    }

    /**
     * Implementation of mouse click handling for internal use
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) {
            return false;
        }
        
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            toggled = !toggled;
            if (onClick != null) {
                onClick.accept(this);
            }
            return true;
        }
        return false;
    }

    /**
     * Gets the toggle state of the button.
     *
     * @return True if toggled on, false if toggled off
     */
    public boolean isToggled() {
        return toggled;
    }

    /**
     * Sets the toggle state of the button.
     *
     * @param toggled The new toggle state
     */
    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
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
        // Nothing to do on tick
    }
    
    @Override
    public void init(Consumer<Button> buttonConsumer) {
        // No regular buttons to register
    }
} 