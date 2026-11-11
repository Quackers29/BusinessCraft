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
 * Fills a grid from column arrays or item counts.
 */
final class GridPopulation {
    private static final Logger LOGGER = LoggerFactory.getLogger(GridPopulation.class);
    private final UIGridBuilder grid;

    GridPopulation(UIGridBuilder grid) {
        this.grid = grid;
    }



    /**
     * Build a grid from column data arrays
     * 
     * @param columnData Arrays of GridContent objects for each column
     * @return This builder for chaining
     */
    UIGridBuilder withColumnData(List<GridContent>[] columnData) {
        if (columnData == null || columnData.length == 0) {
            return grid;
        }

        // Determine the number of rows needed (use the longest column)
        int maxRows = 0;
        for (List<GridContent> column : columnData) {
            maxRows = Math.max(maxRows, column.size());
        }

        // Update total rows for scrolling calculations
        grid.state.rows = maxRows;
        grid.state.totalRows = maxRows;

        // Calculate how many rows are visible based on the available height and row
        // height
        int availableHeight = grid.state.height - (2 * grid.state.verticalMargin);
        int calculatedRowHeight = grid.state.customRowHeight != null ? grid.state.customRowHeight
                : (availableHeight - ((Math.min(grid.state.rows, 10) - 1) * grid.state.verticalSpacing)) / Math.min(grid.state.rows, 10);

        // Determine if we need to enable vertical scrolling
        int recommendedVisibleRows = Math.max(1, availableHeight / (calculatedRowHeight + grid.state.verticalSpacing));
        if (maxRows > recommendedVisibleRows) {
            grid.withVerticalScroll(true, recommendedVisibleRows);
        }

        // Add all content elements to the grid
        for (int colIndex = 0; colIndex < columnData.length; colIndex++) {
            List<GridContent> column = columnData[colIndex];

            for (int rowIndex = 0; rowIndex < column.size(); rowIndex++) {
                GridContent content = column.get(rowIndex);

                switch (content.type) {
                    case TEXT:
                        grid.addLabel(rowIndex, colIndex, (String) content.value, content.textColor);
                        if (content.tooltip != null) {
                            // Update last added element with tooltip
                            grid.state.elements.get(grid.state.elements.size() - 1).tooltip = content.tooltip;
                        }
                        if (content.onClick != null) {
                            // Update last added element with click handler
                            grid.state.elements.get(grid.state.elements.size() - 1).onClick = content.onClick;
                        }
                        break;

                    case ITEM:
                        grid.addItem(rowIndex, colIndex, (net.minecraft.world.item.Item) content.value, 1, content.onClick);
                        if (content.tooltip != null) {
                            // Update last added element with tooltip
                            grid.state.elements.get(grid.state.elements.size() - 1).tooltip = content.tooltip;
                        }
                        break;

                    case ITEM_WITH_QUANTITY:
                        Object[] itemData = (Object[]) content.value;
                        net.minecraft.world.item.Item item = (net.minecraft.world.item.Item) itemData[0];
                        int quantity = (Integer) itemData[1];
                        grid.addItem(rowIndex, colIndex, item, quantity, content.onClick);
                        if (content.tooltip != null) {
                            // Update last added element with tooltip
                            grid.state.elements.get(grid.state.elements.size() - 1).tooltip = content.tooltip;
                        }
                        break;

                    case TOGGLE:
                        Object[] toggleData = (Object[]) content.value;
                        String toggleText = (String) toggleData[0];
                        boolean initialState = (Boolean) toggleData[1];
                        grid.addToggle(rowIndex, colIndex, toggleText, initialState, content.onToggle,
                                content.bgColor, 0x80555555); // Use default disabled color
                        if (content.tooltip != null) {
                            // Update last added element with tooltip
                            grid.state.elements.get(grid.state.elements.size() - 1).tooltip = content.tooltip;
                        }
                        break;

                    case BUTTON:
                        if (content.elementId != null) {
                            grid.addButtonWithTooltip(rowIndex, colIndex, content.elementId, (String) content.value,
                                    content.tooltip, content.onClick, content.bgColor);
                        } else {
                            grid.addButton(rowIndex, colIndex, (String) content.value, content.onClick, content.bgColor);
                            if (content.tooltip != null) {
                                grid.state.elements.get(grid.state.elements.size() - 1).tooltip = content.tooltip;
                            }
                        }
                        break;
                }
            }
        }

        return grid;
    }



    /**
     * Updates an existing grid with new column data while preserving scroll state
     * 
     * @param columnData Arrays of GridContent objects for each column
     * @return This builder for chaining
     */
    UIGridBuilder updateColumnData(List<GridContent>[] columnData) {
        if (columnData == null || columnData.length == 0) {
            return grid;
        }

        // Store current scroll state
        int savedVerticalScrollOffset = grid.state.verticalScrollOffset;
        int savedHorizontalScrollOffset = grid.state.horizontalScrollOffset;
        boolean wasVerticalScrollEnabled = grid.state.verticalScrollEnabled;
        boolean wasHorizontalScrollEnabled = grid.state.horizontalScrollEnabled;
        int savedVisibleRows = grid.state.visibleRows;
        int savedMaxVerticalScrollOffset = grid.state.maxVerticalScrollOffset;
        int savedMaxHorizontalScrollOffset = grid.state.maxHorizontalScrollOffset;

        // Clear existing elements and rebuild with new data
        grid.state.elements.clear();

        // Determine the number of rows needed (use the longest column)
        int maxRows = 0;
        for (List<GridContent> column : columnData) {
            maxRows = Math.max(maxRows, column.size());
        }

        // Update total rows for scrolling calculations
        grid.state.rows = maxRows;
        grid.state.totalRows = maxRows;

        // Restore scroll settings if they were enabled
        if (wasVerticalScrollEnabled) {
            grid.state.verticalScrollEnabled = true;
            grid.state.visibleRows = savedVisibleRows;
            grid.state.maxVerticalScrollOffset = Math.max(0, grid.state.totalRows - grid.state.visibleRows);

            // Restore scroll offset, but clamp it to the new valid range
            grid.state.verticalScrollOffset = Math.min(savedVerticalScrollOffset, grid.state.maxVerticalScrollOffset);
        }

        if (wasHorizontalScrollEnabled) {
            grid.state.horizontalScrollEnabled = true;
            grid.state.horizontalScrollOffset = Math.min(savedHorizontalScrollOffset, savedMaxHorizontalScrollOffset);
            grid.state.maxHorizontalScrollOffset = savedMaxHorizontalScrollOffset;
        }

        // Add all content elements to the grid
        for (int colIndex = 0; colIndex < columnData.length; colIndex++) {
            List<GridContent> column = columnData[colIndex];

            for (int rowIndex = 0; rowIndex < column.size(); rowIndex++) {
                GridContent content = column.get(rowIndex);

                switch (content.type) {
                    case TEXT:
                        grid.addLabel(rowIndex, colIndex, (String) content.value, content.textColor);
                        if (content.tooltip != null) {
                            grid.state.elements.get(grid.state.elements.size() - 1).tooltip = content.tooltip;
                        }
                        if (content.onClick != null) {
                            grid.state.elements.get(grid.state.elements.size() - 1).onClick = content.onClick;
                        }
                        break;

                    case ITEM:
                        grid.addItem(rowIndex, colIndex, (net.minecraft.world.item.Item) content.value, 1, content.onClick);
                        if (content.tooltip != null) {
                            grid.state.elements.get(grid.state.elements.size() - 1).tooltip = content.tooltip;
                        }
                        break;

                    case ITEM_WITH_QUANTITY:
                        Object[] itemData = (Object[]) content.value;
                        net.minecraft.world.item.Item item = (net.minecraft.world.item.Item) itemData[0];
                        int quantity = (Integer) itemData[1];
                        grid.addItem(rowIndex, colIndex, item, quantity, content.onClick);
                        if (content.tooltip != null) {
                            grid.state.elements.get(grid.state.elements.size() - 1).tooltip = content.tooltip;
                        }
                        break;

                    case TOGGLE:
                        Object[] toggleData = (Object[]) content.value;
                        String toggleText = (String) toggleData[0];
                        boolean initialState = (Boolean) toggleData[1];
                        grid.addToggle(rowIndex, colIndex, toggleText, initialState, content.onToggle,
                                content.bgColor, 0x80555555);
                        if (content.tooltip != null) {
                            grid.state.elements.get(grid.state.elements.size() - 1).tooltip = content.tooltip;
                        }
                        break;

                    case BUTTON:
                        grid.addButton(rowIndex, colIndex, (String) content.value, content.onClick, content.bgColor);
                        if (content.tooltip != null) {
                            grid.state.elements.get(grid.state.elements.size() - 1).tooltip = content.tooltip;
                        }
                        break;
                }
            }
        }

        return grid;
    }



    /**
     * Utility method to create a grid from item-quantity pairs
     * 
     * @param itemQuantityPairs Map of items to their quantities
     * @param tooltips          Map of items to their tooltip text (can be null)
     * @param textColor         Color for the item name text
     * @return This builder for chaining
     */
    UIGridBuilder withItemQuantityPairs(Map<net.minecraft.world.item.Item, Integer> itemQuantityPairs,
            Map<net.minecraft.world.item.Item, String> tooltips,
            int textColor) {
        // Clear existing elements
        grid.state.elements.clear();

        // Sort items by name
        List<Map.Entry<net.minecraft.world.item.Item, Integer>> sortedEntries = new ArrayList<>(
                itemQuantityPairs.entrySet());

        sortedEntries.sort((a, b) -> {
            String nameA = a.getKey().getDescriptionId();
            String nameB = b.getKey().getDescriptionId();
            return nameA.compareToIgnoreCase(nameB);
        });

        // Calculate total rows needed
        int totalRows = sortedEntries.size();

        // Adjust rows and columns based on data
        grid.state.columns = 2; // Item and quantity
        grid.state.rows = totalRows;

        // Calculate visible rows based on available height and row height
        int effectiveRowHeight = grid.state.customRowHeight != null ? grid.state.customRowHeight : 16;

        // Calculate available height (accounting for margins)
        int availableHeight = grid.state.height - (grid.state.verticalMargin * 2);

        // Calculate how many rows can fit in the available height
        // Use Math.max to ensure at least 1 row is visible
        int calculatedVisibleRows = Math.max(4, availableHeight / (effectiveRowHeight + grid.state.verticalSpacing));

        DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER,
                "Row calculation: availableHeight={}, effectiveRowHeight={}, spacing={}, calculatedRows={}",
                availableHeight, effectiveRowHeight, grid.state.verticalSpacing, calculatedVisibleRows);

        // Force vertical scrolling if we have more than 4 rows
        if (totalRows > 4) {
            grid.withVerticalScroll(true, calculatedVisibleRows);
        }

        // Add item elements
        for (int i = 0; i < sortedEntries.size(); i++) {
            Map.Entry<net.minecraft.world.item.Item, Integer> entry = sortedEntries.get(i);
            net.minecraft.world.item.Item item = entry.getKey();

            // Get tooltip if available
            String tooltip = tooltips != null ? tooltips.get(item) : null;

            // Create a formatted item name
            String itemName = formatItemName(item.getDescriptionId());

            // Add label for item name (first column)
            grid.addLabel(i, 0, itemName, textColor);
            GridElement labelElement = grid.state.elements.get(grid.state.elements.size() - 1);
            if (tooltip != null) {
                labelElement.tooltip = tooltip;
            }

            // Add item with quantity (second column)
            grid.addItem(i, 1, item, entry.getValue(), null);
            GridElement itemElement = grid.state.elements.get(grid.state.elements.size() - 1);
            itemElement.showQuantity = true; // Always show quantity
            if (tooltip != null) {
                itemElement.tooltip = tooltip;
            }
        }

        return grid;
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
    UIGridBuilder updateItemQuantityPairs(Map<net.minecraft.world.item.Item, Integer> itemQuantityPairs,
            Map<net.minecraft.world.item.Item, String> tooltips,
            int textColor) {
        return updateItemQuantityPairs(itemQuantityPairs, tooltips, null, textColor);
    }



    UIGridBuilder updateItemQuantityPairs(Map<net.minecraft.world.item.Item, Integer> itemQuantityPairs,
            Map<net.minecraft.world.item.Item, String> tooltips,
            Map<net.minecraft.world.item.Item, String> nameOverrides,
            int textColor) {
        // Store current scroll state
        int savedVerticalScrollOffset = grid.state.verticalScrollOffset;
        int savedHorizontalScrollOffset = grid.state.horizontalScrollOffset;
        boolean wasVerticalScrollEnabled = grid.state.verticalScrollEnabled;
        boolean wasHorizontalScrollEnabled = grid.state.horizontalScrollEnabled;
        int savedVisibleRows = grid.state.visibleRows;
        int savedMaxVerticalScrollOffset = grid.state.maxVerticalScrollOffset;
        int savedMaxHorizontalScrollOffset = grid.state.maxHorizontalScrollOffset;

        // Clear existing elements and rebuild with new data
        grid.state.elements.clear();

        // Sort items by name
        List<Map.Entry<net.minecraft.world.item.Item, Integer>> sortedEntries = new ArrayList<>(
                itemQuantityPairs.entrySet());

        sortedEntries.sort((a, b) -> {
            String nameA = a.getKey().getDescriptionId();
            String nameB = b.getKey().getDescriptionId();
            return nameA.compareToIgnoreCase(nameB);
        });

        // Use the same row/column logic as withItemQuantityPairs
        int totalRows = sortedEntries.size();
        grid.state.columns = 2; // Item and quantity
        grid.state.rows = totalRows;
        grid.state.totalRows = totalRows; // Update totalRows for scrolling calculations

        // Add item elements
        for (int i = 0; i < sortedEntries.size(); i++) {
            Map.Entry<net.minecraft.world.item.Item, Integer> entry = sortedEntries.get(i);
            net.minecraft.world.item.Item item = entry.getKey();

            // Get tooltip if available
            String tooltip = tooltips != null ? tooltips.get(item) : null;

            // Create a formatted item name
            String itemName;
            if (nameOverrides != null && nameOverrides.containsKey(item)) {
                itemName = nameOverrides.get(item);
            } else {
                itemName = formatItemName(item.getDescriptionId());
            }

            // Add label for item name
            grid.addLabel(i, 0, itemName, textColor);
            GridElement labelElement = grid.state.elements.get(grid.state.elements.size() - 1);
            if (tooltip != null) {
                labelElement.tooltip = tooltip;
            }

            // Add item with quantity
            grid.addItem(i, 1, item, entry.getValue(), null);
            GridElement itemElement = grid.state.elements.get(grid.state.elements.size() - 1);
            itemElement.showQuantity = true;
            if (tooltip != null) {
                itemElement.tooltip = tooltip;
            }
        }

        // Restore scroll state
        if (wasVerticalScrollEnabled) {
            grid.state.verticalScrollEnabled = true;
            grid.state.visibleRows = savedVisibleRows;
            // Recalculate max scroll based on new totalRows
            grid.state.maxVerticalScrollOffset = Math.max(0, grid.state.totalRows - grid.state.visibleRows);
            // Clamp current scroll to the new valid range
            grid.state.verticalScrollOffset = Math.min(savedVerticalScrollOffset, grid.state.maxVerticalScrollOffset);
        } else if (totalRows > 4) { // If scrolling was not enabled but now needed
            int effectiveRowHeight = grid.state.customRowHeight != null ? grid.state.customRowHeight : 16;
            int availableHeight = grid.state.height - (grid.state.verticalMargin * 2);
            int calculatedVisibleRows = Math.max(4, availableHeight / (effectiveRowHeight + grid.state.verticalSpacing));
            grid.withVerticalScroll(true, calculatedVisibleRows);
            grid.state.verticalScrollOffset = 0; // Start at top for new data
        }

        return grid;
    }



    /**
     * Populates the grid with item-quantity pairs with custom names
     */
    UIGridBuilder withItemQuantityPairs(Map<net.minecraft.world.item.Item, Integer> items,
            Map<net.minecraft.world.item.Item, String> tooltips,
            Map<net.minecraft.world.item.Item, String> nameOverrides,
            int textColor) {
        if (items == null)
            return grid;

        // Store max columns for scrolling
        int totalItems = items.size();
        grid.state.totalRows = totalItems; // One item per row currently (Label | Item+Qty)

        // Clear existing elements
        grid.state.elements.clear();

        boolean wasHorizontalScrollEnabled = grid.state.horizontalScrollEnabled;
        int savedHorizontalScrollOffset = grid.state.horizontalScrollOffset;
        int savedMaxHorizontalScrollOffset = grid.state.maxHorizontalScrollOffset;

        // Reset scroll state if columns changed significantly or if forced
        if (grid.state.visibleRows == 0 || grid.state.autoVisibleRows) {
            grid.state.autoVisibleRows = true;
            int effectiveRowHeight = grid.state.customRowHeight != null ? grid.state.customRowHeight : 16;
            int availableHeight = grid.state.height - (grid.state.verticalMargin * 2);
            int calculatedVisibleRows = Math.max(4, availableHeight / (effectiveRowHeight + grid.state.verticalSpacing));
            grid.withVerticalScroll(true, calculatedVisibleRows);
            grid.state.verticalScrollOffset = 0; // Start at top for new data
        }

        if (wasHorizontalScrollEnabled) {
            grid.state.horizontalScrollEnabled = true;
            grid.state.maxHorizontalScrollOffset = savedMaxHorizontalScrollOffset; // Assuming horizontal columns don't change
            grid.state.horizontalScrollOffset = Math.min(savedHorizontalScrollOffset, grid.state.maxHorizontalScrollOffset);
        }

        // Sort items? No, assume map is ordered (e.g. LinkedHashMap)
        List<Map.Entry<net.minecraft.world.item.Item, Integer>> sortedEntries = new ArrayList<>(items.entrySet());

        // Add item elements
        for (int i = 0; i < sortedEntries.size(); i++) {
            Map.Entry<net.minecraft.world.item.Item, Integer> entry = sortedEntries.get(i);
            net.minecraft.world.item.Item item = entry.getKey();

            // Create a formatted item name
            String itemName;
            if (nameOverrides != null && nameOverrides.containsKey(item)) {
                itemName = nameOverrides.get(item);
            } else {
                itemName = formatItemName(item.getDescriptionId());
            }

            // Determine tooltip
            String tooltip = tooltips != null ? tooltips.get(item) : null;

            // Add label for item name (first column)
            // Fix: Add tooltip to the name label as well for better UX
            grid.addLabelWithTooltip(i, 0, itemName, tooltip, textColor);

            // Add item with quantity (second column)
            grid.addItem(i, 1, item, entry.getValue(), null);
            GridElement element = grid.state.elements.get(grid.state.elements.size() - 1);
            element.showQuantity = true; // Always show quantity
            if (tooltip != null) {
                element.tooltip = tooltip;
            }
        }

        DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER,
                "Updated grid with {} items, preserved scroll offset: {}", sortedEntries.size(),
                grid.state.verticalScrollOffset);

        return grid;
    }



    /**
     * Format an item's description ID into a readable name
     */
    String formatItemName(String descriptionId) {
        // Extract the item name from the description ID (e.g., "block.minecraft.stone"
        // -> "Stone")
        String[] parts = descriptionId.split("\\.");
        if (parts.length > 0) {
            String name = parts[parts.length - 1];
            // Capitalize first letter and replace underscores with spaces
            name = name.substring(0, 1).toUpperCase() + name.substring(1).replace('_', ' ');
            return name;
        }
        return descriptionId;
    }

}
