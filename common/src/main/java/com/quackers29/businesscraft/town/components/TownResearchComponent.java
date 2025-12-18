package com.quackers29.businesscraft.town.components;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.config.registries.UpgradeRegistry;
import com.quackers29.businesscraft.config.registries.UpgradeRequirementRegistry;
import com.quackers29.businesscraft.town.Town;
import net.minecraft.nbt.CompoundTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TownResearchComponent implements TownComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownResearchComponent.class);
    private final Town town;
    private int tickCounter = 0;

    public TownResearchComponent(Town town) {
        this.town = town;
    }

    @Override
    public void tick() {
        String currentUpgradeId = town.getCurrentResearch();
        if (currentUpgradeId == null || currentUpgradeId.isEmpty())
            return;

        // Check if already unlocked
        if (town.hasUpgrade(currentUpgradeId)) {
            town.setCurrentResearch(null);
            return;
        }

        // Progress tick (every 24000 ticks ~ 20 mins = 1 in-game day)
        tickCounter++;
        if (tickCounter >= 24000) {
            tickCounter = 0;

            // Get required days
            UpgradeRequirementRegistry.RequirementEntry req = UpgradeRequirementRegistry.get(currentUpgradeId);
            int requiredDays = (req != null) ? req.researchDays : ConfigLoader.researchDaysPerNode;

            // Increment progress
            town.setResearchProgress(town.getResearchProgress() + 1);

            // Check completion
            if (town.getResearchProgress() >= requiredDays) {
                completeResearch(currentUpgradeId);
            }
        }
    }

    private void completeResearch(String upgradeId) {
        town.addUpgrade(upgradeId);
        town.setCurrentResearch(null);
        town.setResearchProgress(0);

        UpgradeRegistry.UpgradeNode node = UpgradeRegistry.get(upgradeId);
        if (node != null) {
            LOGGER.info("Town {} completed research: {}", town.getName(), node.displayName);
            applyEffects(node);
        }
    }

    private void applyEffects(UpgradeRegistry.UpgradeNode node) {
        if (node.effects == null)
            return;

        for (Map.Entry<String, String> entry : node.effects.entrySet()) {
            String stat = entry.getKey();
            String valueStr = entry.getValue();

            try {
                if (stat.equals("happiness")) {
                    double val = Double.parseDouble(valueStr.replace("+", ""));
                    town.setHappiness(town.getHappiness() + val);
                } else if (stat.equals("pop_cap")) {
                    if (valueStr.startsWith("*")) {
                        double multiplier = Double.parseDouble(valueStr.substring(1));
                        town.setPopulationCap((int) (town.getPopulationCap() * multiplier));
                    } else {
                        int val = Integer.parseInt(valueStr.replace("+", ""));
                        town.setPopulationCap(town.getPopulationCap() + val);
                    }
                } else if (stat.startsWith("storage_cap_")) {
                    // e.g. storage_cap_wood
                    String item = stat.substring("storage_cap_".length());
                    int val = Integer.parseInt(valueStr.replace("+", ""));
                    int current = town.getStorageCap(item);
                    town.setStorageCap(item, current + val);
                }
                // Note: "unlock" values are ignored here as they are checked via
                // town.hasUpgrade() in other components
            } catch (Exception e) {
                LOGGER.error("Failed to apply effect {}:{} for upgrade {}", stat, valueStr, node.id);
            }
        }
    }

    @Override
    public void save(CompoundTag tag) {
        tag.putInt("ResearchTickCounter", tickCounter);
    }

    @Override
    public void load(CompoundTag tag) {
        if (tag.contains("ResearchTickCounter")) {
            tickCounter = tag.getInt("ResearchTickCounter");
        }
    }
}
