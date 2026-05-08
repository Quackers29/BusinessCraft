package com.quackers29.businesscraft.ui.screens.town;

import com.quackers29.businesscraft.town.data.TownLeaderboardData;
import com.quackers29.businesscraft.ui.modal.specialized.BCModalGridScreen;
import com.quackers29.businesscraft.ui.templates.TownInterfaceTheme;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple record for detail entries.
 */
record TownDetailEntry(String property, String value) {}

/**
 * Detail screen showing information about a specific town.
 */
public class TownDetailScreen extends BCModalGridScreen<TownDetailEntry> {

    public TownDetailScreen(Component title, Screen parentScreen, TownLeaderboardData townData, BlockPos currentTownPosition) {
        super(title, parentScreen, null);

        // Configure appearance
        this.withPanelSize(0.6f, 0.5f)
            .withRowHeight(20)
            .withTitleScale(1.5f)
            .withColors(
                TownInterfaceTheme.BACKGROUND_COLOR,
                TownInterfaceTheme.BORDER_COLOR,
                TownInterfaceTheme.TEXT_COLOR,
                TownInterfaceTheme.TEXT_HIGHLIGHT,
                TownInterfaceTheme.TEXT_COLOR
            )
            .withAlternatingRowColors(0x30FFFFFF, 0x20FFFFFF)
            .withBackButtonText("Back to Leaderboard");

        // Add columns
        this.addColumn("Property", TownDetailEntry::property)
            .addColumn("Value", TownDetailEntry::value);

        // Create detail entries
        List<TownDetailEntry> details = new ArrayList<>();
        details.add(new TownDetailEntry("Town Name", townData.name()));
        details.add(new TownDetailEntry("Distance", TownLeaderboardData.formatDistance(townData.distanceTo(currentTownPosition))));
        details.add(new TownDetailEntry("Coordinates", formatPosition(townData.position())));

        // Only show metrics that have values
        if (townData.population() > 0) {
            details.add(new TownDetailEntry("Population", String.format("%d", townData.population())));
        }
        if (townData.happiness() >= 0) {
            details.add(new TownDetailEntry("Happiness", String.format("%.0f%%", townData.happiness())));
        }
        if (townData.tourism() > 0) {
            details.add(new TownDetailEntry("Tourism", String.format("%d visitors", townData.tourism())));
        }
        if (townData.money() > 0) {
            details.add(new TownDetailEntry("Money", townData.money() + " ✰"));
        }

        // Set data
        this.withData(details);
    }

    /**
     * Format BlockPos for display.
     */
    private String formatPosition(BlockPos pos) {
        return String.format("X: %d, Y: %d, Z: %d", pos.getX(), pos.getY(), pos.getZ());
    }
}
