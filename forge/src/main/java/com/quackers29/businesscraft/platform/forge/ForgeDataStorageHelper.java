package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.platform.DataStorageHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Forge implementation of DataStorageHelper.
 * Uses Forge's SavedData system for persistent data storage.
 */
public class ForgeDataStorageHelper implements DataStorageHelper {

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getOrCreateData(Object level, String name, Function<Object, T> loader, Supplier<T> creator) {
        ServerLevel serverLevel = (ServerLevel) level;
        
        // We need to bridge between the Object-based common interface and Forge's SavedData system
        // Create wrapper functions that handle the type conversion
        Function<CompoundTag, T> forgeLoader = (tag) -> loader.apply(tag);
        
        return (T) serverLevel.getDataStorage().computeIfAbsent(
            forgeLoader,
            creator,
            name
        );
    }

    @Override
    public void markDirty(Object level, String name) {
        // In Forge, the SavedData instances handle their own dirty tracking
        // This method is primarily for Fabric compatibility
        // The actual marking dirty is handled by the SavedData instance itself
    }
}