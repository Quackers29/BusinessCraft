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
 * Mouse hit testing, scrolling, and drag for a grid.
 */
final class GridInput {
    private static final Logger LOGGER = LoggerFactory.getLogger(GridInput.class);
    private final GridState state;

    GridInput(GridState state) {
        this.state = state;
    }



    /**
     * Sets the current horizontal scroll offset
     * 
     * @param offset The new offset
     */
    void setHorizontalScrollOffset(int offset) {
        if (!state.horizontalScrollEnabled)
            return;

        // Clamp scroll offset
        if (offset < 0) {
            offset = 0;
        } else if (offset > state.maxHorizontalScrollOffset) {
            offset = state.maxHorizontalScrollOffset;
        }

        state.horizontalScrollOffset = offset;
    }



    /**
     * Scroll left by one column
     */
    void scrollLeft() {
        setHorizontalScrollOffset(state.horizontalScrollOffset - 1);
    }



    /**
     * Scroll right by one column
     */
    void scrollRight() {
        setHorizontalScrollOffset(state.horizontalScrollOffset + 1);
    }



    /**
     * Sets the current vertical scroll offset
     * 
     * @param offset The new offset
     */
    void setVerticalScrollOffset(int offset) {
        int oldOffset = state.verticalScrollOffset;

        // Clamp the offset to valid range
        if (offset < 0) {
            offset = 0;
        } else if (offset > state.maxVerticalScrollOffset) {
            offset = state.maxVerticalScrollOffset;
        }

        state.verticalScrollOffset = offset;

        // Debug if the offset changed
        if (oldOffset != state.verticalScrollOffset) {
            DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER, "Grid vertical offset changed: {} -> {}", oldOffset,
                    state.verticalScrollOffset);
        }
    }



    /**
     * Scroll up by one row
     */
    void scrollUp() {
        setVerticalScrollOffset(state.verticalScrollOffset - 1);
    }



    /**
     * Scroll down by one row
     */
    void scrollDown() {
        setVerticalScrollOffset(state.verticalScrollOffset + 1);
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
    boolean isMouseOverRow(int mouseX, int mouseY, int rowIndex, int columnsToCheck) {
        if (rowIndex < 0) {
            return false;
        }

        // Calculate actual row dimensions based on grid layout
        int rowHeight = state.customRowHeight != null ? state.customRowHeight : 20;
        int rowY = state.y + state.verticalMargin + (rowIndex * (rowHeight + state.verticalSpacing));

        // Apply vertical scroll offset if scrolling is enabled
        if (state.verticalScrollEnabled) {
            // Check if the row is actually visible
            if (rowIndex < state.verticalScrollOffset || rowIndex >= state.verticalScrollOffset + state.visibleRows) {
                return false;
            }
            rowY -= (state.verticalScrollOffset * (rowHeight + state.verticalSpacing));
        }

        // Calculate column area - typically first 1-2 columns for tooltips
        int columnStartX = state.x + state.horizontalMargin;
        int cellWidth = (state.width - 2 * state.horizontalMargin - (state.columns - 1) * state.horizontalSpacing) / state.columns;
        int columnWidth = (cellWidth * columnsToCheck) + ((columnsToCheck - 1) * state.horizontalSpacing);

        // Add small margins to prevent tooltip overlap between rows
        int rowMargin = 1; // 1px margin between rows to prevent bleed

        return mouseX >= columnStartX && mouseX <= columnStartX + columnWidth &&
                mouseY >= rowY + rowMargin && mouseY <= rowY + rowHeight - rowMargin;
    }



    /**
     * Gets the row index at the given mouse coordinates.
     * Accounts for scrolling and margins.
     *
     * @param mouseX Mouse X coordinate
     * @param mouseY Mouse Y coordinate
     * @return Row index (including scroll offset), or -1 if not over a row
     */
    int getClickedRow(int mouseX, int mouseY) {
        // Check if mouse is within grid bounds
        if (mouseX < state.x || mouseX > state.x + state.width || mouseY < state.y || mouseY > state.y + state.height) {
            return -1;
        }

        // Calculate row height
        int cellsHeight = state.height - (state.verticalMargin * 2);
        int effectiveRows = state.verticalScrollEnabled ? Math.min(state.visibleRows, state.rows) : state.rows;
        int rowHeight = state.customRowHeight != null ? state.customRowHeight
                : (cellsHeight - (state.verticalSpacing * (effectiveRows - 1))) / effectiveRows;

        // Calculate relative position within grid
        int relativeY = mouseY - (state.y + state.verticalMargin);

        // Calculate which row was clicked (accounting for spacing)
        int rowWithSpacing = rowHeight + state.verticalSpacing;
        int visibleRowIndex = relativeY / rowWithSpacing;

        // Adjust for scroll offset
        int actualRowIndex = state.verticalScrollEnabled ? visibleRowIndex + state.verticalScrollOffset : visibleRowIndex;

        // Validate row index
        if (actualRowIndex < 0 || actualRowIndex >= state.totalRows) {
            return -1;
        }

        return actualRowIndex;
    }



    /**
     * Handles mouse clicks on the grid elements
     * 
     * @return true if a click was handled
     */
    boolean mouseClicked(int mouseX, int mouseY, int button) {
        // Check for scroll button clicks first
        if (state.horizontalScrollEnabled && state.useScrollButtons) {
            // Check left button
            if (mouseX >= state.x + state.horizontalMargin && mouseX < state.x + state.horizontalMargin + state.scrollButtonSize &&
                    mouseY >= state.y + state.height / 2 - state.scrollButtonSize / 2 && mouseY < state.y + state.height / 2 + state.scrollButtonSize / 2) {
                if (state.horizontalScrollOffset > 0) {
                    scrollLeft();
                    return true;
                }
            }

            // Check right button
            if (mouseX >= state.x + state.width - state.horizontalMargin - state.scrollButtonSize && mouseX < state.x + state.width - state.horizontalMargin &&
                    mouseY >= state.y + state.height / 2 - state.scrollButtonSize / 2 && mouseY < state.y + state.height / 2 + state.scrollButtonSize / 2) {
                if (state.horizontalScrollOffset < state.maxHorizontalScrollOffset) {
                    scrollRight();
                    return true;
                }
            }
        }

        // Check for horizontal scrollbar drag
        if (state.horizontalScrollEnabled && !state.useScrollButtons) {
            int availableWidth = state.width - (2 * state.horizontalMargin) - ((state.columns - 1) * state.horizontalSpacing);

            if (mouseY >= state.y + state.height - state.verticalMargin - state.scrollBarHeight &&
                    mouseY < state.y + state.height - state.verticalMargin &&
                    mouseX >= state.x + state.horizontalMargin &&
                    mouseX < state.x + state.horizontalMargin + availableWidth) {

                // Calculate new scroll position based on click position
                float relativeX = (float) (mouseX - (state.x + state.horizontalMargin)) / availableWidth;
                setHorizontalScrollOffset((int) (relativeX * state.maxHorizontalScrollOffset));
                return true;
            }
        }

        // Check for vertical scrollbar drag - must use the same position as in
        // rendering
        if (state.verticalScrollEnabled && state.totalRows > state.visibleRows) {
            // Use the same scrollbar position as in renderVerticalScrollbar method
            int scrollBarX = state.x + state.width - state.scrollBarWidth - 2; // Match rendering position - inside grid
            int scrollTrackY = state.y + state.verticalMargin + 2; // Match rendering position
            int scrollTrackHeight = state.height - (state.verticalMargin * 2) - 4; // Match rendering calculation

            if (mouseX >= scrollBarX && mouseX < scrollBarX + state.scrollBarWidth &&
                    mouseY >= scrollTrackY && mouseY < scrollTrackY + scrollTrackHeight) {

                // Calculate new scroll position based on click position
                float relativeY = (float) (mouseY - scrollTrackY) / scrollTrackHeight;
                setVerticalScrollOffset((int) (relativeY * state.maxVerticalScrollOffset));
                return true;
            }
        }

        // Calculate cell dimensions for element click detection
        int initialWidth = state.width - (2 * state.horizontalMargin);
        int availableWidth = initialWidth;

        DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER,
                String.format(
                        "[GRID CALC] Grid at (%d,%d) size (%dx%d): width=%d, horizontalMargin=%d, initialWidth=%d",
                        state.x, state.y, state.width, state.height, state.width, state.horizontalMargin, initialWidth));

        if (state.visibleColumns > 0 && state.horizontalScrollEnabled) {
            availableWidth -= ((state.visibleColumns - 1) * state.horizontalSpacing);
            DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER,
                    String.format(
                            "[GRID CALC] Horizontal scroll spacing: visibleColumns=%d, horizontalSpacing=%d, deducted=%d",
                            state.visibleColumns, state.horizontalSpacing, (state.visibleColumns - 1) * state.horizontalSpacing));
        } else {
            availableWidth -= ((state.columns - 1) * state.horizontalSpacing);
            DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER,
                    String.format("[GRID CALC] Normal spacing: columns=%d, horizontalSpacing=%d, deducted=%d",
                            state.columns, state.horizontalSpacing, (state.columns - 1) * state.horizontalSpacing));
        }

        int availableHeight = state.height - (2 * state.verticalMargin);

        // Adjust height for horizontal scrollbar if needed
        if (state.horizontalScrollEnabled && !state.useScrollButtons) {
            availableHeight -= state.scrollBarHeight;
        }

        // Always reserve space for vertical scrollbar when scrolling is enabled (even
        // when not visible)
        if (state.verticalScrollEnabled) {
            int scrollbarReservation = state.scrollBarWidth + 6; // scrollbar width + 6px margin (2px left + 4px right)
            availableWidth -= scrollbarReservation;

            if (state.totalRows > state.visibleRows) {
                DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER,
                        String.format(
                                "[GRID CALC] Scrollbar visible, reservation: scrollBarWidth=%d, margins=6px, total=%d",
                                state.scrollBarWidth, scrollbarReservation));
            } else {
                DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER,
                        String.format(
                                "[GRID CALC] Scrollbar hidden but space reserved, reservation: scrollBarWidth=%d, margins=6px, total=%d",
                                state.scrollBarWidth, scrollbarReservation));
            }
        }

        // Calculate column width
        int columnCount = state.horizontalScrollEnabled ? state.visibleColumns : state.columns;
        int cellWidth = availableWidth / columnCount;
        int cellsWidth = availableWidth + (state.horizontalSpacing * Math.max(0, columnCount - 1));
        int[] columnWidths = state.columnPixelWidths(cellsWidth, columnCount);

        DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER,
                String.format(
                        "[GRID CALC] FINAL: availableWidth=%d, columnCount=%d, cellWidth=%d, verticalScrollEnabled=%b",
                        availableWidth, columnCount, cellWidth, state.verticalScrollEnabled));

        // Calculate row height - use custom height if set, otherwise calculate based on
        // available space
        int rowCount = state.verticalScrollEnabled ? state.visibleRows : state.rows;
        int cellHeight = state.customRowHeight != null ? state.customRowHeight
                : (availableHeight - ((rowCount - 1) * state.verticalSpacing)) / rowCount;

        // Check each element
        for (GridElement element : state.elements) {
            // Skip elements outside the visible range when using horizontal scrolling
            if (state.horizontalScrollEnabled) {
                if (element.column < state.horizontalScrollOffset ||
                        element.column >= state.horizontalScrollOffset + state.visibleColumns) {
                    continue;
                }
            }

            // Skip elements outside the visible range when using vertical scrolling
            if (state.verticalScrollEnabled) {
                if (element.row < state.verticalScrollOffset ||
                        element.row >= state.verticalScrollOffset + state.visibleRows) {
                    continue;
                }
            }

            // Calculate element position and size, adjusting for scroll offsets
            int elementX;
            int elementY;

            if (state.horizontalScrollEnabled) {
                int adjustedColumn = element.column - state.horizontalScrollOffset;
                int startX = state.x + state.horizontalMargin;
                if (state.useScrollButtons) {
                    startX += state.scrollButtonSize; // Account for left scroll button
                }
                elementX = startX + state.columnOffset(columnWidths, adjustedColumn);
            } else {
                elementX = state.x + state.horizontalMargin + state.columnOffset(columnWidths, element.column);
            }

            if (state.verticalScrollEnabled) {
                int adjustedRow = element.row - state.verticalScrollOffset;
                elementY = state.y + state.verticalMargin + (adjustedRow * (cellHeight + state.verticalSpacing));
            } else {
                elementY = state.y + state.verticalMargin + (element.row * (cellHeight + state.verticalSpacing));
            }

            int elementWidth = state.spanWidth(columnWidths,
                    state.horizontalScrollEnabled ? element.column - state.horizontalScrollOffset : element.column,
                    element.colSpan);
            int elementHeight = (cellHeight * element.rowSpan) + ((element.rowSpan - 1) * state.verticalSpacing);

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
     * 
     * @return true if handled
     */
    boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Always handle mouseScrolled regardless of position
        // This matches the Population tab's approach

        // For vertical scrolling
        if (state.verticalScrollEnabled && state.maxVerticalScrollOffset > 0) {
            // Apply scrolling directly based on delta sign (delta > 0 means scroll up)
            // Use the Population tab approach that works
            state.verticalScrollOffset -= (int) Math.signum(delta);

            // Clamp scroll position
            if (state.verticalScrollOffset < 0) {
                state.verticalScrollOffset = 0;
            }
            if (state.verticalScrollOffset > state.maxVerticalScrollOffset) {
                state.verticalScrollOffset = state.maxVerticalScrollOffset;
            }

            DebugConfig.debug(LOGGER, DebugConfig.UI_GRID_BUILDER, "UIGridBuilder scrolling: delta={}, offset={}",
                    delta, state.verticalScrollOffset);
            return true;
        }

        // For horizontal scrolling if vertical scrolling is not enabled
        if (state.horizontalScrollEnabled && state.maxHorizontalScrollOffset > 0) {
            // Apply scrolling directly based on delta sign
            state.horizontalScrollOffset -= (int) Math.signum(delta);

            // Clamp scroll position
            if (state.horizontalScrollOffset < 0) {
                state.horizontalScrollOffset = 0;
            }
            if (state.horizontalScrollOffset > state.maxHorizontalScrollOffset) {
                state.horizontalScrollOffset = state.maxHorizontalScrollOffset;
            }

            return true;
        }

        return false;
    }



    /**
     * Handle mouse dragging for scrolling
     * 
     * @return true if the drag was handled
     */
    boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Only handle left mouse button (button 0)
        if (button != 0) {
            return false;
        }

        // Handle vertical scrollbar dragging
        if (state.isDraggingVertical) {
            if (state.verticalScrollEnabled && state.maxVerticalScrollOffset > 0) {
                int scrollTrackHeight = state.height - (state.verticalMargin * 2);

                // Calculate drag amount in rows
                // First determine how much to scroll per pixel of drag
                float rowsPerPixel = (float) state.totalRows / scrollTrackHeight;

                // Calculate how many rows to scroll based on the drag distance
                int rowsToScroll = (int) Math.signum(mouseY - state.lastMouseY);

                // If the drag is significant enough, apply it
                if (Math.abs(mouseY - state.lastMouseY) > 5) {
                    state.verticalScrollOffset += rowsToScroll;

                    // Clamp scroll position
                    if (state.verticalScrollOffset < 0) {
                        state.verticalScrollOffset = 0;
                    }
                    if (state.verticalScrollOffset > state.maxVerticalScrollOffset) {
                        state.verticalScrollOffset = state.maxVerticalScrollOffset;
                    }

                    state.lastMouseY = mouseY;
                }

                return true;
            }
        }

        // Handle horizontal scrollbar dragging
        if (state.isDraggingHorizontal) {
            if (state.horizontalScrollEnabled && state.maxHorizontalScrollOffset > 0) {
                int scrollTrackWidth = state.width - (state.horizontalMargin * 2);

                // Calculate drag amount in columns
                float columnsPerPixel = (float) state.totalColumns / scrollTrackWidth;

                // Calculate how many columns to scroll based on the drag distance
                int columnsToScroll = (int) Math.signum(mouseX - state.lastMouseX);

                // If the drag is significant enough, apply it
                if (Math.abs(mouseX - state.lastMouseX) > 5) {
                    state.horizontalScrollOffset += columnsToScroll;

                    // Clamp scroll position
                    if (state.horizontalScrollOffset < 0) {
                        state.horizontalScrollOffset = 0;
                    }
                    if (state.horizontalScrollOffset > state.maxHorizontalScrollOffset) {
                        state.horizontalScrollOffset = state.maxHorizontalScrollOffset;
                    }

                    state.lastMouseX = mouseX;
                }

                return true;
            }
        }

        // Check if this is the start of a drag on the scrollbar area
        if (!state.isDraggingVertical && !state.isDraggingHorizontal) {
            // Check vertical scrollbar area
            if (state.verticalScrollEnabled && state.totalRows > state.visibleRows) {
                int scrollBarX = state.x + state.width - state.scrollBarWidth - 2; // Match rendering position - inside grid
                int scrollTrackY = state.y + state.verticalMargin + 2; // Match rendering position
                int scrollTrackHeight = state.height - (state.verticalMargin * 2) - 4; // Match rendering calculation

                if (mouseX >= scrollBarX && mouseX < scrollBarX + state.scrollBarWidth &&
                        mouseY >= scrollTrackY && mouseY < scrollTrackY + scrollTrackHeight) {
                    state.isDraggingVertical = true;
                    state.lastMouseY = mouseY;
                    return true;
                }
            }

            // Check horizontal scrollbar area
            if (state.horizontalScrollEnabled && !state.useScrollButtons) {
                int scrollTrackX = state.x + state.horizontalMargin;
                int scrollTrackY = state.y + state.height - state.verticalMargin - state.scrollBarHeight;
                int scrollTrackWidth = state.width - (state.horizontalMargin * 2);

                if (mouseX >= scrollTrackX && mouseX < scrollTrackX + scrollTrackWidth &&
                        mouseY >= scrollTrackY && mouseY < scrollTrackY + state.scrollBarHeight) {
                    state.isDraggingHorizontal = true;
                    state.lastMouseX = mouseX;
                    return true;
                }
            }

            // Check content area for dragging (direct dragging)
            if (state.verticalScrollEnabled &&
                    mouseX >= state.x && mouseX < state.x + state.width &&
                    mouseY >= state.y && mouseY < state.y + state.height) {
                state.isDraggingVertical = true;
                state.lastMouseY = mouseY;
                return true;
            }
        }

        return false;
    }



    /**
     * Handle mouse release
     */
    boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            boolean wasDragging = state.isDraggingVertical || state.isDraggingHorizontal;
            state.isDraggingVertical = false;
            state.isDraggingHorizontal = false;
            return wasDragging;
        }
        return false;
    }

}
