package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.MenuHelper;

/**
 * Fabric implementation of MenuHelper
 */
public class FabricMenuHelper implements MenuHelper {
    @Override
    public <M, U> void registerScreenFactory(Object menuType, MenuHelper.ScreenFactory<M, U> screenFactory) {
        // This will be called during client setup
        // For now, we'll store the factories and register them later
    }
}
