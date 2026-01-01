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

                // prod_id,display_name,base_cycle_time_days,inputs,outputs,conditions
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

                    float dayTime = 1.0f;
                    try {
                        dayTime = Float.parseFloat(parts[2].trim());
                    } catch (NumberFormatException e) {
                        LOGGER.error("Invalid cycle time for {}: {}", id, parts[2]);
                    }

                    String inputsRaw = parts[3].trim();
                    String outputsRaw = parts[4].trim();

                    // Parse Mixed Inputs (Resources + Conditions)
                    List<ResourceAmount> inputs = new java.util.ArrayList<>();
                    List<Condition> conditions = new java.util.ArrayList<>();

                    String[] inputParts = inputsRaw.split(";");
                    for (String part : inputParts) {
                        part = part.trim();
                        if (part.isEmpty())
                            continue;

                        // Check for operators
                        if (part.contains(">") || part.contains("<") || part.contains("=")) {
                            // It's a condition
                            conditions.addAll(DataParser.parseConditions(part));
                            continue;
                        }

                        // Try to disambiguate Resource vs Condition
                        // Syntax: key:value
                        String[] kv = part.split(":");
                        if (kv.length == 2) {
                            String key = kv[0].trim();
                            String val = kv[1].trim();

                            // Known condition keys
                            if (key.equals("happiness") || key.equals("pop") || key.equals("surplus")) {
                                conditions.addAll(DataParser.parseConditions(part));
                            } else {
                                // Assume Resource with potentially dynamic amount
                                inputs.add(new ResourceAmount(key, val));
                            }
                        } else {
                            // Non-standard format, try condition parser
                            conditions.addAll(DataParser.parseConditions(part));
                        }
                    }

                    List<ResourceAmount> outputs = DataParser.parseResources(outputsRaw);

                    RECIPES.put(id,
                            new ProductionRecipe(id, displayName, dayTime, inputs, outputs, conditions));
                    LOGGER.info("Registered production: {} (Time: {})", id, dayTime);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load {}", CONFIG_FILE_NAME, e);
        }
    }

    private static void createDefaultConfig(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("prod_id,display_name,base_cycle_time_days,inputs,outputs\n");
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
}
