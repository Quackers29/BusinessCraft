package com.quackers29.businesscraft.fabric;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.fabric.event.FabricEventCallbackHandler;
import com.quackers29.businesscraft.fabric.platform.FabricMenuTypeHelper;
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
        
        // Register client-side events
        FabricEventCallbackHandler.registerClientEvents();
        
        // Register client-side packet handlers
        registerClientPackets();
        
        // Register screens for menu types
        registerScreens();
        
        LOGGER.info("BusinessCraft Fabric client setup complete");
    }
    
    /**
     * Register client-side packet handlers
     */
    private void registerClientPackets() {
        try {
            // Register client packets from FabricModMessages
            Class<?> fabricModMessagesClass = Class.forName("com.quackers29.businesscraft.fabric.FabricModMessages");
            java.lang.reflect.Method registerClientPacketsMethod = fabricModMessagesClass.getMethod("registerClientPackets");
            registerClientPacketsMethod.invoke(null);
            LOGGER.info("Client packet handlers registered");
        } catch (Exception e) {
            LOGGER.warn("Could not register client packet handlers", e);
        }
    }
    
    /**
     * Register screens for menu types using Fabric's ScreenRegistry API
     * NOTE: Screens are excluded from Fabric build, so this will use reflection at runtime
     * TODO: Implement proper screen registration once menu types are fully registered
     */
    private void registerScreens() {
        // TODO: Implement screen registration
        // Screens are in common module's ui package which is excluded from Fabric build
        // Need to use reflection to access screen classes at runtime
        // Fabric uses ScreenRegistry.register() similar to Forge's MenuScreens.register()
        LOGGER.info("Screen registration placeholder - screens will be registered when menu types are fully implemented");
    }
}
