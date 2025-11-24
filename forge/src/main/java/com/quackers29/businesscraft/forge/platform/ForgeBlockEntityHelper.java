package com.quackers29.businesscraft.forge.platform;

import com.quackers29.businesscraft.api.BlockEntityHelper;
import com.quackers29.businesscraft.forge.init.ForgeModBlockEntities;
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
