package com.yourdomain.businesscraft.forge.platform;

import com.yourdomain.businesscraft.api.BlockEntityHelper;
import com.yourdomain.businesscraft.forge.init.ForgeModBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Forge implementation of BlockEntityHelper
 */
public class ForgeBlockEntityHelper implements BlockEntityHelper {
    @Override
    public BlockEntityType<?> getTownInterfaceEntityType() {
        return ForgeModBlockEntities.TOWN_INTERFACE_ENTITY.get();
    }
}
