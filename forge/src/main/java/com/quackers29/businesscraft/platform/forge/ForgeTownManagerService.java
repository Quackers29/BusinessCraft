package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.platform.ITownManagerService;
import net.minecraft.core.registries.BuiltInRegistries;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.town.data.TownPaymentBoard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forge implementation of ITownManagerService.
 * Wraps the common module TownManager for platform abstraction.
 * 
 * Enhanced MultiLoader approach: Direct integration with common business logic.
 */
public class ForgeTownManagerService implements ITownManagerService {
    
    // Map to store TownPaymentBoard instances for each town
    // This bridges the common Town class with forge-specific TownPaymentBoard
    private static final Map<UUID, TownPaymentBoard> townPaymentBoards = new ConcurrentHashMap<>();

    @Override
    public UUID registerTown(Object level, Object pos, String name) {
        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos blockPos = (BlockPos) pos;
        Town createdTown = TownManager.get(serverLevel).createTown(blockPos.getX(), blockPos.getY(), blockPos.getZ(), name);
        
        if (createdTown != null) {
            // Create a TownPaymentBoard for this town
            TownPaymentBoard paymentBoard = new TownPaymentBoard();
            townPaymentBoards.put(createdTown.getId(), paymentBoard);
        }
        
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
        
        // Convert item to resource location string using direct registry access
        String itemId;
        if (item instanceof ItemStack) {
            ItemStack itemStack = (ItemStack) item;
            itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
        } else {
            itemId = BuiltInRegistries.ITEM.getKey((net.minecraft.world.item.Item) item).toString();
        }
        
        TownManager.get(serverLevel).addResourceToTown(townId, itemId, count);
    }

    @Override
    public void removeTown(Object level, UUID id) {
        ServerLevel serverLevel = (ServerLevel) level;
        TownManager.get(serverLevel).removeTown(id);
        
        // Clean up the payment board for this town
        townPaymentBoards.remove(id);
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
        
        // Clear all payment boards
        townPaymentBoards.clear();
        
        return count;
    }

    @Override
    public Object getPaymentBoard(Object town) {
        if (town instanceof Town) {
            Town commonTown = (Town) town;
            UUID townId = commonTown.getId();
            
            // Get or create payment board for this town
            TownPaymentBoard paymentBoard = townPaymentBoards.computeIfAbsent(townId, 
                id -> new TownPaymentBoard());
            
            return paymentBoard;
        }
        
        return null;
    }
    
    /**
     * Get the payment boards map for persistence operations.
     * This is used by ForgeTownPersistence to save/load payment board data.
     */
    public static Map<UUID, TownPaymentBoard> getPaymentBoards() {
        return townPaymentBoards;
    }
    
    /**
     * Set a payment board for a specific town (used during loading).
     */
    public static void setPaymentBoard(UUID townId, TownPaymentBoard paymentBoard) {
        townPaymentBoards.put(townId, paymentBoard);
    }
}