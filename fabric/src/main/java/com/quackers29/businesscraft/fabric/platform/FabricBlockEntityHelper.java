package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.BlockEntityHelper;
import com.quackers29.businesscraft.fabric.init.FabricModBlockEntities;

/**
 * Fabric implementation of BlockEntityHelper
 */
public class FabricBlockEntityHelper implements BlockEntityHelper {
    @Override
    public Object getTownInterfaceEntityType() {
        return FabricModBlockEntities.getTownInterfaceEntityType();
    }
}
