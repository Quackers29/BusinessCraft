package com.quackers29.businesscraft.config.registries;

import com.quackers29.businesscraft.api.PlatformAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ItemRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemRegistry.class);
    private static final Map<String, ItemEntry> REGISTRY = new HashMap<>();
    private static final String FILE_NAME = "items.csv";

    public static class ItemEntry {
        public final String id;
        public final String displayName;
        public final ResourceLocation mcItemId;

        public ItemEntry(String id, String displayName, ResourceLocation mcItemId) {
            this.id = id;
            this.displayName = displayName;
            this.mcItemId = mcItemId;
        }

        public Item getItem() {
            return (Item) PlatformAccess.getRegistry().getItem(mcItemId);
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
                    if (line.isEmpty() || line.startsWith("#") || line.startsWith("item_id"))
                        continue;

                    String[] parts = line.split(",");
                    if (parts.length >= 3) {
                        String id = parts[0].trim();
                        String name = parts[1].trim();
                        String mcId = parts[2].trim();

                        try {
                            ResourceLocation rl = new ResourceLocation(mcId);
                            REGISTRY.put(id, new ItemEntry(id, name, rl));
                        } catch (Exception e) {
                            LOGGER.error("Invalid item ID in items.csv: {}", mcId);
                        }
                    }
                }
            }
            LOGGER.info("Loaded {} items from {}", REGISTRY.size(), FILE_NAME);
        } catch (Exception e) {
            LOGGER.error("Failed to load {}", FILE_NAME, e);
        }
    }

    private static void createDefault(File file) {
        try {
            // Ensure directory exists
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            try (FileWriter writer = new FileWriter(file)) {
                writer.write("item_id,display_name,mc_item_id\n");
                writer.write("food,Food,minecraft:wheat\n");
                writer.write("wood,Wood,minecraft:oak_log\n");
                writer.write("stone,Stone,minecraft:cobblestone\n");
                writer.write("iron,Iron Ingot,minecraft:iron_ingot\n");
                writer.write("planks,Planks,minecraft:oak_planks\n");
                writer.write("money,Emeralds,minecraft:emerald\n");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create default {}", FILE_NAME, e);
        }
    }

    public static ItemEntry get(String id) {
        return REGISTRY.get(id);
    }
}
