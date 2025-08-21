package com.quackers29.businesscraft.platform.fabric;

import com.quackers29.businesscraft.platform.MenuHelper;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;

/**
 * Fabric implementation of MenuHelper using Yarn mappings.
 * Implements cross-platform menu operations using Fabric Screen Handler API.
 */
public class FabricMenuHelper implements MenuHelper {
    
    @Override
    public Object createExtendedScreenHandlerFactory(Object factory) {
        // In Fabric, we can directly use ExtendedScreenHandlerFactory
        if (factory instanceof ExtendedScreenHandlerFactory) {
            return factory;
        }
        
        // If it's not already an ExtendedScreenHandlerFactory, we need to wrap it
        // This is a simplification - in practice, you might need more sophisticated handling
        return factory;
    }
}