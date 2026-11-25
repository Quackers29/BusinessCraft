package com.quackers29.businesscraft.client.showcase;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.town.Town;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Population, starter nodes, and stock for one showcase town.
 * The play world and the headless production simulation both call this.
 */
public final class ShowcaseTownSeed {
    private ShowcaseTownSeed() {
    }

    public static void apply(Town town, int index) {
        if (town == null) {
            return;
        }
        if (!town.getUpgrades().isUnlocked("basic_settlement")) {
            town.getUpgrades().unlockNode("basic_settlement");
        }
        if (town.getUpgrades().getModifier("tourist_cap") < 1f) {
            town.getUpgrades().addFlatModifier("tourist_cap", 6f);
        }
        town.setPopulation(ShowcasePlayLayout.TOWNS.get(index).population());
        for (ShowcasePlayLayout.Stock stock : ShowcasePlayLayout.STARTING_STOCK.get(index)) {
            Item item = PlatformAccess.getRegistry().getItem(new ResourceLocation(stock.itemId()));
            if (item != null) {
                town.addResource(item, stock.amount());
            }
        }
    }
}
