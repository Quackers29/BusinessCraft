package com.quackers29.businesscraft.forge.platform;

import com.quackers29.businesscraft.api.EntityHelper;
import com.quackers29.businesscraft.forge.init.ForgeModEntityTypes;

/**
 * Forge implementation of EntityHelper
 */
public class ForgeEntityHelper implements EntityHelper {
    @Override
    public Object getTouristEntityType() {
        return ForgeModEntityTypes.TOURIST.get();
    }

    @Override
    public Object getPlayerByUUID(Object levelObj, java.util.UUID uuid) {
        if (levelObj instanceof net.minecraft.world.level.Level level) {
            return level.getPlayerByUUID(uuid);
        }
        return null;
    }
}
