package com.quackers29.businesscraft.ui.builders.grid;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.ui.builders.grid.GridElementManager.GridElement;
import com.quackers29.businesscraft.ui.builders.grid.GridElementManager.GridContentType;
import com.quackers29.businesscraft.ui.builders.grid.GridLayoutManager.GridPosition;
import com.quackers29.businesscraft.ui.builders.grid.GridScrollManager.ScrollbarBounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Handles rendering of grid components and elements.
 * Manages drawing of backgrounds, borders, elements, and scrollbars.
 * Extracted from UIGridBuilder to improve single responsibility principle.
 */
public class GridRenderingEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(GridRenderingEngine.class);
    
    // Styling properties
    private int backgroundColor = 0x80222222; // Semi-transparent dark gray
    private int borderColor = 0xA0AAAAAA;     // Light gray
    private boolean drawBackground = true;
    private boolean drawBorder = true;
    
    // Scrollbar colors
    private int scrollbarTrackColor = 0x40FFFFFF;  // Light gray semi-transparent
    private int scrollbarThumbColor = 0xA0CCDDFF;  // Light blue semi-transparent
    private int scrollbarActiveColor = 0xFFCCDDFF; // Light blue opaque
    
    /**
     * Sets the background color.
     * 
     * @param color Background color (ARGB format)
     */
    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
    }
    
    /**
     * Sets the border color.
     * 
     * @param color Border color (ARGB format)
     */
    public void setBorderColor(int color) {
        this.borderColor = color;
    }
    
    /**
     * Sets whether to draw the background.
     * 
     * @param draw True to draw background
     */
    public void setDrawBackground(boolean draw) {
        this.drawBackground = draw;
    }
    
    /**
     * Sets whether to draw the border.
     * 
     * @param draw True to draw border
     */
    public void setDrawBorder(boolean draw) {
        this.drawBorder = draw;
    }
    
    /**
     * Renders the complete grid including background, elements, and scrollbars.
     * 
     * @param graphics Graphics context
     * @param layoutManager Layout manager for positioning
     * @param scrollManager Scroll manager for scrollbar state
     * @param elementManager Element manager for grid content
     */
    public void renderGrid(GuiGraphics graphics, GridLayoutManager layoutManager, 
                          GridScrollManager scrollManager, GridElementManager elementManager) {
        
        GridPosition gridBounds = layoutManager.getGridArea();
        
        // Draw background and border
        if (drawBackground) {
            graphics.fill(gridBounds.getX(), gridBounds.getY(), 
                         gridBounds.getRight(), gridBounds.getBottom(), backgroundColor);
        }
        
        if (drawBorder) {
            drawBorder(graphics, gridBounds);
        }
        
        // Render visible elements
        renderElements(graphics, layoutManager, scrollManager, elementManager);
        
        // Render scrollbars
        renderScrollbars(graphics, layoutManager, scrollManager);
    }
    
    /**
     * Renders only the grid elements without background or scrollbars.
     */
    public void renderElements(GuiGraphics graphics, GridLayoutManager layoutManager, 
                              GridScrollManager scrollManager, GridElementManager elementManager) {
        
        // Get visible elements based on scroll offsets
        List<GridElement> visibleElements = elementManager.getVisibleElements(
            scrollManager.getVerticalScrollOffset(),
            scrollManager.getHorizontalScrollOffset(),
            scrollManager.isVerticalScrollEnabled() ? scrollManager.getVisibleRows() : layoutManager.getRows(),
            scrollManager.isHorizontalScrollEnabled() ? scrollManager.getVisibleColumns() : layoutManager.getColumns()
        );
        
        // Render each visible element
        for (GridElement element : visibleElements) {
            renderElement(graphics, element, layoutManager, scrollManager);
        }
    }
    
    /**
     * Renders a single grid element.
     */
    private void renderElement(GuiGraphics graphics, GridElement element, 
                              GridLayoutManager layoutManager, GridScrollManager scrollManager) {
        
        // Calculate display position (accounting for scroll offsets)
        int displayRow = element.getRow() - scrollManager.getVerticalScrollOffset();
        int displayColumn = element.getColumn() - scrollManager.getHorizontalScrollOffset();
        
        // Skip if element is outside visible area
        if (displayRow < 0 || displayColumn < 0) return;
        
        GridPosition cellPos = layoutManager.calculateCellPosition(displayRow, displayColumn);
        
        switch (element.getType()) {
            case TEXT -> renderText(graphics, element, cellPos);
            case BUTTON -> renderButton(graphics, element, cellPos);
            case ITEM -> renderItem(graphics, element, cellPos);
            case ITEM_WITH_QUANTITY -> renderItemWithQuantity(graphics, element, cellPos);
            case TOGGLE -> renderToggle(graphics, element, cellPos);
        }
    }
    
    /**
     * Renders a text element.
     */
    private void renderText(GuiGraphics graphics, GridElement element, GridPosition cellPos) {
        if (element.getValue() instanceof String text) {
            com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
            if (clientHelper != null) {
                Object fontObj = clientHelper.getFont();
                if (fontObj instanceof net.minecraft.client.gui.Font font) {
                    // Center the text in the cell
                    int textX = cellPos.getX() + (cellPos.getWidth() - font.width(text)) / 2;
                    int textY = cellPos.getY() + (cellPos.getHeight() - font.lineHeight) / 2;
                    
                    graphics.drawString(font, text, textX, textY, element.getTextColor());
                }
            }
        }
    }
    
    /**
     * Renders a button element.
     */
    private void renderButton(GuiGraphics graphics, GridElement element, GridPosition cellPos) {
        // Draw button background
        graphics.fill(cellPos.getX(), cellPos.getY(), 
                     cellPos.getRight(), cellPos.getBottom(), element.getBgColor());
        
        // Draw button border
        drawBorder(graphics, cellPos);
        
        // Draw button text
        if (element.getValue() instanceof String text) {
            com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
            if (clientHelper != null) {
                Object fontObj = clientHelper.getFont();
                if (fontObj instanceof net.minecraft.client.gui.Font font) {
                    int textX = cellPos.getX() + (cellPos.getWidth() - font.width(text)) / 2;
                    int textY = cellPos.getY() + (cellPos.getHeight() - font.lineHeight) / 2;
                    graphics.drawString(font, text, textX, textY, element.getTextColor());
                }
            }
        }
    }
    
    /**
     * Renders an item element.
     */
    private void renderItem(GuiGraphics graphics, GridElement element, GridPosition cellPos) {
        if (element.getValue() instanceof ItemStack itemStack) {
            // Center the item in the cell
            int itemX = cellPos.getX() + (cellPos.getWidth() - 16) / 2;
            int itemY = cellPos.getY() + (cellPos.getHeight() - 16) / 2;
            
            graphics.renderItem(itemStack, itemX, itemY);
        }
    }
    
    /**
     * Renders an item with quantity element.
     */
    private void renderItemWithQuantity(GuiGraphics graphics, GridElement element, GridPosition cellPos) {
        if (element.getValue() instanceof ItemStack itemStack) {
            // Center the item in the cell
            int itemX = cellPos.getX() + (cellPos.getWidth() - 16) / 2;
            int itemY = cellPos.getY() + (cellPos.getHeight() - 16) / 2;
            
            graphics.renderItem(itemStack, itemX, itemY);
            
            // Draw quantity overlay
            String quantityText = String.valueOf(element.getQuantity());
            com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
            if (clientHelper != null) {
                Object fontObj = clientHelper.getFont();
                if (fontObj instanceof net.minecraft.client.gui.Font font) {
                    int textX = itemX + 16 - font.width(quantityText);
                    int textY = itemY + 16 - font.lineHeight;
                    graphics.drawString(font, quantityText, textX, textY, 0xFFFFFF);
                }
            }
        }
    }
    
    /**
     * Renders a toggle element.
     */
    private void renderToggle(GuiGraphics graphics, GridElement element, GridPosition cellPos) {
        // Draw toggle background (different color based on state)
        int toggleColor = element.getToggleState() ? 0xFF00AA00 : 0xFF666666; // Green if on, gray if off
        graphics.fill(cellPos.getX(), cellPos.getY(), 
                     cellPos.getRight(), cellPos.getBottom(), toggleColor);
        
        // Draw toggle border
        drawBorder(graphics, cellPos);
        
        // Draw toggle text
        if (element.getValue() instanceof String text) {
            Font font = Minecraft.getInstance().font;
            int textX = cellPos.getX() + (cellPos.getWidth() - font.width(text)) / 2;
            int textY = cellPos.getY() + (cellPos.getHeight() - font.lineHeight) / 2;
            graphics.drawString(font, text, textX, textY, element.getTextColor());
        }
    }
    
    /**
     * Renders scrollbars if scrolling is enabled.
     */
    private void renderScrollbars(GuiGraphics graphics, GridLayoutManager layoutManager, GridScrollManager scrollManager) {
        GridPosition gridBounds = layoutManager.getGridArea();
        
        // Render vertical scrollbar
        if (scrollManager.isVerticalScrollEnabled()) {
            ScrollbarBounds vBounds = scrollManager.calculateVerticalScrollbarBounds(gridBounds);
            if (vBounds != null) {
                renderScrollbar(graphics, vBounds, true, scrollManager.isDraggingVertical());
            }
        }
        
        // Render horizontal scrollbar
        if (scrollManager.isHorizontalScrollEnabled()) {
            ScrollbarBounds hBounds = scrollManager.calculateHorizontalScrollbarBounds(gridBounds);
            if (hBounds != null) {
                renderScrollbar(graphics, hBounds, false, scrollManager.isDraggingHorizontal());
            }
        }
    }
    
    /**
     * Renders a single scrollbar (vertical or horizontal).
     */
    private void renderScrollbar(GuiGraphics graphics, ScrollbarBounds bounds, boolean vertical, boolean isDragging) {
        // Draw scrollbar track
        graphics.fill(bounds.trackX, bounds.trackY, 
                     bounds.trackX + bounds.trackWidth, bounds.trackY + bounds.trackHeight, 
                     scrollbarTrackColor);
        
        // Draw scrollbar thumb
        int thumbColor = isDragging ? scrollbarActiveColor : scrollbarThumbColor;
        graphics.fill(bounds.thumbX, bounds.thumbY, 
                     bounds.thumbX + bounds.thumbWidth, bounds.thumbY + bounds.thumbHeight, 
                     thumbColor);
    }
    
    /**
     * Draws a border around the specified area.
     */
    private void drawBorder(GuiGraphics graphics, GridPosition bounds) {
        // Top border
        graphics.fill(bounds.getX(), bounds.getY(), bounds.getRight(), bounds.getY() + 1, borderColor);
        // Bottom border
        graphics.fill(bounds.getX(), bounds.getBottom() - 1, bounds.getRight(), bounds.getBottom(), borderColor);
        // Left border
        graphics.fill(bounds.getX(), bounds.getY(), bounds.getX() + 1, bounds.getBottom(), borderColor);
        // Right border
        graphics.fill(bounds.getRight() - 1, bounds.getY(), bounds.getRight(), bounds.getBottom(), borderColor);
    }
    
    /**
     * Sets scrollbar colors.
     * 
     * @param trackColor Track background color
     * @param thumbColor Thumb color
     * @param activeColor Active thumb color (when dragging)
     */
    public void setScrollbarColors(int trackColor, int thumbColor, int activeColor) {
        this.scrollbarTrackColor = trackColor;
        this.scrollbarThumbColor = thumbColor;
        this.scrollbarActiveColor = activeColor;
    }
    
    // Getters
    public int getBackgroundColor() { return backgroundColor; }
    public int getBorderColor() { return borderColor; }
    public boolean isDrawBackground() { return drawBackground; }
    public boolean isDrawBorder() { return drawBorder; }
}
