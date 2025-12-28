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
        File reqFile = configDir.resolve("businesscraft").resolve(REQ_FILE).toFile();

        loadNodes(upgradesFile);
        loadRequirements(reqFile);
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

                // node_id,category,display_name,prereq_nodes,benefit_description,effects
                String[] parts = line.split(",");
                if (parts.length >= 6) {
                    String id = parts[0].trim();
                    String category = parts[1].trim();
                    String name = parts[2].trim();
                    String prereqsRaw = parts[3].trim();
                    String desc = parts[4].trim();
                    String effectsRaw = parts[5].trim();
                    if (parts.length >= 7) {
                        // Adaptive fix for malformed CSV (7 columns instead of 6)
                        desc = parts[5].trim();
                        effectsRaw = parts[6].trim();
                    }

                    List<String> prereqs = new ArrayList<>();
                    if (!prereqsRaw.isEmpty()) {
                        for (String p : prereqsRaw.split(";")) {
                            if (!p.trim().isEmpty())
                                prereqs.add(p.trim());
                        }
                    }

                    List<Effect> effects = DataParser.parseEffects(effectsRaw);

                    NODES.put(id, new UpgradeNode(id, category, name, prereqs, desc, effects));
                    LOGGER.info("Registered upgrade node: {}", id);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load {}", UPGRADES_FILE, e);
        }
    }

    private static void loadRequirements(File file) {
        if (!file.exists())
            createDefaultReqs(file);

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                // node_id,research_days,required_items
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String id = parts[0].trim();
                    float days = 0;
                    try {
                        days = Float.parseFloat(parts[1].trim());
                    } catch (NumberFormatException e) {
                    }

                    String itemsRaw = (parts.length > 2) ? parts[2].trim() : "";
                    List<ResourceAmount> costs = DataParser.parseResources(itemsRaw);

                    UpgradeNode node = NODES.get(id);
                    if (node != null) {
                        node.setRequirements(days, costs);
                    } else {
                        LOGGER.warn("Requirement found for unknown node: {}", id);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load {}", REQ_FILE, e);
        }
    }

    private static void createDefaultUpgrades(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("node_id,category,display_name,prereq_nodes,benefit_description,effects\n");
            writer.write(
                    "basic_settlement,housing,Basic Settlement,,Unlocks basic survival,pop_cap:10;storage_cap_all:200;happiness:50;population_maintenance;population_growth\n");
            writer.write(
                    "farming_basic,farming,Basic Farming,basic_settlement,Starts food production,basic_farming\n");
        } catch (IOException e) {
            LOGGER.error("Failed to create {}", UPGRADES_FILE, e);
        }
    }

    private static void createDefaultReqs(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("node_id,research_days,required_items\n");
            writer.write("basic_settlement,0,\n");
            writer.write("farming_basic,1,wood:10\n");
        } catch (IOException e) {
            LOGGER.error("Failed to create {}", REQ_FILE, e);
        }
    }

    public static UpgradeNode get(String id) {
        return NODES.get(id);
    }

    public static Collection<UpgradeNode> getAll() {
        return NODES.values();
    }
}
