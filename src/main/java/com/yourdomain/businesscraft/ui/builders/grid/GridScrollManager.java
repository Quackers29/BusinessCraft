package com.yourdomain.businesscraft.ui.builders.grid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages scrolling functionality for grid components.
 * Handles both horizontal and vertical scrolling with mouse interaction support.
 * Extracted from UIGridBuilder to improve single responsibility principle.
 */
public class GridScrollManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(GridScrollManager.class);
    
    // Horizontal scrolling
    private boolean horizontalScrollEnabled = false;
    private int horizontalScrollOffset = 0;
    private int maxHorizontalScrollOffset = 0;
    private int visibleColumns = 0;
    private int totalColumns = 0;
    private boolean useScrollButtons = false;
    
    // Vertical scrolling
    private boolean verticalScrollEnabled = false;
    private int verticalScrollOffset = 0;
    private int maxVerticalScrollOffset = 0;
    private int visibleRows = 0;
    private int totalRows = 0;
    
    // Scrollbar appearance
    private int scrollBarHeight = 10;
    private int scrollBarWidth = 8;
    private int scrollButtonSize = 20;
    
    // Mouse interaction state
    private boolean isDraggingVertical = false;
    private boolean isDraggingHorizontal = false;
    private double lastMouseY = 0;
    private double lastMouseX = 0;
    
    /**
     * Enables horizontal scrolling with the specified parameters.
     * 
     * @param visibleColumns Number of columns visible at once
     * @param totalColumns Total number of columns in the grid
     */
    public void enableHorizontalScroll(int visibleColumns, int totalColumns) {
        this.horizontalScrollEnabled = true;
        this.visibleColumns = visibleColumns;
        this.totalColumns = totalColumns;
        this.maxHorizontalScrollOffset = Math.max(0, totalColumns - visibleColumns);
        
        // Reset offset if it's now out of bounds
        if (horizontalScrollOffset > maxHorizontalScrollOffset) {
            horizontalScrollOffset = maxHorizontalScrollOffset;
        }
        
        LOGGER.debug("Horizontal scroll enabled: visible={}, total={}, maxOffset={}", 
                    visibleColumns, totalColumns, maxHorizontalScrollOffset);
    }
    
    /**
     * Enables vertical scrolling with the specified parameters.
     * 
     * @param visibleRows Number of rows visible at once
     * @param totalRows Total number of rows in the grid
     */
    public void enableVerticalScroll(int visibleRows, int totalRows) {
        this.verticalScrollEnabled = true;
        this.visibleRows = visibleRows;
        this.totalRows = totalRows;
        this.maxVerticalScrollOffset = Math.max(0, totalRows - visibleRows);
        
        // Reset offset if it's now out of bounds
        if (verticalScrollOffset > maxVerticalScrollOffset) {
            verticalScrollOffset = maxVerticalScrollOffset;
        }
        
        LOGGER.debug("Vertical scroll enabled: visible={}, total={}, maxOffset={}", 
                    visibleRows, totalRows, maxVerticalScrollOffset);
    }
    
    /**
     * Sets whether to use scroll buttons for horizontal scrolling.
     * 
     * @param useButtons True to enable scroll buttons
     */
    public void setUseScrollButtons(boolean useButtons) {
        this.useScrollButtons = useButtons;
    }
    
    /**
     * Sets the horizontal scroll offset.
     * 
     * @param offset The new offset (will be clamped to valid range)
     */
    public void setHorizontalScrollOffset(int offset) {
        int oldOffset = horizontalScrollOffset;
        horizontalScrollOffset = Math.max(0, Math.min(offset, maxHorizontalScrollOffset));
        
        if (oldOffset != horizontalScrollOffset) {
            LOGGER.debug("Horizontal scroll offset changed: {} -> {}", oldOffset, horizontalScrollOffset);
        }
    }
    
    /**
     * Sets the vertical scroll offset.
     * 
     * @param offset The new offset (will be clamped to valid range)
     */
    public void setVerticalScrollOffset(int offset) {
        int oldOffset = verticalScrollOffset;
        verticalScrollOffset = Math.max(0, Math.min(offset, maxVerticalScrollOffset));
        
        if (oldOffset != verticalScrollOffset) {
            LOGGER.debug("Vertical scroll offset changed: {} -> {}", oldOffset, verticalScrollOffset);
        }
    }
    
    /**
     * Scrolls left by one column.
     */
    public void scrollLeft() {
        setHorizontalScrollOffset(horizontalScrollOffset - 1);
    }
    
    /**
     * Scrolls right by one column.
     */
    public void scrollRight() {
        setHorizontalScrollOffset(horizontalScrollOffset + 1);
    }
    
    /**
     * Scrolls up by one row.
     */
    public void scrollUp() {
        setVerticalScrollOffset(verticalScrollOffset - 1);
    }
    
    /**
     * Scrolls down by one row.
     */
    public void scrollDown() {
        setVerticalScrollOffset(verticalScrollOffset + 1);
    }
    
    /**
     * Handles mouse wheel scrolling.
     * 
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param delta Scroll delta (positive = up, negative = down)
     * @param gridBounds The bounds of the grid area
     * @return True if scrolling was handled
     */
    public boolean handleMouseScroll(double mouseX, double mouseY, double delta, GridLayoutManager.GridPosition gridBounds) {
        // Check if mouse is within grid bounds
        if (mouseX < gridBounds.getX() || mouseX > gridBounds.getRight() ||
            mouseY < gridBounds.getY() || mouseY > gridBounds.getBottom()) {
            return false;
        }
        
        if (verticalScrollEnabled) {
            // Apply scrolling (delta > 0 means scroll up)
            setVerticalScrollOffset(verticalScrollOffset - (int)Math.signum(delta));
            return true;
        }
        
        return false;
    }
    
    /**
     * Handles mouse drag for scrollbar interaction.
     * 
     * @param mouseX Current mouse X position
     * @param mouseY Current mouse Y position
     * @param button Mouse button (0 = left)
     * @param dragX Drag delta X
     * @param dragY Drag delta Y
     * @param gridBounds The bounds of the grid area
     * @return True if drag was handled
     */
    public boolean handleMouseDrag(double mouseX, double mouseY, int button, double dragX, double dragY, 
                                  GridLayoutManager.GridPosition gridBounds) {
        if (button != 0) return false; // Only handle left mouse button
        
        // Handle vertical scrollbar dragging
        if (isDraggingVertical && verticalScrollEnabled) {
            ScrollbarBounds vScrollBounds = calculateVerticalScrollbarBounds(gridBounds);
            
            // Calculate new scroll position based on mouse position
            double relativeY = (mouseY - vScrollBounds.trackY) / vScrollBounds.trackHeight;
            int newOffset = (int)(relativeY * maxVerticalScrollOffset);
            setVerticalScrollOffset(newOffset);
            
            return true;
        }
        
        // Handle horizontal scrollbar dragging
        if (isDraggingHorizontal && horizontalScrollEnabled) {
            ScrollbarBounds hScrollBounds = calculateHorizontalScrollbarBounds(gridBounds);
            
            // Calculate new scroll position based on mouse position
            double relativeX = (mouseX - hScrollBounds.trackX) / hScrollBounds.trackWidth;
            int newOffset = (int)(relativeX * maxHorizontalScrollOffset);
            setHorizontalScrollOffset(newOffset);
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Handles mouse press for starting scrollbar drag.
     * 
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param button Mouse button
     * @param gridBounds The bounds of the grid area
     * @return True if press was handled
     */
    public boolean handleMousePress(double mouseX, double mouseY, int button, GridLayoutManager.GridPosition gridBounds) {
        if (button != 0) return false; // Only handle left mouse button
        
        // Check vertical scrollbar
        if (verticalScrollEnabled) {
            ScrollbarBounds vScrollBounds = calculateVerticalScrollbarBounds(gridBounds);
            if (isMouseOverScrollbarThumb(mouseX, mouseY, vScrollBounds, true)) {
                isDraggingVertical = true;
                lastMouseY = mouseY;
                return true;
            }
        }
        
        // Check horizontal scrollbar
        if (horizontalScrollEnabled) {
            ScrollbarBounds hScrollBounds = calculateHorizontalScrollbarBounds(gridBounds);
            if (isMouseOverScrollbarThumb(mouseX, mouseY, hScrollBounds, false)) {
                isDraggingHorizontal = true;
                lastMouseX = mouseX;
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Handles mouse release to stop scrollbar dragging.
     * 
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param button Mouse button
     * @return True if release was handled
     */
    public boolean handleMouseRelease(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left mouse button
            boolean wasHandled = isDraggingVertical || isDraggingHorizontal;
            isDraggingVertical = false;
            isDraggingHorizontal = false;
            return wasHandled;
        }
        return false;
    }
    
    /**
     * Calculates the bounds of the vertical scrollbar.
     */
    public ScrollbarBounds calculateVerticalScrollbarBounds(GridLayoutManager.GridPosition gridBounds) {
        if (!verticalScrollEnabled) return null;
        
        int trackX = gridBounds.getRight() - scrollBarWidth;
        int trackY = gridBounds.getY();
        int trackWidth = scrollBarWidth;
        int trackHeight = gridBounds.getHeight();
        
        // Calculate thumb position and size
        double thumbRatio = (double) visibleRows / totalRows;
        int thumbHeight = Math.max(10, (int)(trackHeight * thumbRatio));
        
        double scrollRatio = maxVerticalScrollOffset > 0 ? 
            (double) verticalScrollOffset / maxVerticalScrollOffset : 0;
        int thumbY = trackY + (int)((trackHeight - thumbHeight) * scrollRatio);
        
        return new ScrollbarBounds(trackX, trackY, trackWidth, trackHeight, 
                                  trackX, thumbY, trackWidth, thumbHeight);
    }
    
    /**
     * Calculates the bounds of the horizontal scrollbar.
     */
    public ScrollbarBounds calculateHorizontalScrollbarBounds(GridLayoutManager.GridPosition gridBounds) {
        if (!horizontalScrollEnabled) return null;
        
        int trackX = gridBounds.getX();
        int trackY = gridBounds.getBottom() - scrollBarHeight;
        int trackWidth = gridBounds.getWidth();
        int trackHeight = scrollBarHeight;
        
        // Calculate thumb position and size
        double thumbRatio = (double) visibleColumns / totalColumns;
        int thumbWidth = Math.max(10, (int)(trackWidth * thumbRatio));
        
        double scrollRatio = maxHorizontalScrollOffset > 0 ? 
            (double) horizontalScrollOffset / maxHorizontalScrollOffset : 0;
        int thumbX = trackX + (int)((trackWidth - thumbWidth) * scrollRatio);
        
        return new ScrollbarBounds(trackX, trackY, trackWidth, trackHeight, 
                                  thumbX, trackY, thumbWidth, trackHeight);
    }
    
    /**
     * Checks if the mouse is over a scrollbar thumb.
     */
    private boolean isMouseOverScrollbarThumb(double mouseX, double mouseY, ScrollbarBounds bounds, boolean vertical) {
        if (bounds == null) return false;
        
        return mouseX >= bounds.thumbX && mouseX <= bounds.thumbX + bounds.thumbWidth &&
               mouseY >= bounds.thumbY && mouseY <= bounds.thumbY + bounds.thumbHeight;
    }
    
    // Getters
    public boolean isHorizontalScrollEnabled() { return horizontalScrollEnabled; }
    public boolean isVerticalScrollEnabled() { return verticalScrollEnabled; }
    public int getHorizontalScrollOffset() { return horizontalScrollOffset; }
    public int getVerticalScrollOffset() { return verticalScrollOffset; }
    public int getMaxHorizontalScrollOffset() { return maxHorizontalScrollOffset; }
    public int getMaxVerticalScrollOffset() { return maxVerticalScrollOffset; }
    public int getVisibleColumns() { return visibleColumns; }
    public int getVisibleRows() { return visibleRows; }
    public int getTotalColumns() { return totalColumns; }
    public int getTotalRows() { return totalRows; }
    public boolean isUseScrollButtons() { return useScrollButtons; }
    public boolean isDraggingVertical() { return isDraggingVertical; }
    public boolean isDraggingHorizontal() { return isDraggingHorizontal; }
    
    /**
     * Represents the bounds of a scrollbar including track and thumb.
     */
    public static class ScrollbarBounds {
        public final int trackX, trackY, trackWidth, trackHeight;
        public final int thumbX, thumbY, thumbWidth, thumbHeight;
        
        public ScrollbarBounds(int trackX, int trackY, int trackWidth, int trackHeight,
                              int thumbX, int thumbY, int thumbWidth, int thumbHeight) {
            this.trackX = trackX;
            this.trackY = trackY;
            this.trackWidth = trackWidth;
            this.trackHeight = trackHeight;
            this.thumbX = thumbX;
            this.thumbY = thumbY;
            this.thumbWidth = thumbWidth;
            this.thumbHeight = thumbHeight;
        }
    }
}