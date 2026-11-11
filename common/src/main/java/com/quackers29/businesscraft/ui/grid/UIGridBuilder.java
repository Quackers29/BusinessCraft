package com.quackers29.businesscraft.ui.grid;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.ui.theme.UiTheme;
import com.quackers29.businesscraft.ui.theme.UiTooltip;
import com.quackers29.businesscraft.ui.util.UiTextWrap;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Grid layout for BusinessCraft screens.
 * Painting, input, cell construction, and list filling live in focused helpers.
 * Screens keep calling this class.
 */
public class UIGridBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(UIGridBuilder.class);

    final GridState state = new GridState();
    private final GridPainter painter = new GridPainter(state);
    private final GridInput input = new GridInput(state);
    private final GridElements elementsApi = new GridElements(state);
    private final GridPopulation population = new GridPopulation(this);

public static UIGridBuilder create(int x, int y, int width, int height, int columns) {
        return new UIGridBuilder(x, y, width, height, 1, columns);
    }



    /**
     * Creates a new grid builder with the specified dimensions and layout
     * 
     * @param x       X position of the grid
     * @param y       Y position of the grid
     * @param width   Width of the grid
     * @param height  Height of the grid
     * @param rows    Number of rows in the grid (can be updated later by data)
     * @param columns Number of columns in the grid
     */
    public UIGridBuilder(int x, int y, int width, int height, int rows, int columns) {
        state.x = x;
        state.y = y;
        state.width = width;
        state.height = height;
        state.rows = rows;
        state.columns = columns;
        state.horizontalSpacing = 10;
        state.verticalSpacing = 2;
        state.horizontalMargin = 15;
        state.verticalMargin = 6;

        // Rate-limit debug logging - only log once per second
        long currentTime = System.currentTimeMillis();
        if (currentTime - state.lastGridCreateLogTime > 1000) {
            DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER,
                    String.format(
                            "[GRID CREATE] New grid at (%d,%d) size (%dx%d), %d columns, verticalScrollEnabled=%b",
                            x, y, width, height, columns, state.verticalScrollEnabled));
            state.lastGridCreateLogTime = currentTime;
        }
    }



    /**
     * Sets the spacing between grid cells
     */
    public UIGridBuilder withSpacing(int horizontalSpacing, int verticalSpacing) {
        state.horizontalSpacing = horizontalSpacing;
        state.verticalSpacing = verticalSpacing;
        return this;
    }



    /**
     * Relative column widths. The last column absorbs rounding so the row still
     * fills the grid. Ignored while horizontal scrolling is on.
     */
    public UIGridBuilder withColumnWeights(float[] weights) {
        state.columnWeights = weights == null ? null : weights.clone();
        return this;
    }



    /**
     * Sets the margins around the grid
     */
    public UIGridBuilder withMargins(int horizontalMargin, int verticalMargin) {
        state.horizontalMargin = horizontalMargin;
        state.verticalMargin = verticalMargin;
        return this;
    }



    /**
     * Sets the background color
     */
    public UIGridBuilder withBackgroundColor(int color) {
        state.backgroundColor = color;
        return this;
    }



    /**
     * Sets the border color
     */
    public UIGridBuilder withBorderColor(int color) {
        state.borderColor = color;
        return this;
    }



    /**
     * Sets whether to draw the background
     */
    public UIGridBuilder drawBackground(boolean draw) {
        state.drawBackground = draw;
        return this;
    }



    /**
     * Sets whether to draw the border
     */
    public UIGridBuilder drawBorder(boolean draw) {
        state.drawBorder = draw;
        return this;
    }



    /**
     * Enables horizontal scrolling with a specific number of visible columns
     * 
     * @param visibleColumns Number of columns to show at once
     * @param totalColumns   Total number of columns in the data (can be more than
     *                       grid columns)
     * @return This builder for chaining
     */
    public UIGridBuilder withHorizontalScroll(int visibleColumns, int totalColumns) {
        state.horizontalScrollEnabled = true;
        state.visibleColumns = visibleColumns;
        state.totalColumns = totalColumns;
        state.maxHorizontalScrollOffset = Math.max(0, totalColumns - visibleColumns);
        return this;
    }



    /**
     * Use buttons for scrolling instead of a scrollbar
     * 
     * @param useButtons Whether to use buttons for scrolling
     * @return This builder for chaining
     */
    public UIGridBuilder useScrollButtons(boolean useButtons) {
        state.useScrollButtons = useButtons;
        return this;
    }



    /**
     * Sets a custom fixed row height
     * 
     * @param height Height in pixels for each row
     * @return This builder for chaining
     */
    public UIGridBuilder withRowHeight(int height) {
        state.customRowHeight = height;
        return this;
    }



    /**
     * Enables vertical scrolling with a specific number of visible rows
     * 
     * @param enable      Whether to enable vertical scrolling
     * @param visibleRows Number of rows to show at once
     * @return This builder for chaining
     */
    public UIGridBuilder withVerticalScroll(boolean enable, int visibleRows) {
        state.verticalScrollEnabled = enable;
        state.visibleRows = visibleRows;
        state.autoVisibleRows = false; // Using static visible rows
        state.totalRows = state.rows;
        if (enable) {
            state.maxVerticalScrollOffset = Math.max(0, state.totalRows - visibleRows);
        }

        DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER,
                String.format("[GRID SCROLL] Grid at (%d,%d) enableVerticalScroll=%b, visibleRows=%d, totalRows=%d",
                        state.x, state.y, enable, visibleRows, state.totalRows));

        return this;
    }



    /**
     * Enables vertical scrolling with automatic visible row calculation based on
     * available height.
     * This replaces static visible row counts with dynamic calculations.
     * 
     * @param enable Whether to enable vertical scrolling
     * @return This builder for chaining
     */
    public UIGridBuilder withVerticalScrollAuto(boolean enable) {
        state.verticalScrollEnabled = enable;
        state.autoVisibleRows = enable; // Track that we're using auto calculation

        if (enable) {
            // Calculate visible rows based on available height and row specifications
            int effectiveRowHeight = state.getEffectiveRowHeight();
            int availableHeight = state.height - (state.verticalMargin * 2);

            // Calculate how many rows can fit in the available space
            int calculatedVisibleRows = Math.max(1, availableHeight / effectiveRowHeight);

            state.visibleRows = calculatedVisibleRows;
            state.totalRows = state.rows;
            state.maxVerticalScrollOffset = Math.max(0, state.totalRows - state.visibleRows);

            DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER,
                    String.format(
                            "[GRID AUTO-SCROLL] Grid at (%d,%d) auto-calculated: availableHeight=%d, rowHeight=%d, visibleRows=%d, totalRows=%d",
                            state.x, state.y, availableHeight, effectiveRowHeight, state.visibleRows, state.totalRows));
        }

        return this;
    }



    /**
     * Updates the total number of rows and recalculates scroll parameters
     * This should be called after adding all elements to ensure proper scrolling
     * 
     * @param newTotalRows The actual number of rows with content
     */
    public void updateTotalRows(int newTotalRows) {
        state.rows = newTotalRows;
        state.totalRows = newTotalRows;

        if (state.verticalScrollEnabled) {
            // Recalculate visible rows if we're using auto mode
            if (state.autoVisibleRows) {
                int effectiveRowHeight = state.getEffectiveRowHeight();
                int availableHeight = state.height - (state.verticalMargin * 2);
                int calculatedVisibleRows = Math.max(1, availableHeight / effectiveRowHeight);
                state.visibleRows = calculatedVisibleRows;

                DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER,
                        "Auto-recalculated visible rows: availableHeight={}, rowHeight={}, visibleRows={}",
                        availableHeight, effectiveRowHeight, state.visibleRows);
            }

            state.maxVerticalScrollOffset = Math.max(0, state.totalRows - state.visibleRows);
            // Clamp current offset to new valid range
            setVerticalScrollOffset(state.verticalScrollOffset);

            DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER,
                    "Updated total rows: {}, visible: {}, maxOffset: {}",
                    state.totalRows, state.visibleRows, state.maxVerticalScrollOffset);
        }
    }



    /**
     * Clears all elements while preserving grid structure and scroll state
     * This allows for data updates without losing scroll position
     */
    public void clearElements() {
        state.elements.clear();
        DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER, "Cleared all elements, preserving scroll state");
    }



    /**
     * Gets the number of visible rows in the grid (accounting for scrolling)
     * 
     * @return Number of rows currently displayed
     */
    public int getVisibleRowCount() {
        return state.verticalScrollEnabled ? state.visibleRows : state.totalRows;
    }



    /**
     * Gets the total number of rows in the grid
     * 
     * @return Total number of rows
     */
    public int getTotalRowCount() {
        return state.totalRows;
    }



    /**
     * Gets the current vertical scroll offset
     *
     * @return Current scroll offset in rows
     */
    public int getVerticalScrollOffset() {
        return state.verticalScrollOffset;
    }



    /**
     * Creates a grid with label-value pairs, which is a very common pattern
     * 
     * @param x          X position
     * @param y          Y position
     * @param width      Width
     * @param height     Height
     * @param labelColor Color for labels
     * @param valueColor Color for values
     * @param pairs      Map of label-value pairs
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
     * @param x           X position
     * @param y           Y position
     * @param width       Width
     * @param height      Height
     * @param labelColor  Color for labels
     * @param buttonColor Color for buttons
     * @param pairs       Map of label to button data (text and click handler)
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
            String buttonText = (String) buttonData[0];
            @SuppressWarnings("unchecked")
            Consumer<Void> onClick = (Consumer<Void>) buttonData[1];
            String elementId = buttonData.length > 2 && buttonData[2] instanceof String id ? id : null;

            columnData[0].add(GridContent.text(label, labelColor));
            columnData[1].add(GridContent.button(buttonText, onClick, buttonColor, elementId));
        }

        // Use the column data
        grid.withColumnData(columnData);

        return grid;
    }



    /**
     * Sets the current horizontal scroll offset
     * 
     * @param offset The new offset
     */
    public void setHorizontalScrollOffset(int offset) {
        input.setHorizontalScrollOffset(offset);
    }



    /**
     * Scroll left by one column
     */
    public void scrollLeft() {
        input.scrollLeft();
    }



    /**
     * Scroll right by one column
     */
    public void scrollRight() {
        input.scrollRight();
    }



    /**
     * Sets the current vertical scroll offset
     * 
     * @param offset The new offset
     */
    public void setVerticalScrollOffset(int offset) {
        input.setVerticalScrollOffset(offset);
    }



    /**
     * Scroll up by one row
     */
    public void scrollUp() {
        input.scrollUp();
    }



    /**
     * Scroll down by one row
     */
    public void scrollDown() {
        input.scrollDown();
    }



    /**
     * Adds a button to the grid at the specified position
     * 
     * @param row     Row index (0-based)
     * @param column  Column index (0-based)
     * @param text    Button text
     * @param onClick Button click handler
     * @param bgColor Background color
     * @return This builder for chaining
     */
    public UIGridBuilder addButton(int row, int column, String text, Consumer<Void> onClick, int bgColor) {
        elementsApi.addButton(row, column, text, onClick, bgColor);
        return this;
    }



    /**
     * Adds a button that spans multiple cells
     */
    public UIGridBuilder addButton(int row, int column, int rowSpan, int colSpan,
            String text, Consumer<Void> onClick, int bgColor) {
        elementsApi.addButton(row, column, rowSpan, colSpan, text, onClick, bgColor);
        return this;
    }



    /**
     * Adds a label to the grid
     */
    public UIGridBuilder addLabel(int row, int column, String text, int textColor) {
        elementsApi.addLabel(row, column, text, textColor);
        return this;
    }



    /**
     * Adds a label with a tooltip to the grid
     * 
     * @param row       Grid row
     * @param column    Grid column
     * @param text      Label text
     * @param tooltip   Tooltip text to show on hover
     * @param textColor Text color
     * @return This builder for chaining
     */
    public UIGridBuilder addLabelWithTooltip(int row, int column, String text, String tooltip, int textColor) {
        elementsApi.addLabelWithTooltip(row, column, text, tooltip, textColor);
        return this;
    }



    /**
     * Adds a label with a tooltip that spans multiple cells
     * 
     * @param row       Grid row
     * @param column    Grid column
     * @param rowSpan   Number of rows to span
     * @param colSpan   Number of columns to span
     * @param text      Label text
     * @param tooltip   Tooltip text to show on hover
     * @param textColor Text color
     * @return This builder for chaining
     */
    public UIGridBuilder addLabelWithTooltip(int row, int column, int rowSpan, int colSpan,
            String text, String tooltip, int textColor) {
        elementsApi.addLabelWithTooltip(row, column, rowSpan, colSpan, text, tooltip, textColor);
        return this;
    }



    /**
     * Adds a centered label to the grid
     */
    public UIGridBuilder addCenteredLabel(int row, int column, String text, int textColor) {
        elementsApi.addCenteredLabel(row, column, text, textColor);
        return this;
    }



    /**
     * Adds a label with specific alignment to the grid
     */
    public UIGridBuilder addLabelWithAlignment(int row, int column, String text, int textColor,
            TextAlignment alignment) {
        elementsApi.addLabelWithAlignment(row, column, text, textColor, alignment);
        return this;
    }



    /**
     * Adds a large centered status indicator to the grid
     * 
     * @param row           Grid row
     * @param column        Grid column
     * @param isActive      Whether the status is active (filled circle) or inactive
     *                      (empty circle)
     * @param activeColor   Color for active state
     * @param inactiveColor Color for inactive state
     * @param size          Size of the indicator in pixels (default 12)
     * @return This builder for chaining
     */
    public UIGridBuilder addStatusIndicator(int row, int column, boolean isActive, int activeColor, int inactiveColor,
            int size) {
        elementsApi.addStatusIndicator(row, column, isActive, activeColor, inactiveColor, size);
        return this;
    }



    /**
     * Adds a large centered status indicator to the grid with default size (14px)
     */
    public UIGridBuilder addStatusIndicator(int row, int column, boolean isActive, int activeColor, int inactiveColor) {
        elementsApi.addStatusIndicator(row, column, isActive, activeColor, inactiveColor);
        return this;
    }



    /**
     * Adds a toggle button to the grid
     */
    public UIGridBuilder addToggle(int row, int column, String text, boolean initialState,
            Consumer<Boolean> onToggle, int enabledColor, int disabledColor) {
        elementsApi.addToggle(row, column, text, initialState, onToggle, enabledColor, disabledColor);
        return this;
    }



    /**
     * Adds a button with a tooltip
     * 
     * @param row     Grid row
     * @param column  Grid column
     * @param text    Button text
     * @param tooltip Tooltip text to show on hover
     * @param onClick Button click handler
     * @param bgColor Background color
     * @return This builder for chaining
     */
    public UIGridBuilder addButtonWithTooltip(int row, int column, String text, String tooltip, Consumer<Void> onClick,
            int bgColor) {
        elementsApi.addButtonWithTooltip(row, column, text, tooltip, onClick, bgColor);
        return this;
    }



    /**
     * Adds a button with optional stable {@code elementId} for automated navigation (showcase harness).
     */
    public UIGridBuilder addButtonWithTooltip(int row, int column, String elementId, String text, String tooltip,
            Consumer<Void> onClick, int bgColor) {
        elementsApi.addButtonWithTooltip(row, column, elementId, text, tooltip, onClick, bgColor);
        return this;
    }



    /**
     * Invokes a button's {@code onClick} handler by stable id (no mouse coordinates).
     */
    public boolean invokeButtonById(String elementId) {
        return elementsApi.invokeButtonById(elementId);
    }



    /**
     * Invokes a button's {@code onClick} handler by visible label (case-insensitive).
     */
    public boolean invokeButtonByLabel(String label) {
        return elementsApi.invokeButtonByLabel(label);
    }



    /**
     * Adds a button with a tooltip that spans multiple cells
     * 
     * @param row     Grid row
     * @param column  Grid column
     * @param rowSpan Number of rows to span
     * @param colSpan Number of columns to span
     * @param text    Button text
     * @param tooltip Tooltip text to show on hover
     * @param onClick Button click handler
     * @param bgColor Background color
     * @return This builder for chaining
     */
    public UIGridBuilder addButtonWithTooltip(int row, int column, int rowSpan, int colSpan,
            String text, String tooltip, Consumer<Void> onClick, int bgColor) {
        elementsApi.addButtonWithTooltip(row, column, rowSpan, colSpan, text, tooltip, onClick, bgColor);
        return this;
    }



    /**
     * Adds a toggle button with a tooltip
     * 
     * @param row           Grid row
     * @param column        Grid column
     * @param text          Toggle button text
     * @param tooltip       Tooltip text to show on hover
     * @param initialState  Initial toggle state
     * @param onToggle      Toggle state change handler
     * @param enabledColor  Color when enabled
     * @param disabledColor Color when disabled
     * @return This builder for chaining
     */
    public UIGridBuilder addToggleWithTooltip(int row, int column, String text, String tooltip, boolean initialState,
            Consumer<Boolean> onToggle, int enabledColor, int disabledColor) {
        elementsApi.addToggleWithTooltip(row, column, text, tooltip, initialState, onToggle, enabledColor, disabledColor);
        return this;
    }



    /**
     * Adds an item display element to the grid
     * 
     * @param row      Row index (0-based)
     * @param column   Column index (0-based)
     * @param item     The Minecraft item to display
     * @param quantity The quantity to display
     * @param onClick  Callback for when the item is clicked (can be null)
     * @return This builder for chaining
     */
    public UIGridBuilder addItem(int row, int column, net.minecraft.world.item.Item item, int quantity,
            Consumer<Void> onClick) {
        elementsApi.addItem(row, column, item, quantity, onClick);
        return this;
    }



    /**
     * Adds an item display element to the grid that spans multiple cells
     * 
     * @param row      Row index (0-based)
     * @param column   Column index (0-based)
     * @param rowSpan  Number of rows to span
     * @param colSpan  Number of columns to span
     * @param item     The Minecraft item to display
     * @param quantity The quantity to display
     * @param onClick  Callback for when the item is clicked (can be null)
     * @return This builder for chaining
     */
    public UIGridBuilder addItem(int row, int column, int rowSpan, int colSpan,
            net.minecraft.world.item.Item item, int quantity, Consumer<Void> onClick) {
        elementsApi.addItem(row, column, rowSpan, colSpan, item, quantity, onClick);
        return this;
    }



    /**
     * Adds an item display element with a tooltip
     * 
     * @param row      Row index (0-based)
     * @param column   Column index (0-based)
     * @param item     The Minecraft item to display
     * @param quantity The quantity to display
     * @param tooltip  Tooltip text to show on hover
     * @param onClick  Callback for when the item is clicked (can be null)
     * @return This builder for chaining
     */
    public UIGridBuilder addItemWithTooltip(int row, int column, net.minecraft.world.item.Item item,
            int quantity, String tooltip, Consumer<Void> onClick) {
        elementsApi.addItemWithTooltip(row, column, item, quantity, tooltip, onClick);
        return this;
    }



    /**
     * Adds an item display element to the grid using an ItemStack
     * 
     * @param row       Row index (0-based)
     * @param column    Column index (0-based)
     * @param itemStack The ItemStack to display
     * @param onClick   Callback for when the item is clicked (can be null)
     * @return This builder for chaining
     */
    public UIGridBuilder addItemStack(int row, int column, net.minecraft.world.item.ItemStack itemStack,
            Consumer<Void> onClick) {
        elementsApi.addItemStack(row, column, itemStack, onClick);
        return this;
    }



    /**
     * Adds an item display element to the grid that spans multiple cells using an
     * ItemStack
     * 
     * @param row       Row index (0-based)
     * @param column    Column index (0-based)
     * @param rowSpan   Number of rows to span
     * @param colSpan   Number of columns to span
     * @param itemStack The ItemStack to display
     * @param onClick   Callback for when the item is clicked (can be null)
     * @return This builder for chaining
     */
    public UIGridBuilder addItemStack(int row, int column, int rowSpan, int colSpan,
            net.minecraft.world.item.ItemStack itemStack, Consumer<Void> onClick) {
        elementsApi.addItemStack(row, column, rowSpan, colSpan, itemStack, onClick);
        return this;
    }



    /**
     * Adds an item display element with a tooltip using an ItemStack
     * 
     * @param row       Row index (0-based)
     * @param column    Column index (0-based)
     * @param itemStack The ItemStack to display
     * @param tooltip   Tooltip text to show on hover (optional, null to use item's
     *                  tooltip)
     * @param onClick   Callback for when the item is clicked (can be null)
     * @return This builder for chaining
     */
    public UIGridBuilder addItemStackWithTooltip(int row, int column, net.minecraft.world.item.ItemStack itemStack,
            String tooltip, Consumer<Void> onClick) {
        elementsApi.addItemStackWithTooltip(row, column, itemStack, tooltip, onClick);
        return this;
    }



    /**
     * Adds multiple overlapping items for visual display (up to 4 items)
     * 
     * @param row        Row in the grid
     * @param column     Column in the grid
     * @param itemStacks List of ItemStacks to display overlapping
     * @param tooltip    Tooltip text for the items
     * @param onClick    Click handler
     * @return This builder for chaining
     */
    public UIGridBuilder addMultiItemDisplay(int row, int column, List<net.minecraft.world.item.ItemStack> itemStacks,
            String tooltip, Consumer<Void> onClick) {
        elementsApi.addMultiItemDisplay(row, column, itemStacks, tooltip, onClick);
        return this;
    }



    /**
     * Checks if the mouse is over a specific row in the grid with proper spacing
     * 
     * @param mouseX         Mouse X coordinate (screen relative)
     * @param mouseY         Mouse Y coordinate (screen relative)
     * @param rowIndex       The row index to check (0-based)
     * @param columnsToCheck Number of columns to check (typically 1-2 for tooltips)
     * @return True if mouse is over the specified row area
     */
    public boolean isMouseOverRow(int mouseX, int mouseY, int rowIndex, int columnsToCheck) {
        return input.isMouseOverRow(mouseX, mouseY, rowIndex, columnsToCheck);
    }



    /**
     * Gets the row index at the given mouse coordinates.
     * Accounts for scrolling and margins.
     *
     * @param mouseX Mouse X coordinate
     * @param mouseY Mouse Y coordinate
     * @return Row index (including scroll offset), or -1 if not over a row
     */
    public int getClickedRow(int mouseX, int mouseY) {
        return input.getClickedRow(mouseX, mouseY);
    }



    /**
     * Renders the grid and its elements
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        painter.render(graphics, mouseX, mouseY);
    }



    /**
     * Adds a status icon to the grid
     */
    public UIGridBuilder addStatusIcon(int row, int column, StatusSymbol symbol) {
        elementsApi.addStatusIcon(row, column, symbol);
        return this;
    }



    /**
     * Handles mouse clicks on the grid elements
     * 
     * @return true if a click was handled
     */
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        return input.mouseClicked(mouseX, mouseY, button);
    }



    /**
     * Handle mouse scrolling
     * 
     * @return true if handled
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return input.mouseScrolled(mouseX, mouseY, delta);
    }



    /**
     * Build a grid from column data arrays
     * 
     * @param columnData Arrays of GridContent objects for each column
     * @return This builder for chaining
     */
    public UIGridBuilder withColumnData(List<GridContent>[] columnData) {
        return population.withColumnData(columnData);
    }



    /**
     * Updates an existing grid with new column data while preserving scroll state
     * 
     * @param columnData Arrays of GridContent objects for each column
     * @return This builder for chaining
     */
    public UIGridBuilder updateColumnData(List<GridContent>[] columnData) {
        return population.updateColumnData(columnData);
    }



    /**
     * Utility method to create a grid from item-quantity pairs
     * 
     * @param itemQuantityPairs Map of items to their quantities
     * @param tooltips          Map of items to their tooltip text (can be null)
     * @param textColor         Color for the item name text
     * @return This builder for chaining
     */
    public UIGridBuilder withItemQuantityPairs(Map<net.minecraft.world.item.Item, Integer> itemQuantityPairs,
            Map<net.minecraft.world.item.Item, String> tooltips,
            int textColor) {
        return population.withItemQuantityPairs(itemQuantityPairs, tooltips, textColor);
    }



    /**
     * Updates an existing grid with new item-quantity pairs while preserving scroll
     * state
     * 
     * @param itemQuantityPairs Map of items to their quantities
     * @param tooltips          Map of items to their tooltip text (can be null)
     * @param textColor         Color for the item name text
     * @return This builder for chaining
     */
    public UIGridBuilder updateItemQuantityPairs(Map<net.minecraft.world.item.Item, Integer> itemQuantityPairs,
            Map<net.minecraft.world.item.Item, String> tooltips,
            int textColor) {
        return population.updateItemQuantityPairs(itemQuantityPairs, tooltips, textColor);
    }



    public UIGridBuilder updateItemQuantityPairs(Map<net.minecraft.world.item.Item, Integer> itemQuantityPairs,
            Map<net.minecraft.world.item.Item, String> tooltips,
            Map<net.minecraft.world.item.Item, String> nameOverrides,
            int textColor) {
        return population.updateItemQuantityPairs(itemQuantityPairs, tooltips, nameOverrides, textColor);
    }



    /**
     * Populates the grid with item-quantity pairs with custom names
     */
    public UIGridBuilder withItemQuantityPairs(Map<net.minecraft.world.item.Item, Integer> items,
            Map<net.minecraft.world.item.Item, String> tooltips,
            Map<net.minecraft.world.item.Item, String> nameOverrides,
            int textColor) {
        return population.withItemQuantityPairs(items, tooltips, nameOverrides, textColor);
    }



    /**
     * Handle mouse dragging for scrolling
     * 
     * @return true if the drag was handled
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return input.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }



    /**
     * Handle mouse release
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return input.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * Keeps hovered tooltips inside this grid. Production uses this so a long
     * recipe tooltip cannot draw outside the list.
     */
    public UIGridBuilder clampTooltipsToPanel() {
        state.clampTooltip = true;
        return this;
    }

    /** Showcase pins the tooltip for one row. */
    public void showcaseHoverRow(int row) {
        state.forcedTooltipRow = row;
    }

    public int showcaseAnchorX() {
        return state.anchorX;
    }

    public int showcaseAnchorY() {
        return state.anchorY;
    }

    /** First non-empty tooltip on {@code row}, for the showcase wait. */
    public String showcaseTooltip(int row) {
        for (GridElement element : state.elements) {
            if (element.row == row && element.tooltip != null && !element.tooltip.isEmpty()) {
                return element.tooltip;
            }
        }
        return null;
    }
}
