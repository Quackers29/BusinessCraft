package com.quackers29.businesscraft.town.viewmodel;

import com.quackers29.businesscraft.api.PlatformAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * View-Model for market price display on the client side.
 * Contains ONLY pre-calculated price data from the server.
 * Client performs ZERO item-to-resource resolution or price calculations.
 *
 * This implements the "dumb terminal" pattern where:
 * - Server resolves all Item → ResourceType mappings
 * - Server calculates all prices (including max-price logic for multi-type items)
 * - Client receives ready-to-display price data
 * - No ResourceRegistry access on client
 */
public class MarketViewModel {

    /**
     * Market price information for a single item
     */
    public static class MarketPriceInfo {
        private final String priceDisplay;          // "12.5 emeralds" or "1.0 emerald"
        private final float priceValue;             // Numeric value for calculations if needed
        private final String resourceTypeUsed;      // "wood", "food", etc. (for debugging/tooltips)
        private final boolean hasKnownPrice;        // false if using default fallback

        public MarketPriceInfo(String priceDisplay, float priceValue, String resourceTypeUsed, boolean hasKnownPrice) {
            this.priceDisplay = priceDisplay;
            this.priceValue = priceValue;
            this.resourceTypeUsed = resourceTypeUsed != null ? resourceTypeUsed : "";
            this.hasKnownPrice = hasKnownPrice;
        }

        public MarketPriceInfo(FriendlyByteBuf buf) {
            this.priceDisplay = buf.readUtf();
            this.priceValue = buf.readFloat();
            this.resourceTypeUsed = buf.readUtf();
            this.hasKnownPrice = buf.readBoolean();
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeUtf(priceDisplay);
            buf.writeFloat(priceValue);
            buf.writeUtf(resourceTypeUsed);
            buf.writeBoolean(hasKnownPrice);
        }

        // Getters for client display (NO CALCULATIONS)
        public String getPriceDisplay() { return priceDisplay; }
        public float getPriceValue() { return priceValue; }
        public String getResourceTypeUsed() { return resourceTypeUsed; }
        public boolean hasKnownPrice() { return hasKnownPrice; }
    }

    // Map of items to their pre-calculated market prices
    private final Map<Item, MarketPriceInfo> itemPrices;

    // Overall market summary (pre-calculated by server)
    private final String marketStatus;              // "Active", "Stable", "Volatile"
    private final int totalPricedItems;             // "127 items with known prices"

    public MarketViewModel(Map<Item, MarketPriceInfo> itemPrices, String marketStatus, int totalPricedItems) {
        this.itemPrices = new HashMap<>(itemPrices);
        this.marketStatus = marketStatus;
        this.totalPricedItems = totalPricedItems;
    }

    public MarketViewModel(FriendlyByteBuf buf) {
        // Read item prices
        int count = buf.readInt();
        this.itemPrices = new HashMap<>();

        for (int i = 0; i < count; i++) {
            var resourceLocation = buf.readResourceLocation();
            var item = (Item) PlatformAccess.getRegistry().getItem(resourceLocation);
            if (item != null) {
                var priceInfo = new MarketPriceInfo(buf);
                this.itemPrices.put(item, priceInfo);
            }
        }

        // Read summary data
        this.marketStatus = buf.readUtf();
        this.totalPricedItems = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        // Write item prices
        buf.writeInt(itemPrices.size());

        itemPrices.forEach((item, priceInfo) -> {
            var key = (ResourceLocation) PlatformAccess.getRegistry().getItemKey(item);
            buf.writeResourceLocation(key);
            priceInfo.toBytes(buf);
        });

        // Write summary data
        buf.writeUtf(marketStatus);
        buf.writeInt(totalPricedItems);
    }

    // Client-side getters (NO CALCULATIONS, NO REGISTRY ACCESS)
    public Map<Item, MarketPriceInfo> getItemPrices() {
        return itemPrices;
    }

    public MarketPriceInfo getPriceInfo(Item item) {
        return itemPrices.get(item);
    }

    /**
     * Gets price value for an item (same behavior as old ClientGlobalMarket.getPrice())
     * Returns 1.0f if no price is known (same fallback as before)
     */
    public float getPriceValue(Item item) {
        MarketPriceInfo info = itemPrices.get(item);
        return (info != null) ? info.getPriceValue() : 1.0f;
    }

    public String getMarketStatus() {
        return marketStatus;
    }

    public int getTotalPricedItems() {
        return totalPricedItems;
    }

    public boolean hasPrice(Item item) {
        return itemPrices.containsKey(item);
    }
}
