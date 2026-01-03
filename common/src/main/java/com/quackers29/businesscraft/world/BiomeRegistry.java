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
    // Map of BiomeID -> List of Variants
    private static final Map<String, List<BiomeKit>> GEN_KITS = new HashMap<>();
    // Global variants applied to all biomes
    private static final List<BiomeKit> GLOBAL_KITS = new ArrayList<>();

    private static final String CONFIG_FILE_NAME = "biomes.csv";

    public static class BiomeKit {
        public List<String> biomeIds; // List of IDs this variant applies to
        public String variantId;
        public String variantName;
        public int weight;
        public List<String> startingNodes;
        public Map<String, Float> startingValues;
        public String description;

        public BiomeKit(List<String> biomeIds, String variantId, String variantName, int weight,
                List<String> startingNodes, Map<String, Float> startingValues, String description) {
            this.biomeIds = biomeIds;
            this.variantId = variantId;
            this.variantName = variantName;
            this.weight = weight;
            this.startingNodes = startingNodes;
            this.startingValues = startingValues;
            this.description = description;
        }
    }

    public static void load() {
        GEN_KITS.clear();
        GLOBAL_KITS.clear();

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
                    continue; // Skip header
                }

                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue; // Skip comments/empty

                // Parse CSV - simplified split (assuming no commas in descriptions/values for
                // now,
                // but user used quotes in description. A proper CSV parser is better,
                // but we can start with simple split if we respect the format).
                // User format:
                // biome_ids,variant_id,variant_name,weight,starting_nodes,starting_values,description

                // We'll use a regex split that respects quotes or just basic split if
                // description is last.
                // Since description is last and quoted, we can limit split.
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                if (parts.length >= 7) {
                    String biomeIdsRaw = parts[0].trim();
                    String variantId = parts[1].trim();
                    String variantName = parts[2].trim();
                    String desc = parts[3].trim().replace("\"", "");
                    int weight = parseWeight(parts[4].trim());
                    String nodesRaw = parts[5].trim();
                    String valuesRaw = parts[6].trim();

                    List<String> biomeIds = new ArrayList<>();
                    if (biomeIdsRaw.equals("*")) {
                        biomeIds.add("*");
                    } else {
                        Collections.addAll(biomeIds, biomeIdsRaw.split(";"));
                    }

                    List<String> startingNodes = new ArrayList<>();
                    if (!nodesRaw.isEmpty()) {
                        for (String n : nodesRaw.split(";")) {
                            if (!n.trim().isEmpty())
                                startingNodes.add(n.trim());
                        }
                    }

                    Map<String, Float> startingValues = new HashMap<>();
                    List<ResourceAmount> valueList = DataParser.parseResources(valuesRaw);
                    for (ResourceAmount ra : valueList) {
                        startingValues.put(ra.resourceId, ra.amount);
                    }

                    BiomeKit kit = new BiomeKit(biomeIds, variantId, variantName, weight, startingNodes, startingValues,
                            desc);

                    if (biomeIds.contains("*")) {
                        GLOBAL_KITS.add(kit);
                        LOGGER.info("Registered GLOBAL variant: {} ({})", variantId, variantName);
                    } else {
                        for (String bid : biomeIds) {
                            String cleanedId = bid.trim();
                            GEN_KITS.computeIfAbsent(cleanedId, k -> new ArrayList<>()).add(kit);
                        }
                        LOGGER.info("Registered variant {} for biomes: {}", variantId, biomeIds);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load {}", CONFIG_FILE_NAME, e);
        }
    }

    private static int parseWeight(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    private static void createDefaultConfig(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("biome_ids,variant_id,variant_name,weight,starting_nodes,starting_values,description\n");
            writer.write(
                    "minecraft:plains,farming,Farming Village,60,basic_settlement;farming_basic,population:6;food:140;wood:70;money:60;happiness:72,\"Strong farming\"\n");
            // ... truncated defaults
        } catch (IOException e) {
            LOGGER.error("Failed to create default {}", CONFIG_FILE_NAME, e);
        }
    }

    /**
     * returns a weighted random BiomeKit for the given biome
     */
    public static BiomeKit get(String biomeId) {
        List<BiomeKit> candidates = new ArrayList<>();

        // 1. Specific variants
        if (GEN_KITS.containsKey(biomeId)) {
            candidates.addAll(GEN_KITS.get(biomeId));
        }

        // 2. Global variants
        candidates.addAll(GLOBAL_KITS);

        if (candidates.isEmpty()) {
            return null;
        }

        // Weighted Selection
        int totalWeight = 0;
        for (BiomeKit kit : candidates) {
            totalWeight += kit.weight;
        }

        int roll = new Random().nextInt(totalWeight);
        int current = 0;

        for (BiomeKit kit : candidates) {
            current += kit.weight;
            if (roll < current) {
                LOGGER.info("BiomeRegistry selected variant '{}' for biome '{}' (Roll: {}/{})", kit.variantName,
                        biomeId, roll, totalWeight);
                return kit;
            }
        }

        return candidates.get(0); // Fallback
    }
}
