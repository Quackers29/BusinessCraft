package com.quackers29.businesscraft.ui.grid;

import java.util.function.Consumer;

/**
 * One cell's data before it is placed on a grid.
 */
public class GridContent {

        GridContentType type;
        Object value;
        int textColor = 0xFFFFFFFF; // Default text color
        String tooltip;
        Consumer<Void> onClick;
        Consumer<Boolean> onToggle;
        int bgColor = 0xA0335599; // Default background color
        String elementId;

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
            content.value = new Object[] { item, quantity };
            return content;
        }

        // Constructor for toggle
        public static GridContent toggle(String text, boolean initialState, Consumer<Boolean> onToggle,
                int enabledColor, int disabledColor) {
            GridContent content = new GridContent();
            content.type = GridContentType.TOGGLE;
            content.value = new Object[] { text, initialState };
            content.onToggle = onToggle;
            content.bgColor = initialState ? enabledColor : disabledColor;
            return content;
        }

        // Constructor for button
        public static GridContent button(String text, Consumer<Void> onClick, int bgColor) {
            return button(text, onClick, bgColor, null);
        }

        public static GridContent button(String text, Consumer<Void> onClick, int bgColor, String elementId) {
            GridContent content = new GridContent();
            content.type = GridContentType.BUTTON;
            content.value = text;
            content.onClick = onClick;
            content.bgColor = bgColor;
            content.elementId = elementId;
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
