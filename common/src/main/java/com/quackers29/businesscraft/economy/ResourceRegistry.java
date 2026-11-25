package com.quackers29.businesscraft.economy;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.config.ConfigWarnings;
import com.quackers29.businesscraft.config.CsvConfig;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ResourceRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceRegistry.class);
    private static final Map<String, ResourceType> RESOURCES = new HashMap<>();
    private static final String CONFIG_FILE_NAME = "items.csv";

    public static void load() {
        RESOURCES.clear();
        File configFile = CsvConfig.ensureInstalled(PlatformAccess.platform.getConfigDirectory(), CONFIG_FILE_NAME);

        try {
            // item_id,display_name,mc_item_id[,base_price]
            for (CsvConfig.Row row : CsvConfig.readRows(configFile)) {
                String[] parts = row.columns();
                if (parts.length < 3) {
                    ConfigWarnings.warn(CONFIG_FILE_NAME, row.lineNumber(),
                            "expected at least 3 columns, found " + parts.length + "; row skipped");
                    continue;
                }
                String id = parts[0];
                String mcItemIdStr = parts[2];
                if (id.isEmpty() || mcItemIdStr.isEmpty()) {
                    ConfigWarnings.warn(CONFIG_FILE_NAME, row.lineNumber(), "empty item_id or mc_item_id; row skipped");
                    continue;
                }
                if (RESOURCES.containsKey(id)) {
                    ConfigWarnings.warn(CONFIG_FILE_NAME, row.lineNumber(),
                            "duplicate item_id '" + id + "'; later row replaces the earlier one");
                }

                try {
                    ResourceLocation itemId = new ResourceLocation(mcItemIdStr);
                    float basePrice = 1.0f;
                    if (parts.length >= 4 && !parts[3].isEmpty()) {
                        try {
                            basePrice = Float.parseFloat(parts[3]);
                        } catch (NumberFormatException e) {
                            ConfigWarnings.warn(CONFIG_FILE_NAME, row.lineNumber(),
                                    "invalid base price '" + parts[3] + "' for " + id + "; using 1.0");
                        }
                    }
                    RESOURCES.put(id, new ResourceType(id, itemId, basePrice));
                } catch (Exception e) {
                    ConfigWarnings.warn(CONFIG_FILE_NAME, row.lineNumber(),
                            "invalid mc_item_id '" + mcItemIdStr + "'");
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load {}", CONFIG_FILE_NAME, e);
        }

        for (ResourceType type : RESOURCES.values()) {
            type.expand();
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
