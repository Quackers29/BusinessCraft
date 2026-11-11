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
 * Paints grid cells, scrollbars, and tooltips.
 */
final class GridPainter {
    private static final Logger LOGGER = LoggerFactory.getLogger(GridPainter.class);
    private final GridState state;

    GridPainter(GridState state) {
        this.state = state;
    }



    /**
     * Renders the grid and its elements
     */
    void render(GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw grid background if enabled
        if (state.drawBackground) {
            graphics.fill(state.x, state.y, state.x + state.width, state.y + state.height, state.backgroundColor);
        }

        // Draw grid border if enabled
        if (state.drawBorder) {
            // Top, Bottom
            graphics.hLine(state.x, state.x + state.width - 1, state.y, state.borderColor);
            graphics.hLine(state.x, state.x + state.width - 1, state.y + state.height - 1, state.borderColor);
            // Left, Right
            graphics.vLine(state.x, state.y, state.y + state.height - 1, state.borderColor);
            graphics.vLine(state.x + state.width - 1, state.y, state.y + state.height - 1, state.borderColor);
        }

        // Calculate cell dimensions
        int cellsWidth = state.width - (state.horizontalMargin * 2);
        int cellsHeight = state.height - (state.verticalMargin * 2);

        // Always reserve space for vertical scrollbar when scrolling is enabled (even
        // when not visible)
        if (state.verticalScrollEnabled) {
            int scrollbarReservation = state.scrollBarWidth + 6; // scrollbar width + 6px margin (2px left + 4px right)
            cellsWidth -= scrollbarReservation;

            if (state.totalRows > state.visibleRows) {
                DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER,
                        String.format(
                                "[RENDER] Grid at (%d,%d): Scrollbar visible (totalRows=%d > visibleRows=%d), reserved %dpx, cellsWidth=%d",
                                state.x, state.y, state.totalRows, state.visibleRows, scrollbarReservation, cellsWidth));
            } else {
                DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER,
                        String.format(
                                "[RENDER] Grid at (%d,%d): Scrollbar hidden but space reserved (totalRows=%d <= visibleRows=%d), reserved %dpx, cellsWidth=%d",
                                state.x, state.y, state.totalRows, state.visibleRows, scrollbarReservation, cellsWidth));
            }
        }

        // Calculate individual cell dimensions
        int effectiveColumns = state.horizontalScrollEnabled ? Math.min(state.columns, state.visibleColumns) : state.columns;
        int effectiveRows = state.verticalScrollEnabled ? Math.min(state.visibleRows, state.rows) : state.rows;

        int cellWidth = (cellsWidth - (state.horizontalSpacing * (effectiveColumns - 1))) / effectiveColumns;
        int[] columnWidths = state.columnPixelWidths(cellsWidth, effectiveColumns);

        // Rate-limit debug logging - only log once per second
        long currentTime = System.currentTimeMillis();
        if (currentTime - state.lastGridRenderLogTime > 1000) {
            DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER,
                    String.format(
                            "[RENDER] Grid at (%d,%d): effectiveColumns=%d, cellsWidth=%d, cellWidth=%d, verticalScrollEnabled=%b",
                            state.x, state.y, effectiveColumns, cellsWidth, cellWidth, state.verticalScrollEnabled));
            state.lastGridRenderLogTime = currentTime;
        }
        int rowHeight = state.customRowHeight != null ? state.customRowHeight
                : (cellsHeight - (state.verticalSpacing * (effectiveRows - 1))) / effectiveRows;

        // Track if any element is currently hovered
        for (GridElement element : state.elements) {
            element.isHovered = false;
        }

        // Render all visible elements
        for (GridElement element : state.elements) {
            // Skip elements outside the visible range for horizontal scrolling
            if (state.horizontalScrollEnabled && (element.column < state.horizontalScrollOffset ||
                    element.column >= state.horizontalScrollOffset + state.visibleColumns)) {
                continue;
            }

            // Skip elements outside the visible range for vertical scrolling
            if (state.verticalScrollEnabled && (element.row < state.verticalScrollOffset ||
                    element.row >= state.verticalScrollOffset + state.visibleRows)) {
                continue;
            }

            // Calculate adjusted column and row positions for scrolling
            int adjustedColumn = state.horizontalScrollEnabled ? element.column - state.horizontalScrollOffset : element.column;
            int adjustedRow = state.verticalScrollEnabled ? element.row - state.verticalScrollOffset : element.row;

            // Calculate element position and size, accounting for spans
            int elementX = state.x + state.horizontalMargin + state.columnOffset(columnWidths, adjustedColumn);
            int elementY = state.y + state.verticalMargin + (adjustedRow * (rowHeight + state.verticalSpacing));
            int elementWidth = state.spanWidth(columnWidths, adjustedColumn, element.colSpan);
            int elementHeight = rowHeight * element.rowSpan + (element.rowSpan - 1) * state.verticalSpacing;

            // Check if element is hovered
            boolean hovered = mouseX >= elementX && mouseX < elementX + elementWidth &&
                    mouseY >= elementY && mouseY < elementY + elementHeight;

            element.isHovered = hovered;
            if (state.forcedTooltipRow >= 0 && element.row == state.forcedTooltipRow
                    && element.tooltip != null && !element.tooltip.isEmpty()) {
                element.isHovered = true;
            }
            if (element.row == 0 && element.column == 0) {
                var pose = graphics.pose().last().pose();
                state.anchorX = Math.round(pose.m30()) + elementX + elementWidth / 2;
                state.anchorY = Math.round(pose.m31()) + elementY + elementHeight / 2;
            }

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
                case STATUS_ICON:
                    renderStatusIcon(graphics, element, elementX, elementY, elementWidth, elementHeight);
                    break;
            }
        }

        // Render scrollbars
        if (state.horizontalScrollEnabled) {
            renderHorizontalScrollbar(graphics, mouseX, mouseY);
        }

        if (state.verticalScrollEnabled) {
            renderVerticalScrollbar(graphics, mouseX, mouseY);
        }

        // Render tooltips for hovered elements
        // Render tooltips for hovered elements
        for (GridElement element : state.elements) {
            boolean hasTooltip = element.tooltip != null && !element.tooltip.isEmpty();
            boolean showTooltip = element.isHovered && (element.isTruncated || hasTooltip);

            if (showTooltip) {
                // Use Minecraft's tooltip rendering (converted to Component)
                com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
                if (clientHelper != null) {
                    Object fontObj = clientHelper.getFont();
                    if (fontObj instanceof net.minecraft.client.gui.Font font) {
                        String tooltipText = element.isTruncated ? element.text
                                : (hasTooltip ? element.tooltip : element.text);
                        String[] lines = tooltipText.split("\n");
                        List<Component> tooltipComponents = new ArrayList<>();
                        for (String line : lines) {
                            tooltipComponents.add(Component.literal(line));
                        }
                        if (state.clampTooltip) {
                            renderClampedTooltip(graphics, font, tooltipComponents, mouseX, mouseY);
                        } else {
                            graphics.renderComponentTooltip(font, tooltipComponents, mouseX, mouseY);
                        }
                        break; // Only show one tooltip at a time
                    }
                }
            }
        }
    }



    /**
     * Renders a button element
     */
    void renderButton(GuiGraphics graphics, GridElement element,
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
        graphics.hLine(x, x + width - 1, y, state.borderColor);
        graphics.hLine(x, x + width - 1, y + height - 1, state.borderColor);
        graphics.vLine(x, y, y + height - 1, state.borderColor);
        graphics.vLine(x + width - 1, y, y + height - 1, state.borderColor);

        // Draw centered text - center vertically in the whole button area
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper != null) {
            Object fontObj = clientHelper.getFont();
            if (fontObj instanceof net.minecraft.client.gui.Font font) {
                int textX = x + width / 2 - font.width(element.text) / 2;
                int textY = y + (height - font.lineHeight) / 2; // Centered within the actual height
                graphics.drawString(font, element.text, textX, textY, UiTheme.TEXT, false);
            }
        }
    }



    /**
     * Renders a label element
     */
    void renderLabel(GuiGraphics graphics, GridElement element,
            int x, int y, int width, int height) {
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper == null)
            return;

        Object fontObj = clientHelper.getFont();
        if (!(fontObj instanceof net.minecraft.client.gui.Font font))
            return;

        // Truncation logic
        String displayText = element.text;
        int padding = 4; // 2px on each side
        int availableWidth = Math.max(0, width - padding);

        element.isTruncated = false;
        if (font.width(displayText) > availableWidth) {
            element.isTruncated = true;
            String ellipsis = "\u2026";
            int ellipsisWidth = font.width(ellipsis);
            if (availableWidth > ellipsisWidth) {
                displayText = font.plainSubstrByWidth(displayText, availableWidth - ellipsisWidth) + ellipsis;
            } else {
                // Too small for even ellipsis, just cut
                displayText = font.plainSubstrByWidth(displayText, availableWidth);
            }
        }

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
        graphics.drawString(font, displayText, textX, textY, element.textColor, false);
    }



    /**
     * Renders a large centered status indicator
     */
    void renderStatusIndicator(GuiGraphics graphics, GridElement element,
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
    void drawFilledCircle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
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
    void drawCircleOutline(GuiGraphics graphics, int centerX, int centerY, int radius, int color,
            int thickness) {
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
    void renderToggle(GuiGraphics graphics, GridElement element,
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
        graphics.hLine(x, x + width - 1, y, state.borderColor);
        graphics.hLine(x, x + width - 1, y + height - 1, state.borderColor);
        graphics.vLine(x, y, y + height - 1, state.borderColor);
        graphics.vLine(x + width - 1, y, y + height - 1, state.borderColor);

        // Draw indicator circle
        int indicatorSize = height - 6;
        int indicatorY = y + 3;
        int indicatorX = element.toggled ? (x + width - indicatorSize - 3) : // Right side when on
                (x + 3); // Left side when off

        // Draw indicator with contrasting color
        graphics.fill(indicatorX, indicatorY, indicatorX + indicatorSize, indicatorY + indicatorSize, 0xFFFFFFFF);

        // Draw centered text
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper != null) {
            Object fontObj = clientHelper.getFont();
            if (fontObj instanceof net.minecraft.client.gui.Font font) {
                int textX = x + width / 2 - font.width(element.text) / 2;
                int textY = y + (height - 8) / 2;
                graphics.drawString(font, element.text, textX, textY, UiTheme.TEXT, false);
            }
        }
    }



    /**
     * Formats a number for display, shortening large numbers with suffixes (K, M,
     * B)
     * 
     * @param number The number to format
     * @return Formatted string representation
     */
    String formatNumber(int number) {
        if (number < 1000) {
            return String.valueOf(number);
        } else if (number < 1000000) {
            // 1000-999999 -> #.#K (1.2K, 10.1K, 500.5K)
            return String.format("%.1fK", number / 1000.0).replace(".0K", "K");
        } else if (number < 1000000000) {
            // 1000000-999999999 -> #.#M (1.2M, 10.5M)
            return String.format("%.1fM", number / 1000000.0).replace(".0M", "M");
        } else {
            // 1000000000+ -> #.#B (1.2B, 9.9B)
            return String.format("%.1fB", number / 1000000000.0).replace(".0B", "B");
        }
    }



    /**
     * Renders an item element
     */
    void renderItem(GuiGraphics graphics, GridElement element,
            int x, int y, int width, int height) {
        // Draw a background for the item (lighter when hovered)
        int bgColor = element.isHovered ? 0x80444444 : 0x60222222;
        graphics.fill(x, y, x + width, y + height, bgColor);

        if (element.item != null) {
            com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
            if (clientHelper == null)
                return;

            Object fontObj = clientHelper.getFont();
            if (!(fontObj instanceof net.minecraft.client.gui.Font font))
                return;

            // Calculate the item position - align to the left with some padding
            int itemSize = 16; // Standard Minecraft item size
            int itemY = y + (height - itemSize) / 2; // Center vertically
            int itemX = x + 5; // Left align with padding

            // Render the item - use the stored ItemStack if available, otherwise create one
            net.minecraft.world.item.ItemStack stack = element.itemStack != null ? element.itemStack
                    : new net.minecraft.world.item.ItemStack(element.item, element.quantity);

            // Render the item
            graphics.renderItem(stack, itemX, itemY);

            // If we have a quantity, show it with an 'x' symbol
            if (element.showQuantity) {
                // Format the quantity
                String quantityText = formatNumber(element.quantity);

                // Calculate the multiplier (x) position - center in remaining space
                String multiplier = ""; // Removed "x"
                int multiplierWidth = 0;
                int contentWidth = width - 10 - itemSize; // Total width minus item and padding
                int multiplierX = itemX + itemSize + 5
                        + (contentWidth - multiplierWidth - font.width(quantityText)) / 2;
                int textY = y + (height - font.lineHeight) / 2; // Center text vertically

                // Draw the multiplier
                graphics.drawString(
                        font,
                        multiplier,
                        multiplierX,
                        textY,
                        UiTheme.MUTED,
                        false
                );

                // Draw the quantity text right-aligned
                graphics.drawString(
                        font,
                        quantityText,
                        x + width - font.width(quantityText) - 5, // Right-aligned with padding
                        textY,
                        UiTheme.TEXT,
                        false
                );
            }
        }
    }



    /**
     * Renders multiple overlapping items for visual display
     */
    void renderMultiItem(GuiGraphics graphics, GridElement element,
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
            com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
            if (clientHelper != null) {
                Object fontObj = clientHelper.getFont();
                if (fontObj instanceof net.minecraft.client.gui.Font font) {
                    String plusText = "+" + (element.multiItems.size() - 4);
                    int textWidth = font.width(plusText);
                    int textX = x + width - textWidth - 5;
                    int textY = y + (height - font.lineHeight) / 2;

                    graphics.drawString(font, plusText, textX, textY, UiTheme.TEXT, false);
                }
            }
        }
    }



    /**
     * Handles mouse clicks on the grid elements
     * 
     * @return true if a click was handled
     */
    void renderStatusIcon(GuiGraphics graphics, GridElement element, int x, int y, int width, int height) {
        if (element.statusSymbol == null)
            return;

        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper == null)
            return;

        Object fontObj = clientHelper.getFont();
        if (!(fontObj instanceof net.minecraft.client.gui.Font font))
            return;

        int centerX = x + width / 2;
        int centerY = y + height / 2;

        // Use Minecraft font for status icons
        // UNLOCKED/CHECKMARK = Green Check
        // LOCKED/CROSS = Red Cross/Lock

        String symbol = "";
        int color = 0xFFFFFFFF;

        switch (element.statusSymbol) {
            case UNLOCKED:
            case CHECKMARK:
                symbol = "✔"; // Checkmark
                color = UiTheme.ON_BG;
                break;
            case LOCKED:
                symbol = "🔒"; // Padlock
                color = UiTheme.OFF_BG;
                break;
            case CROSS:
                symbol = "✖"; // Cross
                color = UiTheme.OFF_BG;
                break;
            case INFO:
                symbol = "ℹ"; // Info
                color = UiTheme.AQUA;
                break;
            case WARNING:
                symbol = "⚠"; // Warning
                color = UiTheme.GOLD;
                break;
        }

        if (!symbol.isEmpty()) {
            graphics.drawCenteredString(font, symbol, centerX, centerY - 4, color);
        }
    }



    /**
     * Check if the mouse is over the vertical scrollbar thumb
     */
    boolean isMouseOverVerticalScrollThumb(int mouseX, int mouseY) {
        if (!state.verticalScrollEnabled || state.totalRows <= state.visibleRows)
            return false;

        // Calculate scroll thumb dimensions and position - must match the rendering
        // position exactly
        int scrollTrackX = state.x + state.width - state.scrollBarWidth - 4; // Match the render position - inside grid with 4px
                                                                     // margin
        int scrollTrackY = state.y + state.verticalMargin + 2; // Match the render position
        int scrollTrackHeight = state.height - (state.verticalMargin * 2) - 4; // Match the render calculation

        // Calculate thumb size and position
        float thumbRatio = Math.min(1.0f, (float) state.visibleRows / state.totalRows);
        int thumbHeight = Math.max(12, Math.round(thumbRatio * scrollTrackHeight)); // Match render minimum

        // Calculate thumb position
        float scrollProgress = (state.totalRows <= state.visibleRows) ? 0
                : (float) state.verticalScrollOffset / (state.totalRows - state.visibleRows);
        int thumbY = scrollTrackY + Math.round((scrollTrackHeight - thumbHeight) * scrollProgress);

        // Check if mouse is over thumb
        return mouseX >= scrollTrackX && mouseX <= scrollTrackX + state.scrollBarWidth &&
                mouseY >= thumbY && mouseY <= thumbY + thumbHeight;
    }



    /**
     * Renders the horizontal scrollbar
     */
    void renderHorizontalScrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!state.horizontalScrollEnabled)
            return;

        // Calculate scroll area dimensions
        int scrollTrackX = state.x + state.horizontalMargin;
        int scrollTrackY = state.y + state.height - state.verticalMargin - state.scrollBarHeight;
        int scrollTrackWidth = state.width - (state.horizontalMargin * 2);

        // Draw scrollbar background
        graphics.fill(scrollTrackX, scrollTrackY,
                scrollTrackX + scrollTrackWidth, scrollTrackY + state.scrollBarHeight,
                0x40000000);

        // Calculate thumb width and position
        float thumbRatio = (float) state.visibleColumns / (float) state.totalColumns;
        int thumbWidth = Math.max(20, (int) (scrollTrackWidth * thumbRatio));

        // Calculate thumb position based on current scroll offset
        float scrollProgress = (state.totalColumns <= state.visibleColumns) ? 0
                : (float) state.horizontalScrollOffset / (state.totalColumns - state.visibleColumns);
        int thumbX = scrollTrackX + Math.round((scrollTrackWidth - thumbWidth) * scrollProgress);

        // Draw the thumb
        boolean isThumbHovered = isMouseOverHorizontalScrollThumb(mouseX, mouseY);
        int thumbColor = isThumbHovered ? 0xCCAAAAAA : 0x80AAAAAA;
        graphics.fill(thumbX, scrollTrackY,
                thumbX + thumbWidth, scrollTrackY + state.scrollBarHeight,
                thumbColor);
    }



    /**
     * Renders the vertical scrollbar
     */
    void renderVerticalScrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!state.verticalScrollEnabled || state.totalRows <= state.visibleRows)
            return;

        // Position scrollbar at the very right edge INSIDE the grid bounds
        int scrollTrackX = state.x + state.width - state.scrollBarWidth - 4; // 4px from right edge
        int scrollTrackY = state.y + state.verticalMargin + 2; // 2px from top margin
        int scrollTrackHeight = state.height - (state.verticalMargin * 2) - 4; // 2px top and bottom padding
        if (scrollTrackY + scrollTrackHeight > state.y + state.height - state.verticalMargin) {
            scrollTrackHeight = state.y + state.height - state.verticalMargin - scrollTrackY;
        }

        // Draw scrollbar background
        graphics.fill(scrollTrackX, scrollTrackY,
                scrollTrackX + state.scrollBarWidth, scrollTrackY + scrollTrackHeight,
                0x40000000); // Original transparency

        // Calculate thumb size and position
        float thumbRatio = Math.min(1.0f, (float) state.visibleRows / state.totalRows);
        int thumbHeight = Math.max(12, Math.round(thumbRatio * scrollTrackHeight)); // Minimum 12px thumb

        // Calculate thumb position based on current scroll offset
        float scrollProgress = (state.totalRows <= state.visibleRows) ? 0
                : (float) state.verticalScrollOffset / (state.totalRows - state.visibleRows);
        int thumbY = scrollTrackY + Math.round((scrollTrackHeight - thumbHeight) * scrollProgress);

        // Draw the thumb (original simple style)
        boolean isThumbHovered = isMouseOverVerticalScrollThumb(mouseX, mouseY);
        int thumbColor = isThumbHovered ? 0xCCAAAAAA : 0x80AAAAAA;
        graphics.fill(scrollTrackX, thumbY,
                scrollTrackX + state.scrollBarWidth, thumbY + thumbHeight,
                thumbColor);
    }



    /**
     * Checks if the mouse is over the horizontal scrollbar thumb
     */
    boolean isMouseOverHorizontalScrollThumb(int mouseX, int mouseY) {
        if (!state.horizontalScrollEnabled)
            return false;

        // Calculate scroll thumb dimensions and position
        int scrollTrackX = state.x + state.horizontalMargin;
        int scrollTrackY = state.y + state.height - state.verticalMargin - state.scrollBarHeight;
        int scrollTrackWidth = state.width - (state.horizontalMargin * 2);

        // Calculate thumb size and position
        float thumbRatio = (float) state.visibleColumns / (float) state.totalColumns;
        int thumbWidth = Math.max(20, (int) (scrollTrackWidth * thumbRatio));

        // Calculate thumb position
        float scrollProgress = (state.totalColumns <= state.visibleColumns) ? 0
                : (float) state.horizontalScrollOffset / (state.totalColumns - state.visibleColumns);
        int thumbX = scrollTrackX + Math.round((scrollTrackWidth - thumbWidth) * scrollProgress);

        // Check if mouse is over thumb
        return mouseX >= thumbX && mouseX < thumbX + thumbWidth &&
                mouseY >= scrollTrackY && mouseY < scrollTrackY + state.scrollBarHeight;
    }


    private void renderClampedTooltip(GuiGraphics graphics, Font font, java.util.List<Component> lines,
            int mouseX, int mouseY) {
        int innerLeft = state.x + 2;
        int innerTop = state.y + 2;
        int innerRight = state.x + Math.max(2, state.width) - 2;
        int innerBottom = state.y + Math.max(2, state.height) - 2;
        int maxText = Math.max(8, (innerRight - innerLeft) - 8);
        java.util.List<Component> wrapped = new java.util.ArrayList<>();
        for (Component line : lines) {
            String raw = line.getString();
            if (raw.isEmpty()) {
                wrapped.add(Component.empty());
                continue;
            }
            for (String part : UiTextWrap.wrap(raw, maxText, font::width)) {
                wrapped.add(Component.literal(part).withStyle(line.getStyle()));
            }
        }
        if (wrapped.isEmpty()) {
            return;
        }
        int textW = 0;
        for (Component line : wrapped) {
            textW = Math.max(textW, font.width(line));
        }
        int chrome = 3;
        int outerW = textW + 8 + chrome * 2;
        int outerH = wrapped.size() * 10 + 6 + chrome * 2;
        int preferX = mouseX + 12;
        int preferY = mouseY - 12;
        UiTooltip.Box box = UiTooltip.placeIn(innerLeft, innerTop, innerRight, innerBottom,
                preferX, preferY, outerW, outerH);
        int textX = box.x() + chrome;
        int textY = box.y() + chrome + 1;
        var pose = graphics.pose().last().pose();
        int originX = Math.round(pose.m30());
        int originY = Math.round(pose.m31());
        graphics.pose().pushPose();
        graphics.pose().translate(0.0, 0.0, 400.0);
        graphics.enableScissor(originX + state.x, originY + state.y,
                originX + state.x + state.width, originY + state.y + state.height);
        int x = box.x();
        int y = box.y();
        int bg = 0xF0100010;
        graphics.fill(x + 1, y + 1, x + outerW - 1, y + outerH - 1, bg);
        graphics.fill(x, y + 1, x + 1, y + outerH - 1, 0x505000FF);
        graphics.fill(x + outerW - 1, y + 1, x + outerW, y + outerH - 1, 0x5028007F);
        graphics.fill(x + 1, y, x + outerW - 1, y + 1, 0x505000FF);
        graphics.fill(x + 1, y + outerH - 1, x + outerW - 1, y + outerH, 0x5028007F);
        for (int i = 0; i < wrapped.size(); i++) {
            graphics.drawString(font, wrapped.get(i), textX, textY + i * 10, 0xFFFFFFFF, false);
        }
        graphics.disableScissor();
        graphics.pose().popPose();
    }
}
