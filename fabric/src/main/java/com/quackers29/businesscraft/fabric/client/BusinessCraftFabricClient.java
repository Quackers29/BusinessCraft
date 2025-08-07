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
        
        try {
            // Initialize platform-agnostic client event handlers using reflection
            // This uses the same pattern as Forge but through reflection to avoid direct imports
            
            Class<?> clientModEventsClass = Class.forName("com.quackers29.businesscraft.event.ClientModEvents");
            java.lang.reflect.Method initializeMethod = clientModEventsClass.getMethod("initialize");
            initializeMethod.invoke(null);
            LOGGER.info("ClientModEvents initialized");
            
            Class<?> clientSetupClass = Class.forName("com.quackers29.businesscraft.client.ClientSetup");
            java.lang.reflect.Method clientInitializeMethod = clientSetupClass.getMethod("initialize");
            clientInitializeMethod.invoke(null);
            LOGGER.info("ClientSetup initialized");
            
            // The actual screen registrations, entity renderers, etc. will be handled
            // by the platform-specific event handlers through PlatformServices.getEventHelper()
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize client systems: " + e.getMessage());
            e.printStackTrace();
        }
        
        DebugConfig.debug(LOGGER, DebugConfig.CLIENT_HANDLERS, "Client initialization complete");
        LOGGER.info("BusinessCraft Fabric client initialized with platform-agnostic client systems.");
    }
}