package com.yourdomain.businesscraft.data;

import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.town.TownManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public class TownSavedData extends SavedData {
    public static final String NAME = BusinessCraft.MOD_ID + "_towns";
    
    public static TownSavedData create() {
        return new TownSavedData();
    }
    
    @Override
    public CompoundTag save(CompoundTag tag) {
        TownManager.getInstance().saveAllTowns(tag);
        return tag;
    }
    
    public static TownSavedData load(CompoundTag tag) {
        TownSavedData data = create();
        TownManager.getInstance().loadAllTowns(tag);
        return data;
    }
} 