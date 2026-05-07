package com.quackers29.businesscraft.ui.screens.town;

import com.quackers29.businesscraft.town.data.TownLeaderboardData;
import com.quackers29.businesscraft.ui.templates.TownInterfaceTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Leaderboard screen showing all towns with sorting options.
 */
public class TownLeaderboardScreen extends Screen {

    private final Screen parentScreen;
    private final BlockPos currentTownPosition;
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

    // Scrolling
    private int scrollOffset = 0;
    private final int rowHeight = 18;
    private final int headerHeight = 50;

    public enum SortMode {
        DISTANCE("Distance"),
        POPULATION("Population"),
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

    public TownLeaderboardScreen(Component title, Screen parentScreen, BlockPos currentTownPosition) {
        super(title);
        this.parentScreen = parentScreen;
        this.currentTownPosition = currentTownPosition;
    }

    /**
     * Set town data and apply initial sorting.
     */
    public void setTownData(List<TownLeaderboardData> towns) {
        this.townData = new ArrayList<>(towns);
        sortData();
    }

    /**
     * Sort data based on current mode.
     */
    private void sortData() {
        townData.sort(getComparatorForMode(sortMode));
    }

    /**
     * Get comparator for the current sort mode.
     */
    private Comparator<TownLeaderboardData> getComparatorForMode(SortMode mode) {
        return switch (mode) {
            case DISTANCE -> Comparator.comparingDouble(data -> data.distanceTo(currentTownPosition));
            case POPULATION -> Comparator.comparingLong(TownLeaderboardData::population).reversed();
            case MONEY -> Comparator.comparingLong(TownLeaderboardData::money).reversed();
        };
    }

    /**
     * Cycle to next sort mode.
     */
    private void cycleSortMode() {
        sortMode = sortMode.next();
        sortData();
        scrollOffset = 0;

        // Recreate buttons to update text
        this.clearWidgets();
        this.init();
    }

    @Override
    protected void init() {
        super.init();

        // Calculate panel dimensions
        this.panelWidth = (int)(this.width * 0.8);
        this.panelHeight = (int)(this.height * 0.7) - 40;
        this.panelLeft = (this.width - panelWidth) / 2;
        this.panelTop = (this.height - panelHeight) / 2 - 10;

        // Back button
        int buttonX = this.width / 2 - 50;
        int buttonY = panelTop + panelHeight + 5;
        this.backButton = this.addRenderableWidget(Button.builder(
            Component.literal("Back"),
            button -> this.minecraft.setScreen(parentScreen))
            .pos(buttonX, buttonY)
            .size(100, 20)
            .build()
        );

        // Sort button
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
        // Render background
        this.renderBackground(graphics);
        graphics.fill(0, 0, this.width, this.height, 0xB0000000);

        // Draw panel
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, TownInterfaceTheme.BACKGROUND_COLOR);
        graphics.hLine(panelLeft, panelLeft + panelWidth - 1, panelTop, TownInterfaceTheme.BORDER_COLOR);
        graphics.hLine(panelLeft, panelLeft + panelWidth - 1, panelTop + panelHeight - 1, TownInterfaceTheme.BORDER_COLOR);
        graphics.vLine(panelLeft, panelTop, panelTop + panelHeight - 1, TownInterfaceTheme.BORDER_COLOR);
        graphics.vLine(panelLeft + panelWidth - 1, panelTop, panelTop + panelHeight - 1, TownInterfaceTheme.BORDER_COLOR);

        // Draw title
        graphics.drawCenteredString(this.font, this.title, this.width / 2, panelTop + 15, TownInterfaceTheme.TEXT_COLOR);

        // Draw header separator
        graphics.hLine(panelLeft + 10, panelLeft + panelWidth - 10, panelTop + headerHeight - 5, TownInterfaceTheme.BORDER_COLOR);

        // Draw column headers
        int col1X = panelLeft + 15;
        int col2X = panelLeft + panelWidth / 3;
        int col3X = panelLeft + 2 * panelWidth / 3;
        int headerY = panelTop + 35;

        graphics.drawString(this.font, "Town Name", col1X, headerY, TownInterfaceTheme.TEXT_HIGHLIGHT);
        graphics.drawString(this.font, "Distance", col2X, headerY, TownInterfaceTheme.TEXT_HIGHLIGHT);
        graphics.drawString(this.font, sortMode.getDisplayName(), col3X, headerY, TownInterfaceTheme.TEXT_HIGHLIGHT);

        // Draw rows
        int contentY = panelTop + headerHeight + 5;
        int contentHeight = panelHeight - headerHeight - 10;
        int visibleRows = contentHeight / rowHeight;

        if (townData.isEmpty()) {
            graphics.drawCenteredString(this.font, "No towns to display", this.width / 2, contentY + 20, TownInterfaceTheme.TEXT_COLOR);
        } else {
            for (int i = scrollOffset; i < Math.min(scrollOffset + visibleRows, townData.size()); i++) {
                TownLeaderboardData town = townData.get(i);
                int rowY = contentY + (i - scrollOffset) * rowHeight;

                // Alternating row background
                if (i % 2 == 0) {
                    graphics.fill(panelLeft + 10, rowY, panelLeft + panelWidth - 10, rowY + rowHeight, 0x30FFFFFF);
                } else {
                    graphics.fill(panelLeft + 10, rowY, panelLeft + panelWidth - 10, rowY + rowHeight, 0x20FFFFFF);
                }

                // Draw data
                graphics.drawString(this.font, town.name(), col1X, rowY + 4, TownInterfaceTheme.TEXT_COLOR);
                graphics.drawString(this.font, TownLeaderboardData.formatDistance(town.distanceTo(currentTownPosition)), col2X, rowY + 4, TownInterfaceTheme.TEXT_COLOR);

                String scoreValue = switch (sortMode) {
                    case DISTANCE -> TownLeaderboardData.formatDistance(town.distanceTo(currentTownPosition));
                    case POPULATION -> String.format("%d", town.population());
                    case MONEY -> town.money() + " ✰";
                };
                graphics.drawString(this.font, scoreValue, col3X, rowY + 4, TownInterfaceTheme.TEXT_COLOR);
            }
        }

        // Render buttons
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if click is on a town row
        if (button == 0 && !townData.isEmpty()) {
            int contentY = panelTop + headerHeight + 5;
            int contentHeight = panelHeight - headerHeight - 10;
            int visibleRows = contentHeight / rowHeight;

            if (mouseX >= panelLeft + 10 && mouseX <= panelLeft + panelWidth - 10 &&
                mouseY >= contentY && mouseY <= contentY + contentHeight) {

                int clickedRow = (int)((mouseY - contentY) / rowHeight) + scrollOffset;
                if (clickedRow >= 0 && clickedRow < townData.size()) {
                    openDetailScreen(townData.get(clickedRow));
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int contentHeight = panelHeight - headerHeight - 10;
        int visibleRows = contentHeight / rowHeight;
        int maxScroll = Math.max(0, townData.size() - visibleRows);

        scrollOffset -= (int)Math.signum(delta);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        return true;
    }

    /**
     * Open detail screen for selected town.
     */
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
