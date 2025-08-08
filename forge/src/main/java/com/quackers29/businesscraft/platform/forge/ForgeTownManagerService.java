package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.platform.ITownManagerService;
import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Forge implementation of ITownManagerService.
 * Wraps the common module TownManager for platform abstraction.
 * 
 * Enhanced MultiLoader approach: Direct integration with common business logic.
 */
public class ForgeTownManagerService implements ITownManagerService {

    @Override
    public UUID registerTown(Object level, Object pos, String name) {
        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos blockPos = (BlockPos) pos;
        Town createdTown = TownManager.get(serverLevel).createTown(blockPos.getX(), blockPos.getY(), blockPos.getZ(), name);
        return createdTown != null ? createdTown.getId() : null;
    }

    @Override
    public Object getTown(Object level, UUID id) {
        ServerLevel serverLevel = (ServerLevel) level;
        return TownManager.get(serverLevel).getTown(id);
    }

    @Override
    public Map<UUID, Object> getAllTowns(Object level) {
        ServerLevel serverLevel = (ServerLevel) level;
        // Convert Collection<Town> to Map<UUID, Object>
        Map<UUID, Object> result = new HashMap<>();
        TownManager.get(serverLevel).getAllTowns().forEach(town -> result.put(town.getId(), town));
        return result;
    }

    @Override
    public boolean canPlaceTownAt(Object level, Object pos) {
        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos blockPos = (BlockPos) pos;
        return TownManager.get(serverLevel).canPlaceTownAt(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    @Override
    public String getTownPlacementError(Object level, Object pos) {
        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos blockPos = (BlockPos) pos;
        return TownManager.get(serverLevel).getTownPlacementError(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    @Override
    public void updateResources(Object level, UUID townId, int breadCount) {
        // This method might be legacy - check if it's still needed
        // For now, convert to addResource using bread item
        ServerLevel serverLevel = (ServerLevel) level;
        TownManager.get(serverLevel).addResourceToTown(townId, "minecraft:bread", breadCount);
    }

    @Override
    public void addResource(Object level, UUID townId, Object item, int count) {
        ServerLevel serverLevel = (ServerLevel) level;
        
        // Convert item to resource location string using platform services
        String itemId;
        if (item instanceof ItemStack) {
            ItemStack itemStack = (ItemStack) item;
            itemId = PlatformServices.getRegistryHelper().getItemId(itemStack.getItem());
        } else {
            itemId = PlatformServices.getRegistryHelper().getItemId(item);
        }
        
        TownManager.get(serverLevel).addResourceToTown(townId, itemId, count);
    }

    @Override
    public void removeTown(Object level, UUID id) {
        ServerLevel serverLevel = (ServerLevel) level;
        TownManager.get(serverLevel).removeTown(id);
    }

    @Override
    public void markDirty(Object level) {
        ServerLevel serverLevel = (ServerLevel) level;
        TownManager.get(serverLevel).markDirty();
    }

    @Override
    public void onServerStopping(Object level) {
        ServerLevel serverLevel = (ServerLevel) level;
        TownManager.remove(serverLevel); // Updated method name
    }

    @Override
    public void clearInstances() {
        // Common TownManager doesn't have clearInstances method
        // Cleanup is handled by individual level removal
    }

    @Override
    public int clearAllTowns(Object level) {
        ServerLevel serverLevel = (ServerLevel) level;
        int count = TownManager.get(serverLevel).getAllTowns().size();
        TownManager.get(serverLevel).clearAllTowns();
        return count;
    }
}