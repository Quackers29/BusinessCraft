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
 * Geometry and mutable layout state extracted from UIGridBuilder. Callers still go through UIGridBuilder.
 */
final class GridState {
    int x, y, width, height;
    int rows, columns;
    int horizontalSpacing, verticalSpacing;
    int horizontalMargin, verticalMargin;
    final java.util.List<GridElement> elements = new java.util.ArrayList<>();
    int backgroundColor = 0x80222222;
    int borderColor = 0xA0AAAAAA;
    boolean drawBackground = true;
    boolean drawBorder = true;
    boolean horizontalScrollEnabled = false;
    int horizontalScrollOffset = 0;
    int maxHorizontalScrollOffset = 0;
    int scrollBarHeight = 10;
    boolean horizontalScrolling = false;
    int visibleColumns = 0;
    int totalColumns = 0;
    int scrollButtonSize = 20;
    boolean useScrollButtons = false;
    boolean verticalScrollEnabled = false;
    int verticalScrollOffset = 0;
    int maxVerticalScrollOffset = 0;
    int scrollBarWidth = UiTheme.SCROLLBAR;
    boolean verticalScrolling = false;
    int visibleRows = 0;
    int totalRows = 0;
    boolean autoVisibleRows = false;
    Integer customRowHeight = 14;
    float[] columnWeights;
    boolean isDraggingVertical = false;
    boolean isDraggingHorizontal = false;
    double lastMouseY = 0;
    double lastMouseX = 0;
    long lastGridCreateLogTime = 0;
    long lastGridRenderLogTime = 0;
    /** Showcase pins a row tooltip. -1 uses the real cursor. */
    int forcedTooltipRow = -1;
    /** Screen-space centre of the first cell, for the showcase cursor. */
    int anchorX = Integer.MIN_VALUE;
    int anchorY = Integer.MIN_VALUE;



    /**
     * Pixel widths for one row. Equal columns use the same integer division as
     * before. Weighted columns give the remainder to the last column.
     */
    int[] columnPixelWidths(int cellsWidth, int columnCount) {
        int count = Math.max(1, columnCount);
        int gaps = horizontalSpacing * Math.max(0, count - 1);
        int usable = Math.max(0, cellsWidth - gaps);
        int[] widths = new int[count];
        boolean weighted = columnWeights != null && columnWeights.length == count && !horizontalScrollEnabled;
        if (!weighted) {
            int cell = count == 0 ? usable : usable / count;
            java.util.Arrays.fill(widths, cell);
            return widths;
        }
        float sum = 0f;
        for (float weight : columnWeights) {
            sum += Math.max(0f, weight);
        }
        if (sum <= 0f) {
            java.util.Arrays.fill(widths, usable / count);
            return widths;
        }
        int assigned = 0;
        for (int i = 0; i < count; i++) {
            if (i == count - 1) {
                widths[i] = Math.max(0, usable - assigned);
            } else {
                widths[i] = (int) Math.floor(usable * (Math.max(0f, columnWeights[i]) / sum));
                assigned += widths[i];
            }
        }
        return widths;
    }



    int columnOffset(int[] widths, int column) {
        int offset = 0;
        int limit = Math.max(0, Math.min(column, widths.length));
        for (int i = 0; i < limit; i++) {
            offset += widths[i] + horizontalSpacing;
        }
        return offset;
    }



    int spanWidth(int[] widths, int column, int span) {
        if (widths.length == 0 || column >= widths.length || column < 0) {
            return 0;
        }
        int width = 0;
        int count = Math.max(1, span);
        for (int i = 0; i < count && column + i < widths.length; i++) {
            if (i > 0) {
                width += horizontalSpacing;
            }
            width += widths[column + i];
        }
        return width;
    }



    /**
     * Gets the effective row height including spacing
     * 
     * @return The calculated row height
     */
    int getEffectiveRowHeight() {
        int baseRowHeight = customRowHeight != null ? customRowHeight : 16;
        return baseRowHeight + verticalSpacing;
    }

}
