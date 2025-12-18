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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpgradeRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeRegistry.class);
    private static final Map<String, UpgradeNode> REGISTRY = new HashMap<>();
    private static final String FILE_NAME = "upgrades.csv";

    public static class UpgradeNode {
        public final String id;
        public final String category;
        public final String displayName;
        public final List<String> prereqNodes;
        public final String description;
        public final Map<String, String> effects; // Target -> Value (string to be parsed later, e.g. "20", "15%")

        public UpgradeNode(String id, String category, String displayName, List<String> prereqNodes, String description,
                Map<String, String> effects) {
            this.id = id;
            this.category = category;
            this.displayName = displayName;
            this.prereqNodes = prereqNodes;
            this.description = description;
            this.effects = effects;
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
                    if (line.isEmpty() || line.startsWith("#") || line.startsWith("node_id"))
                        continue;

                    String[] parts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                    if (parts.length >= 6) {
                        String id = parts[0].trim();
                        String category = parts[1].trim();
                        String name = parts[2].trim();
                        List<String> prereqs = parseList(parts[3]);
                        String desc = parts[4].trim();
                        Map<String, String> effects = parseEffects(parts[5]);

                        REGISTRY.put(id, new UpgradeNode(id, category, name, prereqs, desc, effects));
                    }
                }
            }
            LOGGER.info("Loaded {} upgrades from {}", REGISTRY.size(), FILE_NAME);
        } catch (Exception e) {
            LOGGER.error("Failed to load {}", FILE_NAME, e);
        }
    }

    private static List<String> parseList(String raw) {
        List<String> list = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty())
            return list;
        for (String item : raw.split(";")) {
            if (!item.trim().isEmpty())
                list.add(item.trim());
        }
        return list;
    }

    private static Map<String, String> parseEffects(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.trim().isEmpty())
            return map;

        for (String item : raw.split(";")) {
            item = item.trim();
            if (item.isEmpty())
                continue;

            // Format: target:value OR target (implies unlock)
            if (item.contains(":")) {
                String[] parts = item.split(":", 2);
                map.put(parts[0].trim(), parts[1].trim());
            } else {
                // Unlocks a production or simple flag
                map.put(item, "unlock");
            }
        }
        return map;
    }

    private static void createDefault(File file) {
        try {
            if (!file.getParentFile().exists())
                file.getParentFile().mkdirs();

            try (FileWriter writer = new FileWriter(file)) {
                writer.write("node_id,category,display_name,prereq_nodes,benefit_description,effects\n");
                writer.write(
                        "basic_settlement,housing,Basic Settlement,,,Starter housing logic,pop_cap:10;storage_cap_all:200;happiness:50;population_maintenance;population_growth\n");
                writer.write(
                        "farming_basic,farming,Basic Farming,basic_settlement,,Access to basic farming,basic_farming\n");
                writer.write(
                        "farming_improved,farming,Improved Irrigation,farming_basic,,More food output,basic_farming-output:20%;pop_cap:5\n");
                writer.write("wood_processing,wood,Sawmill,basic_settlement,,Processing wood logs,wood_to_planks\n");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create default {}", FILE_NAME, e);
        }
    }

    public static UpgradeNode get(String id) {
        return REGISTRY.get(id);
    }

    public static java.util.Collection<UpgradeNode> getAll() {
        return REGISTRY.values();
    }
}
