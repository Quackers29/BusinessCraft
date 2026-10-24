package com.quackers29.businesscraft.ui.components.containers;

import com.quackers29.businesscraft.ui.components.basic.BCComponent;
import com.quackers29.businesscraft.ui.builders.UIGridBuilder;
import com.quackers29.businesscraft.ui.templates.TownInterfaceTheme;
import com.quackers29.businesscraft.ui.util.UiTextWrap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Standardized tab content component that provides unified rendering patterns
 * while maintaining the visual appearance of existing tabs.
 */
public class StandardTabContent extends BCComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(StandardTabContent.class);

    // Theme constants
    private static final int TEXT_COLOR = TownInterfaceTheme.TEXT_COLOR;
    private static final int TEXT_HIGHLIGHT = TownInterfaceTheme.TEXT_HIGHLIGHT;
    private static final int BACKGROUND_COLOR = TownInterfaceTheme.BACKGROUND_COLOR;
    private static final int BORDER_COLOR = TownInterfaceTheme.BORDER_COLOR;

    // Content types
    public enum ContentType {
        LABEL_VALUE_GRID, // For Overview tab - animated label-value pairs
        ITEM_LIST, // For Resources tab - scrollable item list
        CUSTOM_LIST, // For Population tab - custom rendered list
        BUTTON_GRID, // For Settings tab - label-button pairs
        PRODUCTION_LIST // For Production tab
    }

    private final ContentType contentType;
    private final String title;
    private UIGridBuilder grid;
    /** Pixel scroll for the overview label list. Long values wrap, so the grid is not used. */
    private int labelScroll;
    private int labelContentHeight;

    // Data suppliers for different content types
    private Supplier<Map<String, String>> labelValueSupplier;
    private Supplier<Map<String, String>> labelValueTooltipSupplier;
    private Supplier<Map<Item, Long>> itemListSupplier;
    private Supplier<Map<Item, String>> itemTooltipSupplier;
    private Supplier<Object[]> customDataSupplier;
    private Supplier<Map<String, Object[]>> buttonGridSupplier;

    // Custom click handler for button grids (for special cases like right-click)
    private java.util.function.Function<Integer, Boolean> customClickHandler;

    /**
     * Creates a new standardized tab content component.
     */
    public StandardTabContent(int width, int height, ContentType contentType, String title) {
        super(width, height);
        this.contentType = contentType;
        this.title = title;
    }

    /**
     * Configure for label-value grid (Overview tab style)
     */
    public StandardTabContent withLabelValueData(Supplier<Map<String, String>> dataSupplier) {
        this.labelValueSupplier = dataSupplier;
        return this;
    }

    /**
     * Configure tooltips for label-value grid
     */
    public StandardTabContent withLabelValueTooltipData(Supplier<Map<String, String>> dataSupplier) {
        this.labelValueTooltipSupplier = dataSupplier;
        return this;
    }

    /**
     * Configure for item list (Resources tab style)
     */
    public StandardTabContent withItemListData(Supplier<Map<Item, Long>> dataSupplier) {
        this.itemListSupplier = dataSupplier;
        return this;
    }

    /**
     * Configure tooltips for item list
     */
    public StandardTabContent withItemTooltipData(Supplier<Map<Item, String>> dataSupplier) {
        this.itemTooltipSupplier = dataSupplier;
        return this;
    }

    private Supplier<Map<Item, String>> itemNamesSupplier;

    /**
     * Configure name overrides for item list
     */
    public StandardTabContent withItemNameData(Supplier<Map<Item, String>> dataSupplier) {
        this.itemNamesSupplier = dataSupplier;
        return this;
    }

    /**
     * Configure for custom list (Population tab style) or Production list
     */
    public StandardTabContent withCustomData(Supplier<Object[]> dataSupplier) {
        this.customDataSupplier = dataSupplier;
        return this;
    }

    /**
     * Configure for button grid (Settings tab style)
     */
    public StandardTabContent withButtonGridData(Supplier<Map<String, Object[]>> dataSupplier) {
        this.buttonGridSupplier = dataSupplier;
        return this;
    }

    /**
     * Configure custom click handler for button grids (for special cases like
     * right-click)
     */
    public StandardTabContent withCustomClickHandler(java.util.function.Function<Integer, Boolean> clickHandler) {
        this.customClickHandler = clickHandler;
        return this;
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        switch (contentType) {
            case LABEL_VALUE_GRID:
                renderLabelValueGrid(guiGraphics, mouseX, mouseY);
                break;
            case ITEM_LIST:
                renderItemList(guiGraphics, mouseX, mouseY);
                break;
            case CUSTOM_LIST:
                renderCustomList(guiGraphics, mouseX, mouseY);
                break;
            case BUTTON_GRID:
                renderButtonGrid(guiGraphics, mouseX, mouseY);
                break;
            case PRODUCTION_LIST:
                renderProductionList(guiGraphics, mouseX, mouseY);
                break;
        }
    }

    /**
     * Overview rows. A value that fits beside its label stays on one line.
     * Anything longer wraps under the label across the full content width.
     */
    private void renderLabelValueGrid(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (labelValueSupplier == null) {
            return;
        }
        Map<String, String> data = labelValueSupplier.get();
        Map<String, String> tooltips = labelValueTooltipSupplier != null ? labelValueTooltipSupplier.get() : null;
        Font font = Minecraft.getInstance().font;

        guiGraphics.fill(x, y, x + width, y + height, BACKGROUND_COLOR);
        guiGraphics.hLine(x, x + width - 1, y, BORDER_COLOR);
        guiGraphics.hLine(x, x + width - 1, y + height - 1, BORDER_COLOR);
        guiGraphics.vLine(x, y, y + height - 1, BORDER_COLOR);
        guiGraphics.vLine(x + width - 1, y, y + height - 1, BORDER_COLOR);

        int pad = 8;
        int innerW = Math.max(8, width - pad * 2 - 6);
        int viewTop = y + 6;
        int viewH = Math.max(font.lineHeight, height - 12);
        int labelCap = (int) (innerW * 0.46f);

        record Row(String key, String value, List<String> lines, boolean beside, int height, String tooltip) {}
        List<Row> rows = new ArrayList<>();
        int contentHeight = 0;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey();
            String value = entry.getValue() == null ? "" : entry.getValue();
            String tooltip = tooltips != null ? tooltips.get(entry.getKey()) : null;
            int valueRoom = Math.max(8, innerW - Math.min(font.width(key) + 6, labelCap) - 4);
            boolean beside = font.width(key) <= labelCap && font.width(value) <= valueRoom;
            List<String> lines = beside
                    ? List.of(value)
                    : UiTextWrap.wrap(value, innerW, text -> font.width(text));
            int rowHeight = beside
                    ? font.lineHeight + 4
                    : font.lineHeight + 2 + lines.size() * (font.lineHeight + 1) + 3;
            rows.add(new Row(key, value, lines, beside, rowHeight, tooltip));
            contentHeight += rowHeight;
        }
        labelContentHeight = contentHeight;
        int maxScroll = Math.max(0, contentHeight - viewH);
        labelScroll = Math.max(0, Math.min(labelScroll, maxScroll));

        // The tab panel translates into the content area. Scissor is screen space
        // and does not follow that pose, so add the current translation.
        var pose = guiGraphics.pose().last().pose();
        int originX = Math.round(pose.m30());
        int originY = Math.round(pose.m31());
        guiGraphics.enableScissor(originX + x + 2, originY + viewTop,
                originX + x + width - 2, originY + viewTop + viewH);
        int cursor = viewTop - labelScroll;
        String hovered = null;
        for (Row row : rows) {
            int rowBottom = cursor + row.height();
            boolean over = mouseX >= x && mouseX < x + width && mouseY >= cursor && mouseY < rowBottom;
            if (over && row.tooltip() != null) {
                hovered = row.tooltip();
            }
            if (rowBottom > viewTop && cursor < viewTop + viewH) {
                guiGraphics.drawString(font, row.key(), x + pad, cursor, TEXT_COLOR, false);
                if (row.beside()) {
                    int valueX = x + pad + Math.min(font.width(row.key()) + 6, labelCap);
                    guiGraphics.drawString(font, row.value(), valueX, cursor, TEXT_HIGHLIGHT, false);
                } else {
                    int lineY = cursor + font.lineHeight + 2;
                    for (String line : row.lines()) {
                        guiGraphics.drawString(font, line, x + pad, lineY, TEXT_HIGHLIGHT, false);
                        lineY += font.lineHeight + 1;
                    }
                }
            }
            cursor += row.height();
        }
        guiGraphics.disableScissor();

        if (maxScroll > 0) {
            int trackX = x + width - 6;
            int thumbH = Math.max(8, viewH * viewH / contentHeight);
            int thumbY = viewTop + (int) ((viewH - thumbH) * (labelScroll / (float) maxScroll));
            guiGraphics.fill(trackX, viewTop, trackX + 3, viewTop + viewH, 0x40FFFFFF);
            guiGraphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, 0xD0FFFFFF);
        }
        if (hovered != null) {
            guiGraphics.renderTooltip(font, Component.literal(hovered), mouseX, mouseY);
        }
    }

    /**
     * Render scrollable item list (Resources tab style)
     */
    private void renderItemList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (itemListSupplier != null) {
            Map<Item, Long> items = itemListSupplier.get();
            Map<Item, String> tooltips = itemTooltipSupplier != null ? itemTooltipSupplier.get() : null;
            Map<Item, String> names = itemNamesSupplier != null ? itemNamesSupplier.get() : null;

            // Create the grid only once to preserve scroll state
            if (grid == null) {
                DebugConfig.debug(LOGGER, DebugConfig.UI_STANDARD_TAB_CONTENT,
                        "Creating new UIGridBuilder for item list with {} items", items.size());
                grid = UIGridBuilder.create(x, y, width, height, 2)
                        .withBackgroundColor(BACKGROUND_COLOR)
                        .withBorderColor(BORDER_COLOR)
                        .withMargins(15, 10)
                        .withSpacing(15, 10)
                        .withRowHeight(14)
                        .drawBorder(true);

                // This method automatically enables scrolling if needed
                Map<Item, Integer> intItems = items.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().intValue()
                        ));
                grid.withItemQuantityPairs(intItems, tooltips, names, TEXT_HIGHLIGHT);
                DebugConfig.debug(LOGGER, DebugConfig.UI_STANDARD_TAB_CONTENT,
                        "Grid created with scrolling enabled: {}", items.size() > 4);
            } else {
                // Update existing grid with new data while preserving scroll state
                Map<Item, Integer> intItems = items.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().intValue()
                        ));
                grid.updateItemQuantityPairs(intItems, tooltips, names, TEXT_HIGHLIGHT);
            }

            // Render the grid (preserves scroll state)
            grid.render(guiGraphics, mouseX, mouseY);
        }
    }

    /**
     * Render custom list (Population tab style) - now uses UIGridBuilder's modular
     * approach
     */
    private void renderCustomList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (customDataSupplier != null) {
            Object[] data = customDataSupplier.get();

            // Extract data arrays from supplier
            String[] names = (String[]) data[0];
            String[] jobs = (String[]) data[1];
            int[] levels = (int[]) data[2];

            // Create column data arrays for 3 columns using UIGridBuilder's modular
            // approach
            @SuppressWarnings("unchecked")
            java.util.List<UIGridBuilder.GridContent>[] columnData = new java.util.List[3];
            columnData[0] = new java.util.ArrayList<>(); // Name column
            columnData[1] = new java.util.ArrayList<>(); // Job column
            columnData[2] = new java.util.ArrayList<>(); // Level column

            // Populate column data
            for (int i = 0; i < names.length; i++) {
                columnData[0].add(UIGridBuilder.GridContent.text(names[i], TEXT_HIGHLIGHT));
                columnData[1].add(UIGridBuilder.GridContent.text(jobs[i], TEXT_COLOR));
                columnData[2].add(UIGridBuilder.GridContent.text("Level " + levels[i], TEXT_COLOR));
            }

            // Create the grid only once to preserve scroll state
            if (grid == null) {
                DebugConfig.debug(LOGGER, DebugConfig.UI_STANDARD_TAB_CONTENT,
                        "Creating new UIGridBuilder for population with {} citizens", names.length);
                grid = UIGridBuilder.create(x, y, width, height, 3)
                        .withRowHeight(16) // Match PopulationTab's itemHeight
                        .withMargins(8, 5) // Match PopulationTab's padding and verticalPadding
                        .withSpacing(10, 0) // Horizontal spacing between columns
                        .withBackgroundColor(BACKGROUND_COLOR)
                        .withBorderColor(BORDER_COLOR)
                        .drawBorder(true)
                        .withColumnData(columnData);

                DebugConfig.debug(LOGGER, DebugConfig.UI_STANDARD_TAB_CONTENT,
                        "Population grid created with scrolling enabled: {}", names.length > 4);
            } else {
                // Update existing grid with new data while preserving scroll state
                grid.updateColumnData(columnData);
            }

            // Render the grid (preserves scroll state)
            grid.render(guiGraphics, mouseX, mouseY);
        }
    }

    /**
     * Render production list (Production tab style)
     */
    private void renderProductionList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (customDataSupplier != null) {
            Object[] data = customDataSupplier.get();

            // Extract data arrays from supplier
            // Format: [Names, Progress/Status, Tooltips]
            String[] names = (String[]) data[0];
            Object[] progress = (Object[]) data[1]; // Can be String[] or StatusSymbol[] or Object[]
            String[] tooltips = (data.length > 2) ? (String[]) data[2] : null;

            int totalRows = names.length;

            // Create the grid only once to preserve scroll state
            // Use 5 columns to simulate 80/20 split (4 cols for Name, 1 col for Status)
            if (grid == null) {
                DebugConfig.debug(LOGGER, DebugConfig.UI_STANDARD_TAB_CONTENT,
                        "Creating new UIGridBuilder for production list with {} items", totalRows);

                grid = UIGridBuilder.create(x, y, width, height, 5) // 5 columns
                        .withRowHeight(16)
                        .withMargins(8, 5)
                        .withSpacing(5, 0) // Reduced horizontal spacing
                        .withBackgroundColor(BACKGROUND_COLOR)
                        .withBorderColor(BORDER_COLOR)
                        .drawBorder(true);

                // Enable auto vertical scrolling
                grid.withVerticalScrollAuto(true);

                // Add content manually to use spans
                for (int i = 0; i < totalRows; i++) {
                    String tooltip = (tooltips != null && i < tooltips.length) ? tooltips[i] : null;

                    // Name column: Span 4 (80%)
                    grid.addLabelWithTooltip(i, 0, 1, 4, names[i], tooltip, TEXT_HIGHLIGHT);

                    // Status/Progress column: Span 1 (20%)
                    Object statusObj = (i < progress.length) ? progress[i] : "-";

                    if (statusObj instanceof UIGridBuilder.StatusSymbol symbol) {
                        grid.addStatusIcon(i, 4, symbol);
                    } else {
                        grid.addLabelWithTooltip(i, 4, 1, 1, String.valueOf(statusObj), tooltip, TEXT_COLOR);
                    }
                }

                // Update internal row count for scrolling
                grid.updateTotalRows(totalRows);
            } else {
                // For updates, we clear elements and rebuild to preserve scroll state
                // This is slightly less efficient than updateColumnData but necessary for
                // custom spans
                grid.clearElements();

                for (int i = 0; i < totalRows; i++) {
                    String tooltip = (tooltips != null && i < tooltips.length) ? tooltips[i] : null;
                    grid.addLabelWithTooltip(i, 0, 1, 4, names[i], tooltip, TEXT_HIGHLIGHT);

                    Object statusObj = (i < progress.length) ? progress[i] : "-";

                    if (statusObj instanceof UIGridBuilder.StatusSymbol symbol) {
                        grid.addStatusIcon(i, 4, symbol);
                    } else {
                        grid.addLabelWithTooltip(i, 4, 1, 1, String.valueOf(statusObj), tooltip, TEXT_COLOR);
                    }
                }
                grid.updateTotalRows(totalRows);
            }

            // Render the grid (preserves scroll state)
            grid.render(guiGraphics, mouseX, mouseY);
        }
    }

    /**
     * Render button grid (Settings tab style)
     */
    private void renderButtonGrid(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (buttonGridSupplier != null) {
            Map<String, Object[]> buttonData = buttonGridSupplier.get();

            // Create/update the grid every time to ensure dynamic button text updates
            grid = UIGridBuilder.createLabelButtonGrid(
                    x, y, width, height,
                    TEXT_COLOR, TownInterfaceTheme.PRIMARY_COLOR,
                    buttonData)
                    .withBackgroundColor(BACKGROUND_COLOR)
                    .withBorderColor(BORDER_COLOR)
                    .withMargins(15, 10)
                    .withSpacing(15, 10)
                    .drawBorder(true);

            // Render the grid
            grid.render(guiGraphics, mouseX, mouseY);
        }
    }

    /**
     * Invokes a settings-style grid button by stable id (showcase harness).
     */
    public boolean invokeShowcaseButtonById(String elementId) {
        if (contentType != ContentType.BUTTON_GRID || buttonGridSupplier == null || elementId == null) {
            return false;
        }
        UIGridBuilder temp = UIGridBuilder.createLabelButtonGrid(
                x, y, width, height,
                TEXT_COLOR, TownInterfaceTheme.PRIMARY_COLOR,
                buttonGridSupplier.get())
                .withBackgroundColor(BACKGROUND_COLOR)
                .withBorderColor(BORDER_COLOR)
                .withMargins(15, 10)
                .withSpacing(15, 10)
                .drawBorder(true);
        return temp.invokeButtonById(elementId);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // For button grids, check if we have a custom click handler first
        if (contentType == ContentType.BUTTON_GRID && customClickHandler != null) {
            if (customClickHandler.apply(button)) {
                return true; // Custom handler handled the click
            }
        }

        if (grid != null) {
            return grid.mouseClicked((int) mouseX, (int) mouseY, button);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (grid != null) {
            return grid.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (grid != null) {
            return grid.mouseReleased(mouseX, mouseY, button);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (contentType == ContentType.LABEL_VALUE_GRID) {
            int viewH = Math.max(1, height - 12);
            int maxScroll = Math.max(0, labelContentHeight - viewH);
            if (maxScroll == 0) {
                return false;
            }
            labelScroll = (int) Math.max(0, Math.min(maxScroll, labelScroll - delta * 12));
            return true;
        }
        if (grid != null) {
            return grid.mouseScrolled(mouseX, mouseY, delta);
        }
        return false;
    }

    /**
     * Force refresh of the content (for external updates)
     */
    public void refresh() {
        // Force grid recreation
        grid = null;
    }
}
