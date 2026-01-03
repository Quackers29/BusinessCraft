package com.quackers29.businesscraft.town.components;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.economy.ResourceRegistry;
import com.quackers29.businesscraft.economy.ResourceType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class TownTradingComponent implements TownComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownTradingComponent.class);

    private final com.quackers29.businesscraft.town.Town town;
    private final Map<String, TradingStock> stocks = new HashMap<>();

    public static class TradingStock {
        public float current;
        public float learnedMin;
        public float learnedMax;

        public TradingStock(float current, float min, float max) {
            this.current = current;
            this.learnedMin = min;
            this.learnedMax = max;
        }
    }

    public TownTradingComponent(com.quackers29.businesscraft.town.Town town) {
        this.town = town;
        // Initialize default stocks for known resources
        // In a real scenario, this might be done lazily or based on town type
    }

    @Override
    public void tick() {
        // Restock logic
        for (Map.Entry<String, TradingStock> entry : stocks.entrySet()) {
            TradingStock stock = entry.getValue();
            if (stock.current < stock.learnedMin) {
                stock.current += ConfigLoader.tradingRestockRate;
                // Clamp to max? Or just let it grow?
                // For now, just simple linear growth
            }
        }
    }

    public float getStock(String resourceId) {
        // "pop" = Current Population
        if ("pop".equals(resourceId)) {
            return (float) town.getPopulation();
        }
        if ("happiness".equals(resourceId)) {
            return town.getHappiness();
        }

        // "tourist" = Current Active Tourists (Active + Pending Spawns)
        // Used for production capping
        if ("tourist".equals(resourceId)) {
            return town.getTouristCount() + town.getPendingTouristSpawns();
        }

        // "tourism" = Cumulative Tourists Arrived (Requirement)
        if ("tourism".equals(resourceId)) {
            return (float) town.getTotalTouristsArrived();
        }

        // "tourism_dist" = Cumulative Distance (Requirement)
        if ("tourism_dist".equals(resourceId)) {
            return (float) town.getTotalTouristDistance();
        }

        // First check if this ID maps to a real item (Economy storage)
        ResourceType type = ResourceRegistry.get(resourceId);
        if (type != null && type.getMcItemId() != null) {
            Object itemObj = com.quackers29.businesscraft.api.PlatformAccess.getRegistry().getItem(type.getMcItemId());
            if (itemObj instanceof net.minecraft.world.item.Item item) {
                return town.getResourceCount(item);
            }
        }

        // Fallback to internal trading stock (for virtual resources?)
        return stocks.containsKey(resourceId) ? stocks.get(resourceId).current : 0.0f;
    }

    public void adjustStock(String resourceId, float amount) {
        // Special non-consumable resources cannot be adjusted via simple stock methods
        if ("tourism".equals(resourceId) || "tourism_dist".equals(resourceId)
                || "pop".equals(resourceId)) {
            return;
        }

        if ("tourist".equals(resourceId)) {
            if (amount > 0) {
                town.addPendingTouristSpawns((int) amount);
            }
            return;
        }

        // 1. Update Real Economy Storage
        ResourceType type = ResourceRegistry.get(resourceId);
        if (type != null && type.getMcItemId() != null) {
            Object itemObj = com.quackers29.businesscraft.api.PlatformAccess.getRegistry().getItem(type.getMcItemId());
            if (itemObj instanceof net.minecraft.world.item.Item item) {
                town.addResource(item, (int) amount);
                return;
            }
        }

        // 2. Fallback to Virtual/Internal Stock
        // Ensure stock exists
        if (!stocks.containsKey(resourceId)) {
            stocks.put(resourceId, new TradingStock(0, 100, ConfigLoader.tradingDefaultMaxStock));
        }

        TradingStock stock = stocks.get(resourceId);
        float newAmount = stock.current + amount;

        // Strict cap check for additions
        if (amount > 0) {
            float cap = getStorageCap(resourceId);
            if (newAmount > cap) {
                newAmount = cap;
            }
        }

        if (newAmount < 0)
            newAmount = 0;

        stock.current = newAmount;
    }

    public float getStorageCap(String resourceId) {
        if (town == null)
            return 999999f;

        // Population Cap
        if ("pop".equals(resourceId)) {
            return town.getUpgrades().getModifier("pop_cap");
        }
        // Tourist Cap
        if ("tourist".equals(resourceId)) {
            return town.getUpgrades().getModifier("tourist_cap");
        }

        // Historical stats have no cap
        if ("tourism".equals(resourceId) || "tourism_dist".equals(resourceId)) {
            return Float.MAX_VALUE;
        }

        float baseGlobal = 0f;
        if (baseGlobal == 0)
            baseGlobal = 50f;

        com.quackers29.businesscraft.town.components.TownUpgradeComponent upgrades = town.getUpgrades();
        if (upgrades != null) {
            float globalMod = upgrades.getModifier("storage_cap_all");
            float specificMod = upgrades.getModifier("storage_cap_" + resourceId);
            return baseGlobal + globalMod + specificMod;
        }
        return baseGlobal;
    }

    @Override
    public void save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<String, TradingStock> entry : stocks.entrySet()) {
            CompoundTag stockTag = new CompoundTag();
            stockTag.putString("id", entry.getKey());
            stockTag.putFloat("current", entry.getValue().current);
            stockTag.putFloat("min", entry.getValue().learnedMin);
            stockTag.putFloat("max", entry.getValue().learnedMax);
            list.add(stockTag);
        }
        tag.put("tradingStocks", list);
    }

    @Override
    public void load(CompoundTag tag) {
        stocks.clear();
        if (tag.contains("tradingStocks")) {
            ListTag list = tag.getList("tradingStocks", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag stockTag = list.getCompound(i);
                String id = stockTag.getString("id");
                float current = stockTag.getFloat("current");
                float min = stockTag.getFloat("min");
                float max = stockTag.getFloat("max");
                stocks.put(id, new TradingStock(current, min, max));
            }
        }
    }
}
