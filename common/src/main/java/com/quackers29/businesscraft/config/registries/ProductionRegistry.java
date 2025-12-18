package com.quackers29.businesscraft.config.registries;

import com.quackers29.businesscraft.api.PlatformAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductionRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductionRegistry.class);
    private static final Map<String, ProductionRecipe> REGISTRY = new HashMap<>();
    private static final String FILE_NAME = "productions.csv";

    public static class ProductionRecipe {
        public final String id;
        public final String displayName;
        public final double baseCycleTimeDays;
        public final List<String> inputs; // Raw strings to be parsed at runtime (e.g. "wood:4", "pop*food:1",
                                          // "happiness:>60")
        public final List<String> outputs;

        public ProductionRecipe(String id, String displayName, double baseCycleTimeDays, List<String> inputs,
                List<String> outputs) {
            this.id = id;
            this.displayName = displayName;
            this.baseCycleTimeDays = baseCycleTimeDays;
            this.inputs = inputs;
            this.outputs = outputs;
        }
    }

    public static void load() {
        REGISTRY.clear();
        try {
            Path configDir = PlatformAccess.platform.getConfigDirectory().resolve("businesscraft");
            File file = configDir.resolve(FILE_NAME).toFile();

            if (!file.exists()) {
                createDefault(file);
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#") || line.startsWith("prod_id"))
                        continue;

                    // Parse CSV line, handling quoted strings if necessary (simple split for now as
                    // per plan)
                    String[] parts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                    if (parts.length >= 5) {
                        String id = parts[0].trim();
                        String name = parts[1].trim();
                        double cycleTime = 1.0;
                        try {
                            cycleTime = Double.parseDouble(parts[2].trim());
                        } catch (NumberFormatException e) {
                            LOGGER.warn("Invalid cycle time for production {}: {}", id, parts[2]);
                        }

                        List<String> inputs = parseList(parts[3]);
                        List<String> outputs = parseList(parts[4]);

                        REGISTRY.put(id, new ProductionRecipe(id, name, cycleTime, inputs, outputs));
                    }
                }
            }
            LOGGER.info("Loaded {} production recipes from {}", REGISTRY.size(), FILE_NAME);
        } catch (Exception e) {
            LOGGER.error("Failed to load {}", FILE_NAME, e);
        }
    }

    private static List<String> parseList(String raw) {
        List<String> list = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty())
            return list;

        // Split by semicolon
        String[] items = raw.split(";");
        for (String item : items) {
            if (!item.trim().isEmpty()) {
                list.add(item.trim());
            }
        }
        return list;
    }

    private static void createDefault(File file) {
        try {
            if (!file.getParentFile().exists())
                file.getParentFile().mkdirs();

            try (FileWriter writer = new FileWriter(file)) {
                writer.write("prod_id,display_name,base_cycle_time_days,inputs,outputs\n");
                writer.write("population_maintenance,Food Consumption,1,pop*food:1,\n");
                writer.write(
                        "population_growth,Natural Population Growth,10,happiness:>60;pop:<pop_cap,population:1\n");
                writer.write("basic_farming,Basic Wheat Farming,1,,food:4\n");
                writer.write("advanced_farming,Advanced Wheat Farming,1,,food:8\n");
                writer.write("wood_to_planks,Wood to Planks,0.5,wood:4,planks:16\n");
                writer.write("passive_mining,Passive Stone Mining,1,,stone:5\n");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create default {}", FILE_NAME, e);
        }
    }

    public static ProductionRecipe get(String id) {
        return REGISTRY.get(id);
    }

    public static java.util.Collection<ProductionRecipe> getAll() {
        return REGISTRY.values();
    }
}
