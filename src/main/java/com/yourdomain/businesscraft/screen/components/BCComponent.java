package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import java.util.function.Consumer;

/**
 * Enhanced base component for BusinessCraft UI system.
 * Provides common functionality for all UI components including
 * positioning, visibility control, and styling.
 */
public abstract class BCComponent implements UIComponent {
    protected int x, y, width, height;
    protected boolean visible = true;
    protected Component tooltip;
    protected Style style;
    protected int backgroundColor = 0x80000000; // Default semi-transparent black
    protected int borderColor = -1; // -1 means no border
    
    /**
     * Create a new BCComponent with the specified dimensions
     */
    public BCComponent(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    /**
     * Position the component at the specified coordinates
     */
    public BCComponent position(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }
    
    /**
     * Set the component's size
     */
    public BCComponent size(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }
    
    /**
     * Set the component's tooltip
     */
    public BCComponent withTooltip(Component tooltip) {
        this.tooltip = tooltip;
        return this;
    }
    
    /**
     * Set the component's text style
     */
    public BCComponent withStyle(Style style) {
        this.style = style;
        return this;
    }
    
    /**
     * Set the component's background color
     */
    public BCComponent withBackgroundColor(int color) {
        this.backgroundColor = color;
        return this;
    }
    
    /**
     * Set the component's border color
     */
    public BCComponent withBorderColor(int color) {
        this.borderColor = color;
        return this;
    }
    
    /**
     * Draw the component's background if it has one
     */
    protected void renderBackground(GuiGraphics guiGraphics) {
        if (backgroundColor != -1) {
            guiGraphics.fill(x, y, x + width, y + height, backgroundColor);
        }
        
        if (borderColor != -1) {
            // Draw the border (just a rectangle outline)
            guiGraphics.hLine(x, x + width - 1, y, borderColor);
            guiGraphics.hLine(x, x + width - 1, y + height - 1, borderColor);
            guiGraphics.vLine(x, y, y + height - 1, borderColor);
            guiGraphics.vLine(x + width - 1, y, y + height - 1, borderColor);
        }
    }
    
    /**
     * Check if the mouse is over this component
     */
    public boolean isMouseOver(int mouseX, int mouseY) {
        return visible && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
    
    /**
     * Render the tooltip if the mouse is over this component and it has a tooltip
     */
    protected void renderTooltipIfNeeded(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (tooltip != null && isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                net.minecraft.client.Minecraft.getInstance().font,
                tooltip,
                mouseX, mouseY
            );
        }
    }
    
    // UIComponent implementation
    
    @Override
    public void init(Consumer<Button> register) {
        // Default implementation does nothing
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        if (!visible) return;
        
        // Update position
        this.x = x;
        this.y = y;
        
        // Render background
        renderBackground(guiGraphics);
        
        // Render content (implemented by subclasses)
        renderContent(guiGraphics, mouseX, mouseY);
        
        // Render tooltip
        renderTooltipIfNeeded(guiGraphics, mouseX, mouseY);
    }
    
    /**
     * Render the component's content
     * Subclasses must implement this to render their specific content
     */
    protected abstract void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY);
    
    @Override
    public void tick() {
        // Default implementation does nothing
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
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    /**
     * Check if the component is visible
     */
    public boolean isVisible() {
        return visible;
    }
    
    /**
     * Get the component's current x position
     */
    public int getX() {
        return x;
    }
    
    /**
     * Get the component's current y position
     */
    public int getY() {
        return y;
    }
} 