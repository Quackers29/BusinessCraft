package com.quackers29.businesscraft.data;

import com.quackers29.businesscraft.BusinessCraft;
import com.quackers29.businesscraft.town.Town;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TownSavedData extends SavedData {
    public static final String NAME = BusinessCraft.MOD_ID + "_towns";
    
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
    }
    
    public static TownSavedData create() {
        return new TownSavedData();
    }
    
    public static TownSavedData load(CompoundTag tag) {
        TownSavedData data = create();
        data.loadFromNbt(tag);
        return data;
    }
} 