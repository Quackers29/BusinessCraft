package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.platform.DataStorageHelper;
import com.quackers29.businesscraft.town.data.ITownPersistence;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

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
        // The challenge is that Forge requires T to extend SavedData, but our interface allows any T
        // We'll use a cast-heavy approach to bridge this gap safely
        
        Function<CompoundTag, SavedData> forgeLoader = (tag) -> {
            T result = loader.apply(tag);
            return (SavedData) result;
        };
        
        Supplier<SavedData> forgeCreator = () -> {
            T result = creator.get();
            return (SavedData) result;
        };
        
        SavedData savedData = serverLevel.getDataStorage().computeIfAbsent(
            forgeLoader,
            forgeCreator,
            name
        );
        
        return (T) savedData;
    }

    @Override
    public void markDirty(Object level, String name) {
        // In Forge, the SavedData instances handle their own dirty tracking
        // This method is primarily for Fabric compatibility
        // The actual marking dirty is handled by the SavedData instance itself
    }
    
    @Override
    public ITownPersistence createTownPersistence(Object level, String identifier) {
        ServerLevel serverLevel = (ServerLevel) level;
        return new ForgeTownPersistence(serverLevel);
    }
}