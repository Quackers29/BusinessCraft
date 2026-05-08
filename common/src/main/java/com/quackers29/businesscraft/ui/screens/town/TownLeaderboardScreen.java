package com.quackers29.businesscraft.ui.screens.town;

import com.quackers29.businesscraft.town.data.TownLeaderboardData;
import com.quackers29.businesscraft.ui.builders.UIGridBuilder;
import com.quackers29.businesscraft.ui.templates.TownInterfaceTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Leaderboard screen showing all towns with sorting options.
 * Uses UIGridBuilder with dynamic equal-width columns.
 */
public class TownLeaderboardScreen extends Screen {

    private final Screen parentScreen;
    private final BlockPos currentTownPosition;
    private final String currentTownName;
    private List<TownLeaderboardData> townData = new ArrayList<>();
    private SortMode sortMode = SortMode.DISTANCE;

    // Panel dimensions
    private int panelWidth;
    private int panelHeight;
    private int panelLeft;
    private int panelTop;

    // UI elements
    private Button backButton;
    private Button sortButton;
    private UIGridBuilder contentGrid;

    // Dynamic columns
    private List<ColumnInfo> visibleColumns = new ArrayList<>();

    public enum SortMode {
        NAME("Name"),
        DISTANCE("Distance"),
        POPULATION("Population"),
        HAPPINESS("Happiness"),
        TOURISM("Tourism"),
        MONEY("Money");

        private final String displayName;

        SortMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public SortMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /**
     * Column information for dynamic layout.
     */
    private record ColumnInfo(
        String header,
        Function<TownLeaderboardData, String> valueExtractor,
        boolean isSorted
    ) {}

    public TownLeaderboardScreen(Component title, Screen parentScreen, BlockPos currentTownPosition, String currentTownName) {
        super(title);
        this.parentScreen = parentScreen;
        this.currentTownPosition = currentTownPosition;
        this.currentTownName = currentTownName != null ? currentTownName : "";
    }

    public void setTownData(List<TownLeaderboardData> towns) {
        this.townData = new ArrayList<>(towns);
        sortData();
    }

    private void sortData() {
        townData.sort(getComparatorForMode(sortMode));
    }

    private Comparator<TownLeaderboardData> getComparatorForMode(SortMode mode) {
        return switch (mode) {
            case NAME -> Comparator.comparing(TownLeaderboardData::name);
            case DISTANCE -> Comparator.comparingDouble(data -> data.distanceTo(currentTownPosition));
            case POPULATION -> Comparator.comparingLong(TownLeaderboardData::population).reversed();
            case HAPPINESS -> Comparator.comparingDouble(TownLeaderboardData::happiness).reversed();
            case TOURISM -> Comparator.comparingLong(TownLeaderboardData::tourism).reversed();
            case MONEY -> Comparator.comparingLong(TownLeaderboardData::money).reversed();
        };
    }

    /**
     * Check if a metric has any non-zero values across all towns.
     */
    private boolean hasAnyNonZeroValues(Function<TownLeaderboardData, Number> extractor) {
        for (TownLeaderboardData town : townData) {
            Number value = extractor.apply(town);
            if (value.doubleValue() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculate which columns to display based on available width and data availability.
     * All columns get equal width. Columns stay in fixed order.
     * When only 3 columns fit, the 3rd column is the currently sorted column (unless sorting by Name/Distance).
     * Columns with all zero values are hidden.
     */
    private void calculateVisibleColumns() {
        visibleColumns.clear();

        // Check which metrics have data
        boolean hasPopulation = hasAnyNonZeroValues(TownLeaderboardData::population);
        boolean hasHappiness = hasAnyNonZeroValues(data -> data.happiness());
        boolean hasTourism = hasAnyNonZeroValues(TownLeaderboardData::tourism);
        boolean hasMoney = hasAnyNonZeroValues(TownLeaderboardData::money);

        int availableWidth = panelWidth - 30; // Account for margins
        int MIN_COLUMN_WIDTH = 100; // Minimum width per column

        // Determine how many total columns we can fit
        int maxColumns = Math.max(2, availableWidth / MIN_COLUMN_WIDTH);

        // Column 1: Town Name (always)
        visibleColumns.add(new ColumnInfo(
            "Town Name",
            TownLeaderboardData::name,
            sortMode == SortMode.NAME
        ));

        // Column 2: Distance (always)
        visibleColumns.add(new ColumnInfo(
            "Distance",
            data -> TownLeaderboardData.formatDistance(data.distanceTo(currentTownPosition)),
            sortMode == SortMode.DISTANCE
        ));

        // Build list of available columns in order
        List<ColumnInfo> availableColumns = new ArrayList<>();

        if (hasPopulation) {
            availableColumns.add(new ColumnInfo(
                "Population",
                data -> data.population() > 0 ? String.format("%d", data.population()) : "-",
                sortMode == SortMode.POPULATION
            ));
        }

        if (hasHappiness) {
            availableColumns.add(new ColumnInfo(
                "Happiness",
                data -> data.happiness() >= 0 ? String.format("%.0f%%", data.happiness()) : "-",
                sortMode == SortMode.HAPPINESS
            ));
        }

        if (hasTourism) {
            availableColumns.add(new ColumnInfo(
                "Tourism",
                data -> data.tourism() > 0 ? String.format("%d", data.tourism()) : "-",
                sortMode == SortMode.TOURISM
            ));
        }

        if (hasMoney) {
            availableColumns.add(new ColumnInfo(
                "Money",
                data -> data.money() > 0 ? data.money() + " ✰" : "-",
                sortMode == SortMode.MONEY
            ));
        }

        // For 3 columns: prioritize showing the sorted column
        if (maxColumns == 3 && availableColumns.size() > 1) {
            // Find the sorted column
            ColumnInfo sortedColumn = null;
            List<ColumnInfo> otherColumns = new ArrayList<>();

            for (ColumnInfo col : availableColumns) {
                if (col.isSorted()) {
                    sortedColumn = col;
                } else {
                    otherColumns.add(col);
                }
            }

            // If we found a sorted column, show it. Otherwise show first available.
            if (sortedColumn != null) {
                visibleColumns.add(sortedColumn);
            } else if (!otherColumns.isEmpty()) {
                visibleColumns.add(otherColumns.get(0));
            }
        }
        // For 4+ columns: show as many as fit
        else {
            int columnsToAdd = Math.min(maxColumns - 2, availableColumns.size()); // -2 for Name and Distance
            for (int i = 0; i < columnsToAdd; i++) {
                visibleColumns.add(availableColumns.get(i));
            }
        }
    }

    /**
     * Build the grid with current data and columns.
     */
    private void buildGrid() {
        int gridX = panelLeft + 10;
        int gridY = panelTop + 50;
        int gridWidth = panelWidth - 20;
        int gridHeight = panelHeight - 60;

        int numColumns = visibleColumns.size();

        contentGrid = UIGridBuilder.create(gridX, gridY, gridWidth, gridHeight, numColumns)
            .withBackgroundColor(0x00000000) // Transparent - we draw panel background
            .withBorderColor(0x00000000) // No border
            .withMargins(5, 5)
            .withSpacing(10, 0)
            .withRowHeight(18)
            .drawBorder(false);

        // Enable vertical scrolling
        int visibleRows = (gridHeight - 10) / 18;
        contentGrid.withVerticalScroll(true, visibleRows);

        // Add header row
        for (int col = 0; col < visibleColumns.size(); col++) {
            ColumnInfo column = visibleColumns.get(col);
            String header = column.isSorted() ? column.header() + " ▼" : column.header();
            int color = column.isSorted() ? TownInterfaceTheme.TEXT_HIGHLIGHT : TownInterfaceTheme.TEXT_COLOR;
            contentGrid.addLabel(0, col, header, color);
        }

        // Add data rows
        if (townData.isEmpty()) {
            int midColumn = visibleColumns.size() / 2;
            contentGrid.addLabel(1, midColumn, "No towns to display", TownInterfaceTheme.TEXT_COLOR);
        } else {
            for (int row = 0; row < townData.size(); row++) {
                TownLeaderboardData town = townData.get(row);
                int dataRow = row + 1; // +1 for header

                // Check if this is the current town (by name)
                boolean isCurrentTown = town.name().equals(currentTownName);

                for (int col = 0; col < visibleColumns.size(); col++) {
                    ColumnInfo column = visibleColumns.get(col);
                    String value = column.valueExtractor().apply(town);

                    // Use gold color for current town's name, normal color for everything else
                    int textColor = (col == 0 && isCurrentTown) ? 0xFFFFAA00 : TownInterfaceTheme.TEXT_COLOR;

                    contentGrid.addLabel(dataRow, col, value, textColor);
                }
            }
        }

        contentGrid.updateTotalRows(townData.size() + 1); // +1 for header
    }

    private void cycleSortMode() {
        sortMode = sortMode.next();
        sortData();
        this.clearWidgets();
        this.init();
    }

    @Override
    protected void init() {
        super.init();

        this.panelWidth = (int)(this.width * 0.8);
        this.panelHeight = (int)(this.height * 0.7) - 40;
        this.panelLeft = (this.width - panelWidth) / 2;
        this.panelTop = (this.height - panelHeight) / 2 - 10;

        calculateVisibleColumns();
        buildGrid();

        int buttonX = this.width / 2 - 50;
        int buttonY = panelTop + panelHeight + 5;
        this.backButton = this.addRenderableWidget(Button.builder(
            Component.literal("Back"),
            button -> this.minecraft.setScreen(parentScreen))
            .pos(buttonX, buttonY)
            .size(100, 20)
            .build()
        );

        buttonY = panelTop + panelHeight + 30;
        this.sortButton = this.addRenderableWidget(Button.builder(
            Component.literal("Sort: " + sortMode.getDisplayName()),
            button -> this.cycleSortMode())
            .pos(buttonX, buttonY)
            .size(100, 20)
            .build()
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        graphics.fill(0, 0, this.width, this.height, 0xB0000000);

        // Panel background and border
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, TownInterfaceTheme.BACKGROUND_COLOR);
        graphics.hLine(panelLeft, panelLeft + panelWidth - 1, panelTop, TownInterfaceTheme.BORDER_COLOR);
        graphics.hLine(panelLeft, panelLeft + panelWidth - 1, panelTop + panelHeight - 1, TownInterfaceTheme.BORDER_COLOR);
        graphics.vLine(panelLeft, panelTop, panelTop + panelHeight - 1, TownInterfaceTheme.BORDER_COLOR);
        graphics.vLine(panelLeft + panelWidth - 1, panelTop, panelTop + panelHeight - 1, TownInterfaceTheme.BORDER_COLOR);

        // Title
        graphics.drawCenteredString(this.font, this.title, this.width / 2, panelTop + 15, TownInterfaceTheme.TEXT_COLOR);

        // Header separator
        graphics.hLine(panelLeft + 10, panelLeft + panelWidth - 10, panelTop + 45, TownInterfaceTheme.BORDER_COLOR);

        // Render grid
        if (contentGrid != null) {
            contentGrid.render(graphics, mouseX, mouseY);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check for row clicks first (before grid handles it)
        if (contentGrid != null && button == 0 && !townData.isEmpty()) {
            int row = contentGrid.getClickedRow((int)mouseX, (int)mouseY);
            if (row > 0 && row <= townData.size()) { // row 0 is header
                openDetailScreen(townData.get(row - 1));
                return true;
            }
        }

        // Let grid handle other interactions (scrollbar, etc)
        if (contentGrid != null && contentGrid.mouseClicked((int)mouseX, (int)mouseY, button)) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (contentGrid != null) {
            return contentGrid.mouseScrolled(mouseX, mouseY, delta);
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void openDetailScreen(TownLeaderboardData town) {
        TownDetailScreen detailScreen = new TownDetailScreen(
            Component.literal("Town Details"),
            this,
            town,
            currentTownPosition
        );
        this.minecraft.setScreen(detailScreen);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
