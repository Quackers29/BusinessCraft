package com.quackers29.businesscraft.town.viewmodel;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.economy.ResourceRegistry;
import com.quackers29.businesscraft.economy.ResourceType;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.components.TownTradingComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Server-side builder for TradingViewModel.
 * Encapsulates all business logic for trading states, prices, and stock levels.
 */
public class TradingViewModelBuilder {

    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.##");
    private static final DecimalFormat STOCK_FORMAT = new DecimalFormat("#,##0");

    public static TradingViewModel build(Town town) {
        if (town == null) {
            return new TradingViewModel(Map.of(), "Unknown", "No Town Data");
        }

        Map<String, TradingViewModel.TradingResourceInfo> infoMap = new HashMap<>();
        TownTradingComponent trading = town.getTrading();

        // Determine currency name once
        String currencyName = "Emeralds";
        Item currencyItem = Items.EMERALD;
        try {
            ResourceLocation currencyLoc = new ResourceLocation(ConfigLoader.currencyItem);
            Item item = BuiltInRegistries.ITEM.get(currencyLoc);
            if (item != Items.AIR) {
                currencyName = item.getDescription().getString();
                currencyItem = item;
            }
        } catch (Exception ignored) {
        }

        // Create a set to track processed resource IDs
        java.util.Set<String> processedIds = new java.util.HashSet<>();

        // 1. Iterate through all REGISTERED resources (from CSV)
        // This ensures we show items that might have 0 stock but are "known" to the
        // system
        for (ResourceType type : ResourceRegistry.getAll()) {
            String id = type.getId();
            processedIds.add(id);

            float stock = trading.getStock(id);
            float cap = trading.getStorageCap(id);
            // Use GlobalMarket price instead of static base value
            float price = com.quackers29.businesscraft.economy.GlobalMarket.get().getPrice(id);

            String displayName = type.getId();
            if (type.getMcItemId() != null) {
                Item item = BuiltInRegistries.ITEM.get(type.getMcItemId());
                if (item != Items.AIR) {
                    displayName = item.getDescription().getString();
                }
            }

            addResourceInfo(infoMap, id, displayName, stock, cap, price, currencyName, town, currencyItem);
        }

        // 2. Iterate through all ACTUAL resources in the town
        // This ensures we catch unregistered items (e.g. Town Interface, Mod Items)
        Map<Item, Integer> allResources = town.getAllResources();
        for (Map.Entry<Item, Integer> entry : allResources.entrySet()) {
            Item item = entry.getKey();
            if (item == currencyItem)
                continue; // Skip currency itself

            // Check if this item maps to a registered resource we already processed
            ResourceType registeredType = ResourceRegistry.getFor(item);
            if (registeredType != null) {
                if (processedIds.contains(registeredType.getId())) {
                    continue; // Already processed
                }
            }

            // This is an unregistered item (or not yet processed)
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            if (processedIds.contains(id))
                continue;

            processedIds.add(id);

            float stock = entry.getValue();
            float cap = trading.getStorageCap(id);
            float price = com.quackers29.businesscraft.economy.GlobalMarket.get().getPrice(id);
            String displayName = item.getDescription().getString();

            addResourceInfo(infoMap, id, displayName, stock, cap, price, currencyName, town, currencyItem);
        }

        return new TradingViewModel(infoMap, currencyName, "Updated: " + java.time.LocalTime.now().toString());
    }

    private static void addResourceInfo(Map<String, TradingViewModel.TradingResourceInfo> infoMap,
            String id, String displayName, float stock, float cap, float price,
            String currencyName, Town town, Item currencyItem) {

        // Logic:
        // "Buy" means Player buys from Town. Town needs Stock > 0.
        // "Sell" means Player sells to Town. Town needs Space < Cap AND Money >= Price.

        boolean canBuy = stock > 0;
        boolean canSell = stock < cap;

        float townCurrency = town.getResourceCount(currencyItem);
        if (townCurrency < price)
            canSell = false; // Town cant afford to pay player

        String statusText = "Available";
        if (stock <= 0)
            statusText = "Out of Stock";
        else if (stock >= cap)
            statusText = "Storage Full";
        else if (townCurrency < price)
            statusText = "Town Broke";

        String stockDisplay = STOCK_FORMAT.format(stock) + " / " + STOCK_FORMAT.format(cap);
        String priceDisplay = PRICE_FORMAT.format(price) + " " + currencyName;

        String tooltip = String.format("Stock: %.0f\nCap: %.0f\nPrice: %.2f %s\nStatus: %s",
                stock, cap, price, currencyName, statusText);

        infoMap.put(id, new TradingViewModel.TradingResourceInfo(
                id,
                displayName,
                stockDisplay,
                stock,
                cap,
                price,
                priceDisplay,
                canBuy,
                canSell,
                statusText,
                tooltip));
    }
}
