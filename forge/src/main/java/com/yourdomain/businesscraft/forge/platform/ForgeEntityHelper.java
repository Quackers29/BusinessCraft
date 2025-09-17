package com.yourdomain.businesscraft.forge.platform;

import com.yourdomain.businesscraft.api.EntityHelper;
import com.yourdomain.businesscraft.forge.init.ForgeModEntityTypes;
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
