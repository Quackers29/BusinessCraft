package com.quackers29.businesscraft.platform.fabric;

import com.quackers29.businesscraft.platform.ITownManagerService;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.UUID;

/**
 * Fabric implementation of ITownManagerService.
 * Provides platform-agnostic access to town management functionality using reflection.
 * Uses reflection to bridge between Fabric and the common module TownManager.
 */
public class FabricTownManagerService implements ITownManagerService {

    @Override
    public UUID registerTown(Object level, Object pos, String name) {
        try {
            ServerWorld serverWorld = (ServerWorld) level;
            BlockPos blockPos = (BlockPos) pos;
            
            // Get TownManager via reflection
            Class<?> townManagerClass = Class.forName("com.quackers29.businesscraft.town.TownManager");
            java.lang.reflect.Method getMethod = townManagerClass.getMethod("get", Object.class);
            Object manager = getMethod.invoke(null, serverWorld);
            
            java.lang.reflect.Method registerTownMethod = manager.getClass().getMethod("registerTown", Object.class, String.class);
            return (UUID) registerTownMethod.invoke(manager, blockPos, name);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register town", e);
        }
    }

    @Override
    public Object getTown(Object level, UUID id) {
        try {
            Object manager = getTownManager(level);
            java.lang.reflect.Method getTownMethod = manager.getClass().getMethod("getTown", UUID.class);
            return getTownMethod.invoke(manager, id);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Map<UUID, Object> getAllTowns(Object level) {
        try {
            Object manager = getTownManager(level);
            java.lang.reflect.Method getAllTownsMethod = manager.getClass().getMethod("getAllTowns");
            return (Map<UUID, Object>) getAllTownsMethod.invoke(manager);
        } catch (Exception e) {
            return java.util.Collections.emptyMap();
        }
    }

    @Override
    public boolean canPlaceTownAt(Object level, Object pos) {
        try {
            Object manager = getTownManager(level);
            java.lang.reflect.Method canPlaceMethod = manager.getClass().getMethod("canPlaceTownAt", Object.class);
            return (Boolean) canPlaceMethod.invoke(manager, pos);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getTownPlacementError(Object level, Object pos) {
        try {
            Object manager = getTownManager(level);
            java.lang.reflect.Method getErrorMethod = manager.getClass().getMethod("getTownPlacementError", Object.class);
            return (String) getErrorMethod.invoke(manager, pos);
        } catch (Exception e) {
            return "Unknown error";
        }
    }

    @Override
    public void updateResources(Object level, UUID townId, int breadCount) {
        try {
            Object manager = getTownManager(level);
            java.lang.reflect.Method updateResourcesMethod = manager.getClass().getMethod("updateResources", UUID.class, int.class);
            updateResourcesMethod.invoke(manager, townId, breadCount);
        } catch (Exception e) {
            // Log error but continue
        }
    }

    @Override
    public void addResource(Object level, UUID townId, Object item, int count) {
        try {
            Object manager = getTownManager(level);
            java.lang.reflect.Method addResourceMethod = manager.getClass().getMethod("addResource", UUID.class, Object.class, int.class);
            addResourceMethod.invoke(manager, townId, item, count);
        } catch (Exception e) {
            // Log error but continue
        }
    }

    @Override
    public void removeTown(Object level, UUID id) {
        try {
            Object manager = getTownManager(level);
            java.lang.reflect.Method removeTownMethod = manager.getClass().getMethod("removeTown", UUID.class);
            removeTownMethod.invoke(manager, id);
        } catch (Exception e) {
            // Log error but continue
        }
    }

    @Override
    public void markDirty(Object level) {
        try {
            Object manager = getTownManager(level);
            java.lang.reflect.Method markDirtyMethod = manager.getClass().getMethod("markDirty");
            markDirtyMethod.invoke(manager);
        } catch (Exception e) {
            // Log error but continue
        }
    }

    @Override
    public void onServerStopping(Object level) {
        try {
            Object manager = getTownManager(level);
            java.lang.reflect.Method onServerStoppingMethod = manager.getClass().getMethod("onServerStopping");
            onServerStoppingMethod.invoke(manager);
        } catch (Exception e) {
            // Log error but continue
        }
    }

    @Override
    public void clearInstances() {
        try {
            Class<?> townManagerClass = Class.forName("com.quackers29.businesscraft.town.TownManager");
            java.lang.reflect.Method clearInstancesMethod = townManagerClass.getMethod("clearInstances");
            clearInstancesMethod.invoke(null);
        } catch (Exception e) {
            // Log error but continue
        }
    }

    @Override
    public int clearAllTowns(Object level) {
        try {
            Object manager = getTownManager(level);
            java.lang.reflect.Method clearAllTownsMethod = manager.getClass().getMethod("clearAllTowns");
            return (Integer) clearAllTownsMethod.invoke(manager);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Helper method to get TownManager instance.
     */
    private Object getTownManager(Object level) throws Exception {
        ServerWorld serverWorld = (ServerWorld) level;
        Class<?> townManagerClass = Class.forName("com.quackers29.businesscraft.town.TownManager");
        java.lang.reflect.Method getMethod = townManagerClass.getMethod("get", Object.class);
        return getMethod.invoke(null, serverWorld);
    }
}