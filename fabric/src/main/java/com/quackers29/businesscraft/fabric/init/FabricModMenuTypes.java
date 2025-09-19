package com.quackers29.businesscraft.fabric.init;

import com.quackers29.businesscraft.api.MenuTypeHelper;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.fabric.platform.FabricMenuTypeHelper;

/**
 * Fabric menu type registration - placeholder
 */
public class FabricModMenuTypes {
    public static void register() {
        // Initialize menu type helper
        FabricMenuTypeHelper.initialize();
        System.out.println("DEBUG: Menu type registration placeholder");
    }
}
