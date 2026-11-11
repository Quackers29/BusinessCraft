package com.quackers29.businesscraft.ui.grid;

import java.util.List;
import java.util.function.Consumer;

/** One placed cell. Package-visible so the painter and input classes can read it. */
final class GridElement {
    enum Type {

        BUTTON,
        LABEL,
        TOGGLE,
        ITEM, // Single Minecraft item with quantity indicator
        MULTI_ITEM, // Multiple overlapping Minecraft items
        STATUS_INDICATOR, // Large centered status indicator
        STATUS_ICON // Icon based status indicator (Checkmark, Lock, etc)
        }


        GridElement.Type type;
        int row, column;
        int rowSpan, colSpan;
        String elementId;
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
        int quantity = 1; // Quantity to display
        boolean showQuantity = true; // Whether to show quantity indicator
        net.minecraft.world.item.ItemStack itemStack; // Full itemstack for rendering
        List<net.minecraft.world.item.ItemStack> multiItems; // Multiple items for overlapping display

        // Status indicator specific fields
        int indicatorSize = 12; // Size of status indicator in pixels
        boolean isTruncated = false; // Track if text was truncated during rendering

        // Status Icon specific fields
        StatusSymbol statusSymbol;

        GridElement(GridElement.Type type, int row, int column, int rowSpan, int colSpan) {
            this.type = type;
            this.row = row;
            this.column = column;
            this.rowSpan = rowSpan;
            this.colSpan = colSpan;
        }
    }
