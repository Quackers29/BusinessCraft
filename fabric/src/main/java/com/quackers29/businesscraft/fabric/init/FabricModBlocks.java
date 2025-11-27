package com.quackers29.businesscraft.fabric.init;

import com.quackers29.businesscraft.init.CommonModBlocks;

/**
 * Fabric block registration.
 * Delegates to CommonModBlocks for shared registration logic.
 */
public class FabricModBlocks {
    public static void register() {
        CommonModBlocks.register();
    }
}
