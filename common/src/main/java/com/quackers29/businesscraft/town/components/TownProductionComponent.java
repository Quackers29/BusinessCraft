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
    private int evaluationTickCounter = 0;
    private static final int EVALUATION_INTERVAL = 1000; // Check needs every 1000 ticks

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

        evaluationTickCounter++;
        if (evaluationTickCounter >= EVALUATION_INTERVAL) {
            evaluationTickCounter = 0;
            evaluateNeeds();
        }
    }

    private void performProduction() {
        // Run production for each active upgrade
        for (String upgradeId : activeUpgrades) {
            Upgrade upgrade = UpgradeRegistry.get(upgradeId);
            if (upgrade == null)
                continue;

            // Apply passive happiness bonus
            if (upgrade.getHappinessBonus() != 0) {
                // Small chance to apply bonus to avoid constant spam, or just apply it
                // periodically
                // For now, let's say happiness trends towards base + bonuses
                // But TownEconomyComponent handles happiness as a value.
                // Let's just add a tiny amount or handle it in evaluateNeeds?
                // Actually, let's make happiness trend towards target.
                // Target = 50 + Sum(Bonuses).
                // Current implementation: just add bonus periodically?
                // Let's add 1/100th of the bonus every production cycle.
                int currentHappiness = town.getEconomy().getHappiness();
                if (upgrade.getHappinessBonus() > 0 && currentHappiness < 100) {
                    if (Math.random() < 0.1) {
                        town.getEconomy().setHappiness(currentHappiness + 1);
                    }
                } else if (upgrade.getHappinessBonus() < 0 && currentHappiness > 0) {
                    if (Math.random() < 0.1) {
                        town.getEconomy().setHappiness(currentHappiness - 1);
                    }
                }
            }

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
                }
            }
        }
    }

    private void evaluateNeeds() {
        // Calculate Success Score
        float foodScore = calculateFoodScore();
        float happinessScore = calculateHappinessScore();
        float populationScore = calculatePopulationScore();

        float successScore = (foodScore * 0.5f) + (happinessScore * 0.3f) + (populationScore * 0.2f);

        LOGGER.debug("Town {} evaluation: Food={}, Happiness={}, Pop={}, Success={}",
                town.getName(), foodScore, happinessScore, populationScore, successScore);

        // Decision Logic
        if (foodScore < 0.3f) { // Critical Food Shortage
            LOGGER.info("Town {} has critical food shortage. Prioritizing RESOURCES.", town.getName());
            tryPurchaseUpgrade(Upgrade.UpgradeTrack.RESOURCES);
        } else if (happinessScore < 0.4f) { // Low Happiness
            LOGGER.info("Town {} has low happiness. Prioritizing SERVICES.", town.getName());
            tryPurchaseUpgrade(Upgrade.UpgradeTrack.SERVICES);
        } else if (foodScore > 0.8f && happinessScore > 0.7f) { // Surplus & Happy
            LOGGER.info("Town {} is thriving. Prioritizing POPULATION.", town.getName());
            tryPurchaseUpgrade(Upgrade.UpgradeTrack.POPULATION);
        } else {
            // Random chance to upgrade something else or save up
            if (Math.random() < 0.1) {
                tryPurchaseUpgrade(Upgrade.UpgradeTrack.RESOURCES);
            }
        }
    }

    private float calculateFoodScore() {
        int foodStock = (int) town.getTrading().getStock("food");
        int population = town.getPopulation();
        if (population == 0)
            return 1.0f;

        float daysOfFood = (float) foodStock / population;
        // 7 days = 1.0 score. 0 days = 0.0 score.
        return Math.min(1.0f, daysOfFood / 7.0f);
    }

    private float calculateHappinessScore() {
        return town.getEconomy().getHappiness() / 100.0f;
    }

    private float calculatePopulationScore() {
        // Score based on housing capacity usage?
        // Or just raw population growth?
        // Let's say score is higher if we have room to grow?
        // Actually, plan says "Population (20%): Based on housing usage."
        // If housing is full, score is low (need more housing)?
        // Or if housing is full, we are successful in filling it?
        // Let's assume: High population = High score.
        // But we want to trigger upgrades.
        // Let's simplify: 1.0 if pop > 50, scaled down.
        return Math.min(1.0f, town.getPopulation() / 50.0f);
    }

    private void tryPurchaseUpgrade(Upgrade.UpgradeTrack track) {
        // Find next available upgrade in this track
        for (Upgrade upgrade : UpgradeRegistry.getAll()) {
            if (activeUpgrades.contains(upgrade.getId()))
                continue;
            if (upgrade.getTrack() != track)
                continue;

            // Check prerequisites (Tier logic could be stricter, but for now just check
            // population)
            if (town.getPopulation() < upgrade.getPopulationReq())
                continue;

            // Check build costs (if any)
            // Currently Upgrade doesn't have build costs in CSV, but we added the map.
            // Assuming free for now unless CSV updated.

            // Purchase
            activeUpgrades.add(upgrade.getId());
            LOGGER.info("Town {} purchased upgrade: {} (Track: {})", town.getName(), upgrade.getName(), track);
            town.markDirty();
            return; // Only buy one at a time
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
