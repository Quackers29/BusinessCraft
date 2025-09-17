package com.quackers29.businesscraft.town.components;

import net.minecraft.nbt.CompoundTag;

public interface TownComponent {
    void tick();
    void save(CompoundTag tag);
    void load(CompoundTag tag);
} 
