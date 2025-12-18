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
import java.util.HashMap;
import java.util.Map;

public class UpgradeRequirementRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeRequirementRegistry.class);
    private static final Map<String, RequirementEntry> REGISTRY = new HashMap<>();
    private static final String FILE_NAME = "upgrade_requirements.csv";

    public static class RequirementEntry {
        public final String nodeId;
        public final int researchDays;
        public final Map<String, Integer> requiredItems;

        public RequirementEntry(String nodeId, int researchDays, Map<String, Integer> requiredItems) {
            this.nodeId = nodeId;
            this.researchDays = researchDays;
            this.requiredItems = requiredItems;
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

                    if (parts.length >= 3) {
                        String id = parts[0].trim();
                        int days = 0;
                        try {
                            days = Integer.parseInt(parts[1].trim());
                        } catch (NumberFormatException e) {
                            LOGGER.warn("Invalid research days: {}", parts[1]);
                        }

                        Map<String, Integer> costs = parseCosts(parts[2]);
                        REGISTRY.put(id, new RequirementEntry(id, days, costs));
                    }
                }
            }
            LOGGER.info("Loaded {} upgrade requirements from {}", REGISTRY.size(), FILE_NAME);
        } catch (Exception e) {
            LOGGER.error("Failed to load {}", FILE_NAME, e);
        }
    }

    private static Map<String, Integer> parseCosts(String raw) {
        Map<String, Integer> map = new HashMap<>();
        if (raw == null || raw.trim().isEmpty())
            return map;

        for (String item : raw.split(";")) {
            String[] parts = item.split(":");
            if (parts.length == 2) {
                try {
                    map.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                } catch (NumberFormatException e) {
                    LOGGER.warn("Invalid cost format: {}", item);
                }
            }
        }
        return map;
    }

    private static void createDefault(File file) {
        try {
            if (!file.getParentFile().exists())
                file.getParentFile().mkdirs();

            try (FileWriter writer = new FileWriter(file)) {
                writer.write("node_id,research_days,required_items\n");
                writer.write("basic_settlement,0,\n");
                writer.write("farming_improved,7,food:20;wood:20\n");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create default {}", FILE_NAME, e);
        }
    }

    public static RequirementEntry get(String nodeId) {
        return REGISTRY.get(nodeId);
    }
}
