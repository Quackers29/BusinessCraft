package com.quackers29.businesscraft.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import com.quackers29.businesscraft.api.PlatformAccess;

import com.quackers29.businesscraft.economy.ResourceRegistry;
import com.quackers29.businesscraft.economy.ResourceType;

public class ClientGlobalMarket {
    private static final ClientGlobalMarket INSTANCE = new ClientGlobalMarket();

    private final Map<String, Float> prices = new HashMap<>();

    private ClientGlobalMarket() {
    }

    public static ClientGlobalMarket get() {
        return INSTANCE;
    }

    public void setPrices(Map<String, Float> newPrices) {
        prices.clear();
        if (newPrices != null) {
            prices.putAll(newPrices);
        }
    }

    public float getPrice(String resourceId) {
        return prices.getOrDefault(resourceId, 1.0f);
    }

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ClientGlobalMarket.class);

    public float getPrice(Item item) {
        Object key = PlatformAccess.getRegistry().getItemKey(item);
        if (key == null)
            return 1.0f;
        String keyStr = key.toString();

        float maxPrice = 1.0f;
        boolean foundAny = false;

        // Try to resolve generic resource types (e.g. "wood" for "oak_log")
        // Check ALL mapping types in case of ambiguity (e.g. "food" vs "bread")
        java.util.List<ResourceType> types = ResourceRegistry.getAllFor(item);
        for (ResourceType type : types) {
            String id = type.getId();
            if (prices.containsKey(id)) {
                float p = prices.get(id);
                // Trust the price in the map. If we find any valid price, use it.
                // If multiple types map to prices, take the highest one to be safe (or could
                // average?)
                // Taking max ensures we don't accidentally show a lower price if a specific
                // item is valued higher.
                if (!foundAny || p > maxPrice) {
                    maxPrice = p;
                }
                foundAny = true;
            }
        }

        if (foundAny) {
            return maxPrice;
        }

        // Try specific ID as fallback
        if (prices.containsKey(keyStr)) {
            return prices.get(keyStr);
        }

        return prices.getOrDefault(keyStr, 1.0f);
    }
}
