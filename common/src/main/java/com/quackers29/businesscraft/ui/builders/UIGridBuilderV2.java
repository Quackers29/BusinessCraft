package com.quackers29.businesscraft.ui.builders;

import com.quackers29.businesscraft.ui.builders.grid.*;
import com.quackers29.businesscraft.ui.builders.grid.GridElementManager.GridElement;
import com.quackers29.businesscraft.ui.builders.grid.GridLayoutManager.GridPosition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Refactored UIGridBuilder using the new component-based architecture.
 * This class serves as a facade that coordinates the four main components:
 * - GridLayoutManager: Handles positioning and layout calculations
 * - GridScrollManager: Manages scrolling behavior and interactions
 * - GridElementManager: Stores and manages grid content elements
 * - GridRenderingEngine: Handles all rendering operations
 * 
 * This approach follows the Single Responsibility Principle and makes the code
 * much more maintainable and testable.
 */
public class UIGridBuilderV2 {
    private static final Logger LOGGER = LoggerFactory.getLogger(UIGridBuilderV2.class);
    
    // Component managers
    private final GridLayoutManager layoutManager;
    private final GridScrollManager scrollManager;
    private final GridElementManager elementManager;
    private final GridRenderingEngine renderingEngine;
    
    /**
     * Creates a new grid builder with the specified dimensions.
     * 
     * @param x X position of the grid
     * @param y Y position of the grid
     * @param width Width of the grid
     * @param height Height of the grid
     * @param rows Number of rows in the grid
     * @param columns Number of columns in the grid
     */
    public UIGridBuilderV2(int x, int y, int width, int height, int rows, int columns) {
        this.layoutManager = new GridLayoutManager(x, y, width, height, rows, columns);
        this.scrollManager = new GridScrollManager();
        this.elementManager = new GridElementManager();
        this.renderingEngine = new GridRenderingEngine();
        
        LOGGER.debug("Created UIGridBuilderV2: {}x{} at ({},{}) with {}x{} cells", 
                    width, height, x, y, rows, columns);
    }
    
    /**
     * Factory method to create a grid builder with automatic row calculation.
     * 
     * @param x X position of the grid
     * @param y Y position of the grid
     * @param width Width of the grid
     * @param height Height of the grid
     * @param columns Number of columns in the grid
     * @return A new grid builder instance
     */
    public static UIGridBuilderV2 create(int x, int y, int width, int height, int columns) {
        return new UIGridBuilderV2(x, y, width, height, 1, columns);
    }
    
    // ===== Layout Configuration Methods =====
    
    /**
     * Sets the spacing between grid elements.
     * 
     * @param horizontalSpacing Horizontal spacing in pixels
     * @param verticalSpacing Vertical spacing in pixels
     * @return This builder for method chaining
     */
    public UIGridBuilderV2 withSpacing(int horizontalSpacing, int verticalSpacing) {
        layoutManager.setSpacing(horizontalSpacing, verticalSpacing);
        return this;
    }
    
    /**
     * Sets the margins around the grid.
     * 
     * @param horizontalMargin Horizontal margin in pixels
     * @param verticalMargin Vertical margin in pixels
     * @return This builder for method chaining
     */
    public UIGridBuilderV2 withMargins(int horizontalMargin, int verticalMargin) {
        layoutManager.setMargins(horizontalMargin, verticalMargin);
        return this;
    }
    
    /**
     * Sets a custom row height.
     * 
     * @param height Height in pixels for each row
     * @return This builder for method chaining
     */
    public UIGridBuilderV2 withRowHeight(int height) {
        layoutManager.setRowHeight(height);
        return this;
    }
    
    // ===== Appearance Configuration Methods =====
    
    /**
     * Sets the background color.
     * 
     * @param color Background color (ARGB format)
     * @return This builder for method chaining
     */
    public UIGridBuilderV2 withBackgroundColor(int color) {
        renderingEngine.setBackgroundColor(color);
        return this;
    }
    
    /**
     * Sets the border color.
     * 
     * @param color Border color (ARGB format)
     * @return This builder for method chaining
     */
    public UIGridBuilderV2 withBorderColor(int color) {
        renderingEngine.setBorderColor(color);
        return this;
    }
    
    /**
     * Sets whether to draw the background.
     * 
     * @param draw True to draw background
     * @return This builder for method chaining
     */
    public UIGridBuilderV2 drawBackground(boolean draw) {
        renderingEngine.setDrawBackground(draw);
        return this;
    }
    
    /**
     * Sets whether to draw the border.
     * 
     * @param draw True to draw border
     * @return This builder for method chaining
     */
    public UIGridBuilderV2 drawBorder(boolean draw) {
        renderingEngine.setDrawBorder(draw);
        return this;
    }
    
    // ===== Scrolling Configuration Methods =====
    
    /**
     * Enables horizontal scrolling.
     * 
     * @param visibleColumns Number of columns visible at once
     * @param totalColumns Total number of columns
     * @return This builder for method chaining
     */
    public UIGridBuilderV2 withHorizontalScroll(int visibleColumns, int totalColumns) {
        scrollManager.enableHorizontalScroll(visibleColumns, totalColumns);
        return this;
    }
    
    /**
     * Enables vertical scrolling.
     * 
     * @param visibleRows Number of rows visible at once
     * @param totalRows Total number of rows
     * @return This builder for method chaining
     */
    public UIGridBuilderV2 withVerticalScroll(int visibleRows, int totalRows) {
        scrollManager.enableVerticalScroll(visibleRows, totalRows);
        return this;
    }
    
    /**
     * Sets whether to use scroll buttons.
     * 
     * @param useButtons True to enable scroll buttons
     * @return This builder for method chaining
     */
    public UIGridBuilderV2 useScrollButtons(boolean useButtons) {
        scrollManager.setUseScrollButtons(useButtons);
        return this;
    }
    
    // ===== Element Addition Methods =====
    
    /**
     * Adds a text element to the grid.
     * 
     * @param row Row position (0-based)
     * @param column Column position (0-based)
     * @param text Text content
     * @param textColor Color of the text
     */
    public void addText(int row, int column, String text, int textColor) {
        elementManager.addText(row, column, text, textColor);
    }
    
    /**
     * Adds a button element to the grid.
     * 
     * @param row Row position (0-based)
     * @param column Column position (0-based)
     * @param text Button text
     * @param onClick Click handler
     * @param buttonColor Button color
     */
    public void addButton(int row, int column, String text, Consumer<Void> onClick, int buttonColor) {
        elementManager.addButton(row, column, text, onClick, buttonColor);
    }
    
    /**
     * Adds an item element to the grid.
     * 
     * @param row Row position (0-based)
     * @param column Column position (0-based)
     * @param itemStack ItemStack to display
     */
    public void addItem(int row, int column, ItemStack itemStack) {
        elementManager.addItem(row, column, itemStack);
    }
    
    /**
     * Adds a toggle element to the grid.
     * 
     * @param row Row position (0-based)
     * @param column Column position (0-based)
     * @param text Toggle text
     * @param initialState Initial toggle state
     * @param onToggle Toggle handler
     */
    public void addToggle(int row, int column, String text, boolean initialState, Consumer<Boolean> onToggle) {
        elementManager.addToggle(row, column, text, initialState, onToggle);
    }
    
    // ===== Interaction Methods =====
    
    /**
     * Handles a mouse click at the specified screen coordinates.
     * 
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @return True if click was handled
     */
    public boolean handleClick(double mouseX, double mouseY) {
        // Convert screen coordinates to grid coordinates
        GridPosition contentArea = layoutManager.getContentArea();
        
        if (mouseX < contentArea.getX() || mouseX > contentArea.getRight() ||
            mouseY < contentArea.getY() || mouseY > contentArea.getBottom()) {
            return false;
        }
        
        // Calculate which cell was clicked
        int cellWidth = contentArea.getWidth() / layoutManager.getColumns();
        int cellHeight = layoutManager.getCustomRowHeight() != null ? 
            layoutManager.getCustomRowHeight() : (contentArea.getHeight() / layoutManager.getRows());
        
        int column = (int)((mouseX - contentArea.getX()) / (cellWidth + layoutManager.getHorizontalSpacing()));
        int row = (int)((mouseY - contentArea.getY()) / (cellHeight + layoutManager.getVerticalSpacing()));
        
        // Adjust for scroll offsets
        row += scrollManager.getVerticalScrollOffset();
        column += scrollManager.getHorizontalScrollOffset();
        
        // Handle the click
        return elementManager.handleClick(row, column) || elementManager.handleToggle(row, column);
    }
    
    /**
     * Handles mouse scrolling.
     * 
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param delta Scroll delta
     * @return True if scrolling was handled
     */
    public boolean handleMouseScroll(double mouseX, double mouseY, double delta) {
        return scrollManager.handleMouseScroll(mouseX, mouseY, delta, layoutManager.getGridArea());
    }
    
    /**
     * Handles mouse drag events.
     * 
     * @param mouseX Current mouse X position
     * @param mouseY Current mouse Y position
     * @param button Mouse button
     * @param dragX Drag delta X
     * @param dragY Drag delta Y
     * @return True if drag was handled
     */
    public boolean handleMouseDrag(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return scrollManager.handleMouseDrag(mouseX, mouseY, button, dragX, dragY, layoutManager.getGridArea());
    }
    
    /**
     * Handles mouse press events.
     * 
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param button Mouse button
     * @return True if press was handled
     */
    public boolean handleMousePress(double mouseX, double mouseY, int button) {
        return scrollManager.handleMousePress(mouseX, mouseY, button, layoutManager.getGridArea());
    }
    
    /**
     * Handles mouse release events.
     * 
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param button Mouse button
     * @return True if release was handled
     */
    public boolean handleMouseRelease(double mouseX, double mouseY, int button) {
        return scrollManager.handleMouseRelease(mouseX, mouseY, button);
    }
    
    // ===== Rendering Methods =====
    
    /**
     * Renders the complete grid.
     * 
     * @param graphics Graphics context for rendering
     */
    public void render(GuiGraphics graphics) {
        renderingEngine.renderGrid(graphics, layoutManager, scrollManager, elementManager);
    }
    
    /**
     * Renders only the grid elements (without background or scrollbars).
     * 
     * @param graphics Graphics context for rendering
     */
    public void renderElements(GuiGraphics graphics) {
        renderingEngine.renderElements(graphics, layoutManager, scrollManager, elementManager);
    }
    
    // ===== Scrolling Control Methods =====
    
    /**
     * Scrolls left by one column.
     */
    public void scrollLeft() {
        scrollManager.scrollLeft();
    }
    
    /**
     * Scrolls right by one column.
     */
    public void scrollRight() {
        scrollManager.scrollRight();
    }
    
    /**
     * Scrolls up by one row.
     */
    public void scrollUp() {
        scrollManager.scrollUp();
    }
    
    /**
     * Scrolls down by one row.
     */
    public void scrollDown() {
        scrollManager.scrollDown();
    }
    
    /**
     * Sets the horizontal scroll offset.
     * 
     * @param offset New scroll offset
     */
    public void setHorizontalScrollOffset(int offset) {
        scrollManager.setHorizontalScrollOffset(offset);
    }
    
    /**
     * Sets the vertical scroll offset.
     * 
     * @param offset New scroll offset
     */
    public void setVerticalScrollOffset(int offset) {
        scrollManager.setVerticalScrollOffset(offset);
    }
    
    // ===== Utility Methods =====
    
    /**
     * Clears all elements from the grid.
     */
    public void clear() {
        elementManager.clear();
    }
    
    /**
     * Gets the number of elements in the grid.
     * 
     * @return Number of elements
     */
    public int getElementCount() {
        return elementManager.getElementCount();
    }
    
    /**
     * Updates the grid dimensions.
     * 
     * @param newRows New number of rows
     * @param newColumns New number of columns
     */
    public void updateDimensions(int newRows, int newColumns) {
        layoutManager.updateDimensions(newRows, newColumns);
    }
    
    // ===== Component Access Methods (for advanced usage) =====
    
    /**
     * Gets the layout manager for advanced layout operations.
     * 
     * @return The grid layout manager
     */
    public GridLayoutManager getLayoutManager() {
        return layoutManager;
    }
    
    /**
     * Gets the scroll manager for advanced scrolling operations.
     * 
     * @return The grid scroll manager
     */
    public GridScrollManager getScrollManager() {
        return scrollManager;
    }
    
    /**
     * Gets the element manager for advanced element operations.
     * 
     * @return The grid element manager
     */
    public GridElementManager getElementManager() {
        return elementManager;
    }
    
    /**
     * Gets the rendering engine for advanced rendering operations.
     * 
     * @return The grid rendering engine
     */
    public GridRenderingEngine getRenderingEngine() {
        return renderingEngine;
    }
}
