package com.quackers29.businesscraft.config.registries;

import com.quackers29.businesscraft.api.PlatformAccess;
import net.minecraft.resources.ResourceLocation;
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

public class BiomeRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(BiomeRegistry.class);
    private static final Map<ResourceLocation, BiomeEntry> REGISTRY = new HashMap<>();
    private static final String FILE_NAME = "biomes.csv";

    public static class BiomeEntry {
        public final ResourceLocation biomeId;
        public final List<String> startingNodes;
        public final Map<String, Integer> startingValues; // key (pop/happiness/item) -> amount

        public BiomeEntry(ResourceLocation biomeId, List<String> startingNodes, Map<String, Integer> startingValues) {
            this.biomeId = biomeId;
            this.startingNodes = startingNodes;
            this.startingValues = startingValues;
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
                    if (line.isEmpty() || line.startsWith("#") || line.startsWith("biome_id"))
                        continue;

                    String[] parts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                    if (parts.length >= 3) {
                        try {
                            ResourceLocation id = new ResourceLocation(parts[0].trim());
                            List<String> nodes = parseList(parts[1]);
                            Map<String, Integer> values = parseValues(parts[2]);

                            REGISTRY.put(id, new BiomeEntry(id, nodes, values));
                        } catch (Exception e) {
                            LOGGER.warn("Invalid biome entry: {}", line);
                        }
                    }
                }
            }
            LOGGER.info("Loaded {} biome configs from {}", REGISTRY.size(), FILE_NAME);
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

    private static Map<String, Integer> parseValues(String raw) {
        Map<String, Integer> map = new HashMap<>();
        if (raw == null || raw.trim().isEmpty())
            return map;

        for (String item : raw.split(";")) {
            String[] parts = item.split(":");
            if (parts.length == 2) {
                try {
                    map.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                } catch (NumberFormatException e) {
                    LOGGER.warn("Invalid value format: {}", item);
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
                writer.write("biome_id,starting_nodes,starting_values\n");
                writer.write(
                        "minecraft:plains,basic_settlement;farming_basic,population:5;food:120;wood:60;money:60;happiness:70\n");
                writer.write(
                        "minecraft:forest,basic_settlement;wood_processing,population:8;food:80;wood:150;money:50;happiness:65\n");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create default {}", FILE_NAME, e);
        }
    }

    public static BiomeEntry get(ResourceLocation id) {
        return REGISTRY.get(id);
    }
}
