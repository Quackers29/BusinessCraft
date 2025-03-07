package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

/**
 * Utility class for creating grid-based UI layouts with various component types.
 * Supports dynamic generation of buttons, labels, toggles, etc. in a grid layout.
 */
public class UIGridBuilder {
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
    
    /**
     * Creates a new grid builder with the specified dimensions and layout
     * 
     * @param x X position of the grid
     * @param y Y position of the grid
     * @param width Width of the grid
     * @param height Height of the grid
     * @param rows Number of rows in the grid
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
        this.verticalSpacing = 10;
        this.horizontalMargin = 15;
        this.verticalMargin = 10;
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
        element.showQuantity = quantity > 1;
        element.onClick = onClick;
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
        element.showQuantity = quantity > 1;
        element.onClick = onClick;
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
        element.showQuantity = quantity > 1;
        element.tooltip = tooltip;
        element.onClick = onClick;
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
        element.showQuantity = itemStack.getCount() > 1;
        element.onClick = onClick;
        element.itemStack = itemStack; // Store full itemstack for rendering
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
        element.showQuantity = itemStack.getCount() > 1;
        element.onClick = onClick;
        element.itemStack = itemStack; // Store full itemstack for rendering
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
        element.showQuantity = itemStack.getCount() > 1;
        element.tooltip = tooltip;
        element.onClick = onClick;
        element.itemStack = itemStack; // Store full itemstack for rendering
        elements.add(element);
        return this;
    }
    
    /**
     * Renders the entire grid with all its elements
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        // Reset hover states
        for (UIGridElement element : elements) {
            element.isHovered = false;
        }
        
        // Draw background and border
        if (drawBackground) {
            graphics.fill(x, y, x + width, y + height, backgroundColor);
        }
        
        if (drawBorder) {
            graphics.hLine(x, x + width - 1, y, borderColor);
            graphics.hLine(x, x + width - 1, y + height - 1, borderColor);
            graphics.vLine(x, y, y + height - 1, borderColor);
            graphics.vLine(x + width - 1, y, y + height - 1, borderColor);
        }
        
        // Calculate cell dimensions
        int availableWidth = width - (2 * horizontalMargin) - ((columns - 1) * horizontalSpacing);
        if (useScrollButtons && horizontalScrollEnabled) {
            availableWidth -= (2 * scrollButtonSize); // Account for scroll buttons
        }
        
        int availableHeight = height - (2 * verticalMargin) - ((rows - 1) * verticalSpacing);
        if (horizontalScrollEnabled && !useScrollButtons) {
            availableHeight -= scrollBarHeight; // Account for scrollbar height
        }
        
        int cellWidth = availableWidth / (horizontalScrollEnabled ? visibleColumns : columns);
        int cellHeight = availableHeight / rows;
        
        // Render scroll buttons if enabled
        if (horizontalScrollEnabled && useScrollButtons) {
            // Draw left button
            boolean leftHovered = mouseX >= x + horizontalMargin && mouseX < x + horizontalMargin + scrollButtonSize &&
                               mouseY >= y + height / 2 - scrollButtonSize / 2 && mouseY < y + height / 2 + scrollButtonSize / 2;
            int leftButtonColor = leftHovered ? 0xA0777777 : 0xA0555555;
            if (horizontalScrollOffset <= 0) {
                leftButtonColor = 0x80444444; // Disabled state
            }
            
            graphics.fill(
                x + horizontalMargin,
                y + height / 2 - scrollButtonSize / 2,
                x + horizontalMargin + scrollButtonSize,
                y + height / 2 + scrollButtonSize / 2,
                leftButtonColor
            );
            
            // Draw left arrow
            graphics.drawString(
                Minecraft.getInstance().font,
                "<",
                x + horizontalMargin + scrollButtonSize / 2 - 3,
                y + height / 2 - 4,
                0xFFFFFFFF
            );
            
            // Draw right button
            boolean rightHovered = mouseX >= x + width - horizontalMargin - scrollButtonSize && mouseX < x + width - horizontalMargin &&
                               mouseY >= y + height / 2 - scrollButtonSize / 2 && mouseY < y + height / 2 + scrollButtonSize / 2;
            int rightButtonColor = rightHovered ? 0xA0777777 : 0xA0555555;
            if (horizontalScrollOffset >= maxHorizontalScrollOffset) {
                rightButtonColor = 0x80444444; // Disabled state
            }
            
            graphics.fill(
                x + width - horizontalMargin - scrollButtonSize,
                y + height / 2 - scrollButtonSize / 2,
                x + width - horizontalMargin,
                y + height / 2 + scrollButtonSize / 2,
                rightButtonColor
            );
            
            // Draw right arrow
            graphics.drawString(
                Minecraft.getInstance().font,
                ">",
                x + width - horizontalMargin - scrollButtonSize / 2 - 3,
                y + height / 2 - 4,
                0xFFFFFFFF
            );
        }
        
        // Render horizontal scrollbar if needed
        if (horizontalScrollEnabled && !useScrollButtons && maxHorizontalScrollOffset > 0) {
            int scrollBarWidth = Math.max(20, (availableWidth * visibleColumns) / totalColumns);
            int scrollBarX = x + horizontalMargin + (int)((float)horizontalScrollOffset / maxHorizontalScrollOffset * (availableWidth - scrollBarWidth));
            
            // Draw scrollbar track
            graphics.fill(
                x + horizontalMargin, 
                y + height - verticalMargin - scrollBarHeight, 
                x + horizontalMargin + availableWidth, 
                y + height - verticalMargin, 
                0x40FFFFFF
            );
            
            // Draw scrollbar thumb
            graphics.fill(
                scrollBarX, 
                y + height - verticalMargin - scrollBarHeight, 
                scrollBarX + scrollBarWidth, 
                y + height - verticalMargin, 
                0xC0FFFFFF
            );
        }
        
        // Render each element
        for (UIGridElement element : elements) {
            // Skip elements outside the visible range when scrolling
            if (horizontalScrollEnabled) {
                if (element.column < horizontalScrollOffset || 
                    element.column >= horizontalScrollOffset + visibleColumns) {
                    continue;
                }
            }
            
            // Calculate element position and size
            int elementX;
            
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
            
            int elementY = y + verticalMargin + (element.row * (cellHeight + verticalSpacing));
            int elementWidth = (cellWidth * element.colSpan) + ((element.colSpan - 1) * horizontalSpacing);
            int elementHeight = (cellHeight * element.rowSpan) + ((element.rowSpan - 1) * verticalSpacing);
            
            // Check if mouse is over this element
            boolean hovered = mouseX >= elementX && mouseX < elementX + elementWidth &&
                             mouseY >= elementY && mouseY < elementY + elementHeight;
            
            // Update hover state
            element.isHovered = hovered;
            
            // Render based on element type
            switch (element.type) {
                case BUTTON:
                    renderButton(graphics, element, elementX, elementY, 
                                elementWidth, elementHeight, hovered);
                    break;
                case LABEL:
                    renderLabel(graphics, element, elementX, elementY, 
                               elementWidth, elementHeight);
                    break;
                case TOGGLE:
                    renderToggle(graphics, element, elementX, elementY, 
                                elementWidth, elementHeight, hovered);
                    break;
                case ITEM:
                    renderItem(graphics, element, elementX, elementY, 
                              elementWidth, elementHeight, hovered);
                    break;
            }
        }
        
        // Render tooltips after all elements are drawn
        renderTooltips(graphics, mouseX, mouseY);
    }
    
    /**
     * Renders tooltips for hovered elements
     */
    private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        for (UIGridElement element : elements) {
            if (element.isHovered && element.tooltip != null) {
                graphics.renderTooltip(
                    Minecraft.getInstance().font,
                    net.minecraft.network.chat.Component.literal(element.tooltip),
                    mouseX, mouseY
                );
                break; // Only show one tooltip at a time
            } else if (element.isHovered && element.type == UIElementType.ITEM && element.itemStack != null) {
                // Show the item's tooltip if available and no custom tooltip is specified
                graphics.renderTooltip(
                    Minecraft.getInstance().font,
                    element.itemStack,
                    mouseX, mouseY
                );
                break; // Only show one tooltip at a time
            }
        }
    }
    
    /**
     * Handles mouse clicks on the grid elements
     * @return true if a click was handled
     */
    public boolean mouseClicked(int mouseX, int mouseY) {
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
        
        // Calculate cell dimensions
        int availableWidth = width - (2 * horizontalMargin) - ((columns - 1) * horizontalSpacing);
        if (useScrollButtons && horizontalScrollEnabled) {
            availableWidth -= (2 * scrollButtonSize); // Account for scroll buttons
        }
        
        int availableHeight = height - (2 * verticalMargin) - ((rows - 1) * verticalSpacing);
        if (horizontalScrollEnabled && !useScrollButtons) {
            availableHeight -= scrollBarHeight; // Account for scrollbar height
        }
        
        int cellWidth = availableWidth / (horizontalScrollEnabled ? visibleColumns : columns);
        int cellHeight = availableHeight / rows;
        
        // Check each element
        for (UIGridElement element : elements) {
            // Skip elements outside the visible range when scrolling
            if (horizontalScrollEnabled) {
                if (element.column < horizontalScrollOffset || 
                    element.column >= horizontalScrollOffset + visibleColumns) {
                    continue;
                }
            }
            
            // Calculate element position and size
            int elementX;
            
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
            
            int elementY = y + verticalMargin + (element.row * (cellHeight + verticalSpacing));
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
        if (!horizontalScrollEnabled || maxHorizontalScrollOffset <= 0) {
            return false;
        }
        
        // Check if mouse is over the grid
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            // Scroll horizontally (delta is positive when scrolling up/left, negative when scrolling down/right)
            int scrollAmount = (int)(-delta);
            setHorizontalScrollOffset(horizontalScrollOffset + scrollAmount);
            return true;
        }
        
        return false;
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
        
        // Draw button background
        graphics.fill(x, y, x + width, y + height, buttonColor);
        
        // Draw button border
        graphics.hLine(x, x + width - 1, y, borderColor);
        graphics.hLine(x, x + width - 1, y + height - 1, borderColor);
        graphics.vLine(x, y, y + height - 1, borderColor);
        graphics.vLine(x + width - 1, y, y + height - 1, borderColor);
        
        // Draw centered text
        int textX = x + width / 2 - Minecraft.getInstance().font.width(element.text) / 2;
        int textY = y + (height - 8) / 2;
        graphics.drawString(Minecraft.getInstance().font, element.text, textX, textY, 0xFFFFFFFF);
    }
    
    /**
     * Renders a label element
     */
    private void renderLabel(GuiGraphics graphics, UIGridElement element, 
                            int x, int y, int width, int height) {
        // Draw text (centered)
        int textX = x + width / 2 - Minecraft.getInstance().font.width(element.text) / 2;
        int textY = y + (height - 8) / 2;
        graphics.drawString(Minecraft.getInstance().font, element.text, textX, textY, element.textColor);
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
                           int x, int y, int width, int height, boolean hovered) {
        // Draw a background for the item (lighter when hovered)
        int bgColor = hovered ? 0x80444444 : 0x60222222;
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
     * Types of UI elements supported in the grid
     */
    private enum UIElementType {
        BUTTON,
        LABEL,
        TOGGLE,
        ITEM   // New element type for Minecraft items with quantity indicator
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
        
        // New fields for item display
        net.minecraft.world.item.Item item; // The Minecraft item to display
        int quantity = 1;    // Quantity to display
        boolean showQuantity = true; // Whether to show quantity indicator
        net.minecraft.world.item.ItemStack itemStack; // Full itemstack for rendering
        
        public UIGridElement(UIElementType type, int row, int column, int rowSpan, int colSpan) {
            this.type = type;
            this.row = row;
            this.column = column;
            this.rowSpan = rowSpan;
            this.colSpan = colSpan;
        }
    }
} 