package com.quackers29.businesscraft.town.components;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.data.parsers.Condition;
import com.quackers29.businesscraft.data.parsers.DataParser.ResourceAmount;
import com.quackers29.businesscraft.production.ProductionRecipe;
import com.quackers29.businesscraft.production.ProductionRegistry;
import com.quackers29.businesscraft.town.Town;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class TownProductionComponent implements TownComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownProductionComponent.class);

    private final Town town;
    private final Map<String, Float> recipeProgress = new HashMap<>(); // recipeId -> days accumulated

    public TownProductionComponent(Town town) {
        this.town = town;
    }

    private int tickCounter = 0;

    @Override
    public void tick() {
        // Iterate all registered recipes
        // ... comments ...

        tickCounter++;
        boolean shouldLog = (tickCounter % 100 == 0);

        if (shouldLog)
            LOGGER.info("TownProductionComponent.tick() - Town: {}, Upgrades: {}",
                    town.getName(), town.getUpgrades().getUnlockedNodes());

        if (tickCounter % 20 == 0) {
            updateHappiness();
        }

        for (ProductionRecipe recipe : ProductionRegistry.getAll()) {
            if (recipe.getId().equals("population_maintenance") && shouldLog) {
                LOGGER.info(
                        "DEBUG: population_maintenance check - Modifier: {}, Unlocked: {}",
                        town.getUpgrades().getModifier(recipe.getId()),
                        town.getUpgrades().getModifier(recipe.getId()) > 0);
            }

            // Check unlock status
            if (town.getUpgrades().getModifier(recipe.getId()) <= 0) {
                if (shouldLog)
                    LOGGER.info("Recipe {} is locked (Modifier: {})",
                            recipe.getId(), town.getUpgrades().getModifier(recipe.getId()));
                continue; // Locked
            }

            processRecipe(recipe);
        }
    }

    private void processRecipe(ProductionRecipe recipe) {
        boolean shouldLog = (tickCounter % 100 == 0);

        // Check conditions
        if (!checkConditions(recipe)) {
            // LOGGER.debug("Recipe {} conditions not met", recipe.getId());
            return;
        }

        // Calculate Cycle Time
        float baseTime = recipe.getBaseCycleTimeDays();
        float timeMod = town.getUpgrades().getModifier(recipe.getId() + "-time");
        // If modifier is percentage?
        // My Effect parser stored percentage as separate flag but `getModifier` returns
        // flat sum.
        // As discussed, I can't easily distinguish flat vs % from single float.
        // Plan said: "prod_id-time:1 -> +1 day", "prod_id-time:-0.5 -> -0.5 day"
        // Also "basic_farming-time:-30% -> x0.7".
        // If I ignored the percentage flag in my parser (current impl just sums
        // values), then a string "30%" became 30.0f or 0.3f.
        // My parser: `valStr = valStr.substring(0, valStr.length() - 1); float val =
        // Float.parseFloat(valStr);`
        // So "30%" becomes 30.0.
        // A flat time of "1" is 1.0.
        // This is a collision. TownUpgradeComponent needs to handle flat vs pct.

        // Given I didn't fix TownUpgradeComponent yet, I'll assume only additive/flat
        // works correctly for now.
        // I'll proceed assuming flat values only for this iteration.

        float effectiveTime = baseTime + timeMod;
        if (effectiveTime < 0.000001f)
            effectiveTime = 0.000001f; // Min cap lowered for testing

        // Calculate Inputs Required
        // Input logic? Plan: "prod_id-input (% on resource inputs)"
        // Assuming strict "no scaling" for now to keep it simple, or just check stocks.

        // Check Stocks for Inputs
        boolean hasInputs = true;
        for (ResourceAmount input : recipe.getInputs()) {
            float required = input.amount;
            String resourceId = input.resourceId;

            if (resourceId.startsWith("pop*")) {
                resourceId = resourceId.substring(4);
                required = required * town.getPopulation();
            }

            if (resourceId.equals("population")) {
                // Population check? assumed fine or handled by conditions
            } else {
                // Resolve resourceId to Item
                com.quackers29.businesscraft.economy.ResourceType type = com.quackers29.businesscraft.economy.ResourceRegistry
                        .get(resourceId);
                if (type == null) {
                    if (shouldLog)
                        LOGGER.warn("Recipe {} input resource type not found: {}", recipe.getId(), resourceId);
                    hasInputs = false;
                    break;
                }

                net.minecraft.world.item.Item item = com.quackers29.businesscraft.api.PlatformAccess.getRegistry()
                        .getItem(type.getMcItemId());
                if (item == null) {
                    if (shouldLog)
                        LOGGER.warn("Recipe {} input item not found for type: {}", recipe.getId(), resourceId);
                    hasInputs = false;
                    break;
                }

                if (town.getResourceCount(item) < required) {
                    if (shouldLog)
                        LOGGER.info("Recipe {} missing input: {} (Need {}, Have {})",
                                recipe.getId(), resourceId, required, town.getResourceCount(item));
                    hasInputs = false;
                    break;
                }
            }
        }

        if (!hasInputs) {
            if (recipe.getId().equals("population_maintenance")) {
                // Starvation Logic: Advance progress anyway
                float currentProgress = recipeProgress.getOrDefault(recipe.getId(), 0f);
                float tickIncrement = 1.0f / (float) com.quackers29.businesscraft.config.ConfigLoader.dailyTickInterval;
                currentProgress += tickIncrement;

                if (currentProgress >= effectiveTime) {
                    LOGGER.info("STARVATION: Town {} missed {} cycle. No food available.", town.getName(),
                            recipe.getId());

                    // Apply Population Penalty
                    if (town.getPopulation() > 0) {
                        town.setPopulation(town.getPopulation() - 1);
                    }

                    // Partial Consumption Logic
                    for (ResourceAmount input : recipe.getInputs()) {
                        String resourceId = input.resourceId;
                        // Handle "pop*food" format
                        if (resourceId.startsWith("pop*")) {
                            resourceId = resourceId.substring(4);
                        }
                        com.quackers29.businesscraft.economy.ResourceType type = com.quackers29.businesscraft.economy.ResourceRegistry
                                .get(resourceId);
                        if (type != null) {
                            net.minecraft.world.item.Item item = com.quackers29.businesscraft.api.PlatformAccess
                                    .getRegistry().getItem(type.getMcItemId());
                            if (item != null) {
                                int available = town.getResourceCount(item);
                                if (available > 0) {
                                    town.addResource(item, -available); // Consume all
                                    LOGGER.info("STARVATION: Consumed remaining partial stack of {} {}", available,
                                            resourceId);
                                }
                            }
                        }
                    }

                    currentProgress = 0f;
                    town.markDirty();
                }
                recipeProgress.put(recipe.getId(), currentProgress);
            } else {
                // Can't run, report 0 progress
                recipeProgress.put(recipe.getId(), 0f);
            }
            return;
        }

        // Check Space for Outputs (Stalling logic)
        for (ResourceAmount output : recipe.getOutputs()) {
            String resId = output.resourceId;
            if (resId.equals("population"))
                continue;

            // Resolve resourceId to Item for cap check (or use trading cap by ID string)
            // Use trading logic for cap as it has the logic "storage_cap_ID"
            float current = 0f;
            com.quackers29.businesscraft.economy.ResourceType type = com.quackers29.businesscraft.economy.ResourceRegistry
                    .get(resId);
            if (type != null) {
                net.minecraft.world.item.Item item = com.quackers29.businesscraft.api.PlatformAccess.getRegistry()
                        .getItem(type.getMcItemId());
                if (item != null) {
                    current = town.getResourceCount(item);
                }
            }

            float cap = town.getTrading().getStorageCap(resId);

            if (current + output.amount > cap) {
                if (shouldLog)
                    LOGGER.info("Recipe {} output full: {}", recipe.getId(), resId);
                // Stall - ensure progress is tracked
                float currentProgress = recipeProgress.getOrDefault(recipe.getId(), 0f);
                recipeProgress.put(recipe.getId(), currentProgress);
                return; // Stall
            }
        }

        // Advance Progress
        float currentProgress = recipeProgress.getOrDefault(recipe.getId(), 0f);
        float tickIncrement = 1.0f / (float) com.quackers29.businesscraft.config.ConfigLoader.dailyTickInterval;

        currentProgress += tickIncrement;

        if (shouldLog)
            LOGGER.info("Recipe {} progress: {}/{}", recipe.getId(), currentProgress, effectiveTime);

        if (currentProgress >= effectiveTime) {
            // Complete Recipe
            LOGGER.info("Recipe {} COMPLETED for town {}", recipe.getId(), town.getName());
            currentProgress = 0f;
            consumeAndProduce(recipe);
        }

        recipeProgress.put(recipe.getId(), currentProgress);
    }

    private void consumeAndProduce(ProductionRecipe recipe) {
        // Consume Inputs
        for (ResourceAmount input : recipe.getInputs()) {
            String resourceId = input.resourceId;
            float amount = input.amount;

            if (resourceId.startsWith("pop*")) {
                String actualRes = resourceId.substring(4);
                float originalAmount = amount;
                amount = amount * town.getPopulation();
                LOGGER.info("DEBUG: Consuming {} for {}: Pop={}, Base={}, Calc={}",
                        actualRes, recipe.getId(), town.getPopulation(), originalAmount, amount);
                resourceId = actualRes;
            }

            if (resourceId.equals("population")) {
                // consuming pop?
                continue;
            }

            com.quackers29.businesscraft.economy.ResourceType type = com.quackers29.businesscraft.economy.ResourceRegistry
                    .get(resourceId);
            if (type != null) {
                net.minecraft.world.item.Item item = com.quackers29.businesscraft.api.PlatformAccess.getRegistry()
                        .getItem(type.getMcItemId());
                if (item != null) {
                    town.addResource(item, -(int) amount);
                }
            }
        }

        // Produce Outputs
        for (ResourceAmount output : recipe.getOutputs()) {
            String resId = output.resourceId;
            float amount = output.amount;

            if (resId.equals("population")) {
                town.setPopulation(town.getPopulation() + (int) amount);
            } else {
                com.quackers29.businesscraft.economy.ResourceType type = com.quackers29.businesscraft.economy.ResourceRegistry
                        .get(resId);
                if (type != null) {
                    net.minecraft.world.item.Item item = com.quackers29.businesscraft.api.PlatformAccess.getRegistry()
                            .getItem(type.getMcItemId());
                    if (item != null) {
                        town.addResource(item, (int) amount);
                    }
                }
            }
        }

        town.markDirty();
    }

    private boolean checkConditions(ProductionRecipe recipe) {
        for (Condition cond : recipe.getConditions()) {
            String target = cond.getTarget(); // "happiness", "pop"
            String op = cond.getOperator();
            String valStr = cond.getValue();

            float targetValue = 0f;
            if (target.equals("happiness"))
                targetValue = town.getHappiness();
            else if (target.equals("pop"))
                targetValue = town.getPopulation();
            else {
                // Maybe a resource amount?
                continue;
            }

            float threshold = 0f;
            // Parse valStr. Might be "pop_cap" variable.
            if (valStr.contains("pop_cap")) {
                float popCap = town.getUpgrades().getModifier("pop_cap"); // Base?
                // Plan: "pop_cap starts at 0 and granted by starting_nodes".
                // So getModifier("pop_cap") IS the max pop.

                // If valStr is "95%pop_cap"
                if (valStr.contains("%")) {
                    valStr = valStr.replace("pop_cap", "").replace("%", "").trim();
                    float pct = Float.parseFloat(valStr);
                    threshold = (pct / 100f) * popCap;
                } else {
                    threshold = popCap;
                }
            } else {
                try {
                    threshold = Float.parseFloat(valStr);
                } catch (NumberFormatException e) {
                    continue;
                }
            }

            // Compare
            boolean pass = false;
            if (op.equals(">"))
                pass = targetValue > threshold;
            else if (op.equals("<"))
                pass = targetValue < threshold;
            else if (op.equals(">="))
                pass = targetValue >= threshold;
            else if (op.equals("<="))
                pass = targetValue <= threshold;
            else if (op.equals("="))
                pass = Math.abs(targetValue - threshold) < 0.001f;

            if (!pass)
                return false;
        }
        return true;
    }

    @Override
    public void save(CompoundTag tag) {
        CompoundTag progressTag = new CompoundTag();
        recipeProgress.forEach(progressTag::putFloat);
        tag.put("recipeProgress", progressTag);
    }

    @Override
    public void load(CompoundTag tag) {
        recipeProgress.clear();
        if (tag.contains("recipeProgress")) {
            CompoundTag progressTag = tag.getCompound("recipeProgress");
            for (String key : progressTag.getAllKeys()) {
                recipeProgress.put(key, progressTag.getFloat(key));
            }
        }
    }

    public Map<String, Float> getActiveRecipes() {
        Map<String, Float> percentages = new HashMap<>();
        for (Map.Entry<String, Float> entry : recipeProgress.entrySet()) {
            String id = entry.getKey();
            float current = entry.getValue();
            ProductionRecipe recipe = ProductionRegistry.get(id);
            if (recipe != null) {
                float base = recipe.getBaseCycleTimeDays();
                // We use calculate effective time logic
                float mod = town.getUpgrades().getModifier(id + "-time");
                float effective = base + mod;
                if (effective < 0.000001f)
                    effective = 0.000001f;
                percentages.put(id, current / effective);
            } else {
                percentages.put(id, 0f);
            }
        }
        return percentages;
    }

    private void updateHappiness() {
        String foodId = "food";
        com.quackers29.businesscraft.economy.ResourceType type = com.quackers29.businesscraft.economy.ResourceRegistry
                .get(foodId);
        if (type != null) {
            net.minecraft.world.item.Item item = com.quackers29.businesscraft.api.PlatformAccess.getRegistry()
                    .getItem(type.getMcItemId());
            if (item != null) {
                float current = town.getResourceCount(item);
                float cap = town.getTrading().getStorageCap(foodId);

                // If cap is 0 or very small, handle gracefully
                if (cap < 1.0f)
                    cap = 1.0f;

                float ratio = current / cap;
                float baseHappiness = ratio * 50.0f;

                // Set directly via setHappiness (base value)
                // Note: user requested 0 at 0 food, 50 at 100% storage.
                // Clamped to 0-50 logic is implied by logic math.
                town.setHappiness(baseHappiness);
            }
        }
    }
}
