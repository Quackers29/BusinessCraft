package com.quackers29.businesscraft.ui.components.basic;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import com.quackers29.businesscraft.ui.util.BCRenderUtils;

/**
 * Enhanced base component for BusinessCraft UI system.
 * Provides common functionality for all UI components including
 * positioning, visibility control, styling, and event handling.
 */
public abstract class BCComponent implements UIComponent {
    // Basic properties
    protected int x, y, width, height;
    protected boolean visible = true;
    protected Component tooltip;
    protected Style style;
    protected int backgroundColor = 0x80000000; // Default semi-transparent black
    protected int borderColor = -1; // -1 means no border
    protected int cornerRadius = 0; // 0 means no rounded corners
    
    // State management
    protected boolean enabled = true;
    protected boolean focused = false;
    protected boolean hovered = false;
    protected String id = "";
    
    // Animation properties
    protected float alpha = 1.0f;
    protected long animationStartTime = 0;
    protected long animationDuration = 0;
    protected float animationStartValue = 0;
    protected float animationEndValue = 0;
    protected String animationProperty = "";
    
    // Event handlers
    protected Map<String, List<Consumer<BCComponent>>> eventHandlers = new HashMap<>();
    
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
     * Set the component's corner radius for rounded corners
     */
    public BCComponent withCornerRadius(int radius) {
        this.cornerRadius = radius;
        return this;
    }
    
    /**
     * Set the component's ID (useful for finding components in a parent container)
     */
    public BCComponent withId(String id) {
        this.id = id;
        return this;
    }
    
    /**
     * Enable or disable the component
     */
    public BCComponent setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }
    
    /**
     * Set the component's opacity (0.0 - 1.0)
     */
    public BCComponent withAlpha(float alpha) {
        this.alpha = Math.max(0, Math.min(1, alpha));
        return this;
    }
    
    /**
     * Start an animation for a property
     */
    public void animate(String property, float endValue, long durationMs) {
        this.animationStartTime = System.currentTimeMillis();
        this.animationDuration = durationMs;
        this.animationProperty = property;
        
        // Set start value based on current property
        switch (property) {
            case "alpha":
                this.animationStartValue = this.alpha;
                break;
            case "x":
                this.animationStartValue = this.x;
                break;
            case "y":
                this.animationStartValue = this.y;
                break;
            // Add more properties as needed
        }
        
        this.animationEndValue = endValue;
    }
    
    /**
     * Update animations if active
     */
    protected void updateAnimation() {
        if (animationDuration <= 0 || animationProperty.isEmpty()) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - animationStartTime;
        
        if (elapsedTime >= animationDuration) {
            // Animation complete, set final value
            applyAnimationValue(animationProperty, animationEndValue);
            // Reset animation state
            animationDuration = 0;
            return;
        }
        
        // Calculate current value based on linear interpolation
        float progress = (float) elapsedTime / animationDuration;
        float currentValue = animationStartValue + (animationEndValue - animationStartValue) * progress;
        
        // Apply the current animation value
        applyAnimationValue(animationProperty, currentValue);
    }
    
    /**
     * Apply an animation value to the appropriate property
     */
    private void applyAnimationValue(String property, float value) {
        switch (property) {
            case "alpha":
                this.alpha = value;
                break;
            case "x":
                this.x = (int) value;
                break;
            case "y":
                this.y = (int) value;
                break;
            // Add more properties as needed
        }
    }
    
    /**
     * Render the component's background (if backgroundColor is set)
     * and border (if borderColor is set)
     */
    protected void renderBackground(GuiGraphics guiGraphics) {
        if (backgroundColor != 0x00000000) {
            int bgColor = applyAlpha(backgroundColor);
            
            if (cornerRadius > 0) {
                // Use the utility method to draw a rounded background with border
                BCRenderUtils.drawRoundedBox(guiGraphics, x, y, x + width, y + height, 
                                            cornerRadius, bgColor, borderColor);
            } else {
                // Use the utility method to draw a regular box with border
                BCRenderUtils.drawBox(guiGraphics, x, y, x + width, y + height, 
                                     bgColor, borderColor);
            }
        } else if (borderColor != -1) {
            // If there's no background but there is a border, just draw the border
            if (cornerRadius > 0) {
                // Draw a rounded outline
                BCRenderUtils.drawRoundedBox(guiGraphics, x, y, x + width, y + height, 
                                            cornerRadius, 0x00000000, borderColor);
            } else {
                // Just draw a regular border
                guiGraphics.hLine(x, x + width - 1, y, borderColor);
                guiGraphics.hLine(x, x + width - 1, y + height - 1, borderColor);
                guiGraphics.vLine(x, y, y + height - 1, borderColor);
                guiGraphics.vLine(x + width - 1, y, y + height - 1, borderColor);
            }
        }
    }
    
    /**
     * Apply the current alpha value to a color
     */
    protected int applyAlpha(int color) {
        if (alpha >= 1.0f) return color;
        
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        
        a = (int)(a * alpha);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    /**
     * Check if the mouse is over this component
     */
    public boolean isMouseOver(int mouseX, int mouseY) {
        return visible && enabled && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
    
    /**
     * Handle mouse hover state changes
     */
    public void checkHovered(int mouseX, int mouseY) {
        boolean isNowHovered = isMouseOver(mouseX, mouseY);
        
        if (isNowHovered && !hovered) {
            // Mouse just entered
            hovered = true;
            triggerEvent("mouseEnter");
        } else if (!isNowHovered && hovered) {
            // Mouse just left
            hovered = false;
            triggerEvent("mouseLeave");
        }
    }
    
    /**
     * Handle mouse click
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver((int)mouseX, (int)mouseY) || !enabled) {
            return false;
        }
        
        focused = true;
        triggerEvent("click");
        return true;
    }
    
    /**
     * Add an event handler
     */
    public BCComponent addEventListener(String event, Consumer<BCComponent> handler) {
        eventHandlers.computeIfAbsent(event, k -> new ArrayList<>()).add(handler);
        return this;
    }
    
    /**
     * Trigger a custom event for this component
     */
    protected void triggerEvent(String event) {
        // Only process events if this component is enabled
        if (!enabled) {
            return;
        }
        
        List<Consumer<BCComponent>> handlers = eventHandlers.get(event);
        if (handlers != null) {
            // Make a copy of the list to avoid concurrent modification issues
            List<Consumer<BCComponent>> handlersCopy = new ArrayList<>(handlers);
            
            for (Consumer<BCComponent> handler : handlersCopy) {
                try {
                    handler.accept(this);
                } catch (Exception e) {
                    // Log the error but don't break the event chain
                    System.err.println("Error in event handler for event " + event + ": " + e.getMessage());
                }
            }
        }
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
    
    /**
     * Handle mouse drag events
     * By default, we just check if the mouse is within our bounds
     * Subclasses should override to add specific drag handling
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!enabled || !visible) {
            return false;
        }
        
        // Default implementation doesn't handle drags, but allows subclasses to check
        // if drag is within bounds before implementing custom logic
        return false;
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
        
        // Update animations
        updateAnimation();
        
        // Check hover state
        checkHovered(mouseX, mouseY);
        
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
        // Update animations
        updateAnimation();
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
     * Check if the component is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Check if the component is focused
     */
    public boolean isFocused() {
        return focused;
    }
    
    /**
     * Set the focus state of the component
     */
    public void setFocused(boolean focused) {
        boolean wasFocused = this.focused;
        this.focused = focused;
        
        if (focused && !wasFocused) {
            triggerEvent("focus");
        } else if (!focused && wasFocused) {
            triggerEvent("blur");
        }
    }
    
    /**
     * Get the component's ID
     */
    public String getId() {
        return id;
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
    
    /**
     * Position the component at the specified coordinates and update any children
     */
    public BCComponent positionWithChildren(int x, int y) {
        int deltaX = x - this.x;
        int deltaY = y - this.y;
        
        // Update this component's position
        this.x = x;
        this.y = y;
        
        // Update all child components if this is a container
        if (this instanceof BCPanel) {
            BCPanel panel = (BCPanel) this;
            for (UIComponent child : panel.getChildren()) {
                if (child instanceof BCComponent) {
                    BCComponent component = (BCComponent) child;
                    component.position(component.getX() + deltaX, component.getY() + deltaY);
                }
            }
        }
        
        return this;
    }
    
    /**
     * Set the component's size and update internal state
     */
    public BCComponent sizeWithUpdates(int width, int height) {
        this.width = width;
        this.height = height;
        
        // Trigger resize event
        triggerEvent("resize");
        
        return this;
    }
    
    /**
     * Center this component within the specified width and height
     */
    public BCComponent centerIn(int containerWidth, int containerHeight) {
        int x = (containerWidth - this.width) / 2;
        int y = (containerHeight - this.height) / 2;
        return this.position(x, y);
    }
    
    /**
     * Show this component with optional animation
     */
    public BCComponent show(boolean animate) {
        if (animate) {
            this.alpha = 0.0f;
            this.animate("alpha", 1.0f, 250);
        } else {
            this.alpha = 1.0f;
        }
        this.visible = true;
        
        // Trigger visibility change event
        triggerEvent("show");
        
        return this;
    }
    
    /**
     * Hide this component with optional animation
     */
    public BCComponent hide(boolean animate) {
        if (animate) {
            this.animate("alpha", 0.0f, 250, () -> {
                this.visible = false;
                triggerEvent("hide");
            });
        } else {
            this.alpha = 0.0f;
            this.visible = false;
            triggerEvent("hide");
        }
        
        return this;
    }
    
    /**
     * Set up animation with completion callback
     */
    public void animate(String property, float endValue, long durationMs, Runnable onComplete) {
        animationProperty = property;
        animationStartTime = System.currentTimeMillis();
        animationDuration = durationMs;
        
        // Set the start value based on current property
        switch (property) {
            case "alpha":
                animationStartValue = alpha;
                break;
            // Add other animatable properties as needed
            default:
                animationStartValue = 0;
                break;
        }
        
        animationEndValue = endValue;
        
        // Store the completion callback
        if (onComplete != null) {
            animationCompletionCallback = onComplete;
        }
    }
    
    // Field to store animation completion callback
    private Runnable animationCompletionCallback = null;
} 