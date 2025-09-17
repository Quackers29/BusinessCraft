package com.quackers29.businesscraft.forge.platform;

import com.quackers29.businesscraft.api.EntityHelper;
import com.quackers29.businesscraft.forge.init.ForgeModEntityTypes;
import net.minecraft.world.entity.EntityType;

/**
 * Forge implementation of EntityHelper
 */
public class ForgeEntityHelper implements EntityHelper {
    @Override
    public EntityType<?> getTouristEntityType() {
        return ForgeModEntityTypes.TOURIST.get();
    }
}
