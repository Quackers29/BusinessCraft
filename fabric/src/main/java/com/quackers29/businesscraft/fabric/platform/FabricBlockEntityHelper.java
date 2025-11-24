package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.BlockEntityHelper;
import com.quackers29.businesscraft.fabric.init.FabricModBlockEntities;

import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Fabric implementation of BlockEntityHelper
 */
public class FabricBlockEntityHelper implements BlockEntityHelper {
    @Override
    public BlockEntityType<?> getTownInterfaceEntityType() {
        return FabricModBlockEntities.getTownInterfaceEntityType();
    }
}
