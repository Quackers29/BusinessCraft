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
                        float basePrice = 1.0f;
                        if (parts.length >= 4) {
                            try {
                                basePrice = Float.parseFloat(parts[3].trim());
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Invalid base price for item {}: {}, using default 1.0", id, parts[3]);
                            }
                        }

                        ResourceType type = new ResourceType(id, itemId, basePrice);
                        // We might want to store display name too, but ResourceType might not support
                        // it yet.
                        // For now, adhere to existing constructor flexibility.
                        RESOURCES.put(id, type);
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
            writer.write("item_id,display_name,mc_item_id,base_price\n");
            writer.write("wood,Wood,minecraft:oak_log,0.5\n");
            writer.write("iron,Iron Ingot,minecraft:iron_ingot,2.0\n");
            writer.write("coal,Coal,minecraft:coal,1.0\n");
            writer.write("food,Food,minecraft:bread,1.5\n");
            writer.write("money,Emeralds,minecraft:emerald,5.0\n"); // Example: Money itself has a value
        } catch (IOException e) {
            LOGGER.error("Failed to create default {}", CONFIG_FILE_NAME, e);
        }
    }

    // ============================================================================
    // DISPLAY MAPPING API - CLIENT ACCESS PERMITTED (Phase 3.1 Architectural Review)
    // ============================================================================
    //
    // These methods are SAFE for client UI to access because they perform PURE DATA
    // TRANSLATION with ZERO BUSINESS LOGIC:
    //
    // **WHAT THESE METHODS DO:**
    //   - Map between identifiers: String ID ↔ Minecraft Item ↔ ResourceType
    //   - Retrieve display names and canonical items for UI rendering
    //   - Enable contract/trade UI to show correct item icons
    //
    // **WHAT THESE METHODS DO NOT DO:**
    //   - NO calculations (prices, quantities, rates)
    //   - NO game state modification
    //   - NO business logic decisions
    //
    // **ARCHITECTURAL ANALOGY:**
    // These methods are like texture lookups or translation keys - they map
    // identifiers to engine objects for rendering purposes only.
    //
    // **EXAMPLES OF PERMITTED CLIENT USAGE:**
    //   ✅ ContractBoardScreen: get(resourceId) → Get ItemStack for icon rendering
    //   ✅ BCModalInventoryScreen: getFor(item) → Get resource ID for view-model lookup
    //   ✅ Display mapping: String "wood" → Item minecraft:oak_log → Texture rendering
    //
    // **EXAMPLES OF PROHIBITED CLIENT USAGE:**
    //   ❌ Calculating production costs (use ProductionStatusViewModel instead)
    //   ❌ Accessing ResourceType.basePrice for calculations (use TradingViewModel instead)
    //   ❌ Using registry data for business logic decisions
    //
    // **SERVER-AUTHORITATIVE GUARANTEE:**
    // All business logic (pricing, production, trading) uses VIEW-MODELS that are
    // calculated server-side and synced to clients as display-ready data.
    //
    // ============================================================================

    /**
     * Gets a ResourceType by its string ID.
     *
     * CLIENT USAGE: Permitted for display mapping only (e.g., rendering contract icons).
     * BUSINESS LOGIC: Use view-models for any calculations involving resources.
     *
     * @param id The resource ID (e.g., "wood", "iron")
     * @return The ResourceType, or null if not found
     */
    public static ResourceType get(String id) {
        return RESOURCES.get(id);
    }

    /**
     * Gets all registered ResourceTypes.
     *
     * SERVER USAGE: Building view-models, iterating resources for calculations.
     * CLIENT USAGE: Generally not needed on client (view-models provide display data).
     *
     * @return Collection of all ResourceTypes
     */
    public static Collection<ResourceType> getAll() {
        return RESOURCES.values();
    }

    /**
     * Gets the primary ResourceType for a Minecraft Item.
     * Returns the first matching resource type (for items mapped to multiple types, use getAllFor).
     *
     * CLIENT USAGE: Permitted for display mapping (e.g., Item → resource ID for view-model lookup).
     * BUSINESS LOGIC: Use view-models for any calculations involving item values.
     *
     * @param item The Minecraft item
     * @return The primary ResourceType, or null if not registered
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

    /**
     * Gets ALL ResourceTypes that match a Minecraft Item.
     * Some items may be registered as multiple resource types (e.g., an item could be both "food" and "trade_good").
     *
     * SERVER USAGE: Market price resolution uses this for max-price logic across multiple types.
     * CLIENT USAGE: Permitted for display mapping, but generally not needed (server sends resolved prices).
     *
     * @param item The Minecraft item
     * @return List of all matching ResourceTypes (may be empty)
     */
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
