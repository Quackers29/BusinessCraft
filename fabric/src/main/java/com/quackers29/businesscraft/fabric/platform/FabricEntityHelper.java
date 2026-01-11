package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.EntityHelper;
import com.quackers29.businesscraft.fabric.init.FabricModEntityTypes;

/**
 * Fabric implementation of EntityHelper
 */
public class FabricEntityHelper implements EntityHelper {
    // Simple transient storage for Fabric since we don't have easy PersistentData
    // access without Cardinal Components
    private final java.util.Map<java.util.UUID, java.util.UUID> pathCreationModes = new java.util.HashMap<>();

    @Override
    public Object getTouristEntityType() {
        return FabricModEntityTypes.TOURIST;
    }

    @Override
    public void setPathCreationTarget(Object playerObj, java.util.UUID townId) {
        if (playerObj instanceof net.minecraft.world.entity.player.Player player) {
            pathCreationModes.put(player.getUUID(), townId);
        }
    }

    @Override
    public void clearPathCreationTarget(Object playerObj) {
        if (playerObj instanceof net.minecraft.world.entity.player.Player player) {
            pathCreationModes.remove(player.getUUID());
        }
    }

    @Override
    public Object getPlayerByUUID(Object levelObj, java.util.UUID uuid) {
        if (levelObj instanceof net.minecraft.world.level.Level level) {
            return level.getPlayerByUUID(uuid);
        }
        return null;
    }
}
