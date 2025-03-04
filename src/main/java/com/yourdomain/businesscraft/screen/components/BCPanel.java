package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panel component for BusinessCraft UI system.
 * Acts as a container for other UI components with layout management.
 */
public class BCPanel extends BCComponent {
    protected final List<UIComponent> children = new ArrayList<>();
    protected BCLayout layout = null;
    protected int padding = 5; // Default padding
    
    /**
     * Create a new panel with the specified dimensions
     */
    public BCPanel(int width, int height) {
        super(width, height);
    }
    
    /**
     * Add a child component to this panel
     */
    public BCPanel addChild(UIComponent child) {
        children.add(child);
        return this;
    }
    
    /**
     * Remove a child component from this panel
     */
    public BCPanel removeChild(UIComponent child) {
        children.remove(child);
        return this;
    }
    
    /**
     * Clear all child components
     */
    public BCPanel clearChildren() {
        children.clear();
        return this;
    }
    
    /**
     * Set the layout manager for this panel
     */
    public BCPanel withLayout(BCLayout layout) {
        this.layout = layout;
        return this;
    }
    
    /**
     * Set the padding for this panel
     */
    public BCPanel withPadding(int padding) {
        this.padding = padding;
        return this;
    }
    
    /**
     * Layout the children using the current layout manager
     */
    public void layoutChildren() {
        if (layout != null) {
            layout.layout(this, children);
        }
    }
    
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Layout children if a layout manager is set
        layoutChildren();
        
        // Render children
        for (UIComponent child : children) {
            if (child.isVisible()) {
                child.render(guiGraphics, child.getX(), child.getY(), mouseX, mouseY);
            }
        }
    }
    
    @Override
    public void init(Consumer<Button> register) {
        // Initialize all children
        for (UIComponent child : children) {
            child.init(register);
        }
    }
    
    @Override
    public void tick() {
        // Tick all children
        for (UIComponent child : children) {
            child.tick();
        }
    }
    
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        
        // When hiding the panel, also hide all children
        if (!visible) {
            for (UIComponent child : children) {
                child.setVisible(false);
            }
        }
    }
    
    /**
     * Get the inner left position (accounts for padding)
     */
    public int getInnerLeft() {
        return x + padding;
    }
    
    /**
     * Get the inner top position (accounts for padding)
     */
    public int getInnerTop() {
        return y + padding;
    }
    
    /**
     * Get the inner width (accounts for padding)
     */
    public int getInnerWidth() {
        return width - (padding * 2);
    }
    
    /**
     * Get the inner height (accounts for padding)
     */
    public int getInnerHeight() {
        return height - (padding * 2);
    }
    
    /**
     * Get the list of child components
     */
    public List<UIComponent> getChildren() {
        return children;
    }
} 