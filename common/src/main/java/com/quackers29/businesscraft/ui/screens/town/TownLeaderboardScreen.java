package com.quackers29.businesscraft.ui.screens.town;

import com.quackers29.businesscraft.town.data.TownLeaderboardData;
import com.quackers29.businesscraft.ui.modal.specialized.BCModalGridScreen;
import com.quackers29.businesscraft.ui.templates.TownInterfaceTheme;
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
public class TownLeaderboardScreen extends BCModalGridScreen<TownLeaderboardData> {

    private final BlockPos currentTownPosition;
    private List<TownLeaderboardData> townData = new ArrayList<>();
    private SortMode sortMode = SortMode.DISTANCE;
    private Button sortButton;

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
        super(title, parentScreen, null);
        this.currentTownPosition = currentTownPosition;

        // Configure appearance
        this.withPanelSize(0.8f, 0.7f)
            .withRowHeight(18)
            .withTitleScale(1.5f)
            .withColors(
                TownInterfaceTheme.BACKGROUND_COLOR,
                TownInterfaceTheme.BORDER_COLOR,
                TownInterfaceTheme.TEXT_COLOR,
                TownInterfaceTheme.TEXT_HIGHLIGHT,
                TownInterfaceTheme.TEXT_COLOR
            )
            .withAlternatingRowColors(0x30FFFFFF, 0x20FFFFFF)
            .withScrollbarColors(0x40FFFFFF, 0xA0CCDDFF, 0xFFCCDDFF);

        // Add columns
        this.addColumn("Town Name", data -> data.name())
            .addColumn("Distance", data -> TownLeaderboardData.formatDistance(data.distanceTo(currentTownPosition)))
            .addColumn(sortMode.getDisplayName(), this::getScoreValue);

        // Set row click handler to open detail screen
        this.withRowClickHandler(this::openDetailScreen);
    }

    /**
     * Set town data and apply initial sorting.
     */
    public void setTownData(List<TownLeaderboardData> towns) {
        this.townData = new ArrayList<>(towns);
        sortAndRefresh();
    }

    /**
     * Get score value for current sort mode.
     */
    private String getScoreValue(TownLeaderboardData data) {
        return switch (sortMode) {
            case DISTANCE -> TownLeaderboardData.formatDistance(data.distanceTo(currentTownPosition));
            case POPULATION -> String.format("%d", data.population());
            case MONEY -> data.money() + " ✰"; // Emerald symbol
        };
    }

    /**
     * Sort data and refresh display.
     */
    private void sortAndRefresh() {
        townData.sort(getComparatorForMode(sortMode));
        this.withData(townData);

        // Reinitialize to update columns
        if (this.minecraft != null) {
            this.init();
        }
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
        sortAndRefresh();

        // Update sort button text
        if (sortButton != null) {
            sortButton.setMessage(Component.literal("Sort: " + sortMode.getDisplayName()));
        }
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
    protected void init() {
        super.init();

        // Add sort button below the close button
        int buttonX = this.width / 2 - 50;
        int panelHeight = (int)(this.height * 0.7) - 40;
        int panelTop = (this.height - panelHeight) / 2 - 10;
        int buttonY = panelTop + panelHeight + 30; // Below "Back" button

        this.sortButton = this.addRenderableWidget(Button.builder(
            Component.literal("Sort: " + sortMode.getDisplayName()),
            button -> this.cycleSortMode())
            .pos(buttonX, buttonY)
            .size(100, 20)
            .build()
        );
    }
}
