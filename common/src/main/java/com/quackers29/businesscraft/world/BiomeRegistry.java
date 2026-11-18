package com.quackers29.businesscraft.world;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.config.ConfigWarnings;
import com.quackers29.businesscraft.config.CsvConfig;
import com.quackers29.businesscraft.data.parsers.DataParser;
import com.quackers29.businesscraft.data.parsers.DataParser.ResourceAmount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
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

        File configFile = CsvConfig.ensureInstalled(PlatformAccess.platform.getConfigDirectory(), CONFIG_FILE_NAME);

        try {
            // biome_ids,variant_id,variant_name,description,weight,starting_nodes,starting_values
            for (CsvConfig.Row row : CsvConfig.readRows(configFile)) {
                String[] parts = row.columns();
                if (parts.length < 7) {
                    ConfigWarnings.warn(CONFIG_FILE_NAME, row.lineNumber(),
                            "expected 7 columns, found " + parts.length + "; row skipped");
                    continue;
                }

                String biomeIdsRaw = parts[0];
                String variantId = parts[1];
                String variantName = parts[2];
                String desc = parts[3];
                int weight = parseWeight(parts[4], row.lineNumber());
                String nodesRaw = parts[5];
                String valuesRaw = parts[6];

                List<String> biomeIds = new ArrayList<>();
                if (biomeIdsRaw.equals("*")) {
                    biomeIds.add("*");
                } else {
                    for (String b : biomeIdsRaw.split(";")) {
                        if (!b.trim().isEmpty())
                            biomeIds.add(b.trim());
                    }
                }

                List<String> startingNodes = new ArrayList<>();
                for (String n : nodesRaw.split(";")) {
                    if (!n.trim().isEmpty())
                        startingNodes.add(n.trim());
                }

                Map<String, Float> startingValues = new HashMap<>();
                for (ResourceAmount ra : DataParser.parseResources(valuesRaw)) {
                    startingValues.put(ra.resourceId, ra.amount);
                }

                BiomeKit kit = new BiomeKit(biomeIds, variantId, variantName, weight, startingNodes, startingValues,
                        desc);

                if (biomeIds.contains("*")) {
                    GLOBAL_KITS.add(kit);
                    LOGGER.info("Registered GLOBAL variant: {} ({})", variantId, variantName);
                } else {
                    for (String bid : biomeIds) {
                        GEN_KITS.computeIfAbsent(bid, k -> new ArrayList<>()).add(kit);
                    }
                    LOGGER.info("Registered variant {} for biomes: {}", variantId, biomeIds);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load {}", CONFIG_FILE_NAME, e);
        }
    }

    /** Every loaded kit once: biome-specific kits first, then the global ones. */
    public static List<BiomeKit> getAllKits() {
        Set<BiomeKit> seen = new LinkedHashSet<>();
        for (List<BiomeKit> kits : GEN_KITS.values()) {
            seen.addAll(kits);
        }
        seen.addAll(GLOBAL_KITS);
        return new ArrayList<>(seen);
    }

    private static int parseWeight(String s, int line) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            ConfigWarnings.warn(CONFIG_FILE_NAME, line, "invalid weight '" + s + "'; using 10");
            return 10;
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

    /**
     * Retrieves a specific BiomeKit matching the biome and variant name.
     * Used for retrieving starting stats for AI calculations.
     */
    public static BiomeKit getSpecificKit(String biomeId, String variantName) {
        // 1. Check specific variants
        if (GEN_KITS.containsKey(biomeId)) {
            for (BiomeKit kit : GEN_KITS.get(biomeId)) {
                if (kit.variantName.equals(variantName)) {
                    return kit;
                }
            }
        }

        // 2. Check global kits
        for (BiomeKit kit : GLOBAL_KITS) {
            if (kit.variantName.equals(variantName)) {
                return kit;
            }
        }

        // 3. Fallback: If variant name not found (e.g. migration issue or rename), use
        // default random get
        return get(biomeId);
    }
}
