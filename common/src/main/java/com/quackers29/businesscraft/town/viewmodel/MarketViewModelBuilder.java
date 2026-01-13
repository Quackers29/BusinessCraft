package com.quackers29.businesscraft.town.viewmodel;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.economy.GlobalMarket;
import com.quackers29.businesscraft.economy.ResourceRegistry;
import com.quackers29.businesscraft.economy.ResourceType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SERVER-SIDE ONLY builder for MarketViewModel.
 *
 * This class contains ALL market price calculation logic that was previously
 * duplicated in ClientGlobalMarket.getPrice(Item). The client will receive
 * pre-calculated prices and perform zero calculations.
 *
 * KEY RESPONSIBILITIES:
 * - Resolve Item → ResourceType mappings using ResourceRegistry
 * - Calculate prices for all items using GlobalMarket prices
 * - Implement max-price logic for multi-type items (same as ClientGlobalMarket)
 * - Format prices as display strings ("12.5 emeralds" or "1.0 emerald")
 * - Handle fallback to 1.0f default price (maintains existing behavior)
 */
public class MarketViewModelBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarketViewModelBuilder.class);

    /**
     * Builds a complete market view-model with pre-calculated prices for all items.
     *
     * This method replicates the exact logic from
     * ClientGlobalMarket.getPrice(Item):
     * 1. Resolves items to resource types using ResourceRegistry.getAllFor()
     * 2. Gets prices from GlobalMarket.get().getPrice()
     * 3. Implements max-price logic for items with multiple resource types
     * 4. Returns 1.0f fallback for items with no known price
     *
     * @return Complete market view-model with all pre-calculated prices
     */
    public static MarketViewModel buildMarketViewModel() {
        LOGGER.debug("[SERVER] Building market view-model...");

        Map<Item, MarketViewModel.MarketPriceInfo> itemPrices = new HashMap<>();
        GlobalMarket market = GlobalMarket.get();

        // Iterate through all registered items in the game
        Iterable<Item> allItems = PlatformAccess.getRegistry().getItems();

        int pricedItems = 0;
        for (Item item : allItems) {
            if (item == null)
                continue;

            // Get item's resource location for debugging
            ResourceLocation itemKey = PlatformAccess.getRegistry().getItemKey(item);
            if (itemKey == null)
                continue;

            String itemKeyStr = itemKey.toString();

            // REPLICATE EXACT LOGIC FROM ClientGlobalMarket.getPrice(Item) lines 37-75
            float maxPrice = 1.0f;
            boolean foundAny = false;
            String resourceTypeUsed = null;

            // Try to resolve generic resource types (e.g. "wood" for "oak_log")
            // Check ALL mapping types in case of ambiguity (e.g. "food" vs "bread")
            List<ResourceType> types = ResourceRegistry.getAllFor(item);
            for (ResourceType type : types) {
                String id = type.getId();
                float price = market.getPrice(id);

                // Only consider prices that differ from default 1.0f
                if (price != 1.0f || foundAny) {
                    // Trust the price from GlobalMarket. If we find any valid price, use it.
                    // If multiple types map to prices, take the highest one to be safe.
                    // This matches ClientGlobalMarket logic (lines 48-63)
                    if (!foundAny || price > maxPrice) {
                        maxPrice = price;
                        resourceTypeUsed = id;
                    }
                    foundAny = true;
                }
            }

            // Try specific item ID as fallback (matches ClientGlobalMarket lines 70-72)
            if (!foundAny) {
                float specificPrice = market.getPrice(itemKeyStr);
                if (specificPrice != 1.0f) {
                    maxPrice = specificPrice;
                    resourceTypeUsed = itemKeyStr;
                    foundAny = true;
                }
            }

            // Format price as display string
            String priceDisplay = formatPriceDisplay(maxPrice);

            // Create price info
            MarketViewModel.MarketPriceInfo priceInfo = new MarketViewModel.MarketPriceInfo(
                    priceDisplay,
                    maxPrice,
                    resourceTypeUsed,
                    foundAny);

            itemPrices.put(item, priceInfo);

            if (foundAny) {
                pricedItems++;
                LOGGER.debug("[SERVER] Item {} → {} (type: {})", itemKeyStr, priceDisplay, resourceTypeUsed);
            }
        }

        // Calculate market status (simple heuristic based on price variance)
        String marketStatus = calculateMarketStatus(itemPrices);

        LOGGER.debug("[SERVER] Market view-model built: {} items with known prices", pricedItems);

        return new MarketViewModel(itemPrices, marketStatus, pricedItems);
    }

    /**
     * Formats a price value as a display string.
     * Examples: "12.5 emeralds", "1.0 emerald", "0.5 emeralds"
     *
     * Note: Currently hardcoded to "emeralds" - could be made configurable
     * to support different currencies in the future.
     */
    private static String formatPriceDisplay(float price) {
        // Format to 1 decimal place
        String formattedPrice = String.format("%.1f", price);

        // Singular vs plural
        if (price == 1.0f) {
            return formattedPrice + " emerald";
        } else {
            return formattedPrice + " emeralds";
        }
    }

    /**
     * Calculates overall market status based on price distribution.
     * Returns "Active", "Stable", or "Volatile" based on simple heuristics.
     *
     * This is a placeholder for future market analysis features.
     */
    private static String calculateMarketStatus(Map<Item, MarketViewModel.MarketPriceInfo> itemPrices) {
        int knownPrices = 0;
        float sumPrices = 0f;
        float maxPrice = 0f;
        float minPrice = Float.MAX_VALUE;

        for (MarketViewModel.MarketPriceInfo info : itemPrices.values()) {
            if (info.hasKnownPrice()) {
                float price = info.getPriceValue();
                knownPrices++;
                sumPrices += price;
                maxPrice = Math.max(maxPrice, price);
                minPrice = Math.min(minPrice, price);
            }
        }

        if (knownPrices == 0) {
            return "Inactive";
        }

        float avgPrice = sumPrices / knownPrices;
        float priceRange = maxPrice - minPrice;

        // Simple heuristic: if price range is large relative to average, market is
        // volatile
        if (priceRange > avgPrice * 2.0f) {
            return "Volatile";
        } else {
            return "Stable";
        }
    }
}
