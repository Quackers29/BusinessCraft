package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.MenuHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Fabric implementation of MenuHelper
 * Stores screen factories for later registration during client setup
 */
public class FabricMenuHelper implements MenuHelper {
    // Store screen factories mapped by menu type
    private static final Map<Object, MenuHelper.ScreenFactory<?, ?>> screenFactories = new HashMap<>();
    
    // @Override - temporarily removed due to classpath issue with common module interfaces
    public <M, U> void registerScreenFactory(Object menuType, MenuHelper.ScreenFactory<M, U> screenFactory) {
        // Store the factory for later registration during client setup
        screenFactories.put(menuType, screenFactory);
    }
    
    /**
     * Get all registered screen factories
     * Called during client setup to register screens with Fabric's API
     */
    public static Map<Object, MenuHelper.ScreenFactory<?, ?>> getScreenFactories() {
        return screenFactories;
    }
}
