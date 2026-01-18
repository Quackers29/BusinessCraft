package com.quackers29.businesscraft.data;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.town.Town;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TownSavedData extends SavedData {
    public static final String NAME = PlatformAccess.getPlatform().getModId() + "_towns";

    private final Map<UUID, Town> towns = new ConcurrentHashMap<>();

    public Map<UUID, Town> getTowns() {
        return towns;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag townsTag = new CompoundTag();
        towns.forEach((id, town) -> {
            CompoundTag townTag = new CompoundTag();
            town.save(townTag);
            townsTag.put(id.toString(), townTag);
        });
        tag.put("towns", townsTag);

        // Save Global Market data
        com.quackers29.businesscraft.economy.GlobalMarket.get().save(tag);

        return tag;
    }

    public void loadFromNbt(CompoundTag tag) {
        towns.clear();
        if (tag.contains("towns")) {
            CompoundTag townsTag = tag.getCompound("towns");
            townsTag.getAllKeys().forEach(key -> {
                UUID id = UUID.fromString(key);
                towns.put(id, Town.load(townsTag.getCompound(key)));
            });
        }

        // Load Global Market data
        com.quackers29.businesscraft.economy.GlobalMarket.get().load(tag);
    }

    public static TownSavedData create() {
        // Reset GlobalMarket when creating new world data to prevent
        // prices from persisting across worlds
        com.quackers29.businesscraft.economy.GlobalMarket.get().reset();
        return new TownSavedData();
    }

    public static TownSavedData load(CompoundTag tag) {
        TownSavedData data = create();
        data.loadFromNbt(tag);
        return data;
    }
}
