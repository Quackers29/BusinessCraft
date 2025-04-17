package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Panel component for BusinessCraft UI system.
 * Acts as a container for other UI components with layout management.
 */
public class BCPanel extends BCComponent {
    protected final List<UIComponent> children = new ArrayList<>();
    protected Map<String, UIComponent> namedChildren = new HashMap<>();
    protected BCLayout layout = null;
    protected int padding = 5; // Default padding
    protected boolean clipChildren = false; // Whether to clip children to the panel's bounds
    protected boolean automaticLayout = true; // Whether to automatically layout children on render

    // Scroll handling
    protected boolean scrollable = false;
    protected int scrollOffset = 0;
    protected int maxScrollOffset = 0;
    protected int contentHeight = 0;
    protected int scrollBarWidth = 5;
    protected boolean scrolling = false;
    
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
        
        // If the child has an ID, add it to the named children map
        if (child instanceof BCComponent) {
            BCComponent bcChild = (BCComponent) child;
            String id = bcChild.getId();
            if (id != null && !id.isEmpty()) {
                namedChildren.put(id, child);
            }
        }
        
        return this;
    }
    
    /**
     * Remove a child component from this panel
     */
    public BCPanel removeChild(UIComponent child) {
        children.remove(child);
        
        // Remove from named children if present
        if (child instanceof BCComponent) {
            BCComponent bcChild = (BCComponent) child;
            String id = bcChild.getId();
            if (id != null && !id.isEmpty() && namedChildren.get(id) == child) {
                namedChildren.remove(id);
            }
        }
        
        return this;
    }
    
    /**
     * Clear all child components
     */
    public BCPanel clearChildren() {
        children.clear();
        namedChildren.clear();
        return this;
    }
    
    /**
     * Find a child component by ID
     */
    public Optional<UIComponent> findChildById(String id) {
        return Optional.ofNullable(namedChildren.get(id));
    }
    
    /**
     * Find child components matching a predicate
     */
    public List<UIComponent> findChildren(Predicate<UIComponent> predicate) {
        List<UIComponent> result = new ArrayList<>();
        for (UIComponent child : children) {
            if (predicate.test(child)) {
                result.add(child);
            }
            
            // Search recursively in child panels
            if (child instanceof BCPanel) {
                result.addAll(((BCPanel) child).findChildren(predicate));
            }
        }
        return result;
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
     * Set whether to clip children to the panel's bounds
     */
    public BCPanel withClipChildren(boolean clip) {
        this.clipChildren = clip;
        return this;
    }
    
    /**
     * Set whether this panel is scrollable
     */
    public BCPanel withScrollable(boolean scrollable) {
        this.scrollable = scrollable;
        return this;
    }
    
    /**
     * Set whether to automatically layout children on render
     */
    public BCPanel withAutomaticLayout(boolean automatic) {
        this.automaticLayout = automatic;
        return this;
    }
    
    /**
     * Get the current scroll offset
     */
    public int getScrollOffset() {
        return scrollOffset;
    }
    
    /**
     * Set the scroll offset
     */
    public void setScrollOffset(int offset) {
        if (!scrollable) return;
        
        // Calculate content height if needed
        updateContentHeight();
        
        // Clamp scroll offset
        if (offset < 0) {
            offset = 0;
        } else if (offset > maxScrollOffset) {
            offset = maxScrollOffset;
        }
        
        if (this.scrollOffset != offset) {
            this.scrollOffset = offset;
            triggerEvent("scroll");
        }
    }
    
    /**
     * Handle mouse scroll
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (!scrollable || !isMouseOver((int)mouseX, (int)mouseY)) {
            return false;
        }
        
        // Delta is positive when scrolling up, negative when scrolling down
        int scrollAmount = (int)(-scrollDelta * 10); // Adjust sensitivity
        setScrollOffset(scrollOffset + scrollAmount);
        return true;
    }
    
    /**
     * Update the content height based on children
     */
    private void updateContentHeight() {
        contentHeight = 0;
        
        for (UIComponent child : children) {
            if (child.isVisible()) {
                int childBottom = child.getY() - getInnerTop() + child.getHeight();
                if (childBottom > contentHeight) {
                    contentHeight = childBottom;
                }
            }
        }
        
        // Calculate max scroll offset
        int visibleHeight = getInnerHeight();
        maxScrollOffset = Math.max(0, contentHeight - visibleHeight);
    }
    
    /**
     * Layout the children using the current layout manager
     */
    public void layoutChildren() {
        if (layout != null) {
            layout.layout(this, children);
        }
        
        // Update content height for scrolling
        if (scrollable) {
            updateContentHeight();
        }
    }
    
    /**
     * Enable scissor test to clip children to panel bounds
     */
    private void enableScissor(GuiGraphics guiGraphics) {
        if (clipChildren) {
            guiGraphics.pose().pushPose();
            guiGraphics.enableScissor(
                x + padding, 
                y + padding, 
                x + width - padding, 
                y + height - padding
            );
        }
    }
    
    /**
     * Disable scissor test
     */
    private void disableScissor(GuiGraphics guiGraphics) {
        if (clipChildren) {
            guiGraphics.disableScissor();
            guiGraphics.pose().popPose();
        }
    }
    
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Layout children if a layout manager is set and automatic layout is enabled
        if (automaticLayout) {
            layoutChildren();
        }
        
        // Enable scissor if clipping is enabled
        enableScissor(guiGraphics);
        
        // Render scrollbar if needed
        if (scrollable && maxScrollOffset > 0) {
            int scrollbarHeight = Math.max(20, (getInnerHeight() * getInnerHeight()) / contentHeight);
            int scrollbarY = y + padding + (int)((float)scrollOffset / maxScrollOffset * (getInnerHeight() - scrollbarHeight));
            
            // Draw scrollbar track
            guiGraphics.fill(
                x + width - padding - scrollBarWidth, 
                y + padding, 
                x + width - padding, 
                y + height - padding, 
                0x40FFFFFF
            );
            
            // Draw scrollbar thumb
            guiGraphics.fill(
                x + width - padding - scrollBarWidth, 
                scrollbarY, 
                x + width - padding, 
                scrollbarY + scrollbarHeight, 
                0xC0FFFFFF
            );
        }
        
        // Render children
        for (UIComponent child : children) {
            if (child.isVisible()) {
                // Apply scroll offset if scrollable
                int childY = child.getY();
                if (scrollable) {
                    childY -= scrollOffset;
                }
                
                // Only render if child is at least partially visible
                if (!scrollable || 
                    (childY + child.getHeight() > y + padding && 
                     childY < y + height - padding)) {
                    child.render(guiGraphics, child.getX(), childY, mouseX, mouseY);
                }
            }
        }
        
        // Disable scissor
        disableScissor(guiGraphics);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver((int)mouseX, (int)mouseY) || !enabled) {
            return false;
        }
        
        // Check if clicking on scrollbar
        if (scrollable && maxScrollOffset > 0) {
            int scrollbarX = x + width - padding - scrollBarWidth;
            if (mouseX >= scrollbarX && mouseX <= x + width - padding) {
                scrolling = true;
                handleScrollbarDrag(mouseY);
                return true;
            }
        }
        
        // Pass the click to children
        boolean handled = false;
        for (int i = children.size() - 1; i >= 0; i--) {
            UIComponent child = children.get(i);
            if (child.isVisible() && child instanceof BCComponent) {
                // Adjust y position for scrolling
                double adjustedY = mouseY;
                if (scrollable) {
                    adjustedY += scrollOffset;
                }
                
                if (((BCComponent) child).mouseClicked(mouseX, adjustedY, button)) {
                    handled = true;
                    break;
                }
            }
        }
        
        // Handle panel click if not handled by children
        if (!handled) {
            super.mouseClicked(mouseX, mouseY, button);
        }
        
        return true;
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!enabled || !visible) {
            return false;
        }
        
        // Handle scrolling if enabled
        if (scrollable && scrolling) {
            // Forward to scrollbar dragging handler
            handleScrollbarDrag(mouseY);
            return true;
        }
        
        // Forward event to children in reverse order to handle top components first
        for (int i = children.size() - 1; i >= 0; i--) {
            UIComponent child = children.get(i);
            
            if (child instanceof BCComponent) {
                BCComponent component = (BCComponent) child;
                
                // Adjust y position for scrolling
                double adjustedY = mouseY;
                if (scrollable) {
                    adjustedY += scrollOffset;
                }
                
                if (component.isVisible() && component.mouseDragged(mouseX, adjustedY, button, dragX, dragY)) {
                    return true;
                }
            }
        }
        
        // Fall back to default behavior
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // Stop scrolling
        scrolling = false;
        
        // Pass to children
        for (UIComponent child : children) {
            if (child.isVisible() && child instanceof BCComponent) {
                // Adjust y position for scrolling
                double adjustedY = mouseY;
                if (scrollable) {
                    adjustedY += scrollOffset;
                }
                
                if (((BCComponent) child).mouseReleased(mouseX, adjustedY, button)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Handle scrollbar dragging
     */
    private void handleScrollbarDrag(double mouseY) {
        int scrollbarHeight = Math.max(20, (getInnerHeight() * getInnerHeight()) / contentHeight);
        float scrollableTrackSize = getInnerHeight() - scrollbarHeight;
        float relativeY = (float)((mouseY - (y + padding)) / scrollableTrackSize);
        
        // Clamp to 0-1 range
        relativeY = Math.max(0, Math.min(1, relativeY));
        
        // Set scroll offset based on relative position
        setScrollOffset((int)(relativeY * maxScrollOffset));
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
        super.tick();
        
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
        return width - (padding * 2) - (scrollable ? scrollBarWidth : 0);
    }
    
    /**
     * Get the inner height (accounts for padding)
     */
    public int getInnerHeight() {
        return height - (padding * 2);
    }
    
    /**
     * Get the list of child components (needed for positional updates)
     * 
     * @return List of child components
     */
    public List<UIComponent> getChildren() {
        return new ArrayList<>(children); // Return a copy to prevent external modification
    }
    
    /**
     * Create a panel between two points with optional styling
     * 
     * @param x1 Starting X coordinate
     * @param y1 Starting Y coordinate
     * @param x2 Ending X coordinate
     * @param y2 Ending Y coordinate
     * @param backgroundColor Background color for the panel
     * @param borderColor Border color for the panel
     * @param cornerRadius Corner radius for rounded corners
     * @return A new BCPanel instance positioned between the two points
     */
    public static BCPanel createBetweenPoints(int x1, int y1, int x2, int y2, 
                                             int backgroundColor, int borderColor, int cornerRadius) {
        // Ensure correct coordinate order
        int left = Math.min(x1, x2);
        int top = Math.min(y1, y2);
        int width = Math.abs(x2 - x1);
        int height = Math.abs(y2 - y1);
        
        // Create panel with calculated dimensions
        BCPanel panel = new BCPanel(width, height);
        panel.position(left, top);
        
        // Apply styling
        panel.withBackgroundColor(backgroundColor);
        panel.withBorderColor(borderColor);
        panel.withCornerRadius(cornerRadius);
        
        return panel;
    }
    
    /**
     * Create a panel between two points with default styling from the current theme
     * 
     * @param x1 Starting X coordinate
     * @param y1 Starting Y coordinate
     * @param x2 Ending X coordinate
     * @param y2 Ending Y coordinate
     * @return A new BCPanel instance positioned between the two points with theme styling
     */
    public static BCPanel createBetweenPoints(int x1, int y1, int x2, int y2) {
        BCTheme theme = BCTheme.get();
        return createBetweenPoints(x1, y1, x2, y2, 
                                  theme.getPanelBackground(), 
                                  theme.getPanelBorder(),
                                  theme.hasRoundedCorners() ? 3 : 0);
    }
} 