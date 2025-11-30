package com.quackers29.businesscraft.town.components;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.economy.GlobalMarket;
import com.quackers29.businesscraft.production.Upgrade;
import com.quackers29.businesscraft.production.UpgradeRegistry;
import com.quackers29.businesscraft.town.Town;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TownProductionComponent implements TownComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownProductionComponent.class);

    private final Town town;
    private final List<String> activeUpgrades = new ArrayList<>();
    private int productionTickCounter = 0;

    public TownProductionComponent(Town town) {
        this.town = town;
    }

    @Override
    public void tick() {
        productionTickCounter++;
        if (productionTickCounter >= ConfigLoader.productionTickInterval) {
            productionTickCounter = 0;
            performProduction();
        }
    }

    private void performProduction() {
        // 1. Check for new upgrades
        checkNewUpgrades();

        // 2. Run production for each active upgrade
        for (String upgradeId : activeUpgrades) {
            Upgrade upgrade = UpgradeRegistry.get(upgradeId);
            if (upgrade == null)
                continue;

            // Check inputs
            boolean hasInputs = true;
            for (Map.Entry<String, Float> input : upgrade.getInputRates().entrySet()) {
                float currentStock = town.getTrading().getStock(input.getKey());
                if (currentStock < input.getValue()) {
                    hasInputs = false;
                    break;
                }
            }

            if (hasInputs) {
                // Consume inputs
                for (Map.Entry<String, Float> input : upgrade.getInputRates().entrySet()) {
                    town.getTrading().adjustStock(input.getKey(), -input.getValue());
                }

                // Produce outputs
                for (Map.Entry<String, Float> output : upgrade.getOutputRates().entrySet()) {
                    town.getTrading().adjustStock(output.getKey(), output.getValue());
                    // Record production in global market (as volume, though technically not a
                    // trade)
                    // Or maybe just log it?
                }
            }
        }
    }

    private void checkNewUpgrades() {
        int population = town.getPopulation();
        for (Upgrade upgrade : UpgradeRegistry.getAll()) {
            if (!activeUpgrades.contains(upgrade.getId()) && population >= upgrade.getPopulationReq()) {
                activeUpgrades.add(upgrade.getId());
                LOGGER.info("Town {} acquired new upgrade: {}", town.getName(), upgrade.getName());
                town.markDirty();
            }
        }
    }

    public List<String> getActiveUpgrades() {
        return activeUpgrades;
    }

    @Override
    public void save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (String upgradeId : activeUpgrades) {
            list.add(StringTag.valueOf(upgradeId));
        }
        tag.put("upgrades", list);
    }

    @Override
    public void load(CompoundTag tag) {
        activeUpgrades.clear();
        if (tag.contains("upgrades")) {
            ListTag list = tag.getList("upgrades", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                activeUpgrades.add(list.getString(i));
            }
        }
    }
}
