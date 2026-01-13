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

        // Iterate through all known resources in the registry
        for (ResourceType type : ResourceRegistry.getAll()) {
            String id = type.getId();

            // Skip "special" types that aren't items if any (like population logic handled
            // separately?)
            // But TownTradingComponent handles "pop", "tourist" etc.
            // ResourceRegistry usually contains actual items (wood, iron, etc)

            float stock = trading.getStock(id);
            float cap = trading.getStorageCap(id);
            float price = type.getBaseValue();

            String displayName = type.getId(); // Fallback
            // Try to get nice display name from item
            if (type.getMcItemId() != null) {
                Item item = BuiltInRegistries.ITEM.get(type.getMcItemId());
                if (item != Items.AIR) {
                    displayName = item.getDescription().getString();
                }
            }

            // Calculate status
            boolean canBuy = true; // Player can buy FROM town? (Town sells) -> Town needs stock
            boolean canSell = true; // Player can sell TO town? (Town buys) -> Town needs space + money

            // Logic:
            // "Buy" means Player buys from Town. Town needs Stock > 0.
            // "Sell" means Player sells to Town. Town needs Space < Cap AND Money >= Price.

            if (stock <= 0)
                canBuy = false;

            if (stock >= cap)
                canSell = false;

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

        // Also add special resources if needed, but usually trading UI is mainly for
        // items.
        // If we want to show population caps etc here, we could, but let's stick to
        // ResourceRegistry for now.

        return new TradingViewModel(infoMap, currencyName, "Updated: " + java.time.LocalTime.now().toString());
    }
}
