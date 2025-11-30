package com.quackers29.businesscraft.economy;

import net.minecraft.nbt.CompoundTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class GlobalMarket {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalMarket.class);
    private static final GlobalMarket INSTANCE = new GlobalMarket();

    private final Map<String, Float> prices = new HashMap<>();
    private final Map<String, Long> totalVolume = new HashMap<>();

    private GlobalMarket() {
    }

    public static GlobalMarket get() {
        return INSTANCE;
    }

    public float getPrice(String resourceId) {
        return prices.getOrDefault(resourceId, 1.0f);
    }

    public void setPrice(String resourceId, float price) {
        prices.put(resourceId, price);
    }

    public void recordTrade(String resourceId, float quantity, float unitPrice) {
        // Update volume
        long currentVol = totalVolume.getOrDefault(resourceId, 0L);
        totalVolume.put(resourceId, currentVol + (long) quantity);

        // Update price (weighted average or simple learning)
        // For now, simple convergence towards the traded price
        float currentPrice = getPrice(resourceId);
        float newPrice = (currentPrice * 0.9f) + (unitPrice * 0.1f); // 10% learning rate
        prices.put(resourceId, newPrice);

        LOGGER.debug("Market update {}: price {} -> {}, vol {}", resourceId, currentPrice, newPrice, quantity);
    }

    public void load(CompoundTag tag) {
        if (tag.contains("prices")) {
            CompoundTag pricesTag = tag.getCompound("prices");
            for (String key : pricesTag.getAllKeys()) {
                prices.put(key, pricesTag.getFloat(key));
            }
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
