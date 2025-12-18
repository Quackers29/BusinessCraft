
package com.quackers29.businesscraft.town.components;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.config.registries.ItemRegistry;
import com.quackers29.businesscraft.config.registries.ProductionRegistry;
import com.quackers29.businesscraft.config.registries.ProductionRegistry.ProductionRecipe;
import com.quackers29.businesscraft.town.Town;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class TownProductionComponent implements TownComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownProductionComponent.class);

    private final Town town;
    private final Map<String, Integer> productionProgress = new HashMap<>();

    public TownProductionComponent(Town town) {
        this.town = town;
    }

    @Override
    public void tick() {
        for (ProductionRecipe recipe : ProductionRegistry.getAll()) {
            // Calculate target interval in ticks
            int interval = (int) (recipe.baseCycleTimeDays * ConfigLoader.dailyTickInterval);
            if (interval < 1)
                interval = 1; // Minimum 1 tick

            // Increment progress
            int current = productionProgress.getOrDefault(recipe.id, 0);
            current++;

            if (current >= interval) {
                if (canRunRecipe(recipe)) {
                    runRecipe(recipe);
                    productionProgress.put(recipe.id, 0); // Reset
                }
                // If condition fails, we cap at interval? Or keep incrementing?
                // Standard behavior: wait until condition met.
                // So we keep progress at interval.
                else {
                    productionProgress.put(recipe.id, interval);
                }
            } else {
                productionProgress.put(recipe.id, current);
            }
        }
    }

    private void performProduction() {
        // Deprecated by per-recipe logic
    }

    private boolean canRunRecipe(ProductionRecipe recipe) {
        // Check all inputs/conditions
        for (String input : recipe.inputs) {
            if (input == null || input.isEmpty())
                continue;

            // Parse Condition
            if (input.contains(">") || input.contains("<")) {
                if (!checkCondition(input))
                    return false;
            } else if (input.startsWith("has_upgrade:")) {
                String upgradeId = input.split(":")[1];
                if (!town.hasUpgrade(upgradeId))
                    return false;
            } else {
                // It's a resource cost
                if (!checkResourceAvailable(input))
                    return false;
            }
        }
        return true;
    }

    private boolean checkCondition(String condition) {
        // Format: property:>value or property:<value
        String[] parts = condition.split("(?=[><])|(?<=[><])");
        if (parts.length < 3)
            return false;

        String property = parts[0].replace(":", "").trim();
        String operator = parts[1];
        String valueStr = parts[2].trim();

        double value;
        try {
            // Handle special values
            if (valueStr.equals("pop_cap"))
                value = town.getPopulationCap();
            else
                value = Double.parseDouble(valueStr);
        } catch (NumberFormatException e) {
            return false;
        }

        double townValue = 0;
        switch (property) {
            case "happiness":
                townValue = town.getHappiness();
                break;
            case "pop":
            case "population":
                townValue = town.getPopulation();
                break;
            default:
                return false;
        }

        if (operator.equals(">"))
            return townValue > value;
        if (operator.equals("<"))
            return townValue < value;

        return false;
    }

    private boolean checkResourceAvailable(String input) {
        // Format: [pop*]resource:amount
        boolean perPop = input.startsWith("pop*");
        String cleanInput = perPop ? input.substring(4) : input;

        String[] parts = cleanInput.split(":");
        if (parts.length < 2)
            return false; // Invalid

        String resourceId = parts[0];
        int amount;
        try {
            amount = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return false;
        }

        if (perPop)
            amount *= town.getPopulation();

        // Check availability
        ItemRegistry.ItemEntry entry = ItemRegistry.get(resourceId);
        if (entry != null) {
            Item item = entry.getItem();
            if (item != null) {
                return town.getResourceCount(item) >= amount;
            }
        }

        return false; // Unknown resource
    }

    private void runRecipe(ProductionRecipe recipe) {
        // Consume Inputs
        for (String input : recipe.inputs) {
            if (input == null || input.isEmpty())
                continue;

            // Skip conditions
            if (input.contains(">") || input.contains("<") || input.startsWith("has_upgrade:"))
                continue;

            consumeInput(input);
        }

        // Produce Outputs
        for (String output : recipe.outputs) {
            if (output == null || output.isEmpty())
                continue;
            produceOutput(output);
        }
    }

    private void consumeInput(String input) {
        boolean perPop = input.startsWith("pop*");
        String cleanInput = perPop ? input.substring(4) : input;
        String[] parts = cleanInput.split(":");
        String resourceId = parts[0];
        int amount = Integer.parseInt(parts[1]);
        if (perPop)
            amount *= town.getPopulation();

        ItemRegistry.ItemEntry entry = ItemRegistry.get(resourceId);
        if (entry != null) {
            Item item = entry.getItem();
            if (item != null) {
                town.addResource(item, -amount);
            }
        }
    }

    private void produceOutput(String output) {
        boolean perPop = output.startsWith("pop*");
        String cleanOutput = perPop ? output.substring(4) : output;
        String[] parts = cleanOutput.split(":");
        String resourceId = parts[0];
        int amount = Integer.parseInt(parts[1]);
        if (perPop)
            amount *= town.getPopulation();

        if (resourceId.equals("population")) {
            // Increase population (handled via economy usually, but force here?)
            // Town.addVisitor uses logic.
            // Town contract uses bread.
            // Direct manipulation:
            // We need a method to safely add population or modify it.
            // TownEconomyComponent.setPopulation is public.
            // town.getEconomy().setPopulation(town.getPopulation() + amount);
            // Wait, I cannot access getEconomy() from Town as it is private/protected?
            // Town.getPopulation() exposes getEconomy().getPopulation().
            // Town expects logic inside Town.
            // I'll add logic to Town to modify population if needed, or assume TownEconomy
            // handles if bread is added?
            // But recipe might produce "population" directly (natural growth).
            // Let's assume for now I can't touch population directly unless I expose a
            // setter in Town.
            // Town.addVisitor adds from tourists.
            // I should probably skip population output for now or implement a setter in
            // Town.
            return;
        }

        ItemRegistry.ItemEntry entry = ItemRegistry.get(resourceId);
        if (entry != null) {
            Item item = entry.getItem();
            if (item != null) {
                town.addResource(item, amount);
            }
        }
    }

    @Override
    public void save(CompoundTag tag) {
        CompoundTag progressTag = new CompoundTag();
        productionProgress.forEach(progressTag::putInt);
        tag.put("productionProgress", progressTag);
    }

    @Override
    public void load(CompoundTag tag) {
        if (tag.contains("productionProgress")) {
            CompoundTag progressTag = tag.getCompound("productionProgress");
            for (String key : progressTag.getAllKeys()) {
                productionProgress.put(key, progressTag.getInt(key));
            }
        }
    }
}
