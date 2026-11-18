package com.quackers29.businesscraft.production;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.config.ConfigWarnings;
import com.quackers29.businesscraft.config.CsvConfig;
import com.quackers29.businesscraft.data.parsers.Condition;
import com.quackers29.businesscraft.data.parsers.DataParser;
import com.quackers29.businesscraft.data.parsers.DataParser.ResourceAmount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductionRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductionRegistry.class);
    private static final Map<String, ProductionRecipe> RECIPES = new HashMap<>();
    private static final Map<String, Float> ESTIMATED_VALUES = new HashMap<>(); // Base price estimation
    private static final Map<String, Float> EFFORT_VALUES = new HashMap<>(); // Raw effort (min/unit)
    private static final String CONFIG_FILE_NAME = "productions.csv";

    public static void load() {
        RECIPES.clear();
        File configFile = CsvConfig.ensureInstalled(PlatformAccess.platform.getConfigDirectory(), CONFIG_FILE_NAME);

        try {
            // prod_id,display_name,base_cycle_time_minutes,inputs,outputs
            // The inputs column mixes resources (wood:25), conditions (happiness:>60) and global
            // modifiers (min, excess, 10%) that apply to every resource input of the row.
            for (CsvConfig.Row row : CsvConfig.readRows(configFile)) {
                String[] parts = row.columns();
                if (parts.length < 5) {
                    ConfigWarnings.warn(CONFIG_FILE_NAME, row.lineNumber(),
                            "expected 5 columns, found " + parts.length + "; row skipped");
                    continue;
                }

                String id = parts[0];
                String displayName = parts[1];
                if (id.isEmpty()) {
                    ConfigWarnings.warn(CONFIG_FILE_NAME, row.lineNumber(), "empty prod_id; row skipped");
                    continue;
                }
                if (RECIPES.containsKey(id)) {
                    ConfigWarnings.warn(CONFIG_FILE_NAME, row.lineNumber(),
                            "duplicate prod_id '" + id + "'; later row replaces the earlier one");
                }

                float cycleTime = 1.0f;
                try {
                    cycleTime = Float.parseFloat(parts[2]);
                } catch (NumberFormatException e) {
                    ConfigWarnings.warn(CONFIG_FILE_NAME, row.lineNumber(),
                            "invalid cycle time '" + parts[2] + "' for " + id + "; using " + cycleTime);
                }
                if (cycleTime <= 0) {
                    ConfigWarnings.warn(CONFIG_FILE_NAME, row.lineNumber(),
                            "cycle time of " + cycleTime + " for " + id + " must be above 0");
                }

                String inputsRaw = parts[3];
                String outputsRaw = parts[4];

                List<ResourceAmount> inputs = new ArrayList<>();
                List<Condition> conditions = new ArrayList<>();
                List<String> globalModifiers = new ArrayList<>(); // e.g. "min", "excess", "10%"

                for (String part : inputsRaw.split(";")) {
                    part = part.trim();
                    if (part.isEmpty())
                        continue;

                    // Explicit operators are conditions
                    if (part.contains(">") || part.contains("<") || part.contains("=")) {
                        conditions.addAll(DataParser.parseConditions(part));
                        continue;
                    }

                    if (part.contains(":")) {
                        String[] kv = part.split(":");
                        if (kv.length == 2) {
                            String key = kv[0].trim();
                            String val = kv[1].trim();
                            if (key.equals("happiness") || key.equals("pop") || key.equals("surplus")) {
                                conditions.addAll(DataParser.parseConditions(part));
                            } else {
                                inputs.add(new ResourceAmount(key, val));
                            }
                        } else {
                            conditions.addAll(DataParser.parseConditions(part));
                        }
                    } else if (part.equalsIgnoreCase("min") || part.equalsIgnoreCase("excess")
                            || part.endsWith("%")) {
                        globalModifiers.add(part);
                    } else {
                        ConfigWarnings.warn(CONFIG_FILE_NAME, row.lineNumber(),
                                "invalid input token '" + part + "' in " + id);
                    }
                }

                // A global modifier ("wood:25;min") means every resource input must be above it.
                for (String mod : globalModifiers) {
                    for (ResourceAmount input : inputs) {
                        conditions.add(new Condition(input.resourceId, ">", mod, false));
                    }
                }

                List<ResourceAmount> outputs = DataParser.parseResources(outputsRaw);
                long wanted = java.util.Arrays.stream(outputsRaw.split(";")).filter(t -> !t.trim().isEmpty()).count();
                if (outputs.size() != wanted) {
                    ConfigWarnings.warn(CONFIG_FILE_NAME, row.lineNumber(),
                            "outputs '" + outputsRaw + "' of " + id + " are not all in resource:amount form");
                }

                RECIPES.put(id, new ProductionRecipe(id, displayName, cycleTime, inputs, outputs, conditions));
                LOGGER.info("Registered production: {} (Time: {})", id, cycleTime);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load {}", CONFIG_FILE_NAME, e);
        }

        calculateEstimatedValues();
    }

    private static void calculateEstimatedValues() {
        ESTIMATED_VALUES.clear();
        EFFORT_VALUES.clear();

        // Build reverse lookup: Resource -> producing recipes
        Map<String, java.util.List<ProductionRecipe>> producers = new HashMap<>();
        for (ProductionRecipe recipe : RECIPES.values()) {
            for (ResourceAmount output : recipe.getOutputs()) {
                producers.computeIfAbsent(output.resourceId, k -> new java.util.ArrayList<>()).add(recipe);
            }
        }

        // Cache for recursion
        Map<String, Float> effortCache = new HashMap<>();
        java.util.Set<String> visiting = new java.util.HashSet<>();

        // Calculate for all known outputs
        for (String resourceId : producers.keySet()) {
            float effort = recursiveGetEffort(resourceId, producers, effortCache, visiting);
            float estimatedPrice = 20.0f * effort;
            ESTIMATED_VALUES.put(resourceId, estimatedPrice);
            EFFORT_VALUES.put(resourceId, effort);

            if (com.quackers29.businesscraft.debug.DebugConfig
                    .isEnabled(com.quackers29.businesscraft.debug.DebugConfig.SMART_GPI_DEBUG)) {
                LOGGER.info("[SmartGPI] Calculated for {}: Effort={} min/unit, EstPrice={}, Formula=Cycle+Inputs/Yield",
                        resourceId, effort, estimatedPrice);
            }
        }
    }

    private static float recursiveGetEffort(String resourceId,
            Map<String, java.util.List<ProductionRecipe>> producers,
            Map<String, Float> cache,
            java.util.Set<String> visiting) {

        if (cache.containsKey(resourceId))
            return cache.get(resourceId);
        if (visiting.contains(resourceId))
            return Float.MAX_VALUE; // Cycle detected

        List<ProductionRecipe> recipes = producers.get(resourceId);
        if (recipes == null || recipes.isEmpty()) {
            // No recipe -> Base resource or Input
            // If it's something like "pop" or "happiness", cost is 0?
            // If it's "wood" but no woodcutting recipe yet?? (User said woodcutting exists)
            // Let's assume 0 effort for base inputs like "population" or infinite
            // resources?
            // Or maybe a small default?
            return 0.0f;
        }

        visiting.add(resourceId);
        float minEffort = Float.MAX_VALUE;

        for (ProductionRecipe recipe : recipes) {
            float cycleTime = recipe.getBaseCycleTimeMinutes();
            float inputsCost = 0;
            boolean invalid = false;

            for (ResourceAmount input : recipe.getInputs()) {
                float quantity = resolveQuantity(input);
                if (quantity > 0) {
                    float inputEffort = recursiveGetEffort(input.resourceId, producers, cache, visiting);
                    if (inputEffort == Float.MAX_VALUE) {
                        // Cycle or impossible dependency
                        invalid = true;
                        break;
                    }
                    inputsCost += quantity * inputEffort;
                }
            }

            if (invalid)
                continue;

            float totalCost = cycleTime + inputsCost;

            // Find output quantity for this resource
            float outputQty = 1.0f; // Default
            for (ResourceAmount out : recipe.getOutputs()) {
                if (out.resourceId.equals(resourceId)) {
                    outputQty = resolveQuantity(out);
                    break;
                }
            }

            if (outputQty <= 0)
                outputQty = 1.0f; // Avoid div by zero

            float unitEffort = totalCost / outputQty;
            if (unitEffort < minEffort) {
                minEffort = unitEffort;
            }
        }

        visiting.remove(resourceId);

        if (minEffort == Float.MAX_VALUE)
            minEffort = 1.0f; // Fallback if all paths failed

        cache.put(resourceId, minEffort);
        return minEffort;
    }

    private static float resolveQuantity(ResourceAmount ra) {
        float q = ra.amount;
        // If quantity is dynamic (0), parse expression heuristic
        if (q <= 0.0001f && ra.amountExpression != null && !ra.amountExpression.isEmpty()) {
            // Heuristic: "1*pop" -> 1
            String valStr = ra.amountExpression.split("[^0-9.]")[0];
            try {
                if (!valStr.isEmpty()) {
                    q = Float.parseFloat(valStr);
                }
            } catch (Exception e) {
            }

            if (q <= 0.0001f)
                q = 1.0f;
        }
        return q;
    }

    public static ProductionRecipe get(String id) {
        return RECIPES.get(id);
    }

    public static Collection<ProductionRecipe> getAll() {
        return RECIPES.values();
    }

    public static float getEstimatedValue(String resourceId) {
        return ESTIMATED_VALUES.getOrDefault(resourceId, 1.0f);
    }

    public static float getEffort(String resourceId) {
        return EFFORT_VALUES.getOrDefault(resourceId, 1.0f);
    }

    public static Map<String, Float> getAllEstimatedValues() {
        return java.util.Collections.unmodifiableMap(ESTIMATED_VALUES);
    }
}
