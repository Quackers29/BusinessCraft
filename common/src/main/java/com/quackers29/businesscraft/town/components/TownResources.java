package com.quackers29.businesscraft.town.components;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import com.quackers29.businesscraft.api.PlatformAccess;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Stores various resources in a town with their quantities.
 * Uses long for resource counts to support large-scale economies (max ~9.2 quintillion).
 */
public class TownResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownResources.class);
    private final Map<Item, Long> resources = new HashMap<>();
    private final String instanceId = java.util.UUID.randomUUID().toString().substring(0, 8);

    public TownResources() {
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Created new TownResources instance {}", instanceId);
    }

    /**
     * Add a specific resource to the town
     *
     * @param item  The item that represents the resource
     * @param count The amount to add (can be negative to remove resources)
     */
    public void addResource(Item item, long count) {
        if (item == null)
            return;

        if (count > 0) {
            // Adding resources with overflow protection
            long current = resources.getOrDefault(item, 0L);
            try {
                long result = Math.addExact(current, count);
                resources.put(item, result);
            } catch (ArithmeticException e) {
                // Overflow - cap at max
                resources.put(item, Long.MAX_VALUE);
            }
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Added {} of resource {}", count,
                    PlatformAccess.getRegistry().getItemKey(item));
        } else if (count < 0) {
            // Removing resources (count is negative)
            long currentAmount = resources.getOrDefault(item, 0L);
            long newAmount = Math.max(0, currentAmount + count); // Ensure we don't go below 0

            // Special logging for emeralds (more important for debugging)
            boolean isEmerald = item == net.minecraft.world.item.Items.EMERALD;

            if (currentAmount > 0) {
                if (isEmerald && count <= -5) {
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Emerald change: {} -> {} (removed {})",
                            currentAmount, newAmount, -count);
                } else {
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
                            "Removed {} of resource {} (new amount: {})",
                            -count, PlatformAccess.getRegistry().getItemKey(item), newAmount);
                }

                // Store the updated amount
                resources.put(item, newAmount);
            } else {
                if (isEmerald) {
                    LOGGER.warn("Failed emerald reduction: Attempted to remove {} emeralds, but current amount is 0",
                            -count);
                }
            }
        }
    }

    /**
     * Get the count of a specific resource
     *
     * @param item The resource item to check
     * @return The amount of the resource
     */
    public long getResourceCount(Item item) {
        return resources.getOrDefault(item, 0L);
    }

    /**
     * Consume a specific amount of resources
     *
     * @param item  The resource item to consume
     * @param count The amount to consume
     * @return True if the resources were successfully consumed
     */
    public boolean consumeResource(Item item, long count) {
        if (item == null || count <= 0)
            return false;

        long currentCount = resources.getOrDefault(item, 0L);
        if (currentCount < count)
            return false;

        resources.put(item, currentCount - count);
        return true;
    }

    /**
     * Gets all resources as an unmodifiable map
     *
     * @return Map of all resources
     */
    public Map<Item, Long> getAllResources() {
        return Collections.unmodifiableMap(resources);
    }

    /**
     * Save resources to NBT
     *
     * @param tag The tag to save to
     */
    public void save(CompoundTag tag) {
        CompoundTag resourcesTag = new CompoundTag();

        for (Map.Entry<Item, Long> entry : resources.entrySet()) {
            Object keyObj = PlatformAccess.getRegistry().getItemKey(entry.getKey());
            if (keyObj instanceof net.minecraft.resources.ResourceLocation key) {
                if (key != null) {
                    resourcesTag.putLong(key.toString(), entry.getValue());
                }
            }
        }

        tag.put("resources", resourcesTag);
    }

    /**
     * Load resources from NBT
     *
     * @param tag The tag to load from
     */
    public void load(CompoundTag tag) {
        resources.clear();

        if (tag.contains("resources")) {
            CompoundTag resourcesTag = tag.getCompound("resources");

            for (String key : resourcesTag.getAllKeys()) {
                ResourceLocation resourceLocation = new ResourceLocation(key);
                Object itemObj = PlatformAccess.getRegistry().getItem(resourceLocation);
                if (itemObj instanceof net.minecraft.world.item.Item item) {

                    if (item != null && item != Items.AIR) {
                        long amount = resourcesTag.getLong(key);
                        // Sanitize negative values
                        if (amount < 0)
                            amount = 0;
                        resources.put(item, amount);
                    }
                }
            }
        }
    }
}
