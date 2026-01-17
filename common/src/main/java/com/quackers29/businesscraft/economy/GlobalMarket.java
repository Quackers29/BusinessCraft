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
    // Prices can be very low (e.g., 2000 wheat = 1 emerald), but not zero
    private static final float MIN_PRICE = 0.001f;

    private final Map<String, Float> prices = new HashMap<>();
    private final Map<String, Long> totalVolume = new HashMap<>();

    private GlobalMarket() {
    }

    public static GlobalMarket get() {
        return INSTANCE;
    }

    public float getPrice(String resourceId) {
        float price = prices.getOrDefault(resourceId, 1.0f);
        return Math.max(price, MIN_PRICE);
    }

    public void setPrice(String resourceId, float price) {
        // Enforce minimum price floor
        prices.put(resourceId, Math.max(price, MIN_PRICE));
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
    }

    public void load(CompoundTag tag) {
        if (tag.contains("prices")) {
            CompoundTag pricesTag = tag.getCompound("prices");
            for (String key : pricesTag.getAllKeys()) {
                // Enforce minimum price floor on load to fix corrupted data
                float loadedPrice = pricesTag.getFloat(key);
                prices.put(key, Math.max(loadedPrice, MIN_PRICE));
            }
            LOGGER.info("Loaded {} market prices (with {} floor enforced)", prices.size(), MIN_PRICE);
        }
        if (tag.contains("volume")) {
            CompoundTag volTag = tag.getCompound("volume");
            for (String key : volTag.getAllKeys()) {
                totalVolume.put(key, volTag.getLong(key));
            }
        }
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
