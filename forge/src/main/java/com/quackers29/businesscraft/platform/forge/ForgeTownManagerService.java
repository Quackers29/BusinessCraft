package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.platform.ITownManagerService;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;

/**
 * Forge implementation of ITownManagerService.
 * Wraps the existing TownManager for platform abstraction.
 */
public class ForgeTownManagerService implements ITownManagerService {

    @Override
    public UUID registerTown(Object level, Object pos, String name) {
        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos blockPos = (BlockPos) pos;
        return TownManager.get(serverLevel).registerTown(blockPos, name);
    }

    @Override
    public Object getTown(Object level, UUID id) {
        ServerLevel serverLevel = (ServerLevel) level;
        return TownManager.get(serverLevel).getTown(id);
    }

    @Override
    public Map<UUID, Object> getAllTowns(Object level) {
        ServerLevel serverLevel = (ServerLevel) level;
        // Convert Map<UUID, Town> to Map<UUID, Object>
        Map<UUID, Object> result = new java.util.HashMap<>();
        TownManager.get(serverLevel).getAllTowns().forEach((uuid, town) -> result.put(uuid, town));
        return result;
    }

    @Override
    public boolean canPlaceTownAt(Object level, Object pos) {
        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos blockPos = (BlockPos) pos;
        return TownManager.get(serverLevel).canPlaceTownAt(blockPos);
    }

    @Override
    public String getTownPlacementError(Object level, Object pos) {
        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos blockPos = (BlockPos) pos;
        return TownManager.get(serverLevel).getTownPlacementError(blockPos);
    }

    @Override
    public void updateResources(Object level, UUID townId, int breadCount) {
        ServerLevel serverLevel = (ServerLevel) level;
        TownManager.get(serverLevel).updateResources(townId, breadCount);
    }

    @Override
    public void addResource(Object level, UUID townId, Object item, int count) {
        ServerLevel serverLevel = (ServerLevel) level;
        ItemStack itemStack = (ItemStack) item;
        TownManager.get(serverLevel).addResource(townId, itemStack.getItem(), count);
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
        TownManager.get(serverLevel).onServerStopping();
    }

    @Override
    public void clearInstances() {
        TownManager.clearInstances();
    }

    @Override
    public int clearAllTowns(Object level) {
        ServerLevel serverLevel = (ServerLevel) level;
        return TownManager.get(serverLevel).clearAllTowns();
    }
}