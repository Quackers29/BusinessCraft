package com.quackers29.businesscraft.client.showcase;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.economy.ResourceRegistry;
import com.quackers29.businesscraft.economy.ResourceType;
import com.quackers29.businesscraft.production.ProductionRegistry;
import com.quackers29.businesscraft.production.UpgradeRegistry;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.components.TownUpgradeComponent;
import net.minecraft.world.item.Item;

/**
 * Applies one {@link ShowcaseProductionLayout.TownSpec} to a town that already has its biome kit.
 * The production showcase and the headless simulation both call this, so the unlocks, stock,
 * population, work units and stalled recipes have one implementation.
 */
public final class ShowcaseProductionSeed {
    private ShowcaseProductionSeed() {
    }

    public static void apply(Town town, ShowcaseProductionLayout.TownSpec spec) {
        TownUpgradeComponent upgrades = town.getUpgrades();
        for (ShowcaseProductionLayout.Unlock unlock : spec.unlocks()) {
            if (UpgradeRegistry.get(unlock.node()) == null) {
                throw new IllegalStateException(spec.name() + ": unknown upgrade node " + unlock.node());
            }
            for (int level = 0; level < unlock.level(); level++) {
                upgrades.unlockNode(unlock.node());
            }
        }
        for (String recipe : spec.stalled()) {
            if (ProductionRegistry.get(recipe) == null) {
                throw new IllegalStateException(spec.name() + ": unknown recipe " + recipe);
            }
            // The flat modifier is added to the upgrade modifier; cancelling it locks the recipe.
            upgrades.addFlatModifier(recipe, -upgrades.getModifier(recipe));
        }
        if (spec.population() > 0) {
            town.setPopulation(spec.population());
        }
        for (ShowcaseProductionLayout.Stock stock : spec.stock()) {
            ResourceType type = ResourceRegistry.get(stock.resource());
            Item item = type == null ? null : PlatformAccess.getRegistry().getItem(type.getMcItemId());
            if (item == null) {
                throw new IllegalStateException(spec.name() + ": unknown resource " + stock.resource());
            }
            long target = Math.round(town.getTrading().getStorageCap(stock.resource()) * stock.percentOfCap() / 100.0);
            town.addResource(item, target - town.getResourceCount(item));
        }
        if (spec.workUnitPercent() >= 0) {
            town.setWorkUnits(town.getWorkUnitCap() * spec.workUnitPercent() / 100);
        }
    }
}
