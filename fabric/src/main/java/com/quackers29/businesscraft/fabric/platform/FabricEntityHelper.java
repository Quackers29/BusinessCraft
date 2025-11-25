package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.EntityHelper;
import com.quackers29.businesscraft.fabric.init.FabricModEntityTypes;

/**
 * Fabric implementation of EntityHelper
 */
public class FabricEntityHelper implements EntityHelper {
    @Override
    public Object getTouristEntityType() {
        return FabricModEntityTypes.TOURIST;
    }
}
