package com.quackers29.businesscraft.platform.fabric;

import com.quackers29.businesscraft.platform.ITownManagerService;
import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.ItemStack;

import java.util.Map;
import java.util.UUID;

/**
 * Fabric implementation of ITownManagerService.
 * Provides platform-agnostic access to town management functionality.
 * Now uses direct access to the common module TownManager instead of reflection.
 * 
 * Enhanced MultiLoader approach: Direct integration with common business logic.
 */
public class FabricTownManagerService implements ITownManagerService {

    @Override
    public UUID registerTown(Object level, Object pos, String name) {
        ServerWorld serverWorld = (ServerWorld) level;
        
        // Convert position coordinate types if needed (Fabric BlockPos vs Minecraft BlockPos)
        int x, y, z;
        if (pos instanceof BlockPos) {
            BlockPos blockPos = (BlockPos) pos;
            x = blockPos.getX();
            y = blockPos.getY(); 
            z = blockPos.getZ();
        } else {
            // Try to extract coordinates generically
            try {
                x = (Integer) pos.getClass().getMethod("getX").invoke(pos);
                y = (Integer) pos.getClass().getMethod("getY").invoke(pos);
                z = (Integer) pos.getClass().getMethod("getZ").invoke(pos);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid position type: " + pos.getClass());
            }
        }
        
        Town createdTown = TownManager.get(serverWorld).createTown(x, y, z, name);
        return createdTown != null ? createdTown.getId() : null;
    }

    @Override
    public Object getTown(Object level, UUID id) {
        ServerWorld serverLevel = (ServerWorld) level;
        return TownManager.get(serverLevel).getTown(id);
    }

    @Override
    public Map<UUID, Object> getAllTowns(Object level) {
        ServerWorld serverLevel = (ServerWorld) level;
        // Convert Collection<Town> to Map<UUID, Object>
        Map<UUID, Object> result = new java.util.HashMap<>();
        TownManager.get(serverLevel).getAllTowns().forEach(town -> result.put(town.getId(), town));
        return result;
    }

    @Override
    public boolean canPlaceTownAt(Object level, Object pos) {
        ServerWorld serverLevel = (ServerWorld) level;
        
        // Convert position coordinate types if needed
        int x, y, z;
        if (pos instanceof BlockPos) {
            BlockPos blockPos = (BlockPos) pos;
            x = blockPos.getX();
            y = blockPos.getY(); 
            z = blockPos.getZ();
        } else if (pos instanceof net.minecraft.util.math.BlockPos) {
            net.minecraft.util.math.BlockPos fabricPos = (net.minecraft.util.math.BlockPos) pos;
            x = fabricPos.getX();
            y = fabricPos.getY();
            z = fabricPos.getZ();
        } else {
            return false;
        }
        
        return TownManager.get(serverLevel).canPlaceTownAt(x, y, z);
    }

    @Override
    public String getTownPlacementError(Object level, Object pos) {
        ServerWorld serverLevel = (ServerWorld) level;
        
        // Convert position coordinate types if needed
        int x, y, z;
        if (pos instanceof BlockPos) {
            BlockPos blockPos = (BlockPos) pos;
            x = blockPos.getX();
            y = blockPos.getY(); 
            z = blockPos.getZ();
        } else if (pos instanceof net.minecraft.util.math.BlockPos) {
            net.minecraft.util.math.BlockPos fabricPos = (net.minecraft.util.math.BlockPos) pos;
            x = fabricPos.getX();
            y = fabricPos.getY();
            z = fabricPos.getZ();
        } else {
            return "Invalid position type";
        }
        
        return TownManager.get(serverLevel).getTownPlacementError(x, y, z);
    }

    @Override
    public void updateResources(Object level, UUID townId, int breadCount) {
        // This method might be legacy - convert to addResource using bread item
        ServerWorld serverLevel = (ServerWorld) level;
        TownManager.get(serverLevel).addResourceToTown(townId, "minecraft:bread", breadCount);
    }

    @Override
    public void addResource(Object level, UUID townId, Object item, int count) {
        ServerWorld serverLevel = (ServerWorld) level;
        
        // Convert item types if needed - using registry helper for consistency
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
        ServerWorld serverLevel = (ServerWorld) level;
        TownManager.get(serverLevel).removeTown(id);
    }

    @Override
    public void markDirty(Object level) {
        ServerWorld serverLevel = (ServerWorld) level;
        TownManager.get(serverLevel).markDirty();
    }

    @Override
    public void onServerStopping(Object level) {
        ServerWorld serverLevel = (ServerWorld) level;
        TownManager.remove(serverLevel);
    }

    @Override
    public void clearInstances() {
        // Common TownManager doesn't have clearInstances method
        // Cleanup is handled by individual level removal
    }

    @Override
    public int clearAllTowns(Object level) {
        ServerWorld serverLevel = (ServerWorld) level;
        int count = TownManager.get(serverLevel).getAllTowns().size();
        TownManager.get(serverLevel).clearAllTowns();
        return count;
    }
}