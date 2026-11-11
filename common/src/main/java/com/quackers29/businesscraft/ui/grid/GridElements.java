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
 * Builds the cells on a grid.
 */
final class GridElements {
    private static final Logger LOGGER = LoggerFactory.getLogger(GridElements.class);
    private final GridState state;

    GridElements(GridState state) {
        this.state = state;
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
    GridElements addButton(int row, int column, String text, Consumer<Void> onClick, int bgColor) {
        GridElement element = new GridElement(GridElement.Type.BUTTON, row, column, 1, 1);
        element.text = text;
        element.onClick = onClick;
        element.backgroundColor = bgColor;
        state.elements.add(element);
        return this;
    }



    /**
     * Adds a button that spans multiple cells
     */
    GridElements addButton(int row, int column, int rowSpan, int colSpan,
            String text, Consumer<Void> onClick, int bgColor) {
        GridElement element = new GridElement(GridElement.Type.BUTTON, row, column, rowSpan, colSpan);
        element.text = text;
        element.onClick = onClick;
        element.backgroundColor = bgColor;
        state.elements.add(element);
        return this;
    }



    /**
     * Adds a label to the grid
     */
    GridElements addLabel(int row, int column, String text, int textColor) {
        GridElement element = new GridElement(GridElement.Type.LABEL, row, column, 1, 1);
        element.text = text;
        element.textColor = textColor;
        state.elements.add(element);
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
    GridElements addLabelWithTooltip(int row, int column, String text, String tooltip, int textColor) {
        GridElement element = new GridElement(GridElement.Type.LABEL, row, column, 1, 1);
        element.text = text;
        element.tooltip = tooltip;
        element.textColor = textColor;
        state.elements.add(element);
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
    GridElements addLabelWithTooltip(int row, int column, int rowSpan, int colSpan,
            String text, String tooltip, int textColor) {
        GridElement element = new GridElement(GridElement.Type.LABEL, row, column, rowSpan, colSpan);
        element.text = text;
        element.tooltip = tooltip;
        element.textColor = textColor;
        state.elements.add(element);
        return this;
    }



    /**
     * Adds a centered label to the grid
     */
    GridElements addCenteredLabel(int row, int column, String text, int textColor) {
        GridElement element = new GridElement(GridElement.Type.LABEL, row, column, 1, 1);
        element.text = text;
        element.textColor = textColor;
        element.textAlignment = TextAlignment.CENTER;
        state.elements.add(element);
        return this;
    }



    /**
     * Adds a label with specific alignment to the grid
     */
    GridElements addLabelWithAlignment(int row, int column, String text, int textColor,
            TextAlignment alignment) {
        GridElement element = new GridElement(GridElement.Type.LABEL, row, column, 1, 1);
        element.text = text;
        element.textColor = textColor;
        element.textAlignment = alignment;
        state.elements.add(element);
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
    GridElements addStatusIndicator(int row, int column, boolean isActive, int activeColor, int inactiveColor,
            int size) {
        GridElement element = new GridElement(GridElement.Type.STATUS_INDICATOR, row, column, 1, 1);
        element.text = isActive ? "●" : "○";
        element.textColor = isActive ? activeColor : inactiveColor;
        element.indicatorSize = size;
        state.elements.add(element);
        return this;
    }



    /**
     * Adds a large centered status indicator to the grid with default size (14px)
     */
    GridElements addStatusIndicator(int row, int column, boolean isActive, int activeColor, int inactiveColor) {
        return addStatusIndicator(row, column, isActive, activeColor, inactiveColor, 14);
    }



    /**
     * Adds a toggle button to the grid
     */
    GridElements addToggle(int row, int column, String text, boolean initialState,
            Consumer<Boolean> onToggle, int enabledColor, int disabledColor) {
        GridElement element = new GridElement(GridElement.Type.TOGGLE, row, column, 1, 1);
        element.text = text;
        element.toggled = initialState;
        element.onToggle = onToggle;
        element.backgroundColor = initialState ? enabledColor : disabledColor;
        element.altBackgroundColor = initialState ? disabledColor : enabledColor;
        state.elements.add(element);
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
    GridElements addButtonWithTooltip(int row, int column, String text, String tooltip, Consumer<Void> onClick,
            int bgColor) {
        return addButtonWithTooltip(row, column, null, text, tooltip, onClick, bgColor);
    }



    /**
     * Adds a button with optional stable {@code elementId} for automated navigation (showcase harness).
     */
    GridElements addButtonWithTooltip(int row, int column, String elementId, String text, String tooltip,
            Consumer<Void> onClick, int bgColor) {
        GridElement element = new GridElement(GridElement.Type.BUTTON, row, column, 1, 1);
        element.elementId = elementId;
        element.text = text;
        element.tooltip = tooltip;
        element.onClick = onClick;
        element.backgroundColor = bgColor;
        state.elements.add(element);
        return this;
    }



    /**
     * Invokes a button's {@code onClick} handler by stable id (no mouse coordinates).
     */
    boolean invokeButtonById(String elementId) {
        if (elementId == null || elementId.isBlank()) {
            return false;
        }
        for (GridElement element : state.elements) {
            if (element.type == GridElement.Type.BUTTON && elementId.equals(element.elementId)) {
                return invokeButton(element);
            }
        }
        return false;
    }



    /**
     * Invokes a button's {@code onClick} handler by visible label (case-insensitive).
     */
    boolean invokeButtonByLabel(String label) {
        if (label == null || label.isBlank()) {
            return false;
        }
        for (GridElement element : state.elements) {
            if (element.type == GridElement.Type.BUTTON && element.text != null
                    && element.text.equalsIgnoreCase(label.trim())) {
                return invokeButton(element);
            }
        }
        return false;
    }



    private static boolean invokeButton(GridElement element) {
        if (element.onClick != null) {
            element.onClick.accept(null);
            return true;
        }
        return false;
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
    GridElements addButtonWithTooltip(int row, int column, int rowSpan, int colSpan,
            String text, String tooltip, Consumer<Void> onClick, int bgColor) {
        GridElement element = new GridElement(GridElement.Type.BUTTON, row, column, rowSpan, colSpan);
        element.text = text;
        element.tooltip = tooltip;
        element.onClick = onClick;
        element.backgroundColor = bgColor;
        state.elements.add(element);
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
    GridElements addToggleWithTooltip(int row, int column, String text, String tooltip, boolean initialState,
            Consumer<Boolean> onToggle, int enabledColor, int disabledColor) {
        GridElement element = new GridElement(GridElement.Type.TOGGLE, row, column, 1, 1);
        element.text = text;
        element.tooltip = tooltip;
        element.toggled = initialState;
        element.onToggle = onToggle;
        element.backgroundColor = initialState ? enabledColor : disabledColor;
        element.altBackgroundColor = initialState ? disabledColor : enabledColor;
        state.elements.add(element);
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
    GridElements addItem(int row, int column, net.minecraft.world.item.Item item, int quantity,
            Consumer<Void> onClick) {
        GridElement element = new GridElement(GridElement.Type.ITEM, row, column, 1, 1);
        element.item = item;
        element.quantity = quantity;
        element.onClick = onClick;
        element.showQuantity = true; // Always show quantity, even for single items
        state.elements.add(element);
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
    GridElements addItem(int row, int column, int rowSpan, int colSpan,
            net.minecraft.world.item.Item item, int quantity, Consumer<Void> onClick) {
        GridElement element = new GridElement(GridElement.Type.ITEM, row, column, rowSpan, colSpan);
        element.item = item;
        element.quantity = quantity;
        element.onClick = onClick;
        element.showQuantity = true; // Always show quantity, even for single items
        state.elements.add(element);
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
    GridElements addItemWithTooltip(int row, int column, net.minecraft.world.item.Item item,
            int quantity, String tooltip, Consumer<Void> onClick) {
        GridElement element = new GridElement(GridElement.Type.ITEM, row, column, 1, 1);
        element.item = item;
        element.quantity = quantity;
        element.tooltip = tooltip;
        element.onClick = onClick;
        element.showQuantity = true; // Always show quantity, even for single items
        state.elements.add(element);
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
    GridElements addItemStack(int row, int column, net.minecraft.world.item.ItemStack itemStack,
            Consumer<Void> onClick) {
        GridElement element = new GridElement(GridElement.Type.ITEM, row, column, 1, 1);
        element.item = itemStack.getItem();
        element.quantity = itemStack.getCount();
        element.itemStack = itemStack; // Store full itemstack for rendering
        element.onClick = onClick;
        element.showQuantity = true; // Always show quantity, even for single items
        state.elements.add(element);
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
    GridElements addItemStack(int row, int column, int rowSpan, int colSpan,
            net.minecraft.world.item.ItemStack itemStack, Consumer<Void> onClick) {
        GridElement element = new GridElement(GridElement.Type.ITEM, row, column, rowSpan, colSpan);
        element.item = itemStack.getItem();
        element.quantity = itemStack.getCount();
        element.itemStack = itemStack; // Store full itemstack for rendering
        element.onClick = onClick;
        element.showQuantity = true; // Always show quantity, even for single items
        state.elements.add(element);
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
    GridElements addItemStackWithTooltip(int row, int column, net.minecraft.world.item.ItemStack itemStack,
            String tooltip, Consumer<Void> onClick) {
        GridElement element = new GridElement(GridElement.Type.ITEM, row, column, 1, 1);
        element.item = itemStack.getItem();
        element.quantity = itemStack.getCount();
        element.itemStack = itemStack; // Store full itemstack for rendering
        element.tooltip = tooltip;
        element.onClick = onClick;
        element.showQuantity = true; // Always show quantity, even for single items
        state.elements.add(element);
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
    GridElements addMultiItemDisplay(int row, int column, List<net.minecraft.world.item.ItemStack> itemStacks,
            String tooltip, Consumer<Void> onClick) {
        if (itemStacks == null || itemStacks.isEmpty()) {
            return this;
        }

        GridElement element = new GridElement(GridElement.Type.MULTI_ITEM, row, column, 1, 1);
        element.multiItems = new ArrayList<>(itemStacks.subList(0, Math.min(4, itemStacks.size()))); // Max 4 items
        element.tooltip = tooltip;
        element.onClick = onClick;
        element.showQuantity = false; // Don't show quantities for multi-item display
        state.elements.add(element);
        return this;
    }



    /**
     * Adds a status icon to the grid
     */
    GridElements addStatusIcon(int row, int column, StatusSymbol symbol) {
        GridElement element = new GridElement(GridElement.Type.STATUS_ICON, row, column, 1, 1);
        element.statusSymbol = symbol;
        state.elements.add(element);
        return this;
    }

}
