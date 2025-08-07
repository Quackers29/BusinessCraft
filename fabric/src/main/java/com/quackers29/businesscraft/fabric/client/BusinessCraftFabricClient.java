package com.quackers29.businesscraft.fabric.client;

import com.quackers29.businesscraft.debug.DebugConfig;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric client-side mod initializer.
 * Handles client-specific initialization for Fabric platform.
 */
public class BusinessCraftFabricClient implements ClientModInitializer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessCraftFabricClient.class);
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("BusinessCraft Fabric client initializing...");
        
        // TODO: Initialize client-side systems (once moved to common module)
        // - Screen registrations
        // - Entity renderers  
        // - Particle effects
        // - Key bindings
        // - Visualization system
        
        DebugConfig.debug(LOGGER, DebugConfig.CLIENT_HANDLERS, "Client initialization complete");
        LOGGER.info("BusinessCraft Fabric client initialized (basic client initialization - full client features pending common module migration).");
    }
}