package com.quackers29.businesscraft.forge.init;

import com.quackers29.businesscraft.init.CommonModBlocks;

/**
 * Forge-specific block registrations.
 * Delegates to CommonModBlocks for shared registration logic.
 */
public class ForgeModBlocks {
    public static void register() {
        CommonModBlocks.register();
    }
}
