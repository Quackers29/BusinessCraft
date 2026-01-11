package com.quackers29.businesscraft.forge.platform;

import com.quackers29.businesscraft.api.EntityHelper;
import com.quackers29.businesscraft.forge.init.ForgeModEntityTypes;
import net.minecraft.world.entity.EntityType;

/**
 * Forge implementation of EntityHelper
 */
public class ForgeEntityHelper implements EntityHelper {
    @Override
    public Object getTouristEntityType() {
        return ForgeModEntityTypes.TOURIST.get();
    }

    @Override
    public void setPathCreationTarget(Object playerObj, java.util.UUID townId) {
        if (playerObj instanceof net.minecraft.world.entity.player.Player player) {
            player.getPersistentData().putUUID("CurrentTownBlock", townId);
        }
    }

    @Override
    public void clearPathCreationTarget(Object playerObj) {
        if (playerObj instanceof net.minecraft.world.entity.player.Player player) {
            player.getPersistentData().remove("CurrentTownBlock");
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
