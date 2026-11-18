package com.quackers29.businesscraft.production;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.config.ConfigWarnings;
import com.quackers29.businesscraft.config.CsvConfig;
import com.quackers29.businesscraft.data.parsers.DataParser;
import com.quackers29.businesscraft.data.parsers.Effect;
import com.quackers29.businesscraft.data.parsers.DataParser.ResourceAmount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class UpgradeRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeRegistry.class);
    private static final Map<String, UpgradeNode> NODES = new HashMap<>();
    private static final String UPGRADES_FILE = "upgrades.csv";

    public static void load() {
        NODES.clear();
        File file = CsvConfig.ensureInstalled(PlatformAccess.platform.getConfigDirectory(), UPGRADES_FILE);

        try {
            // node_id,category,display_name,repeat,prereq_nodes,benefit_description,research_minutes,required_items,effects
            for (CsvConfig.Row row : CsvConfig.readRows(file)) {
                String[] parts = row.columns();
                if (parts.length < 9) {
                    ConfigWarnings.warn(UPGRADES_FILE, row.lineNumber(),
                            "expected 9 columns, found " + parts.length + "; row skipped");
                    continue;
                }
                try {
                    String id = parts[0];
                    String category = parts[1];
                    String name = parts[2];
                    String repeat = parts[3];
                    String prereqsRaw = parts[4];
                    String desc = parts[5];
                    String minutesRaw = parts[6];
                    String costsRaw = parts[7];
                    String effectsRaw = parts[8];

                    if (id.isEmpty()) {
                        ConfigWarnings.warn(UPGRADES_FILE, row.lineNumber(), "empty node_id; row skipped");
                        continue;
                    }
                    if (NODES.containsKey(id)) {
                        ConfigWarnings.warn(UPGRADES_FILE, row.lineNumber(),
                                "duplicate node_id '" + id + "'; later row replaces the earlier one");
                    }

                    List<String> prereqs = new ArrayList<>();
                    for (String p : prereqsRaw.split(";")) {
                        if (!p.trim().isEmpty())
                            prereqs.add(p.trim());
                    }

                    List<Effect> effects = DataParser.parseEffects(effectsRaw);

                    float minutes = 0;
                    try {
                        if (!minutesRaw.isEmpty())
                            minutes = Float.parseFloat(minutesRaw);
                    } catch (NumberFormatException e) {
                        ConfigWarnings.warn(UPGRADES_FILE, row.lineNumber(),
                                "invalid research minutes '" + minutesRaw + "' for " + id + "; using 0");
                    }
                    List<ResourceAmount> costs = DataParser.parseResources(costsRaw);
                    long wantedCosts = java.util.Arrays.stream(costsRaw.split(";")).filter(t -> !t.trim().isEmpty())
                            .count();
                    if (costs.size() != wantedCosts) {
                        ConfigWarnings.warn(UPGRADES_FILE, row.lineNumber(),
                                "required_items '" + costsRaw + "' of " + id + " are not all in resource:amount form");
                    }

                    UpgradeNode node = new UpgradeNode(id, category, name, repeat, prereqs, desc, effects);
                    node.setRequirements(minutes, costs);
                    NODES.put(id, node);

                    LOGGER.info("Registered upgrade node: {}", id);
                } catch (Exception e) {
                    ConfigWarnings.warn(UPGRADES_FILE, row.lineNumber(), "could not parse row: " + e);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load {}", UPGRADES_FILE, e);
        }
    }

    public static UpgradeNode get(String id) {
        return NODES.get(id);
    }

    public static Collection<UpgradeNode> getAll() {
        return NODES.values();
    }
}
