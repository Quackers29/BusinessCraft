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

    private record ResolvedResource(String id, float amount) {
    }

    private ResolvedResource resolveDynamicAmount(String rawId, String expression) {
        String finalId = rawId;
        String finalExpr = expression;

        // Backward compatibility for pop* prefix in ID
        if (finalId.startsWith("pop*")) {
            finalId = finalId.substring(4);
            finalExpr = finalExpr + "*pop";
        } else if (finalId.endsWith("*pop")) {
            finalId = finalId.substring(0, finalId.length() - 4);
            finalExpr = finalExpr + "*pop";
        }

        float value = evaluateExpression(finalExpr);
        return new ResolvedResource(finalId, value);
    }

    private float evaluateExpression(String expr) {
        if (expr == null || expr.isEmpty())
            return 0f;

        float result = 1.0f;
        String[] parts = expr.split("\\*");

        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty())
                continue;

            if (part.equalsIgnoreCase("pop")) {
                result *= town.getPopulation();
            } else if (part.equalsIgnoreCase("happiness")) {
                result *= town.getHappiness();
            } else {
                try {
                    result *= Float.parseFloat(part);
                } catch (NumberFormatException e) {
                    // Try as upgrade modifier (e.g. storage_cap_all)
                    float mod = town.getUpgrades().getModifier(part);
                    // If modifier not found, getModifier returns 0.
                    // This is acceptable behavior for unknown variables (multiplier becomes 0).
                    result *= mod;
                }
            }
        }
        return result;
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
        float effectiveTime = getEffectiveCycleTime(recipe);

        // Calculate Inputs Required
        // Input logic? Plan: "prod_id-input (% on resource inputs)"
        // Assuming strict "no scaling" for now to keep it simple, or just check stocks.

        // Check Stocks for Inputs
        boolean hasInputs = true;
        for (ResourceAmount input : recipe.getInputs()) {
            ResolvedResource resolved = resolveDynamicAmount(input.resourceId, input.amountExpression);
            String resourceId = resolved.id();
            float required = resolved.amount();

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
                        ResolvedResource resolved = resolveDynamicAmount(input.resourceId, input.amountExpression);
                        String resourceId = resolved.id();
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
            ResolvedResource resolved = resolveDynamicAmount(output.resourceId, output.amountExpression);
            String resId = resolved.id();
            float amount = resolved.amount();

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

            if (current + amount > cap) {
                if (shouldLog)
                    LOGGER.info("Recipe {} output full: {} (Need space for {})", recipe.getId(), resId, amount);
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
            ResolvedResource resolved = resolveDynamicAmount(input.resourceId, input.amountExpression);
            String resourceId = resolved.id();
            float amount = resolved.amount();

            if (input.resourceId.contains("*") || input.amountExpression.contains("*")) { // Log only dynamic
                                                                                          // consumption
                LOGGER.info("DEBUG: Consuming {} for {}: Pop={}, Expr={}, Calc={}",
                        resourceId, recipe.getId(), town.getPopulation(), input.amountExpression, amount);
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
            ResolvedResource resolved = resolveDynamicAmount(output.resourceId, output.amountExpression);
            String resId = resolved.id();
            float amount = resolved.amount();

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
            else if (target.equals("surplus")) {
                // General surplus condition: target="surplus", value="resource_id"
                String resourceId = valStr;

                float prod = getProductionRate(resourceId);
                float cons = getConsumptionRate(resourceId);

                boolean pass = prod > cons;
                if (!pass)
                    return false;
                continue;
            } else {
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
            progressTag.getAllKeys().forEach(key -> recipeProgress.put(key, progressTag.getFloat(key)));
        }
    }

    public float getProductionRate(String resourceId) {
        float totalPerDay = 0f;
        for (ProductionRecipe recipe : ProductionRegistry.getAll()) {
            // Check if unlocked
            if (town.getUpgrades().getModifier(recipe.getId()) <= 0)
                continue;

            // Check outputs for resource
            for (ResourceAmount output : recipe.getOutputs()) {
                ResolvedResource resolvedIndex = resolveDynamicAmount(output.resourceId, "0"); // Only ID matters here
                if (resolvedIndex.id().equals(resourceId)) {
                    float cycleTime = getEffectiveCycleTime(recipe);
                    if (cycleTime > 0) {
                        ResolvedResource resolvedAmt = resolveDynamicAmount(output.resourceId, output.amountExpression);
                        totalPerDay += (resolvedAmt.amount() / cycleTime);
                    }
                }
            }
        }
        return totalPerDay;
    }

    public float getConsumptionRate(String resourceId) {
        float totalPerDay = 0f;
        for (ProductionRecipe recipe : ProductionRegistry.getAll()) {
            // Check if unlocked
            if (town.getUpgrades().getModifier(recipe.getId()) <= 0)
                continue;

            // Check inputs for resource
            for (ResourceAmount input : recipe.getInputs()) {
                float amount = 0f;
                // Resolve dynamic input
                ResolvedResource resolved = resolveDynamicAmount(input.resourceId, input.amountExpression);

                // Check direct match
                if (resolved.id().equals(resourceId)) {
                    amount = resolved.amount();
                }

                if (amount > 0) {
                    float cycleTime = getEffectiveCycleTime(recipe);
                    if (cycleTime > 0) {
                        totalPerDay += (amount / cycleTime);
                    }
                }
            }
        }
        return totalPerDay;
    }

    public Map<String, Float> getActiveRecipes() {
        Map<String, Float> percentages = new HashMap<>();
        for (Map.Entry<String, Float> entry : recipeProgress.entrySet()) {
            String id = entry.getKey();
            float current = entry.getValue();
            ProductionRecipe recipe = ProductionRegistry.get(id);
            if (recipe != null) {
                float effective = getEffectiveCycleTime(recipe);
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

                // User logic: 100% base happiness at "min stock level" (e.g. 60% of cap)
                float minStockPct = com.quackers29.businesscraft.config.ConfigLoader.minStockPercent / 100.0f;
                // Safety clamp to avoid divide by zero if user sets 0%
                if (minStockPct < 0.01f)
                    minStockPct = 0.01f;

                float minStockLevel = cap * minStockPct;

                float ratio = current / minStockLevel;
                float baseHappiness = ratio * 50.0f;

                // Clamp max base to 50 (reached at minStockLevel)
                if (baseHappiness > 50.0f)
                    baseHappiness = 50.0f;

                town.setHappiness(baseHappiness);
            }
        }
    }

    private float getEffectiveCycleTime(ProductionRecipe recipe) {
        float baseTime = recipe.getBaseCycleTimeDays();
        float modifier = town.getUpgrades().getModifier(recipe.getId());

        // Modifier acts as speed multiplier.
        // 1.0 = Base Speed
        // 2.0 = Double Speed (Half Time)
        if (modifier <= 0.0001f)
            return Float.MAX_VALUE; // Effectively stopped

        return baseTime / modifier;
    }
}
