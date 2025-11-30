package com.quackers29.businesscraft.economy;

import com.quackers29.businesscraft.api.PlatformAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a tradeable resource type (e.g., "wood", "iron").
 * Handles mapping canonical items and fuzzy equivalents.
 */
public class ResourceType {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceType.class);

    private final String id;
    private final ResourceLocation canonicalItemId;
    private final Map<ResourceLocation, Float> equivalents = new HashMap<>();

    public ResourceType(String id, ResourceLocation canonicalItemId) {
        this.id = id;
        this.canonicalItemId = canonicalItemId;
    }

    public String getId() {
        return id;
    }

    public ResourceLocation getCanonicalItemId() {
        return canonicalItemId;
    }

    public Map<ResourceLocation, Float> getEquivalents() {
        return equivalents;
    }

    /**
     * Expands the list of equivalents based on tags, food properties, and
     * heuristics.
     */
    public void expand() {
        // Add canonical item itself (1.0 unit)
        equivalents.put(canonicalItemId, 1.0f);

        // Get the canonical item object
        Item canonicalItem = (Item) PlatformAccess.getRegistry().getItem(canonicalItemId);
        if (canonicalItem == null || canonicalItem == Items.AIR) {
            LOGGER.warn("Canonical item {} for resource {} not found in registry", canonicalItemId, id);
            return;
        }

        // 1. Tag-based expansion
        // We try to guess the tag based on the resource ID (e.g., "wood" -> "logs",
        // "planks")
        // This is a simplified approach; a more robust one would allow configuring tags
        // in CSV.
        // For now, we'll hardcode some common tag mappings or iterate all items to
        // check tags.
        // Since we can't easily iterate all tags without a specific tag key, we'll
        // iterate items and check tags if possible,
        // or rely on platform helper to get items in a tag if we knew the tag.

        // Better approach for now: Iterate all registered items and check if they match
        // criteria
        Iterable<Item> allItems = PlatformAccess.getRegistry().getItems();

        for (Item item : allItems) {
            ResourceLocation itemId = PlatformAccess.getRegistry().getItemKey(item);
            if (itemId.equals(canonicalItemId))
                continue;

            // Food saturation logic
            if ("food".equals(id) && item.isEdible()) {
                FoodProperties foodProps = item.getFoodProperties();
                FoodProperties canonicalProps = canonicalItem.getFoodProperties();

                if (foodProps != null && canonicalProps != null) {
                    float saturationRatio = foodProps.getSaturationModifier() / canonicalProps.getSaturationModifier();
                    // Clamp reasonable values
                    if (saturationRatio > 0.1f && saturationRatio < 10.0f) {
                        equivalents.put(itemId, saturationRatio);
                    }
                }
            }

            // Heuristics for other resources
            // This is "fuzzy" matching based on name
            String path = itemId.getPath();
            if (id.equals("wood")) {
                if (path.endsWith("_log") || path.endsWith("_wood")) {
                    equivalents.put(itemId, 1.0f);
                }
            } else if (id.equals("iron")) {
                if (path.contains("iron_ingot")) {
                    equivalents.put(itemId, 1.0f);
                } else if (path.contains("iron_nugget")) {
                    equivalents.put(itemId, 0.11f); // 1/9
                } else if (path.contains("iron_block")) {
                    equivalents.put(itemId, 9.0f);
                }
            } else if (id.equals("coal")) {
                if (path.contains("coal") && !path.contains("block") && !path.contains("ore")) {
                    equivalents.put(itemId, 1.0f); // Charcoal, Coal
                }
            }
        }

        LOGGER.info("Expanded resource {}: found {} equivalents", id, equivalents.size());
    }

    public float getUnitValue(Item item) {
        ResourceLocation key = PlatformAccess.getRegistry().getItemKey(item);
        return equivalents.getOrDefault(key, 0.0f);
    }
}
