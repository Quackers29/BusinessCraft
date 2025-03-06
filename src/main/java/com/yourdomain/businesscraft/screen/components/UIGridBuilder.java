package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
     * Renders the entire grid with all its elements
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
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
        int availableHeight = height - (2 * verticalMargin) - ((rows - 1) * verticalSpacing);
        int cellWidth = availableWidth / columns;
        int cellHeight = availableHeight / rows;
        
        // Render each element
        for (UIGridElement element : elements) {
            // Calculate element position and size
            int elementX = x + horizontalMargin + (element.column * (cellWidth + horizontalSpacing));
            int elementY = y + verticalMargin + (element.row * (cellHeight + verticalSpacing));
            int elementWidth = (cellWidth * element.colSpan) + ((element.colSpan - 1) * horizontalSpacing);
            int elementHeight = (cellHeight * element.rowSpan) + ((element.rowSpan - 1) * verticalSpacing);
            
            // Check if mouse is over this element
            boolean hovered = mouseX >= elementX && mouseX < elementX + elementWidth &&
                             mouseY >= elementY && mouseY < elementY + elementHeight;
            
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
            }
        }
    }
    
    /**
     * Handles mouse clicks on the grid elements
     * @return true if a click was handled
     */
    public boolean mouseClicked(int mouseX, int mouseY) {
        // Calculate cell dimensions
        int availableWidth = width - (2 * horizontalMargin) - ((columns - 1) * horizontalSpacing);
        int availableHeight = height - (2 * verticalMargin) - ((rows - 1) * verticalSpacing);
        int cellWidth = availableWidth / columns;
        int cellHeight = availableHeight / rows;
        
        // Check each element
        for (UIGridElement element : elements) {
            // Calculate element position and size
            int elementX = x + horizontalMargin + (element.column * (cellWidth + horizontalSpacing));
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
                            element.toggled = !element.toggled;
                            // Swap colors
                            int temp = element.backgroundColor;
                            element.backgroundColor = element.altBackgroundColor;
                            element.altBackgroundColor = temp;
                            element.onToggle.accept(element.toggled);
                            return true;
                        }
                        break;
                        
                    case LABEL:
                        // Labels don't respond to clicks
                        break;
                }
                
                break;
            }
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
        int textX = x + width / 2 - net.minecraft.client.Minecraft.getInstance().font.width(element.text) / 2;
        int textY = y + (height - 8) / 2;
        graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, element.text, textX, textY, 0xFFFFFFFF);
    }
    
    /**
     * Renders a label element
     */
    private void renderLabel(GuiGraphics graphics, UIGridElement element, 
                            int x, int y, int width, int height) {
        // Draw text (centered)
        int textX = x + width / 2 - net.minecraft.client.Minecraft.getInstance().font.width(element.text) / 2;
        int textY = y + (height - 8) / 2;
        graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, element.text, textX, textY, element.textColor);
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
        int textX = x + width / 2 - net.minecraft.client.Minecraft.getInstance().font.width(element.text) / 2;
        int textY = y + (height - 8) / 2;
        graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, element.text, textX, textY, 0xFFFFFFFF);
    }
    
    /**
     * Types of UI elements supported in the grid
     */
    private enum UIElementType {
        BUTTON,
        LABEL,
        TOGGLE
    }
    
    /**
     * Class representing a single element in the grid
     */
    private static class UIGridElement {
        // Location in grid
        UIElementType type;
        int row, column;
        int rowSpan, colSpan;
        
        // Common properties
        String text;
        int backgroundColor;
        int altBackgroundColor; // Used for toggle states
        int textColor = 0xFFFFFFFF;
        boolean toggled;
        
        // Event handlers
        Consumer<Void> onClick;
        Consumer<Boolean> onToggle;
        
        public UIGridElement(UIElementType type, int row, int column, int rowSpan, int colSpan) {
            this.type = type;
            this.row = row;
            this.column = column;
            this.rowSpan = rowSpan;
            this.colSpan = colSpan;
        }
    }
} 