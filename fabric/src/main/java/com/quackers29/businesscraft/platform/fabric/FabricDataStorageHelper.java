package com.quackers29.businesscraft.platform.fabric;

import com.quackers29.businesscraft.platform.DataStorageHelper;
import com.quackers29.businesscraft.town.data.ITownPersistence;
import net.minecraft.nbt.NbtCompound;
// Fabric uses ServerWorld, not ServerWorld
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Fabric implementation of DataStorageHelper.
 * Uses Fabric's PersistentState system for persistent data storage.
 * Note: In Fabric, PersistentState works similarly to Forge's SavedData but with slightly different APIs.
 */
public class FabricDataStorageHelper implements DataStorageHelper {

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getOrCreateData(Object level, String name, Function<Object, T> loader, Supplier<T> creator) {
        ServerWorld serverWorld = (ServerWorld) level;
        
        // In Fabric, we use PersistentState which works similarly to Forge's SavedData
        // Bridge between the Object-based common interface and Fabric's PersistentState system
        Function<NbtCompound, PersistentState> fabricLoader = (tag) -> {
            T result = loader.apply(tag);
            return (PersistentState) result;
        };
        
        Supplier<PersistentState> fabricCreator = () -> {
            T result = creator.get();
            return (PersistentState) result;
        };
        
        PersistentState persistentState = serverWorld.getPersistentStateManager().getOrCreate(
            fabricLoader,
            fabricCreator,
            name
        );
        
        return (T) persistentState;
    }

    @Override
    public void markDirty(Object level, String name) {
        // In Fabric, like Forge, the SavedData instances handle their own dirty tracking
        // This method is primarily for API compatibility
        // The actual marking dirty is handled by the SavedData instance itself
        
        // Note: In some Fabric versions, you might need to explicitly mark dirty
        // For now, we'll use the same approach as Forge where SavedData handles it
    }
    
    @Override
    public ITownPersistence createTownPersistence(Object level, String identifier) {
        // Convert level to Fabric's ServerWorld (which should be compatible with ServerWorld)
        ServerWorld serverLevel = (ServerWorld) level;
        return new FabricTownPersistence(serverLevel, identifier);
    }
}