package com.quackers29.businesscraft.world;

import com.quackers29.businesscraft.api.PlatformAccess;
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
import java.util.*;

public class BiomeRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(BiomeRegistry.class);
    private static final Map<String, BiomeKit> KITS = new HashMap<>(); // Keyed by Biome ID (e.g. "minecraft:plains")
    private static final String CONFIG_FILE_NAME = "biomes.csv";

    public static class BiomeKit {
        public String biomeId;
        public List<String> startingNodes;
        public Map<String, Float> startingValues;

        public BiomeKit(String biomeId, List<String> startingNodes, Map<String, Float> startingValues) {
            this.biomeId = biomeId;
            this.startingNodes = startingNodes;
            this.startingValues = startingValues;
        }
    }

    public static void load() {
        KITS.clear();
        Path configDir = PlatformAccess.platform.getConfigDirectory();
        File configFile = configDir.resolve("businesscraft").resolve(CONFIG_FILE_NAME).toFile();

        if (!configFile.exists()) {
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

                // biome_id,starting_nodes,starting_values
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String biomeId = parts[0].trim();
                    String nodesRaw = parts[1].trim();
                    String valuesRaw = parts[2].trim();

                    List<String> startingNodes = new ArrayList<>();
                    if (!nodesRaw.isEmpty()) {
                        for (String n : nodesRaw.split(";")) {
                            if (!n.trim().isEmpty())
                                startingNodes.add(n.trim());
                        }
                    }

                    Map<String, Float> startingValues = new HashMap<>();
                    // starting_values format: packed key:amount list.
                    // Uses DataParser.parseResources which returns List<ResourceAmount>
                    List<ResourceAmount> valueList = DataParser.parseResources(valuesRaw);
                    for (ResourceAmount ra : valueList) {
                        startingValues.put(ra.resourceId, ra.amount);
                    }

                    KITS.put(biomeId, new BiomeKit(biomeId, startingNodes, startingValues));
                    LOGGER.info("Registered biome kit for: {}", biomeId);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load {}", CONFIG_FILE_NAME, e);
        }
    }

    private static void createDefaultConfig(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("biome_id,starting_nodes,starting_values\n");
            writer.write(
                    "minecraft:plains,basic_settlement;farming_basic,population:5;food:120;wood:60;money:60;happiness:70\n");
            writer.write(
                    "minecraft:forest,basic_settlement;wood_processing,population:8;food:80;wood:150;money:50;happiness:65\n");
        } catch (IOException e) {
            LOGGER.error("Failed to create default {}", CONFIG_FILE_NAME, e);
        }
    }

    public static BiomeKit get(String biomeId) {
        return KITS.get(biomeId);
    }
    // Fallback?
}
