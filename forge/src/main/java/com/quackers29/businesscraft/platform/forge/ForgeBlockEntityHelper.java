package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.platform.BlockEntityHelper;
import com.quackers29.businesscraft.platform.InventoryHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forge implementation of the BlockEntityHelper interface using the capability system.
 * This class provides cross-platform block entity capability management for
 * inventory attachment and custom data storage.
 */
public class ForgeBlockEntityHelper implements BlockEntityHelper {
    
    // Thread-safe storage for custom data attachments
    private final Map<BlockEntity, Map<String, Object>> customDataStorage = new ConcurrentHashMap<>();
    
    // Track capability attachments for proper cleanup
    private final Map<BlockEntity, Map<String, LazyOptional<?>>> capabilityAttachments = new ConcurrentHashMap<>();
    
    @Override
    public Object attachInventory(BlockEntity blockEntity, InventoryHelper.PlatformInventory inventory, @Nullable Direction side) {
        // For Forge, we expect the inventory to wrap an IItemHandler
        
        // Get the ItemStackHandler from the inventory wrapper
        Object platformInventory = inventory.getPlatformInventory();
        if (!(platformInventory instanceof IItemHandler itemHandler)) {
            throw new IllegalArgumentException("Platform inventory must be an IItemHandler for Forge");
        }
        
        // Create LazyOptional capability
        LazyOptional<IItemHandler> lazyOptional = LazyOptional.of(() -> itemHandler);
        
        // Store capability attachment for cleanup
        capabilityAttachments
            .computeIfAbsent(blockEntity, k -> new HashMap<>())
            .put(getInventoryCapabilityKey() + (side != null ? "_" + side.getName() : ""), lazyOptional);
        
        return lazyOptional;
    }
    
    @Override
    public @Nullable InventoryHelper.PlatformInventory getInventory(BlockEntity blockEntity, @Nullable Direction side) {
        // This would typically query the block entity's getCapability method
        // For now, we'll return null as we need the block entity to implement the capability system
        // This will be properly implemented when we abstract the TownInterfaceEntity
        return null;
    }
    
    @Override
    public <T> Object attachData(BlockEntity blockEntity, String key, T data) {
        customDataStorage
            .computeIfAbsent(blockEntity, k -> new ConcurrentHashMap<>())
            .put(key, data);
        
        // Return a handle that can be used for removal
        return new DataHandle(blockEntity, key);
    }
    
    @Override
    public @Nullable <T> T getData(BlockEntity blockEntity, String key, Class<T> dataClass) {
        Map<String, Object> entityData = customDataStorage.get(blockEntity);
        if (entityData != null) {
            Object data = entityData.get(key);
            if (dataClass.isInstance(data)) {
                return dataClass.cast(data);
            }
        }
        return null;
    }
    
    @Override
    public void removeData(BlockEntity blockEntity, String key) {
        Map<String, Object> entityData = customDataStorage.get(blockEntity);
        if (entityData != null) {
            entityData.remove(key);
            if (entityData.isEmpty()) {
                customDataStorage.remove(blockEntity);
            }
        }
    }
    
    @Override
    public void invalidateAllAttachments(BlockEntity blockEntity) {
        // Invalidate all capability attachments
        Map<String, LazyOptional<?>> attachments = capabilityAttachments.remove(blockEntity);
        if (attachments != null) {
            attachments.values().forEach(LazyOptional::invalidate);
        }
        
        // Remove custom data
        customDataStorage.remove(blockEntity);
    }
    
    @Override
    public boolean hasCapability(BlockEntity blockEntity, String capabilityKey, @Nullable Direction side) {
        // Check if we have a stored capability attachment
        Map<String, LazyOptional<?>> attachments = capabilityAttachments.get(blockEntity);
        if (attachments != null) {
            String fullKey = capabilityKey + (side != null ? "_" + side.getName() : "");
            LazyOptional<?> capability = attachments.get(fullKey);
            return capability != null && capability.isPresent();
        }
        return false;
    }
    
    @Override
    public String getInventoryCapabilityKey() {
        return "inventory";
    }
    
    /**
     * Simple data handle for tracking custom data attachments.
     */
    private static class DataHandle {
        private final BlockEntity blockEntity;
        private final String key;
        
        public DataHandle(BlockEntity blockEntity, String key) {
            this.blockEntity = blockEntity;
            this.key = key;
        }
        
        public BlockEntity getBlockEntity() {
            return blockEntity;
        }
        
        public String getKey() {
            return key;
        }
    }
}