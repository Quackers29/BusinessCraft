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
    private static final String CONFIG_FILE_NAME = "items.csv";

    public static void load() {
        RESOURCES.clear();
        Path configDir = PlatformAccess.platform.getConfigDirectory();
        File configFile = configDir.resolve("businesscraft").resolve(CONFIG_FILE_NAME).toFile();

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
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
                // Expecting: item_id,display_name,mc_item_id
                if (parts.length >= 3) {
                    String id = parts[0].trim();
                    String displayName = parts[1].trim();
                    String mcItemIdStr = parts[2].trim();

                    if (id.isEmpty() || mcItemIdStr.isEmpty())
                        continue;

                    try {
                        ResourceLocation itemId = new ResourceLocation(mcItemIdStr);
                        // ResourceType constructor might need update or we keep it as is.
                        // Original ResourceType(id, itemId).
                        ResourceType type = new ResourceType(id, itemId);
                        // We might want to store display name too, but ResourceType might not support
                        // it yet.
                        // For now, adhere to existing constructor flexibility.
                        RESOURCES.put(id, type);
                        LOGGER.info("Registered item: {} -> {} ({})", id, itemId, displayName);
                    } catch (Exception e) {
                        LOGGER.error("Invalid item ID in {}: {}", CONFIG_FILE_NAME, mcItemIdStr);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load {}", CONFIG_FILE_NAME, e);
        }

        // Expand resources (find equivalents)
        for (ResourceType type : RESOURCES.values()) {
            type.expand();
        }
    }

    private static void createDefaultConfig(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("item_id,display_name,mc_item_id\n");
            writer.write("wood,Wood,minecraft:oak_log\n");
            writer.write("iron,Iron Ingot,minecraft:iron_ingot\n");
            writer.write("coal,Coal,minecraft:coal\n");
            writer.write("food,Food,minecraft:bread\n");
            writer.write("money,Emeralds,minecraft:emerald\n");
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
