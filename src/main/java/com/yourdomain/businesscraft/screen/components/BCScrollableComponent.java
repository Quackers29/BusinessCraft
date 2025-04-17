package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import java.util.function.Consumer;

/**
 * Enhanced scrollable component base class.
 * Provides advanced scrolling functionality with momentum and virtualization.
 */
public abstract class BCScrollableComponent extends BCComponent {
    // Scroll state
    protected float scrollOffset = 0;
    protected float targetScrollOffset = 0;
    protected float scrollVelocity = 0;
    protected float scrollAcceleration = 0.92f;
    protected boolean isDragging = false;
    protected double lastMouseY = 0;
    protected long lastDragTime = 0;
    
    // Scroll limits
    protected int contentHeight = 0;
    protected int viewportHeight = 0;
    protected int maxScrollOffset = 0;
    
    // Scrollbar styling
    protected int scrollbarWidth = 6;
    protected int scrollbarMinHeight = 20;
    protected int scrollbarColor = 0x80FFFFFF;
    protected int scrollbarHoverColor = 0xA0FFFFFF;
    protected boolean isScrollbarHovered = false;
    protected boolean isScrollbarDragging = false;
    
    public BCScrollableComponent(int width, int height) {
        super(width, height);
    }
    
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Update scroll animation
        updateScroll();
        
        // Save graphics state
        guiGraphics.pose().pushPose();
        
        // Apply scroll translation
        guiGraphics.pose().translate(0, -scrollOffset, 0);
        
        // Render visible content
        renderScrollContent(guiGraphics, mouseX, mouseY + (int)scrollOffset);
        
        // Restore graphics state
        guiGraphics.pose().popPose();
        
        // Render scrollbar if needed
        if (maxScrollOffset > 0) {
            renderScrollbar(guiGraphics, mouseX, mouseY);
        }
    }
    
    /**
     * Render the actual scrollable content
     * Subclasses must implement this to render their specific content
     */
    protected abstract void renderScrollContent(GuiGraphics guiGraphics, int mouseX, int mouseY);
    
    /**
     * Update scroll position and animation
     */
    protected void updateScroll() {
        if (!isDragging) {
            // Apply momentum scrolling
            if (Math.abs(scrollVelocity) > 0.01f) {
                targetScrollOffset += scrollVelocity;
                scrollVelocity *= scrollAcceleration;
            }
            
            // Smoothly animate to target position
            float delta = targetScrollOffset - scrollOffset;
            if (Math.abs(delta) > 0.01f) {
                scrollOffset += delta * 0.3f;
            } else {
                scrollOffset = targetScrollOffset;
            }
            
            // Clamp scroll position
            clampScroll();
        }
    }
    
    /**
     * Ensure scroll position is within bounds
     */
    protected void clampScroll() {
        targetScrollOffset = Math.max(0, Math.min(targetScrollOffset, maxScrollOffset));
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
    }
    
    /**
     * Render the scrollbar
     */
    protected void renderScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Calculate scrollbar dimensions
        int scrollbarX = x + width - scrollbarWidth - 2;
        float viewportRatio = (float)height / contentHeight;
        int scrollbarHeight = Math.max(scrollbarMinHeight, (int)(height * viewportRatio));
        float scrollRatio = scrollOffset / maxScrollOffset;
        int scrollbarY = y + (int)((height - scrollbarHeight) * scrollRatio);
        
        // Update hover state
        isScrollbarHovered = mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth &&
                            mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight;
        
        // Draw scrollbar background
        guiGraphics.fill(
            scrollbarX,
            y,
            scrollbarX + scrollbarWidth,
            y + height,
            0x20FFFFFF
        );
        
        // Draw scrollbar
        guiGraphics.fill(
            scrollbarX,
            scrollbarY,
            scrollbarX + scrollbarWidth,
            scrollbarY + scrollbarHeight,
            isScrollbarHovered || isScrollbarDragging ? scrollbarHoverColor : scrollbarColor
        );
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isMouseOver((int)mouseX, (int)mouseY) || !enabled) {
            return false;
        }
        
        // Apply scroll with smooth animation
        targetScrollOffset -= (float)(delta * 20.0f);
        clampScroll();
        return true;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver((int)mouseX, (int)mouseY) || !enabled) {
            return false;
        }
        
        // Check if clicking scrollbar
        if (isScrollbarHovered) {
            isScrollbarDragging = true;
            lastMouseY = mouseY;
            return true;
        }
        
        // Start content drag
        isDragging = true;
        lastMouseY = mouseY;
        lastDragTime = System.currentTimeMillis();
        scrollVelocity = 0;
        return true;
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!enabled) {
            return false;
        }
        
        if (isDragging) {
            // Handle content dragging
            double delta = mouseY - lastMouseY;
            targetScrollOffset -= delta;
            scrollOffset = targetScrollOffset;
            clampScroll();
            
            // Calculate velocity for momentum
            long currentTime = System.currentTimeMillis();
            long timeDelta = currentTime - lastDragTime;
            if (timeDelta > 0) {
                scrollVelocity = (float)(-delta / timeDelta) * 16;
            }
            
            lastMouseY = mouseY;
            lastDragTime = currentTime;
            return true;
        } else if (isScrollbarDragging) {
            // Handle scrollbar dragging
            float viewportRatio = (float)height / contentHeight;
            float dragRatio = (float)(dragY / (height * (1 - viewportRatio)));
            targetScrollOffset += dragRatio * maxScrollOffset;
            scrollOffset = targetScrollOffset;
            clampScroll();
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDragging = false;
        isScrollbarDragging = false;
        return true;
    }
    
    /**
     * Set the total height of the scrollable content
     */
    public void setContentHeight(int height) {
        this.contentHeight = height;
        this.maxScrollOffset = Math.max(0, contentHeight - this.height);
        clampScroll();
    }
    
    /**
     * Get the current scroll offset
     */
    public float getScrollOffset() {
        return scrollOffset;
    }
    
    /**
     * Set the scroll offset directly (without animation)
     */
    public void setScrollOffset(float offset) {
        this.scrollOffset = offset;
        this.targetScrollOffset = offset;
        this.scrollVelocity = 0;
        clampScroll();
    }
    
    /**
     * Smoothly scroll to the specified offset
     */
    public void smoothScrollTo(float offset) {
        this.targetScrollOffset = offset;
        clampScroll();
    }
    
    /**
     * Check if a y-coordinate is currently visible in the viewport
     */
    public boolean isPositionVisible(int y) {
        return y >= scrollOffset && y <= scrollOffset + height;
    }
} 