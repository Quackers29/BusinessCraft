package com.quackers29.businesscraft.economy;

import net.minecraft.nbt.CompoundTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class GlobalMarket {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalMarket.class);
    private static final GlobalMarket INSTANCE = new GlobalMarket();

    // Minimum price floor to prevent prices from collapsing to absolute zero
    // Floor at 0.0001 = 10,000 items per emerald (for bulk items like sticks, dirt)
    private static final float MIN_PRICE = 0.0001f;

    // Drop rate when an auction fails (no bids) - 5% per failed auction
    private static final float FAILED_AUCTION_DROP_RATE = 0.05f;

    private final Map<String, Float> prices = new HashMap<>();
    private final Map<String, Long> totalVolume = new HashMap<>();
    private Runnable dirtyCallback;

    public void setDirtyCallback(Runnable callback) {
        this.dirtyCallback = callback;
    }

    private void markDirty() {
        if (dirtyCallback != null) {
            dirtyCallback.run();
        }
    }

    public static GlobalMarket get() {
        return INSTANCE;
    }

    public void reset() {
        prices.clear();
        totalVolume.clear();
        LOGGER.info("GlobalMarket reset - all prices cleared for new world");
        markDirty();
    }

    public float getPrice(String resourceId) {
        float price = prices.getOrDefault(resourceId, 1.0f);
        return Math.max(price, MIN_PRICE);
    }

    public void setPrice(String resourceId, float price) {
        prices.put(resourceId, Math.max(price, MIN_PRICE));
        markDirty();
    }

    public Map<String, Float> getPrices() {
        return java.util.Collections.unmodifiableMap(prices);
    }

    public void recordTrade(String resourceId, float quantity, float unitPrice) {
        // Update volume
        long currentVol = totalVolume.getOrDefault(resourceId, 0L);
        totalVolume.put(resourceId, currentVol + (long) quantity);

        // Update price (weighted average or simple learning)
        // For now, simple convergence towards the traded price
        float currentPrice = getPrice(resourceId);
        float newPrice = (currentPrice * 0.9f) + (unitPrice * 0.1f); // 10% learning rate
        // Enforce minimum price floor
        newPrice = Math.max(newPrice, MIN_PRICE);
        prices.put(resourceId, newPrice);

        LOGGER.debug("Market update {}: price {} -> {}, vol {}", resourceId, currentPrice, newPrice, quantity);
        markDirty();
    }

    public void recordFailedAuction(String resourceId) {
        float currentPrice = getPrice(resourceId);
        float newPrice = Math.max(currentPrice * (1 - FAILED_AUCTION_DROP_RATE), MIN_PRICE);
        prices.put(resourceId, newPrice);

        LOGGER.info("Failed auction: {} price {} -> {} ({}% drop)",
                resourceId, String.format("%.4f", currentPrice), String.format("%.4f", newPrice),
                (int)(FAILED_AUCTION_DROP_RATE * 100));
        markDirty();
    }

    public void load(CompoundTag tag) {
        // Clear existing data first to prevent cross-world contamination
        prices.clear();
        totalVolume.clear();

        if (tag.contains("prices")) {
            CompoundTag pricesTag = tag.getCompound("prices");
            for (String key : pricesTag.getAllKeys()) {
                // Enforce minimum price floor on load to fix corrupted data
                float loadedPrice = pricesTag.getFloat(key);
                prices.put(key, Math.max(loadedPrice, MIN_PRICE));
            }
        }
        if (tag.contains("volume")) {
            CompoundTag volTag = tag.getCompound("volume");
            for (String key : volTag.getAllKeys()) {
                totalVolume.put(key, volTag.getLong(key));
            }
        }

        LOGGER.info("GlobalMarket loaded - {} prices, {} volume entries", prices.size(), totalVolume.size());
    }

    public void save(CompoundTag tag) {
        CompoundTag pricesTag = new CompoundTag();
        prices.forEach(pricesTag::putFloat);
        tag.put("prices", pricesTag);

        CompoundTag volTag = new CompoundTag();
        totalVolume.forEach(volTag::putLong);
        tag.put("volume", volTag);
    }
}
