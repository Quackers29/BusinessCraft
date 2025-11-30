package com.quackers29.businesscraft.economy;

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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ResourceRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceRegistry.class);
    private static final Map<String, ResourceType> RESOURCES = new HashMap<>();
    private static final String CONFIG_FILE_NAME = "tradeable_items.csv";

    public static void load() {
        RESOURCES.clear();
        Path configDir = PlatformAccess.platform.getConfigDirectory();
        File configFile = configDir.resolve(CONFIG_FILE_NAME).toFile();

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

                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String id = parts[0].trim();
                    String itemIdStr = parts[1].trim();

                    if (id.isEmpty() || itemIdStr.isEmpty())
                        continue;

                    try {
                        ResourceLocation itemId = new ResourceLocation(itemIdStr);
                        ResourceType type = new ResourceType(id, itemId);
                        RESOURCES.put(id, type);
                        LOGGER.info("Registered resource: {} -> {}", id, itemId);
                    } catch (Exception e) {
                        LOGGER.error("Invalid item ID in {}: {}", CONFIG_FILE_NAME, itemIdStr);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load {}", CONFIG_FILE_NAME, e);
        }

        // Expand resources (find equivalents)
        // This should be done after items are registered
        for (ResourceType type : RESOURCES.values()) {
            type.expand();
        }
    }

    private static void createDefaultConfig(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("resource_id,canonical_item_id\n");
            writer.write("wood,minecraft:oak_log\n");
            writer.write("iron,minecraft:iron_ingot\n");
            writer.write("coal,minecraft:coal\n");
            writer.write("food,minecraft:bread\n");
            writer.write("emerald,minecraft:emerald\n");
        } catch (IOException e) {
            LOGGER.error("Failed to create default {}", CONFIG_FILE_NAME, e);
        }
    }

    public static ResourceType get(String id) {
        return RESOURCES.get(id);
    }

    public static Collection<ResourceType> getAll() {
        return RESOURCES.values();
    }

    /**
     * Finds the resource type for a given item.
     * Returns null if the item is not a tradeable resource.
     */
    public static ResourceType getFor(net.minecraft.world.item.Item item) {
        ResourceLocation itemId = PlatformAccess.getRegistry().getItemKey(item);
        for (ResourceType type : RESOURCES.values()) {
            if (type.getEquivalents().containsKey(itemId)) {
                return type;
            }
        }
        return null;
    }
}
