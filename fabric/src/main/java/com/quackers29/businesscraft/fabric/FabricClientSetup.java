package com.quackers29.businesscraft.fabric;

import com.quackers29.businesscraft.api.PlatformAccess;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric client-side initialization
 */
public class FabricClientSetup implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft Fabric Client");
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("BusinessCraft Fabric client setup starting");
        
        // Initialize client-side platform helpers
        PlatformAccess.client = BusinessCraftFabric.CLIENT;
        PlatformAccess.render = BusinessCraftFabric.RENDER;
        
        // TODO: Register client-side components (screens, renderers, key bindings)
        // This will be implemented in Phase 5
        
        LOGGER.info("BusinessCraft Fabric client setup complete");
    }
}
