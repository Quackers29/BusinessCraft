package com.yourdomain.businesscraft.ui.builders;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourdomain.businesscraft.debug.DebugConfig;

/**
 * Utility class for creating grid-based UI layouts with various component types.
 * Supports dynamic generation of buttons, labels, toggles, etc. in a grid layout.
 */
public class UIGridBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(UIGridBuilder.class);

    // Grid configuration
    private int x, y, width, height;
    private int rows, columns;
    private int horizontalSpacing, verticalSpacing;
    private int horizontalMargin, verticalMargin;
    
    // Component holders
    private List<UIGridElement> elements = new ArrayList<>();
    
    // Background styling
    private int backgroundColor = 0x80222222; // Semi-transparent dark gray
    private int borderColor = 0xA0AAAAAA; // Light gray
    private boolean drawBackground = true;
    private boolean drawBorder = true;
    
    // Scrolling functionality
    private boolean horizontalScrollEnabled = false;
    private int horizontalScrollOffset = 0;
    private int maxHorizontalScrollOffset = 0;
    private int scrollBarHeight = 10;
    private boolean horizontalScrolling = false;
    private int visibleColumns = 0;
    private int totalColumns = 0;
    private int scrollButtonSize = 20;
    private boolean useScrollButtons = false;
    
    // Vertical scrolling functionality
    private boolean verticalScrollEnabled = false;
    private int verticalScrollOffset = 0;
    private int maxVerticalScrollOffset = 0;
    private int scrollBarWidth = 8;
    private boolean verticalScrolling = false;
    private int visibleRows = 0;
    private int totalRows = 0;
    
    // Custom row height (default 14px - drastically smaller than before)
    private Integer customRowHeight = 14;
    
    // Drag scrolling state variables
    private boolean isDraggingVertical = false;
    private boolean isDraggingHorizontal = false;
    private double lastMouseY = 0;
    private double lastMouseX = 0;
    
    /**
     * Creates a new grid builder with the specified dimensions and layout
     * 
     * @param x X position of the grid
     * @param y Y position of the grid
     * @param width Width of the grid
     * @param height Height of the grid
     * @param columns Number of columns in the grid
     * @return A new grid builder with default values
     */
    public static UIGridBuilder create(int x, int y, int width, int height, int columns) {
        return new UIGridBuilder(x, y, width, height, 1, columns);
    }
    
    /**
     * Creates a new grid builder with the specified dimensions and layout
     * 
     * @param x X position of the grid
     * @param y Y position of the grid
     * @param width Width of the grid
     * @param height Height of the grid
     * @param rows Number of rows in the grid (can be updated later by data)
     * @param columns Number of columns in the grid
     */
    public UIGridBuilder(int x, int y, int width, int height, int rows, int columns) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.rows = rows;
        this.columns = columns;
        this.horizontalSpacing = 10;
        this.verticalSpacing = 2;
        this.horizontalMargin = 15;
        this.verticalMargin = 6;
    }
    
    /**
     * Sets the spacing between grid cells
     */
    public UIGridBuilder withSpacing(int horizontalSpacing, int verticalSpacing) {
        this.horizontalSpacing = horizontalSpacing;
        this.verticalSpacing = verticalSpacing;
        return this;
    }
    
    /**
     * Sets the margins around the grid
     */
    public UIGridBuilder withMargins(int horizontalMargin, int verticalMargin) {
        this.horizontalMargin = horizontalMargin;
        this.verticalMargin = verticalMargin;
        return this;
    }
    
    /**
     * Sets the background color
     */
    public UIGridBuilder withBackgroundColor(int color) {
        this.backgroundColor = color;
        return this;
    }
    
    /**
     * Sets the border color
     */
    public UIGridBuilder withBorderColor(int color) {
        this.borderColor = color;
        return this;
    }
    
    /**
     * Sets whether to draw the background
     */
    public UIGridBuilder drawBackground(boolean draw) {
        this.drawBackground = draw;
        return this;
    }
    
    /**
     * Sets whether to draw the border
     */
    public UIGridBuilder drawBorder(boolean draw) {
        this.drawBorder = draw;
        return this;
    }
    
    /**
     * Enables horizontal scrolling with a specific number of visible columns
     * @param visibleColumns Number of columns to show at once
     * @param totalColumns Total number of columns in the data (can be more than grid columns)
     * @return This builder for chaining
     */
    public UIGridBuilder withHorizontalScroll(int visibleColumns, int totalColumns) {
        this.horizontalScrollEnabled = true;
        this.visibleColumns = visibleColumns;
        this.totalColumns = totalColumns;
        this.maxHorizontalScrollOffset = Math.max(0, totalColumns - visibleColumns);
        return this;
    }
    
    /**
     * Use buttons for scrolling instead of a scrollbar
     * @param useButtons Whether to use buttons for scrolling
     * @return This builder for chaining
     */
    public UIGridBuilder useScrollButtons(boolean useButtons) {
        this.useScrollButtons = useButtons;
        return this;
    }
    
    /**
     * Sets the current horizontal scroll offset
     * @param offset The new offset
     */
    public void setHorizontalScrollOffset(int offset) {
        if (!horizontalScrollEnabled) return;
        
        // Clamp scroll offset
        if (offset < 0) {
            offset = 0;
        } else if (offset > maxHorizontalScrollOffset) {
            offset = maxHorizontalScrollOffset;
        }
        
        horizontalScrollOffset = offset;
    }
    
    /**
     * Scroll left by one column
     */
    public void scrollLeft() {
        setHorizontalScrollOffset(horizontalScrollOffset - 1);
    }
    
    /**
     * Scroll right by one column
     */
    public void scrollRight() {
        setHorizontalScrollOffset(horizontalScrollOffset + 1);
    }
    
    /**
     * Sets a custom fixed row height
     * @param height Height in pixels for each row
     * @return This builder for chaining
     */
    public UIGridBuilder withRowHeight(int height) {
        this.customRowHeight = height;
        return this;
    }
    
    /**
     * Enables vertical scrolling with a specific number of visible rows
     * @param enable Whether to enable vertical scrolling
     * @param visibleRows Number of rows to show at once
     * @return This builder for chaining
     */
    public UIGridBuilder withVerticalScroll(boolean enable, int visibleRows) {
        this.verticalScrollEnabled = enable;
        this.visibleRows = visibleRows;
        this.totalRows = rows;
        if (enable) {
            this.maxVerticalScrollOffset = Math.max(0, totalRows - visibleRows);
        }
        return this;
    }
    
    /**
     * Sets the current vertical scroll offset
     * @param offset The new offset
     */
    public void setVerticalScrollOffset(int offset) {
        int oldOffset = verticalScrollOffset;
        
        // Clamp the offset to valid range
        if (offset < 0) {
            offset = 0;
        } else if (offset > maxVerticalScrollOffset) {
            offset = maxVerticalScrollOffset;
        }
        
        verticalScrollOffset = offset;
        
        // Debug if the offset changed
        if (oldOffset != verticalScrollOffset) {
            DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER, "Grid vertical offset changed: {} -> {}", oldOffset, verticalScrollOffset);
        }
    }
    
    /**
     * Updates the total number of rows and recalculates scroll parameters
     * This should be called after adding all elements to ensure proper scrolling
     * @param newTotalRows The actual number of rows with content
     */
    public void updateTotalRows(int newTotalRows) {
        this.rows = newTotalRows;
        this.totalRows = newTotalRows;
        
        if (verticalScrollEnabled) {
            this.maxVerticalScrollOffset = Math.max(0, totalRows - visibleRows);
            // Clamp current offset to new valid range
            setVerticalScrollOffset(verticalScrollOffset);
            
            DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER, 
                "Updated total rows: {}, visible: {}, maxOffset: {}", 
                totalRows, visibleRows, maxVerticalScrollOffset);
        }
    }
    
    /**
     * Clears all elements while preserving grid structure and scroll state
     * This allows for data updates without losing scroll position
     */
    public void clearElements() {
        elements.clear();
        DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER, "Cleared all elements, preserving scroll state");
    }
    
    /**
     * Scroll up by one row
     */
    public void scrollUp() {
        setVerticalScrollOffset(verticalScrollOffset - 1);
    }
    
    /**
     * Scroll down by one row
     */
    public void scrollDown() {
        setVerticalScrollOffset(verticalScrollOffset + 1);
    }
    
    /**
     * Adds a button to the grid at the specified position
     * 
     * @param row Row index (0-based)
     * @param column Column index (0-based)
     * @param text Button text
     * @param onClick Button click handler
     * @param bgColor Background color
     * @return This builder for chaining
     */
    public UIGridBuilder addButton(int row, int column, String text, Consumer<Void> onClick, int bgColor) {
        UIGridElement element = new UIGridElement(UIElementType.BUTTON, row, column, 1, 1);
        element.text = text;
        element.onClick = onClick;
        element.backgroundColor = bgColor;
        elements.add(element);
        return this;
    }
    
    /**
     * Adds a button that spans multiple cells
     */
    public UIGridBuilder addButton(int row, int column, int rowSpan, int colSpan, 
                                  String text, Consumer<Void> onClick, int bgColor) {
        UIGridElement element = new UIGridElement(UIElementType.BUTTON, row, column, rowSpan, colSpan);
        element.text = text;
        element.onClick = onClick;
        element.backgroundColor = bgColor;
        elements.add(element);
        return this;
    }
    
    /**
     * Adds a label to the grid
     */
    public UIGridBuilder addLabel(int row, int column, String text, int textColor) {
        UIGridElement element = new UIGridElement(UIElementType.LABEL, row, column, 1, 1);
        element.text = text;
        element.textColor = textColor;
        elements.add(element);
        return this;
    }
    
    /**
     * Adds a label with a tooltip to the grid
     * 
     * @param row Grid row
     * @param column Grid column
     * @param text Label text
     * @param tooltip Tooltip text to show on hover
     * @param textColor Text color
     * @return This builder for chaining
     */
    public UIGridBuilder addLabelWithTooltip(int row, int column, String text, String tooltip, int textColor) {
        UIGridElement element = new UIGridElement(UIElementType.LABEL, row, column, 1, 1);
        element.text = text;
        element.tooltip = tooltip;
        element.textColor = textColor;
        elements.add(element);
        return this;
    }
    
    /**
     * Adds a centered label to the grid
     */
    public UIGridBuilder addCenteredLabel(int row, int column, String text, int textColor) {
        UIGridElement element = new UIGridElement(UIElementType.LABEL, row, column, 1, 1);
        element.text = text;
        element.textColor = textColor;
        element.textAlignment = TextAlignment.CENTER;
        elements.add(element);
        return this;
    }
    
    /**
     * Adds a label with specific alignment to the grid
     */
    public UIGridBuilder addLabelWithAlignment(int row, int column, String text, int textColor, TextAlignment alignment) {
        UIGridElement element = new UIGridElement(UIElementType.LABEL, row, column, 1, 1);
        element.text = text;
        element.textColor = textColor;
        element.textAlignment = alignment;
        elements.add(element);
        return this;
    }
    
    /**
     * Adds a large centered status indicator to the grid
     * @param row Grid row
     * @param column Grid column  
     * @param isActive Whether the status is active (filled circle) or inactive (empty circle)
     * @param activeColor Color for active state
     * @param inactiveColor Color for inactive state
     * @param size Size of the indicator in pixels (default 12)
     * @return This builder for chaining
     */
    public UIGridBuilder addStatusIndicator(int row, int column, boolean isActive, int activeColor, int inactiveColor, int size) {
        UIGridElement element = new UIGridElement(UIElementType.STATUS_INDICATOR, row, column, 1, 1);
        element.text = isActive ? "●" : "○";
        element.textColor = isActive ? activeColor : inactiveColor;
        element.indicatorSize = size;
        elements.add(element);
        return this;
    }
    
    /**
     * Adds a large centered status indicator to the grid with default size (14px)
     */
    public UIGridBuilder addStatusIndicator(int row, int column, boolean isActive, int activeColor, int inactiveColor) {
        return addStatusIndicator(row, column, isActive, activeColor, inactiveColor, 14);
    }
    
    /**
     * Adds a toggle button to the grid
     */
    public UIGridBuilder addToggle(int row, int column, String text, boolean initialState,
                                  Consumer<Boolean> onToggle, int enabledColor, int disabledColor) {
        UIGridElement element = new UIGridElement(UIElementType.TOGGLE, row, column, 1, 1);
        element.text = text;
        element.toggled = initialState;
        element.onToggle = onToggle;
        element.backgroundColor = initialState ? enabledColor : disabledColor;
        element.altBackgroundColor = initialState ? disabledColor : enabledColor;
        elements.add(element);
        return this;
    }
    
    /**
     * Adds a button with a tooltip
     * 
     * @param row Grid row
     * @param column Grid column
     * @param text Button text
     * @param tooltip Tooltip text to show on hover
     * @param onClick Button click handler
     * @param bgColor Background color
     * @return This builder for chaining
     */
    public UIGridBuilder addButtonWithTooltip(int row, int column, String text, String tooltip, Consumer<Void> onClick, int bgColor) {
        UIGridElement element = new UIGridElement(UIElementType.BUTTON, row, column, 1, 1);
        element.text = text;
        element.tooltip = tooltip;
        element.onClick = onClick;
        element.backgroundColor = bgColor;
        elements.add(element);
        return this;
    }
    
    /**
     * Adds a button with a tooltip that spans multiple cells
     * 
     * @param row Grid row
     * @param column Grid column
     * @param rowSpan Number of rows to span
     * @param colSpan Number of columns to span
     * @param text Button text
     * @param tooltip Tooltip text to show on hover
     * @param onClick Button click handler
     * @param bgColor Background color
     * @return This builder for chaining
     */
    public UIGridBuilder addButtonWithTooltip(int row, int column, int rowSpan, int colSpan, 
                                         String text, String tooltip, Consumer<Void> onClick, int bgColor) {
        UIGridElement element = new UIGridElement(UIElementType.BUTTON, row, column, rowSpan, colSpan);
        element.text = text;
        element.tooltip = tooltip;
        element.onClick = onClick;
        element.backgroundColor = bgColor;
        elements.add(element);
        return this;
    }
    
    /**
     * Adds a toggle button with a tooltip
     * 
     * @param row Grid row
     * @param column Grid column
     * @param text Toggle button text
     * @param tooltip Tooltip text to show on hover
     * @param initialState Initial toggle state
     * @param onToggle Toggle state change handler
     * @param enabledColor Color when enabled
     * @param disabledColor Color when disabled
     * @return This builder for chaining
     */
    public UIGridBuilder addToggleWithTooltip(int row, int column, String text, String tooltip, boolean initialState,
                                          Consumer<Boolean> onToggle, int enabledColor, int disabledColor) {
        UIGridElement element = new UIGridElement(UIElementType.TOGGLE, row, column, 1, 1);
        element.text = text;
        element.tooltip = tooltip;
        element.toggled = initialState;
        element.onToggle = onToggle;
        element.backgroundColor = initialState ? enabledColor : disabledColor;
        element.altBackgroundColor = initialState ? disabledColor : enabledColor;
        elements.add(element);
        return this;
    }
    
    /**
     * Adds an item display element to the grid
     * 
     * @param row Row index (0-based)
     * @param column Column index (0-based)
     * @param item The Minecraft item to display
     * @param quantity The quantity to display
     * @param onClick Callback for when the item is clicked (can be null)
     * @return This builder for chaining
     */
    public UIGridBuilder addItem(int row, int column, net.minecraft.world.item.Item item, int quantity, Consumer<Void> onClick) {
        UIGridElement element = new UIGridElement(UIElementType.ITEM, row, column, 1, 1);
        element.item = item;
        element.quantity = quantity;
        element.onClick = onClick;
        element.showQuantity = true; // Always show quantity, even for single items
        elements.add(element);
        return this;
    }
    
    /**
     * Adds an item display element to the grid that spans multiple cells
     * 
     * @param row Row index (0-based)
     * @param column Column index (0-based)
     * @param rowSpan Number of rows to span
     * @param colSpan Number of columns to span
     * @param item The Minecraft item to display
     * @param quantity The quantity to display
     * @param onClick Callback for when the item is clicked (can be null)
     * @return This builder for chaining
     */
    public UIGridBuilder addItem(int row, int column, int rowSpan, int colSpan,
                               net.minecraft.world.item.Item item, int quantity, Consumer<Void> onClick) {
        UIGridElement element = new UIGridElement(UIElementType.ITEM, row, column, rowSpan, colSpan);
        element.item = item;
        element.quantity = quantity;
        element.onClick = onClick;
        element.showQuantity = true; // Always show quantity, even for single items
        elements.add(element);
        return this;
    }
    
    /**
     * Adds an item display element with a tooltip
     * 
     * @param row Row index (0-based)
     * @param column Column index (0-based)
     * @param item The Minecraft item to display
     * @param quantity The quantity to display
     * @param tooltip Tooltip text to show on hover
     * @param onClick Callback for when the item is clicked (can be null)
     * @return This builder for chaining
     */
    public UIGridBuilder addItemWithTooltip(int row, int column, net.minecraft.world.item.Item item, 
                                          int quantity, String tooltip, Consumer<Void> onClick) {
        UIGridElement element = new UIGridElement(UIElementType.ITEM, row, column, 1, 1);
        element.item = item;
        element.quantity = quantity;
        element.tooltip = tooltip;
        element.onClick = onClick;
        element.showQuantity = true; // Always show quantity, even for single items
        elements.add(element);
        return this;
    }
    
    /**
     * Adds an item display element to the grid using an ItemStack
     * 
     * @param row Row index (0-based)
     * @param column Column index (0-based)
     * @param itemStack The ItemStack to display
     * @param onClick Callback for when the item is clicked (can be null)
     * @return This builder for chaining
     */
    public UIGridBuilder addItemStack(int row, int column, net.minecraft.world.item.ItemStack itemStack, Consumer<Void> onClick) {
        UIGridElement element = new UIGridElement(UIElementType.ITEM, row, column, 1, 1);
        element.item = itemStack.getItem();
        element.quantity = itemStack.getCount();
        element.itemStack = itemStack; // Store full itemstack for rendering
        element.onClick = onClick;
        element.showQuantity = true; // Always show quantity, even for single items
        elements.add(element);
        return this;
    }
    
    /**
     * Adds an item display element to the grid that spans multiple cells using an ItemStack
     * 
     * @param row Row index (0-based)
     * @param column Column index (0-based)
     * @param rowSpan Number of rows to span
     * @param colSpan Number of columns to span
     * @param itemStack The ItemStack to display
     * @param onClick Callback for when the item is clicked (can be null)
     * @return This builder for chaining
     */
    public UIGridBuilder addItemStack(int row, int column, int rowSpan, int colSpan,
                               net.minecraft.world.item.ItemStack itemStack, Consumer<Void> onClick) {
        UIGridElement element = new UIGridElement(UIElementType.ITEM, row, column, rowSpan, colSpan);
        element.item = itemStack.getItem();
        element.quantity = itemStack.getCount();
        element.itemStack = itemStack; // Store full itemstack for rendering
        element.onClick = onClick;
        element.showQuantity = true; // Always show quantity, even for single items
        elements.add(element);
        return this;
    }
    
    /**
     * Adds an item display element with a tooltip using an ItemStack
     * 
     * @param row Row index (0-based)
     * @param column Column index (0-based)
     * @param itemStack The ItemStack to display
     * @param tooltip Tooltip text to show on hover (optional, null to use item's tooltip)
     * @param onClick Callback for when the item is clicked (can be null)
     * @return This builder for chaining
     */
    public UIGridBuilder addItemStackWithTooltip(int row, int column, net.minecraft.world.item.ItemStack itemStack, 
                                          String tooltip, Consumer<Void> onClick) {
        UIGridElement element = new UIGridElement(UIElementType.ITEM, row, column, 1, 1);
        element.item = itemStack.getItem();
        element.quantity = itemStack.getCount();
        element.itemStack = itemStack; // Store full itemstack for rendering
        element.tooltip = tooltip;
        element.onClick = onClick;
        element.showQuantity = true; // Always show quantity, even for single items
        elements.add(element);
        return this;
    }
    
    /**
     * Adds multiple overlapping items for visual display (up to 4 items)
     * @param row Row in the grid
     * @param column Column in the grid
     * @param itemStacks List of ItemStacks to display overlapping
     * @param tooltip Tooltip text for the items
     * @param onClick Click handler
     * @return This builder for chaining
     */
    public UIGridBuilder addMultiItemDisplay(int row, int column, List<net.minecraft.world.item.ItemStack> itemStacks, 
                                           String tooltip, Consumer<Void> onClick) {
        if (itemStacks == null || itemStacks.isEmpty()) {
            return this;
        }
        
        UIGridElement element = new UIGridElement(UIElementType.MULTI_ITEM, row, column, 1, 1);
        element.multiItems = new ArrayList<>(itemStacks.subList(0, Math.min(4, itemStacks.size()))); // Max 4 items
        element.tooltip = tooltip;
        element.onClick = onClick;
        element.showQuantity = false; // Don't show quantities for multi-item display
        elements.add(element);
        return this;
    }
    
    /**
     * Checks if the mouse is over a specific row in the grid with proper spacing
     * @param mouseX Mouse X coordinate (screen relative)
     * @param mouseY Mouse Y coordinate (screen relative)
     * @param rowIndex The row index to check (0-based)
     * @param columnsToCheck Number of columns to check (typically 1-2 for tooltips)
     * @return True if mouse is over the specified row area
     */
    public boolean isMouseOverRow(int mouseX, int mouseY, int rowIndex, int columnsToCheck) {
        if (rowIndex < 0) {
            return false;
        }
        
        // Calculate actual row dimensions based on grid layout
        int rowHeight = customRowHeight != null ? customRowHeight : 20;
        int rowY = y + verticalMargin + (rowIndex * (rowHeight + verticalSpacing));
        
        // Apply vertical scroll offset if scrolling is enabled
        if (verticalScrollEnabled) {
            rowY -= (verticalScrollOffset * (rowHeight + verticalSpacing));
        }
        
        // Calculate column area - typically first 1-2 columns for tooltips
        int columnStartX = x + horizontalMargin;
        int cellWidth = (width - 2 * horizontalMargin - (columns - 1) * horizontalSpacing) / columns;
        int columnWidth = (cellWidth * columnsToCheck) + ((columnsToCheck - 1) * horizontalSpacing);
        
        // Add small margins to prevent tooltip overlap between rows
        int rowMargin = 1; // 1px margin between rows to prevent bleed
        
        return mouseX >= columnStartX && mouseX <= columnStartX + columnWidth &&
               mouseY >= rowY + rowMargin && mouseY <= rowY + rowHeight - rowMargin;
    }
    
    /**
     * Gets the number of visible rows in the grid (accounting for scrolling)
     * @return Number of rows currently displayed
     */
    public int getVisibleRowCount() {
        return verticalScrollEnabled ? visibleRows : totalRows;
    }
    
    /**
     * Gets the total number of rows in the grid
     * @return Total number of rows
     */
    public int getTotalRowCount() {
        return totalRows;
    }
    
    /**
     * Gets the current vertical scroll offset
     * @return Current scroll offset in rows
     */
    public int getVerticalScrollOffset() {
        return verticalScrollOffset;
    }
    
    /**
     * Renders the grid and its elements
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw grid background if enabled
        if (drawBackground) {
            graphics.fill(x, y, x + width, y + height, backgroundColor);
        }
        
        // Draw grid border if enabled
        if (drawBorder) {
            // Top, Bottom
            graphics.hLine(x, x + width - 1, y, borderColor);
            graphics.hLine(x, x + width - 1, y + height - 1, borderColor);
            // Left, Right
            graphics.vLine(x, y, y + height - 1, borderColor);
            graphics.vLine(x + width - 1, y, y + height - 1, borderColor);
        }
        
        // Calculate cell dimensions
        int cellsWidth = width - (horizontalMargin * 2);
        int cellsHeight = height - (verticalMargin * 2);
        
        // Calculate individual cell dimensions
        int effectiveColumns = horizontalScrollEnabled ? Math.min(columns, visibleColumns) : columns;
        int effectiveRows = verticalScrollEnabled ? Math.min(visibleRows, rows) : rows;
        
        int cellWidth = (cellsWidth - (horizontalSpacing * (effectiveColumns - 1))) / effectiveColumns;
        int rowHeight = customRowHeight != null ? customRowHeight : 
                       (cellsHeight - (verticalSpacing * (effectiveRows - 1))) / effectiveRows;
        
        // Track if any element is currently hovered
        for (UIGridElement element : elements) {
            element.isHovered = false;
        }
        
        // Render all visible elements
        for (UIGridElement element : elements) {
            // Skip elements outside the visible range for horizontal scrolling
            if (horizontalScrollEnabled && (element.column < horizontalScrollOffset || 
                element.column >= horizontalScrollOffset + visibleColumns)) {
                continue;
            }
            
            // Skip elements outside the visible range for vertical scrolling
            if (verticalScrollEnabled && (element.row < verticalScrollOffset || 
                element.row >= verticalScrollOffset + visibleRows)) {
                continue;
            }
            
            // Calculate adjusted column and row positions for scrolling
            int adjustedColumn = horizontalScrollEnabled ? element.column - horizontalScrollOffset : element.column;
            int adjustedRow = verticalScrollEnabled ? element.row - verticalScrollOffset : element.row;
            
            // Calculate element position and size, accounting for spans
            int elementX = x + horizontalMargin + (adjustedColumn * (cellWidth + horizontalSpacing));
            int elementY = y + verticalMargin + (adjustedRow * (rowHeight + verticalSpacing));
            int elementWidth = cellWidth * element.colSpan + (element.colSpan - 1) * horizontalSpacing;
            int elementHeight = rowHeight * element.rowSpan + (element.rowSpan - 1) * verticalSpacing;
            
            // Check if element is hovered
            boolean hovered = mouseX >= elementX && mouseX < elementX + elementWidth &&
                              mouseY >= elementY && mouseY < elementY + elementHeight;
            
            element.isHovered = hovered;
            
            // Render the element based on its type
            switch (element.type) {
                case BUTTON:
                    renderButton(graphics, element, elementX, elementY, elementWidth, elementHeight, hovered);
                    break;
                case LABEL:
                    renderLabel(graphics, element, elementX, elementY, elementWidth, elementHeight);
                    break;
                case TOGGLE:
                    renderToggle(graphics, element, elementX, elementY, elementWidth, elementHeight, hovered);
                    break;
                case ITEM:
                    renderItem(graphics, element, elementX, elementY, elementWidth, elementHeight);
                    break;
                case MULTI_ITEM:
                    renderMultiItem(graphics, element, elementX, elementY, elementWidth, elementHeight);
                    break;
                case STATUS_INDICATOR:
                    renderStatusIndicator(graphics, element, elementX, elementY, elementWidth, elementHeight);
                    break;
            }
        }
        
        // Render scrollbars
        if (horizontalScrollEnabled) {
            renderHorizontalScrollbar(graphics, mouseX, mouseY);
        }
        
        if (verticalScrollEnabled) {
            renderVerticalScrollbar(graphics, mouseX, mouseY);
        }
        
        // Render tooltips for hovered elements
        for (UIGridElement element : elements) {
            if (element.isHovered && element.tooltip != null && !element.tooltip.isEmpty()) {
                // Use Minecraft's tooltip rendering (converted to Component)
                Minecraft mc = Minecraft.getInstance();
                graphics.renderTooltip(mc.font, Component.literal(element.tooltip), mouseX, mouseY);
                break; // Only show one tooltip at a time
            }
        }
    }
    
    /**
     * Renders a button element
     */
    private void renderButton(GuiGraphics graphics, UIGridElement element, 
                             int x, int y, int width, int height, boolean hovered) {
        // Determine button color (brighter when hovered)
        int buttonColor = element.backgroundColor;
        if (hovered) {
            // Make color more opaque when hovered
            int alpha = (buttonColor >> 24) & 0xFF;
            int rgb = buttonColor & 0xFFFFFF;
            buttonColor = ((Math.min(255, alpha + 0x20)) << 24) | rgb;
        }
        
        // Draw button background - use the full height and width provided
        graphics.fill(x, y, x + width, y + height, buttonColor);
        
        // Draw button border
        graphics.hLine(x, x + width - 1, y, borderColor);
        graphics.hLine(x, x + width - 1, y + height - 1, borderColor);
        graphics.vLine(x, y, y + height - 1, borderColor);
        graphics.vLine(x + width - 1, y, y + height - 1, borderColor);
        
        // Draw centered text - center vertically in the whole button area
        Font font = Minecraft.getInstance().font;
        int textX = x + width / 2 - font.width(element.text) / 2;
        int textY = y + (height - font.lineHeight) / 2; // Centered within the actual height
        graphics.drawString(font, element.text, textX, textY, 0xFFFFFFFF);
    }
    
    /**
     * Renders a label element
     */
    private void renderLabel(GuiGraphics graphics, UIGridElement element, 
                            int x, int y, int width, int height) {
        Font font = Minecraft.getInstance().font;
        
        // Use the text as provided (truncation should be done before adding to grid)
        String displayText = element.text;
        
        // Calculate text position based on alignment
        int textX;
        int textWidth = font.width(displayText);
        
        switch (element.textAlignment) {
            case CENTER:
                textX = x + (width - textWidth) / 2; // Centered
                break;
            case RIGHT:
                textX = x + width - textWidth - 2; // Right-aligned with 2px padding
                break;
            case LEFT:
            default:
                textX = x + 2; // Left-aligned with 2px padding
                break;
        }
        
        int textY = y + (height - font.lineHeight) / 2; // Vertically centered
        graphics.drawString(font, displayText, textX, textY, element.textColor);
    }
    
    /**
     * Renders a large centered status indicator
     */
    private void renderStatusIndicator(GuiGraphics graphics, UIGridElement element,
                                     int x, int y, int width, int height) {
        
        // Calculate center position for the indicator
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        int radius = element.indicatorSize / 2;
        
        // Draw flat 2D circle indicator 
        if (element.text.equals("●")) {
            // Draw filled circle (ON state)
            drawFilledCircle(graphics, centerX, centerY, radius, element.textColor);
        } else {
            // Draw empty circle (OFF state) 
            drawCircleOutline(graphics, centerX, centerY, radius, element.textColor, 1);
        }
    }
    
    /**
     * Draws a filled circle
     */
    private void drawFilledCircle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                if (dx * dx + dy * dy <= radius * radius) {
                    graphics.fill(centerX + dx, centerY + dy, centerX + dx + 1, centerY + dy + 1, color);
                }
            }
        }
    }
    
    /**
     * Draws a circle outline
     */
    private void drawCircleOutline(GuiGraphics graphics, int centerX, int centerY, int radius, int color, int thickness) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int distSq = dx * dx + dy * dy;
                int outerRadiusSq = radius * radius;
                int innerRadiusSq = (radius - thickness) * (radius - thickness);
                
                if (distSq <= outerRadiusSq && distSq >= innerRadiusSq) {
                    graphics.fill(centerX + dx, centerY + dy, centerX + dx + 1, centerY + dy + 1, color);
                }
            }
        }
    }
    
    /**
     * Renders a toggle button element
     */
    private void renderToggle(GuiGraphics graphics, UIGridElement element, 
                             int x, int y, int width, int height, boolean hovered) {
        // Determine toggle color
        int toggleColor = element.backgroundColor;
        if (hovered) {
            // Make color more opaque when hovered
            int alpha = (toggleColor >> 24) & 0xFF;
            int rgb = toggleColor & 0xFFFFFF;
            toggleColor = ((Math.min(255, alpha + 0x20)) << 24) | rgb;
        }
        
        // Draw toggle background
        graphics.fill(x, y, x + width, y + height, toggleColor);
        
        // Draw toggle border
        graphics.hLine(x, x + width - 1, y, borderColor);
        graphics.hLine(x, x + width - 1, y + height - 1, borderColor);
        graphics.vLine(x, y, y + height - 1, borderColor);
        graphics.vLine(x + width - 1, y, y + height - 1, borderColor);
        
        // Draw indicator circle
        int indicatorSize = height - 6;
        int indicatorY = y + 3;
        int indicatorX = element.toggled ? 
                        (x + width - indicatorSize - 3) : // Right side when on
                        (x + 3);                          // Left side when off
                        
        // Draw indicator with contrasting color
        graphics.fill(indicatorX, indicatorY, indicatorX + indicatorSize, indicatorY + indicatorSize, 0xFFFFFFFF);
        
        // Draw centered text
        int textX = x + width / 2 - Minecraft.getInstance().font.width(element.text) / 2;
        int textY = y + (height - 8) / 2;
        graphics.drawString(Minecraft.getInstance().font, element.text, textX, textY, 0xFFFFFFFF);
    }
    
    /**
     * Formats a number for display, shortening large numbers with suffixes (K, M, B)
     * 
     * @param number The number to format
     * @return Formatted string representation
     */
    private String formatNumber(int number) {
        if (number < 1000) {
            return String.valueOf(number);
        } else if (number < 10000) {
            // 1000-9999 -> #.#K (1.2K, 9.9K)
            return String.format("%.1fK", number / 1000.0).replace(".0K", "K");
        } else if (number < 1000000) {
            // 10000-999999 -> ##K (10K, 999K)
            return Math.round(number / 1000.0) + "K";
        } else if (number < 10000000) {
            // 1000000-9999999 -> #.#M (1.2M, 9.9M)
            return String.format("%.1fM", number / 1000000.0).replace(".0M", "M");
        } else if (number < 1000000000) {
            // 10000000-999999999 -> ##M (10M, 999M)
            return Math.round(number / 1000000.0) + "M";
            } else {
            // 1000000000+ -> #.#B (1.2B, 9.9B)
            return String.format("%.1fB", number / 1000000000.0).replace(".0B", "B");
        }
    }

    /**
     * Renders an item element
     */
    private void renderItem(GuiGraphics graphics, UIGridElement element, 
                           int x, int y, int width, int height) {
        // Draw a background for the item (lighter when hovered)
        int bgColor = element.isHovered ? 0x80444444 : 0x60222222;
        graphics.fill(x, y, x + width, y + height, bgColor);
        
        if (element.item != null) {
            Font font = Minecraft.getInstance().font;
            
            // Calculate the item position - align to the left with some padding
            int itemSize = 16; // Standard Minecraft item size
            int itemY = y + (height - itemSize) / 2; // Center vertically
            int itemX = x + 5; // Left align with padding
            
            // Render the item - use the stored ItemStack if available, otherwise create one
            net.minecraft.world.item.ItemStack stack = element.itemStack != null ? 
                element.itemStack : new net.minecraft.world.item.ItemStack(element.item, element.quantity);
            
            // Render the item
            graphics.renderItem(stack, itemX, itemY);
            
            // If we have a quantity, show it with an 'x' symbol
            if (element.showQuantity) {
                // Format the quantity
                String quantityText = formatNumber(element.quantity);
                
                // Calculate the multiplier (x) position - center in remaining space
                String multiplier = "x"; // Use simple ASCII "x" instead of Unicode
                int multiplierWidth = font.width(multiplier);
                int contentWidth = width - 10 - itemSize; // Total width minus item and padding
                int multiplierX = itemX + itemSize + 5 + (contentWidth - multiplierWidth - font.width(quantityText)) / 2;
                int textY = y + (height - font.lineHeight) / 2; // Center text vertically
                
                // Draw the multiplier
                graphics.drawString(
                    font,
                    multiplier,
                    multiplierX,
                    textY,
                    0xFFAAAAAA // Light gray color for the multiplier
                );
                
                // Draw the quantity text right-aligned
                graphics.drawString(
                    font,
                    quantityText,
                    x + width - font.width(quantityText) - 5, // Right-aligned with padding
                    textY,
                    0xFFFFFFFF // White color for the quantity
                );
            }
        }
    }
    
    /**
     * Renders multiple overlapping items for visual display
     */
    private void renderMultiItem(GuiGraphics graphics, UIGridElement element, 
                                 int x, int y, int width, int height) {
        if (element.multiItems == null || element.multiItems.isEmpty()) {
            return;
        }
        
        // Draw a background for the items (lighter when hovered)
        int bgColor = element.isHovered ? 0x80444444 : 0x60222222;
        graphics.fill(x, y, x + width, y + height, bgColor);
        
        int itemSize = 16; // Standard Minecraft item size
        int itemY = y + (height - itemSize) / 2; // Center vertically
        int startX = x + 5; // Starting position with padding
        
        // Calculate spacing to spread items across full available width
        int availableWidth = width - 10 - itemSize; // Total width minus padding and one item width
        int itemCount = Math.min(4, element.multiItems.size());
        int overlapOffset;
        
        if (itemCount <= 1) {
            overlapOffset = 0; // Single item, no offset needed
        } else {
            // Spread items across available width, minimum 8px spacing, maximum 20px
            overlapOffset = Math.min(20, Math.max(8, availableWidth / (itemCount - 1)));
        }
        
        // Render up to 4 items with improved spacing
        for (int i = 0; i < itemCount; i++) {
            net.minecraft.world.item.ItemStack stack = element.multiItems.get(i);
            if (!stack.isEmpty()) {
                int itemX = startX + (i * overlapOffset);
                
                // Make sure we don't go out of bounds
                if (itemX + itemSize <= x + width - 5) {
                    graphics.renderItem(stack, itemX, itemY);
                }
            }
        }
        
        // If we have more than 4 items, show a "+" indicator
        if (element.multiItems.size() > 4) {
            Font font = Minecraft.getInstance().font;
            String plusText = "+" + (element.multiItems.size() - 4);
            int textWidth = font.width(plusText);
            int textX = x + width - textWidth - 5;
            int textY = y + (height - font.lineHeight) / 2;
            
            graphics.drawString(font, plusText, textX, textY, 0xFFFFFFFF);
        }
    }
    
    /**
     * Types of UI elements supported in the grid
     */
    private enum UIElementType {
        BUTTON,
        LABEL,
        TOGGLE,
        ITEM,       // Single Minecraft item with quantity indicator
        MULTI_ITEM, // Multiple overlapping Minecraft items
        STATUS_INDICATOR // Large centered status indicator
    }
    
    /**
     * Text alignment options for grid elements
     */
    public enum TextAlignment {
        LEFT,
        CENTER,
        RIGHT
    }
    
    /**
     * Class representing a single element in the grid
     */
    private static class UIGridElement {
        UIElementType type;
        int row, column;
        int rowSpan, colSpan;
        String text;
        int textColor = 0xFFFFFFFF;
        int backgroundColor = 0xA0335599;
        int altBackgroundColor = 0x80555555; // For toggle buttons
        boolean toggled = false;
        Consumer<Void> onClick;
        Consumer<Boolean> onToggle;
        String tooltip; // Tooltip text to display on hover
        boolean isHovered = false; // Track hover state for tooltip rendering
        TextAlignment textAlignment = TextAlignment.LEFT; // Text alignment for labels
        
        // New fields for item display
        net.minecraft.world.item.Item item; // The Minecraft item to display
        int quantity = 1;    // Quantity to display
        boolean showQuantity = true; // Whether to show quantity indicator
        net.minecraft.world.item.ItemStack itemStack; // Full itemstack for rendering
        List<net.minecraft.world.item.ItemStack> multiItems; // Multiple items for overlapping display
        
        // Status indicator specific fields
        int indicatorSize = 12; // Size of status indicator in pixels
        
        public UIGridElement(UIElementType type, int row, int column, int rowSpan, int colSpan) {
            this.type = type;
            this.row = row;
            this.column = column;
            this.rowSpan = rowSpan;
            this.colSpan = colSpan;
        }
    }
    
    /**
     * Handles mouse clicks on the grid elements
     * @return true if a click was handled
     */
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        // Check for scroll button clicks first
        if (horizontalScrollEnabled && useScrollButtons) {
            // Check left button
            if (mouseX >= x + horizontalMargin && mouseX < x + horizontalMargin + scrollButtonSize &&
                mouseY >= y + height / 2 - scrollButtonSize / 2 && mouseY < y + height / 2 + scrollButtonSize / 2) {
                if (horizontalScrollOffset > 0) {
                    scrollLeft();
                    return true;
                }
            }
            
            // Check right button
            if (mouseX >= x + width - horizontalMargin - scrollButtonSize && mouseX < x + width - horizontalMargin &&
                mouseY >= y + height / 2 - scrollButtonSize / 2 && mouseY < y + height / 2 + scrollButtonSize / 2) {
                if (horizontalScrollOffset < maxHorizontalScrollOffset) {
                    scrollRight();
                    return true;
                }
            }
        }
        
        // Check for horizontal scrollbar drag
        if (horizontalScrollEnabled && !useScrollButtons) {
            int availableWidth = width - (2 * horizontalMargin) - ((columns - 1) * horizontalSpacing);
            
            if (mouseY >= y + height - verticalMargin - scrollBarHeight && 
                mouseY < y + height - verticalMargin &&
                mouseX >= x + horizontalMargin && 
                mouseX < x + horizontalMargin + availableWidth) {
                
                // Calculate new scroll position based on click position
                float relativeX = (float)(mouseX - (x + horizontalMargin)) / availableWidth;
                setHorizontalScrollOffset((int)(relativeX * maxHorizontalScrollOffset));
                return true;
            }
        }
        
        // Check for vertical scrollbar drag - must use the same position as in rendering
        if (verticalScrollEnabled && totalRows > visibleRows) {
            // Use the same scrollbar position as in renderVerticalScrollbar method
            int scrollBarX = x + width + 20; // Match rendering position - outside grid
            int scrollTrackY = y + verticalMargin + 2; // Match rendering position
            int scrollTrackHeight = height - (verticalMargin * 2) - 4; // Match rendering calculation
            
            if (mouseX >= scrollBarX && mouseX < scrollBarX + scrollBarWidth &&
                mouseY >= scrollTrackY && mouseY < scrollTrackY + scrollTrackHeight) {
                
                // Calculate new scroll position based on click position
                float relativeY = (float)(mouseY - scrollTrackY) / scrollTrackHeight;
                setVerticalScrollOffset((int)(relativeY * maxVerticalScrollOffset));
                return true;
            }
        }
        
        // Calculate cell dimensions for element click detection
        int availableWidth = width - (2 * horizontalMargin);
        if (visibleColumns > 0 && horizontalScrollEnabled) {
            availableWidth -= ((visibleColumns - 1) * horizontalSpacing);
        } else {
            availableWidth -= ((columns - 1) * horizontalSpacing);
        }
        
        int availableHeight = height - (2 * verticalMargin);
        
        // Adjust height for horizontal scrollbar if needed
        if (horizontalScrollEnabled && !useScrollButtons) {
            availableHeight -= scrollBarHeight;
        }
        
        // Adjust width for vertical scrollbar if needed
        if (verticalScrollEnabled) {
            availableWidth -= scrollBarWidth;
        }
        
        // Calculate column width
        int columnCount = horizontalScrollEnabled ? visibleColumns : columns;
        int cellWidth = availableWidth / columnCount;
        
        // Calculate row height - use custom height if set, otherwise calculate based on available space
        int rowCount = verticalScrollEnabled ? visibleRows : rows;
        int cellHeight = customRowHeight != null ? customRowHeight : 
                        (availableHeight - ((rowCount - 1) * verticalSpacing)) / rowCount;
        
        // Check each element
        for (UIGridElement element : elements) {
            // Skip elements outside the visible range when using horizontal scrolling
            if (horizontalScrollEnabled) {
                if (element.column < horizontalScrollOffset || 
                    element.column >= horizontalScrollOffset + visibleColumns) {
                    continue;
                }
            }
            
            // Skip elements outside the visible range when using vertical scrolling
            if (verticalScrollEnabled) {
                if (element.row < verticalScrollOffset || 
                    element.row >= verticalScrollOffset + visibleRows) {
                    continue;
                }
            }
            
            // Calculate element position and size, adjusting for scroll offsets
            int elementX;
            int elementY;
            
            if (horizontalScrollEnabled) {
                int adjustedColumn = element.column - horizontalScrollOffset;
                int startX = x + horizontalMargin;
                if (useScrollButtons) {
                    startX += scrollButtonSize; // Account for left scroll button
                }
                elementX = startX + (adjustedColumn * (cellWidth + horizontalSpacing));
            } else {
                elementX = x + horizontalMargin + (element.column * (cellWidth + horizontalSpacing));
            }
            
            if (verticalScrollEnabled) {
                int adjustedRow = element.row - verticalScrollOffset;
                elementY = y + verticalMargin + (adjustedRow * (cellHeight + verticalSpacing));
            } else {
                elementY = y + verticalMargin + (element.row * (cellHeight + verticalSpacing));
            }
            
            int elementWidth = (cellWidth * element.colSpan) + ((element.colSpan - 1) * horizontalSpacing);
            int elementHeight = (cellHeight * element.rowSpan) + ((element.rowSpan - 1) * verticalSpacing);
            
            // Check if click is within this element
            if (mouseX >= elementX && mouseX < elementX + elementWidth &&
                mouseY >= elementY && mouseY < elementY + elementHeight) {
                
                // Handle based on element type
                switch (element.type) {
                    case BUTTON:
                        if (element.onClick != null) {
                            element.onClick.accept(null);
                            return true;
                        }
                        break;
                        
                    case TOGGLE:
                        if (element.onToggle != null) {
                            // Toggle state
                            element.toggled = !element.toggled;
                            
                            // Swap colors
                            int temp = element.backgroundColor;
                            element.backgroundColor = element.altBackgroundColor;
                            element.altBackgroundColor = temp;
                            
                            // Call handler
                            element.onToggle.accept(element.toggled);
                            return true;
                        }
                        break;
                        
                    case ITEM:
                        if (element.onClick != null) {
                            element.onClick.accept(null);
                            return true;
                        }
                        break;
                    case MULTI_ITEM:
                        if (element.onClick != null) {
                            element.onClick.accept(null);
                            return true;
                        }
                        break;
                }
            }
        }
        
        // No element was clicked
        return false;
    }
    
    /**
     * Handle mouse scrolling
     * @return true if handled
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Always handle mouseScrolled regardless of position
        // This matches the Population tab's approach
        
        // For vertical scrolling
        if (verticalScrollEnabled && maxVerticalScrollOffset > 0) {
            // Apply scrolling directly based on delta sign (delta > 0 means scroll up)
            // Use the Population tab approach that works
            verticalScrollOffset -= (int)Math.signum(delta);
            
            // Clamp scroll position
            if (verticalScrollOffset < 0) {
                verticalScrollOffset = 0;
            }
            if (verticalScrollOffset > maxVerticalScrollOffset) {
                verticalScrollOffset = maxVerticalScrollOffset;
            }
            
            DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER, "UIGridBuilder scrolling: delta={}, offset={}", delta, verticalScrollOffset);
            return true;
        }
        
        // For horizontal scrolling if vertical scrolling is not enabled
        if (horizontalScrollEnabled && maxHorizontalScrollOffset > 0) {
            // Apply scrolling directly based on delta sign
            horizontalScrollOffset -= (int)Math.signum(delta);
            
            // Clamp scroll position
            if (horizontalScrollOffset < 0) {
                horizontalScrollOffset = 0;
            }
            if (horizontalScrollOffset > maxHorizontalScrollOffset) {
                horizontalScrollOffset = maxHorizontalScrollOffset;
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Enum for different types of grid content
     */
    public enum GridContentType {
        TEXT,
        ITEM,
        ITEM_WITH_QUANTITY,
        TOGGLE,
        BUTTON
    }
    
    /**
     * Class representing a single grid content item
     */
    public static class GridContent {
        private GridContentType type;
        private Object value;
        private int textColor = 0xFFFFFFFF;  // Default text color
        private String tooltip;
        private Consumer<Void> onClick;
        private Consumer<Boolean> onToggle;
        private int bgColor = 0xA0335599;    // Default background color
        
        // Constructor for text
        public static GridContent text(String text, int textColor) {
            GridContent content = new GridContent();
            content.type = GridContentType.TEXT;
            content.value = text;
            content.textColor = textColor;
            return content;
        }
        
        // Constructor for item
        public static GridContent item(net.minecraft.world.item.Item item) {
            GridContent content = new GridContent();
            content.type = GridContentType.ITEM;
            content.value = item;
            return content;
        }
        
        // Constructor for item with quantity
        public static GridContent itemWithQuantity(net.minecraft.world.item.Item item, int quantity) {
            GridContent content = new GridContent();
            content.type = GridContentType.ITEM_WITH_QUANTITY;
            content.value = new Object[]{item, quantity};
            return content;
        }
        
        // Constructor for toggle
        public static GridContent toggle(String text, boolean initialState, Consumer<Boolean> onToggle, 
                                        int enabledColor, int disabledColor) {
            GridContent content = new GridContent();
            content.type = GridContentType.TOGGLE;
            content.value = new Object[]{text, initialState};
            content.onToggle = onToggle;
            content.bgColor = initialState ? enabledColor : disabledColor;
            return content;
        }
        
        // Constructor for button
        public static GridContent button(String text, Consumer<Void> onClick, int bgColor) {
            GridContent content = new GridContent();
            content.type = GridContentType.BUTTON;
            content.value = text;
            content.onClick = onClick;
            content.bgColor = bgColor;
            return content;
        }
        
        // Add tooltip to any content type
        public GridContent withTooltip(String tooltip) {
            this.tooltip = tooltip;
            return this;
        }
        
        // Add click handler to any content type
        public GridContent withClickHandler(Consumer<Void> onClick) {
            this.onClick = onClick;
            return this;
        }
    }
    
    /**
     * Build a grid from column data arrays
     * 
     * @param columnData Arrays of GridContent objects for each column
     * @return This builder for chaining
     */
    public UIGridBuilder withColumnData(List<GridContent>[] columnData) {
        if (columnData == null || columnData.length == 0) {
            return this;
        }
        
        // Determine the number of rows needed (use the longest column)
        int maxRows = 0;
        for (List<GridContent> column : columnData) {
            maxRows = Math.max(maxRows, column.size());
        }
        
        // Update total rows for scrolling calculations
        this.rows = maxRows;
        this.totalRows = maxRows;
        
        // Calculate how many rows are visible based on the available height and row height
        int availableHeight = height - (2 * verticalMargin);
        int calculatedRowHeight = customRowHeight != null ? customRowHeight : 
                             (availableHeight - ((Math.min(rows, 10) - 1) * verticalSpacing)) / Math.min(rows, 10);
        
        // Determine if we need to enable vertical scrolling
        int recommendedVisibleRows = Math.max(1, availableHeight / (calculatedRowHeight + verticalSpacing));
        if (maxRows > recommendedVisibleRows) {
            withVerticalScroll(true, recommendedVisibleRows);
        }
        
        // Add all content elements to the grid
        for (int colIndex = 0; colIndex < columnData.length; colIndex++) {
            List<GridContent> column = columnData[colIndex];
            
            for (int rowIndex = 0; rowIndex < column.size(); rowIndex++) {
                GridContent content = column.get(rowIndex);
                
                switch (content.type) {
                    case TEXT:
                        addLabel(rowIndex, colIndex, (String)content.value, content.textColor);
                        if (content.tooltip != null) {
                            // Update last added element with tooltip
                            elements.get(elements.size() - 1).tooltip = content.tooltip;
                        }
                        if (content.onClick != null) {
                            // Update last added element with click handler
                            elements.get(elements.size() - 1).onClick = content.onClick;
                        }
                        break;
                        
                    case ITEM:
                        addItem(rowIndex, colIndex, (net.minecraft.world.item.Item)content.value, 1, content.onClick);
                        if (content.tooltip != null) {
                            // Update last added element with tooltip
                            elements.get(elements.size() - 1).tooltip = content.tooltip;
                        }
                        break;
                        
                    case ITEM_WITH_QUANTITY:
                        Object[] itemData = (Object[])content.value;
                        net.minecraft.world.item.Item item = (net.minecraft.world.item.Item)itemData[0];
                        int quantity = (Integer)itemData[1];
                        addItem(rowIndex, colIndex, item, quantity, content.onClick);
                        if (content.tooltip != null) {
                            // Update last added element with tooltip
                            elements.get(elements.size() - 1).tooltip = content.tooltip;
                        }
                        break;
                        
                    case TOGGLE:
                        Object[] toggleData = (Object[])content.value;
                        String toggleText = (String)toggleData[0];
                        boolean initialState = (Boolean)toggleData[1];
                        addToggle(rowIndex, colIndex, toggleText, initialState, content.onToggle, 
                                 content.bgColor, 0x80555555); // Use default disabled color
                        if (content.tooltip != null) {
                            // Update last added element with tooltip
                            elements.get(elements.size() - 1).tooltip = content.tooltip;
                        }
                        break;
                        
                    case BUTTON:
                        addButton(rowIndex, colIndex, (String)content.value, content.onClick, content.bgColor);
                        if (content.tooltip != null) {
                            // Update last added element with tooltip
                            elements.get(elements.size() - 1).tooltip = content.tooltip;
                        }
                        break;
                }
            }
        }
        
        return this;
    }

    /**
     * Updates an existing grid with new column data while preserving scroll state
     * 
     * @param columnData Arrays of GridContent objects for each column
     * @return This builder for chaining
     */
    public UIGridBuilder updateColumnData(List<GridContent>[] columnData) {
        if (columnData == null || columnData.length == 0) {
            return this;
        }
        
        // Store current scroll state
        int savedVerticalScrollOffset = this.verticalScrollOffset;
        int savedHorizontalScrollOffset = this.horizontalScrollOffset;
        boolean wasVerticalScrollEnabled = this.verticalScrollEnabled;
        boolean wasHorizontalScrollEnabled = this.horizontalScrollEnabled;
        int savedVisibleRows = this.visibleRows;
        int savedMaxVerticalScrollOffset = this.maxVerticalScrollOffset;
        int savedMaxHorizontalScrollOffset = this.maxHorizontalScrollOffset;
        
        // Clear existing elements and rebuild with new data
        this.elements.clear();
        
        // Determine the number of rows needed (use the longest column)
        int maxRows = 0;
        for (List<GridContent> column : columnData) {
            maxRows = Math.max(maxRows, column.size());
        }
        
        // Update total rows for scrolling calculations
        this.rows = maxRows;
        this.totalRows = maxRows;
        
        // Restore scroll settings if they were enabled
        if (wasVerticalScrollEnabled) {
            this.verticalScrollEnabled = true;
            this.visibleRows = savedVisibleRows;
            this.maxVerticalScrollOffset = Math.max(0, this.totalRows - this.visibleRows);
            
            // Restore scroll offset, but clamp it to the new valid range
            this.verticalScrollOffset = Math.min(savedVerticalScrollOffset, this.maxVerticalScrollOffset);
        }
        
        if (wasHorizontalScrollEnabled) {
            this.horizontalScrollEnabled = true;
            this.horizontalScrollOffset = Math.min(savedHorizontalScrollOffset, savedMaxHorizontalScrollOffset);
            this.maxHorizontalScrollOffset = savedMaxHorizontalScrollOffset;
        }
        
        // Add all content elements to the grid
        for (int colIndex = 0; colIndex < columnData.length; colIndex++) {
            List<GridContent> column = columnData[colIndex];
            
            for (int rowIndex = 0; rowIndex < column.size(); rowIndex++) {
                GridContent content = column.get(rowIndex);
                
                switch (content.type) {
                    case TEXT:
                        addLabel(rowIndex, colIndex, (String)content.value, content.textColor);
                        if (content.tooltip != null) {
                            elements.get(elements.size() - 1).tooltip = content.tooltip;
                        }
                        if (content.onClick != null) {
                            elements.get(elements.size() - 1).onClick = content.onClick;
                        }
                        break;
                        
                    case ITEM:
                        addItem(rowIndex, colIndex, (net.minecraft.world.item.Item)content.value, 1, content.onClick);
                        if (content.tooltip != null) {
                            elements.get(elements.size() - 1).tooltip = content.tooltip;
                        }
                        break;
                        
                    case ITEM_WITH_QUANTITY:
                        Object[] itemData = (Object[])content.value;
                        net.minecraft.world.item.Item item = (net.minecraft.world.item.Item)itemData[0];
                        int quantity = (Integer)itemData[1];
                        addItem(rowIndex, colIndex, item, quantity, content.onClick);
                        if (content.tooltip != null) {
                            elements.get(elements.size() - 1).tooltip = content.tooltip;
                        }
                        break;
                        
                    case TOGGLE:
                        Object[] toggleData = (Object[])content.value;
                        String toggleText = (String)toggleData[0];
                        boolean initialState = (Boolean)toggleData[1];
                        addToggle(rowIndex, colIndex, toggleText, initialState, content.onToggle, 
                                 content.bgColor, 0x80555555);
                        if (content.tooltip != null) {
                            elements.get(elements.size() - 1).tooltip = content.tooltip;
                        }
                        break;
                        
                    case BUTTON:
                        addButton(rowIndex, colIndex, (String)content.value, content.onClick, content.bgColor);
                        if (content.tooltip != null) {
                            elements.get(elements.size() - 1).tooltip = content.tooltip;
                        }
                        break;
                }
            }
        }
        
        return this;
    }
    
    /**
     * Utility method to create a grid from item-quantity pairs
     * 
     * @param itemQuantityPairs Map of items to their quantities
     * @param textColor Color for the item name text
     * @return This builder for chaining
     */
    public UIGridBuilder withItemQuantityPairs(Map<net.minecraft.world.item.Item, Integer> itemQuantityPairs, int textColor) {
        // Clear existing elements
        this.elements.clear();
        
        // Sort items by name
        List<Map.Entry<net.minecraft.world.item.Item, Integer>> sortedEntries = 
            new ArrayList<>(itemQuantityPairs.entrySet());
        
        sortedEntries.sort((a, b) -> {
            String nameA = a.getKey().getDescriptionId();
            String nameB = b.getKey().getDescriptionId();
            return nameA.compareToIgnoreCase(nameB);
        });
        
        // Calculate total rows needed
        int totalRows = sortedEntries.size();
        
        // Adjust rows and columns based on data
        this.columns = 2; // Item and quantity
        this.rows = totalRows;
        
        // Calculate visible rows based on available height and row height
        int effectiveRowHeight = this.customRowHeight != null ? this.customRowHeight : 16;
        
        // Calculate available height (accounting for margins)
        int availableHeight = this.height - (this.verticalMargin * 2);
        
        // Calculate how many rows can fit in the available height
        // Use Math.max to ensure at least 1 row is visible
        int calculatedVisibleRows = Math.max(4, availableHeight / (effectiveRowHeight + this.verticalSpacing));
        
        DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER, "Row calculation: availableHeight={}, effectiveRowHeight={}, spacing={}, calculatedRows={}", 
                    availableHeight, effectiveRowHeight, this.verticalSpacing, calculatedVisibleRows);
        
        // Force vertical scrolling if we have more than 4 rows
        if (totalRows > 4) {
            this.withVerticalScroll(true, calculatedVisibleRows);
        }
        
        // Add item elements
        for (int i = 0; i < sortedEntries.size(); i++) {
            Map.Entry<net.minecraft.world.item.Item, Integer> entry = sortedEntries.get(i);
            
            // Create a formatted item name
            String itemName = formatItemName(entry.getKey().getDescriptionId());
            
            // Add label for item name (first column)
            addLabel(i, 0, itemName, textColor);
            
            // Add item with quantity (second column)
            UIGridElement element = addItem(i, 1, entry.getKey(), entry.getValue(), null).elements.get(elements.size() - 1);
            element.showQuantity = true; // Always show quantity
        }
        
        return this;
    }
    
    /**
     * Updates an existing grid with new item-quantity pairs while preserving scroll state
     * 
     * @param itemQuantityPairs Map of items to their quantities
     * @param textColor Color for the item name text
     * @return This builder for chaining
     */
    public UIGridBuilder updateItemQuantityPairs(Map<net.minecraft.world.item.Item, Integer> itemQuantityPairs, int textColor) {
        // Store current scroll state
        int savedVerticalScrollOffset = this.verticalScrollOffset;
        int savedHorizontalScrollOffset = this.horizontalScrollOffset;
        boolean wasVerticalScrollEnabled = this.verticalScrollEnabled;
        boolean wasHorizontalScrollEnabled = this.horizontalScrollEnabled;
        int savedVisibleRows = this.visibleRows;
        int savedMaxVerticalScrollOffset = this.maxVerticalScrollOffset;
        int savedMaxHorizontalScrollOffset = this.maxHorizontalScrollOffset;
        
        // Clear existing elements and rebuild with new data
        this.elements.clear();
        
        // Sort items by name
        List<Map.Entry<net.minecraft.world.item.Item, Integer>> sortedEntries = 
            new ArrayList<>(itemQuantityPairs.entrySet());
        
        sortedEntries.sort((a, b) -> {
            String nameA = a.getKey().getDescriptionId();
            String nameB = b.getKey().getDescriptionId();
            return nameA.compareToIgnoreCase(nameB);
        });
        
        // Calculate total rows needed
        int totalRows = sortedEntries.size();
        
        // Adjust rows and columns based on data
        this.columns = 2; // Item and quantity
        this.rows = totalRows;
        this.totalRows = totalRows;
        
        // Restore scroll settings if they were enabled
        if (wasVerticalScrollEnabled) {
            this.verticalScrollEnabled = true;
            this.visibleRows = savedVisibleRows;
            this.maxVerticalScrollOffset = Math.max(0, totalRows - this.visibleRows);
            
            // Restore scroll offset, but clamp it to the new max
            this.verticalScrollOffset = Math.min(savedVerticalScrollOffset, this.maxVerticalScrollOffset);
        } else if (totalRows > 4) {
            // Enable scrolling if we have more than 4 rows
            int effectiveRowHeight = this.customRowHeight != null ? this.customRowHeight : 16;
            int availableHeight = this.height - (this.verticalMargin * 2);
            int calculatedVisibleRows = Math.max(4, availableHeight / (effectiveRowHeight + this.verticalSpacing));
            
            this.withVerticalScroll(true, calculatedVisibleRows);
            this.verticalScrollOffset = 0; // Start at top for new data
        }
        
        if (wasHorizontalScrollEnabled) {
            this.horizontalScrollEnabled = true;
            this.maxHorizontalScrollOffset = savedMaxHorizontalScrollOffset;
            this.horizontalScrollOffset = Math.min(savedHorizontalScrollOffset, this.maxHorizontalScrollOffset);
        }
        
        // Add item elements
        for (int i = 0; i < sortedEntries.size(); i++) {
            Map.Entry<net.minecraft.world.item.Item, Integer> entry = sortedEntries.get(i);
            
            // Create a formatted item name
            String itemName = formatItemName(entry.getKey().getDescriptionId());
            
            // Add label for item name (first column)
            addLabel(i, 0, itemName, textColor);
            
            // Add item with quantity (second column)
            UIGridElement element = addItem(i, 1, entry.getKey(), entry.getValue(), null).elements.get(elements.size() - 1);
            element.showQuantity = true; // Always show quantity
        }
        
        DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER, "Updated grid with {} items, preserved scroll offset: {}", sortedEntries.size(), this.verticalScrollOffset);
        
        return this;
    }

    /**
     * Format an item's description ID into a readable name
     */
    private String formatItemName(String descriptionId) {
        // Extract the item name from the description ID (e.g., "block.minecraft.stone" -> "Stone")
        String[] parts = descriptionId.split("\\.");
        if (parts.length > 0) {
            String name = parts[parts.length - 1];
            // Capitalize first letter and replace underscores with spaces
            name = name.substring(0, 1).toUpperCase() + name.substring(1).replace('_', ' ');
            return name;
        }
        return descriptionId;
    }

    /**
     * Check if the mouse is over the vertical scrollbar thumb
     */
    private boolean isMouseOverVerticalScrollThumb(int mouseX, int mouseY) {
        if (!verticalScrollEnabled || totalRows <= visibleRows) return false;
        
        // Calculate scroll thumb dimensions and position - must match the rendering position exactly
        int scrollTrackX = this.x + this.width + 20; // Match the render position - outside grid
        int scrollTrackY = this.y + verticalMargin + 2; // Match the render position
        int scrollTrackHeight = this.height - (verticalMargin * 2) - 4; // Match the render calculation
        
        // Calculate thumb size and position
        float thumbRatio = Math.min(1.0f, (float)visibleRows / totalRows);
        int thumbHeight = Math.max(12, Math.round(thumbRatio * scrollTrackHeight)); // Match render minimum
        
        // Calculate thumb position
        float scrollProgress = (totalRows <= visibleRows) ? 0 : 
                              (float)verticalScrollOffset / (totalRows - visibleRows);
        int thumbY = scrollTrackY + Math.round((scrollTrackHeight - thumbHeight) * scrollProgress);
        
        // Check if mouse is over thumb
        return mouseX >= scrollTrackX && mouseX <= scrollTrackX + scrollBarWidth &&
               mouseY >= thumbY && mouseY <= thumbY + thumbHeight;
    }

    /**
     * Renders the horizontal scrollbar
     */
    private void renderHorizontalScrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!horizontalScrollEnabled) return;
        
        // Calculate scroll area dimensions
        int scrollTrackX = this.x + horizontalMargin;
        int scrollTrackY = this.y + height - verticalMargin - scrollBarHeight;
        int scrollTrackWidth = this.width - (horizontalMargin * 2);
        
        // Draw scrollbar background
        graphics.fill(scrollTrackX, scrollTrackY, 
                     scrollTrackX + scrollTrackWidth, scrollTrackY + scrollBarHeight, 
                     0x40000000);
        
        // Calculate thumb width and position
        float thumbRatio = (float)visibleColumns / (float)totalColumns;
        int thumbWidth = Math.max(20, (int)(scrollTrackWidth * thumbRatio));
        
        // Calculate thumb position based on current scroll offset
        float scrollProgress = (totalColumns <= visibleColumns) ? 0 : 
                              (float)horizontalScrollOffset / (totalColumns - visibleColumns);
        int thumbX = scrollTrackX + Math.round((scrollTrackWidth - thumbWidth) * scrollProgress);
        
        // Draw the thumb
        boolean isThumbHovered = isMouseOverHorizontalScrollThumb(mouseX, mouseY);
        int thumbColor = isThumbHovered ? 0xCCAAAAAA : 0x80AAAAAA;
        graphics.fill(thumbX, scrollTrackY, 
                     thumbX + thumbWidth, scrollTrackY + scrollBarHeight, 
                     thumbColor);
    }

    /**
     * Renders the vertical scrollbar
     */
    private void renderVerticalScrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!verticalScrollEnabled || totalRows <= visibleRows) return;
        
        // Calculate scroll area dimensions - position much further right to avoid button overlap
        int scrollTrackX = this.x + this.width + 20; // 20px outside the grid boundary
        int scrollTrackY = this.y + verticalMargin + 2; // 2px from top margin
        int scrollTrackHeight = this.height - (verticalMargin * 2) - 4; // 2px top and bottom padding
        
        // Ensure scrollbar doesn't exceed component boundaries
        if (scrollTrackX + scrollBarWidth > this.x + this.width) {
            scrollTrackX = this.x + this.width - scrollBarWidth;
        }
        if (scrollTrackY + scrollTrackHeight > this.y + this.height - verticalMargin) {
            scrollTrackHeight = this.y + this.height - verticalMargin - scrollTrackY;
        }
        
        // Draw scrollbar background
        graphics.fill(scrollTrackX, scrollTrackY, 
                     scrollTrackX + scrollBarWidth, scrollTrackY + scrollTrackHeight, 
                     0x40000000); // Original transparency
        
        // Calculate thumb size and position
        float thumbRatio = Math.min(1.0f, (float)visibleRows / totalRows);
        int thumbHeight = Math.max(12, Math.round(thumbRatio * scrollTrackHeight)); // Minimum 12px thumb
        
        // Calculate thumb position based on current scroll offset
        float scrollProgress = (totalRows <= visibleRows) ? 0 : 
                              (float)verticalScrollOffset / (totalRows - visibleRows);
        int thumbY = scrollTrackY + Math.round((scrollTrackHeight - thumbHeight) * scrollProgress);
        
        // Draw the thumb (original simple style)
        boolean isThumbHovered = isMouseOverVerticalScrollThumb(mouseX, mouseY);
        int thumbColor = isThumbHovered ? 0xCCAAAAAA : 0x80AAAAAA;
        graphics.fill(scrollTrackX, thumbY, 
                     scrollTrackX + scrollBarWidth, thumbY + thumbHeight, 
                     thumbColor);
    }

    /**
     * Checks if the mouse is over the horizontal scrollbar thumb
     */
    private boolean isMouseOverHorizontalScrollThumb(int mouseX, int mouseY) {
        if (!horizontalScrollEnabled) return false;
        
        // Calculate scroll thumb dimensions and position
        int scrollTrackX = this.x + horizontalMargin;
        int scrollTrackY = this.y + height - verticalMargin - scrollBarHeight;
        int scrollTrackWidth = this.width - (horizontalMargin * 2);
        
        // Calculate thumb size and position
        float thumbRatio = (float)visibleColumns / (float)totalColumns;
        int thumbWidth = Math.max(20, (int)(scrollTrackWidth * thumbRatio));
        
        // Calculate thumb position
        float scrollProgress = (totalColumns <= visibleColumns) ? 0 : 
                              (float)horizontalScrollOffset / (totalColumns - visibleColumns);
        int thumbX = scrollTrackX + Math.round((scrollTrackWidth - thumbWidth) * scrollProgress);
        
        // Check if mouse is over thumb
        return mouseX >= thumbX && mouseX < thumbX + thumbWidth &&
               mouseY >= scrollTrackY && mouseY < scrollTrackY + scrollBarHeight;
    }

    /**
     * Handle mouse dragging for scrolling
     * @return true if the drag was handled
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Only handle left mouse button (button 0)
        if (button != 0) {
            return false;
        }
        
        // Handle vertical scrollbar dragging
        if (isDraggingVertical) {
            if (verticalScrollEnabled && maxVerticalScrollOffset > 0) {
                int scrollTrackHeight = height - (verticalMargin * 2);
                
                // Calculate drag amount in rows
                // First determine how much to scroll per pixel of drag
                float rowsPerPixel = (float)totalRows / scrollTrackHeight;
                
                // Calculate how many rows to scroll based on the drag distance
                int rowsToScroll = (int)Math.signum(mouseY - lastMouseY);
                
                // If the drag is significant enough, apply it
                if (Math.abs(mouseY - lastMouseY) > 5) {
                    verticalScrollOffset += rowsToScroll;
                    
                    // Clamp scroll position
                    if (verticalScrollOffset < 0) {
                        verticalScrollOffset = 0;
                    }
                    if (verticalScrollOffset > maxVerticalScrollOffset) {
                        verticalScrollOffset = maxVerticalScrollOffset;
                    }
                    
                    lastMouseY = mouseY;
                }
                
                return true;
            }
        }
        
        // Handle horizontal scrollbar dragging
        if (isDraggingHorizontal) {
            if (horizontalScrollEnabled && maxHorizontalScrollOffset > 0) {
                int scrollTrackWidth = width - (horizontalMargin * 2);
                
                // Calculate drag amount in columns
                float columnsPerPixel = (float)totalColumns / scrollTrackWidth;
                
                // Calculate how many columns to scroll based on the drag distance
                int columnsToScroll = (int)Math.signum(mouseX - lastMouseX);
                
                // If the drag is significant enough, apply it
                if (Math.abs(mouseX - lastMouseX) > 5) {
                    horizontalScrollOffset += columnsToScroll;
                    
                    // Clamp scroll position
                    if (horizontalScrollOffset < 0) {
                        horizontalScrollOffset = 0;
                    }
                    if (horizontalScrollOffset > maxHorizontalScrollOffset) {
                        horizontalScrollOffset = maxHorizontalScrollOffset;
                    }
                    
                    lastMouseX = mouseX;
                }
                
                return true;
            }
        }
        
        // Check if this is the start of a drag on the scrollbar area
        if (!isDraggingVertical && !isDraggingHorizontal) {
            // Check vertical scrollbar area
            if (verticalScrollEnabled && totalRows > visibleRows) {
                int scrollBarX = x + width + 20; // Match rendering position - outside grid
                int scrollTrackY = y + verticalMargin + 2; // Match rendering position
                int scrollTrackHeight = height - (verticalMargin * 2) - 4; // Match rendering calculation
                
                if (mouseX >= scrollBarX && mouseX < scrollBarX + scrollBarWidth &&
                    mouseY >= scrollTrackY && mouseY < scrollTrackY + scrollTrackHeight) {
                    isDraggingVertical = true;
                    lastMouseY = mouseY;
                    return true;
                }
            }
            
            // Check horizontal scrollbar area
            if (horizontalScrollEnabled && !useScrollButtons) {
                int scrollTrackX = x + horizontalMargin;
                int scrollTrackY = y + height - verticalMargin - scrollBarHeight;
                int scrollTrackWidth = width - (horizontalMargin * 2);
                
                if (mouseX >= scrollTrackX && mouseX < scrollTrackX + scrollTrackWidth &&
                    mouseY >= scrollTrackY && mouseY < scrollTrackY + scrollBarHeight) {
                    isDraggingHorizontal = true;
                    lastMouseX = mouseX;
                    return true;
                }
            }
            
            // Check content area for dragging (direct dragging)
            if (verticalScrollEnabled && 
                mouseX >= x && mouseX < x + width &&
                mouseY >= y && mouseY < y + height) {
                isDraggingVertical = true;
                lastMouseY = mouseY;
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Handle mouse release
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            boolean wasDragging = isDraggingVertical || isDraggingHorizontal;
            isDraggingVertical = false;
            isDraggingHorizontal = false;
            return wasDragging;
        }
        return false;
    }

    /**
     * Creates a grid with label-value pairs, which is a very common pattern
     * 
     * @param x X position 
     * @param y Y position
     * @param width Width
     * @param height Height
     * @param labelColor Color for labels
     * @param valueColor Color for values
     * @param pairs Map of label-value pairs
     * @return A new grid builder with the pairs added
     */
    public static UIGridBuilder createLabelValueGrid(
            int x, int y, int width, int height, 
            int labelColor, int valueColor, 
            Map<String, String> pairs) {
        
        UIGridBuilder grid = create(x, y, width, height, 2)
            .withRowHeight(26); // Use consistent row height
        
        // Create column data arrays
        @SuppressWarnings("unchecked")
        List<GridContent>[] columnData = new List[2];
        columnData[0] = new ArrayList<>(); // Label column
        columnData[1] = new ArrayList<>(); // Value column
        
        // Add pairs
        for (Map.Entry<String, String> entry : pairs.entrySet()) {
            columnData[0].add(GridContent.text(entry.getKey(), labelColor));
            columnData[1].add(GridContent.text(entry.getValue(), valueColor));
        }
        
        // Use the column data
        grid.withColumnData(columnData);
        
        return grid;
    }
    
    /**
     * Creates a grid with label-button pairs, which is a common pattern
     * 
     * @param x X position
     * @param y Y position
     * @param width Width
     * @param height Height
     * @param labelColor Color for labels
     * @param buttonColor Color for buttons
     * @param pairs Map of label to button data (text and click handler)
     * @return A new grid builder with the pairs added
     */
    public static UIGridBuilder createLabelButtonGrid(
            int x, int y, int width, int height,
            int labelColor, int buttonColor,
            Map<String, Object[]> pairs) {
        
        UIGridBuilder grid = create(x, y, width, height, 2)
            .withRowHeight(26); // Use consistent row height
        
        // Create column data arrays
        @SuppressWarnings("unchecked")
        List<GridContent>[] columnData = new List[2];
        columnData[0] = new ArrayList<>(); // Label column
        columnData[1] = new ArrayList<>(); // Button column
        
        // Add pairs
        for (Map.Entry<String, Object[]> entry : pairs.entrySet()) {
            String label = entry.getKey();
            Object[] buttonData = entry.getValue();
            String buttonText = (String)buttonData[0];
            @SuppressWarnings("unchecked") 
            Consumer<Void> onClick = (Consumer<Void>)buttonData[1];
            
            columnData[0].add(GridContent.text(label, labelColor));
            columnData[1].add(GridContent.button(buttonText, onClick, buttonColor));
        }
        
        // Use the column data
        grid.withColumnData(columnData);
        
        return grid;
    }


} 