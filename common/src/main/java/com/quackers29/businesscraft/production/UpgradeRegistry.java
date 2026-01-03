package com.quackers29.businesscraft.production;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.data.parsers.DataParser;
import com.quackers29.businesscraft.data.parsers.Effect;
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

public class UpgradeRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeRegistry.class);
    private static final Map<String, UpgradeNode> NODES = new HashMap<>();
    private static final String UPGRADES_FILE = "upgrades.csv";
    private static final String REQ_FILE = "upgrade_requirements.csv";

    public static void load() {
        NODES.clear();
        Path configDir = PlatformAccess.platform.getConfigDirectory();

        // Fix path to point to config/businesscraft/
        File upgradesFile = configDir.resolve("businesscraft").resolve(UPGRADES_FILE).toFile();

        loadNodes(upgradesFile);
    }

    private static void loadNodes(File file) {
        if (!file.exists())
            createDefaultUpgrades(file);

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                // New Schema:
                // node_id,category,display_name,repeat,prereq_nodes,benefit_description,research_days,required_items,effects
                // 0 1 2 3 4 5 6 7 8
                String[] parts = line.split(",");
                if (parts.length >= 9) {
                    try {
                        String id = parts[0].trim();
                        String category = parts[1].trim();
                        String name = parts[2].trim();
                        String repeat = parts[3].trim();
                        String prereqsRaw = parts[4].trim();
                        String desc = parts[5].trim();

                        String daysRaw = parts[6].trim();
                        String costsRaw = parts[7].trim();
                        String effectsRaw = parts[8].trim();

                        List<String> prereqs = new ArrayList<>();
                        if (!prereqsRaw.isEmpty()) {
                            for (String p : prereqsRaw.split(";")) {
                                if (!p.trim().isEmpty())
                                    prereqs.add(p.trim());
                            }
                        }

                        List<Effect> effects = DataParser.parseEffects(effectsRaw);

                        float days = 0;
                        try {
                            if (!daysRaw.isEmpty())
                                days = Float.parseFloat(daysRaw);
                        } catch (NumberFormatException e) {
                            LOGGER.warn("Invalid research days for node {}: {}", id, daysRaw);
                        }
                        List<ResourceAmount> costs = DataParser.parseResources(costsRaw);

                        UpgradeNode node = new UpgradeNode(id, category, name, repeat, prereqs, desc, effects);
                        node.setRequirements(days, costs);
                        NODES.put(id, node);

                        LOGGER.info("Registered upgrade node: {}", id);
                    } catch (Exception e) {
                        LOGGER.error("Error parsing upgrade line: {}", line, e);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load {}", UPGRADES_FILE, e);
        }
    }

    private static void createDefaultUpgrades(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(
                    "node_id,category,display_name,repeat,prereq_nodes,benefit_description,research_days,required_items,effects\n");
            writer.write(
                    "basic_settlement,housing,Basic Settlement,,,Unlocks basic survival,0,,pop_cap:10;tourist_cap:2;storage_cap_all:200;happiness:50;population_maintenance;population_growth;basic_taxes\n");
            writer.write(
                    "farming_basic,farming,Basic Farming,,basic_settlement,Starts food production,1,wood:10,basic_farming\n");
        } catch (IOException e) {
            LOGGER.error("Failed to create {}", UPGRADES_FILE, e);
        }
    }

    public static UpgradeNode get(String id) {
        return NODES.get(id);
    }

    public static Collection<UpgradeNode> getAll() {
        return NODES.values();
    }
}
