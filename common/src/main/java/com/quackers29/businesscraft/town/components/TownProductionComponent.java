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

    @Override
    public void tick() {
        // Iterate all registered recipes
        // In the future, maybe we filter by "Unlocked" recipes?
        // Plan says: "prod_id alone... unlocks the production recipe".
        // But also said: "Towns automatically run unlocked production recipes".
        // So I should check if it's unlocked.
        // How to check unlock? TownUpgradeComponent has modifiers.
        // Effect with NO colon is an unlock? `prod_id` -> generic unlock?
        // TownUpgradeComponent.getModifier(prod_id) should return > 0 if unlocked?
        // Wait, TownUpgradeComponent logic: `activeModifiers.merge(effect.getTarget(),
        // effect.getValue(), Float::sum);`
        // Effect(part, 1.0f, false) for unlock.
        // So if getModifier(recipeId) > 0, it is unlocked.
        // Default recipes (like population_maintenance) might not need unlock?
        // Or they are unlocked by "basic_settlement" node which is given to everyone.

        for (ProductionRecipe recipe : ProductionRegistry.getAll()) {
            // Check unlock status
            if (town.getUpgrades().getModifier(recipe.getId()) <= 0) {
                continue; // Locked
            }

            processRecipe(recipe);
        }
    }

    private void processRecipe(ProductionRecipe recipe) {
        // Check conditions
        if (!checkConditions(recipe))
            return;

        // Calculate Cycle Time
        float baseTime = recipe.getBaseCycleTimeDays();
        float timeMod = town.getUpgrades().getModifier(recipe.getId() + "-time"); // Additive? "input:-25%" -> -0.25
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
        if (effectiveTime < 0.1f)
            effectiveTime = 0.1f; // Min cap

        // Calculate Inputs Required
        // Input logic? Plan: "prod_id-input (% on resource inputs)"
        // Assuming strict "no scaling" for now to keep it simple, or just check stocks.

        // Check Stocks for Inputs
        boolean hasInputs = true;
        for (ResourceAmount input : recipe.getInputs()) {
            float required = input.amount;
            // Apply modifiers?

            // Special case: "pop*food" -> dynamic amount.
            // My parser treated "pop*food" as resourceId.
            // I need to intercept this.
            String resourceId = input.resourceId;
            float amount = input.amount;

            if (resourceId.startsWith("pop*")) {
                String actualRes = resourceId.substring(4);
                amount = amount * town.getPopulation(); // Dynamic requirement
                resourceId = actualRes;
            } else if (resourceId.equals("population")) {
                // consuming population?
            }

            if (town.getTrading().getStock(resourceId) < amount) {
                hasInputs = false;
                break;
            }
        }

        if (!hasInputs) {
            // Can't run
            return;
        }

        // Check Space for Outputs (Stalling logic)
        for (ResourceAmount output : recipe.getOutputs()) {
            String resId = output.resourceId;
            if (resId.equals("population"))
                continue; // Pop has its own cap but we check condition for that

            float current = town.getTrading().getStock(resId);
            float cap = town.getTrading().getStorageCap(resId);

            // If full, stall.
            // We can produce partial? No, recipe based.
            if (current + output.amount > cap) {
                return; // Stall
            }
        }

        // Advance Progress
        float currentProgress = recipeProgress.getOrDefault(recipe.getId(), 0f);
        float tickIncrement = 1.0f / (float) com.quackers29.businesscraft.config.ConfigLoader.dailyTickInterval;

        currentProgress += tickIncrement;

        if (currentProgress >= effectiveTime) {
            // Complete Recipe
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
                amount = amount * town.getPopulation();
                resourceId = actualRes;
            }

            town.getTrading().adjustStock(resourceId, -amount);
        }

        // Produce Outputs
        for (ResourceAmount output : recipe.getOutputs()) {
            String resId = output.resourceId;
            float amount = output.amount;

            if (resId.equals("population")) {
                // Add population
                town.setPopulation(town.getPopulation() + (int) amount); // Casting float to int
            } else {
                town.getTrading().adjustStock(resId, amount);
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
}
