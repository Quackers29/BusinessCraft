package com.quackers29.businesscraft.production;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.data.parsers.Condition;
import com.quackers29.businesscraft.data.parsers.DataParser;
import com.quackers29.businesscraft.data.parsers.DataParser.ResourceAmount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
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
        Path configDir = PlatformAccess.platform.getConfigDirectory();
        File configFile = configDir.resolve("businesscraft").resolve(CONFIG_FILE_NAME).toFile();

        if (!configFile.exists()) {
            // Create parent directory if needed
            configFile.getParentFile().mkdirs();
            createDefaultConfig(configFile);
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                // prod_id,display_name,base_cycle_time_minutes,inputs,outputs,conditions
                // Note: The plan had "inputs,outputs" earlier, but let's check parsing.
                // Plan: prod_id,display_name,base_cycle_time_days,inputs,outputs
                // And inputs contains conditions?
                // Plan text: "inputs and outputs: packed semicolon-separated list... Condition
                // checks: happiness:>60... Recipe runs only if all inputs/conditions
                // satisfied."
                // Wait, listing conditions in "inputs" column?
                // Plan example: "population_growth... inputs: happiness:>60;pop:<pop_cap...
                // outputs: population:1"
                // So yes, conditions are mixed in with inputs in the plan's example.
                // However, my `ProductionRecipe` class separates them.
                // I should parse the 'inputs' column and split into Resources and Conditions.

                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1); // Split by comma but ignore
                                                                                      // quotes, preserve trailing
                                                                                      // empties
                // Simple split might suffice if no
                // commas in values.
                // CSVs usually imply simple split if no quotes used.
                // Let's assume simple split for now, but packed strings use ; and : so commas
                // are safe separators for columns.

                if (parts.length >= 5) {
                    String id = parts[0].trim();
                    String displayName = parts[1].trim();

                    float cycleTime = 1.0f;
                    try {
                        cycleTime = Float.parseFloat(parts[2].trim());
                    } catch (NumberFormatException e) {
                        LOGGER.error("Invalid cycle time for {}: {}", id, parts[2]);
                    }

                    String inputsRaw = parts[3].trim();
                    String outputsRaw = parts[4].trim();

                    // Parse Mixed Inputs (Resources + Conditions)
                    List<ResourceAmount> inputs = new java.util.ArrayList<>();
                    List<Condition> conditions = new java.util.ArrayList<>();
                    List<String> globalModifiers = new java.util.ArrayList<>(); // e.g. "min", "excess", "10%"

                    String[] inputParts = inputsRaw.split(";");

                    // Pass 1: Categorize parts
                    for (String part : inputParts) {
                        part = part.trim();
                        if (part.isEmpty())
                            continue;

                        // Check for conditions (explicit operators)
                        if (part.contains(">") || part.contains("<") || part.contains("=")) {
                            conditions.addAll(DataParser.parseConditions(part));
                            continue;
                        }

                        // Check for key:value pair (Input or specific condition)
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
                        } else {
                            // No colon, no operators -> Global Modifier?
                            // e.g. "min", "excess", "10%"
                            // Or legacy "wood" (invalid)?
                            // Valid global modifiers: "min", "excess" (case insensitive?), or ends with "%"
                            if (part.equalsIgnoreCase("min") || part.equalsIgnoreCase("excess") || part.endsWith("%")) {
                                globalModifiers.add(part);
                            } else {
                                LOGGER.warn("Invalid input token in {}: {}", id, part);
                            }
                        }
                    }

                    // Pass 2: Apply global modifiers to all inputs
                    for (String mod : globalModifiers) {
                        for (ResourceAmount input : inputs) {
                            // Generate condition: inputId > mod
                            // e.g. "wood:25;min" -> Condition(wood, >, min)
                            // We use ">" as implied operator for these threshold checks
                            // Or should it be ">" for min/excess/percentage?
                            // User said "wood:>min". So implicit operator is likely ">".
                            // For "excess", implies we have excess, so > excess_threshold.
                            // For "10%", implies > 10% cap.

                            // Note: If user wants <, they must specify explicitly e.g. wood:<10%
                            // The global shortcut implies "require at least this".
                            conditions.add(new Condition(input.resourceId, ">", mod, false));
                        }
                    }

                    List<ResourceAmount> outputs = DataParser.parseResources(outputsRaw);

                    RECIPES.put(id,
                            new ProductionRecipe(id, displayName, cycleTime, inputs, outputs, conditions));
                    LOGGER.info("Registered production: {} (Time: {})", id, cycleTime);
                }
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

    private static void createDefaultConfig(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("prod_id,display_name,base_cycle_time_minutes,inputs,outputs\n");
            writer.write("population_maintenance,Food Consumption,1,pop*food:1,\n");
            writer.write("population_growth,Natural Population Growth,10,happiness:>60;pop:<pop_cap,population:1\n");
            writer.write("basic_farming,Basic Wheat Farming,1,,food:4\n");
            writer.write("wood_to_planks,Wood to Planks,0.5,wood:4,planks:16\n");
        } catch (IOException e) {
            LOGGER.error("Failed to create default {}", CONFIG_FILE_NAME, e);
        }
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
