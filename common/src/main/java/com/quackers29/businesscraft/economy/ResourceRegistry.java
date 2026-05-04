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
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String id = parts[0].trim();
                    String displayName = parts[1].trim();
                    String mcItemIdStr = parts[2].trim();

                    if (id.isEmpty() || mcItemIdStr.isEmpty())
                        continue;

                    try {
                        ResourceLocation itemId = new ResourceLocation(mcItemIdStr);
                        float basePrice = 1.0f;
                        if (parts.length >= 4) {
                            try {
                                basePrice = Float.parseFloat(parts[3].trim());
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Invalid base price for item {}: {}, using default 1.0", id, parts[3]);
                            }
                        }

                        ResourceType type = new ResourceType(id, itemId, basePrice);
                        RESOURCES.put(id, type);
                    } catch (Exception e) {
                        LOGGER.error("Invalid item ID in {}: {}", CONFIG_FILE_NAME, mcItemIdStr);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load {}", CONFIG_FILE_NAME, e);
        }

        for (ResourceType type : RESOURCES.values()) {
            type.expand();
        }
    }

    private static void createDefaultConfig(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("item_id,display_name,mc_item_id,base_price\n");
            writer.write("wood,Wood,minecraft:oak_log,0.5\n");
            writer.write("iron,Iron Ingot,minecraft:iron_ingot,2.0\n");
            writer.write("coal,Coal,minecraft:coal,1.0\n");
            writer.write("food,Food,minecraft:bread,1.5\n");
            writer.write("money,Emeralds,minecraft:emerald,5.0\n");
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

    public static java.util.List<ResourceType> getAllFor(net.minecraft.world.item.Item item) {
        java.util.List<ResourceType> matching = new java.util.ArrayList<>();
        ResourceLocation itemId = PlatformAccess.getRegistry().getItemKey(item);
        for (ResourceType type : RESOURCES.values()) {
            if (type.getEquivalents().containsKey(itemId)) {
                matching.add(type);
            }
        }
        return matching;
    }
}
