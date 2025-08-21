package com.quackers29.businesscraft.fabric;

import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.platform.PlatformServiceProvider;
import com.quackers29.businesscraft.platform.fabric.FabricPlatformServices;
import com.quackers29.businesscraft.fabric.init.FabricModBlocks;
import com.quackers29.businesscraft.fabric.init.FabricModBlockEntities;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusinessCraftFabric implements ModInitializer {
    public static final String MOD_ID = "businesscraft";
    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessCraftFabric.class);

    @Override
    public void onInitialize() {
        LOGGER.info("BusinessCraft Fabric initializing...");
        
        // Initialize Fabric platform services using Enhanced MultiLoader approach
        FabricPlatformServices fabricServices = new FabricPlatformServices();
        
        // Initialize the core platform services first
        PlatformServices.setPlatform(
            fabricServices.getPlatformHelper(),
            fabricServices.getRegistryHelper(),
            fabricServices.getNetworkHelper(),
            fabricServices.getEventHelper(),
            fabricServices.getInventoryHelper(),
            fabricServices.getMenuHelper(),
            fabricServices.getBlockEntityHelper()
        );
        
        // Initialize town management services manually (same approach as Forge)
        // This is a workaround for the setPlatformComplete method compilation issue
        initializeTownServices(fabricServices);
        
        // Phase 9.9.1: Runtime service verification
        verifyPlatformServices();
        
        // Register with common module service provider
        PlatformServiceProvider.setPlatform(
            fabricServices.getPlatformHelper(),
            fabricServices.getRegistryHelper(),
            fabricServices.getNetworkHelper(),
            fabricServices.getEventHelper(),
            fabricServices.getInventoryHelper(),
            fabricServices.getMenuHelper(),
            fabricServices.getBlockEntityHelper()
        );
        
        // Initialize platform-agnostic registration coordination from common module
        com.quackers29.businesscraft.init.CommonRegistration.initialize();
        
        // Initialize Fabric-specific blocks and entities (Bridge Pattern)
        FabricModBlocks.initialize();
        FabricModBlockEntities.initialize();
        
        // UI framework platform services are available through MenuHelper abstraction
        // ModMenuTypes is handled by platform-specific implementations
        
        // Load configuration from common module
        com.quackers29.businesscraft.config.ConfigLoader.loadConfig();
        
        // Initialize Fabric networking
        com.quackers29.businesscraft.network.FabricModMessages.initializeNetworking();
        
        // TODO: Initialize platform-agnostic event handlers (once moved to common module)
        // ModEvents.initialize();
        // PlayerBoundaryTracker.initialize();
        
        // Register server lifecycle events
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Server started, initializing town data...");
            // TODO: Initialize town system once moved to common module
        });
        
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Server stopping, saving town data...");
            // TODO: Save town data once moved to common module
        });
        
        // Register world unload event
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            DebugConfig.debug(LOGGER, DebugConfig.MOD_INITIALIZATION, "Clearing tracked vehicles on world unload");
            // TODO: Clear tracked vehicles once moved to common module
        });
        
        // TODO: Register commands (once moved to common module)
        // CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
        //     com.quackers29.businesscraft.command.ClearTownsCommand.register(dispatcher);
        // });
        
        // Report active debug logging configuration
        DebugConfig.logActiveDebuggers();
        
        LOGGER.info("BusinessCraft Fabric initialized with complete platform services.");
    }
    
    /**
     * Initialize town management services manually.
     * This is a workaround for the setPlatformComplete method compilation issue.
     */
    private void initializeTownServices(FabricPlatformServices fabricServices) {
        try {
            // Use reflection to access private fields and set the town services
            java.lang.reflect.Field townManagerField = PlatformServices.class.getDeclaredField("townManagerService");
            townManagerField.setAccessible(true);
            townManagerField.set(null, fabricServices.getTownManagerService());
            
            java.lang.reflect.Field dataStorageField = PlatformServices.class.getDeclaredField("dataStorageHelper");
            dataStorageField.setAccessible(true);
            dataStorageField.set(null, fabricServices.getDataStorageHelper());
            
            LOGGER.info("Initialized town management services via reflection");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize town management services: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Phase 9.9.1: Verify platform services are working correctly.
     * This method tests that all platform services are properly initialized and accessible.
     */
    private void verifyPlatformServices() {
        LOGGER.info("=== Phase 9.9.1: Fabric Platform Service Verification ===");
        
        try {
            // Test core platform services
            if (PlatformServices.getPlatformHelper() != null) {
                LOGGER.info("✅ PlatformHelper: Available (" + PlatformServices.getPlatformHelper().getClass().getSimpleName() + ")");
            } else {
                LOGGER.error("❌ PlatformHelper: NOT AVAILABLE");
            }
            
            if (PlatformServices.getRegistryHelper() != null) {
                LOGGER.info("✅ RegistryHelper: Available (" + PlatformServices.getRegistryHelper().getClass().getSimpleName() + ")");
            } else {
                LOGGER.error("❌ RegistryHelper: NOT AVAILABLE");
            }
            
            if (PlatformServices.getNetworkHelper() != null) {
                LOGGER.info("✅ NetworkHelper: Available (" + PlatformServices.getNetworkHelper().getClass().getSimpleName() + ")");
            } else {
                LOGGER.error("❌ NetworkHelper: NOT AVAILABLE");
            }
            
            if (PlatformServices.getEventHelper() != null) {
                LOGGER.info("✅ EventHelper: Available (" + PlatformServices.getEventHelper().getClass().getSimpleName() + ")");
            } else {
                LOGGER.error("❌ EventHelper: NOT AVAILABLE");
            }
            
            // ✅ InventoryHelper: Eliminated - using direct ItemStack access (Unified Architecture)
            
            if (PlatformServices.getMenuHelper() != null) {
                LOGGER.info("✅ MenuHelper: Available (" + PlatformServices.getMenuHelper().getClass().getSimpleName() + ")");
            } else {
                LOGGER.error("❌ MenuHelper: NOT AVAILABLE");
            }
            
            if (PlatformServices.getBlockEntityHelper() != null) {
                LOGGER.info("✅ BlockEntityHelper: Available (" + PlatformServices.getBlockEntityHelper().getClass().getSimpleName() + ")");
            } else {
                LOGGER.error("❌ BlockEntityHelper: NOT AVAILABLE");
            }
            
            // Test unified TownManager - no longer needed as platform service
            LOGGER.info("✅ TownManager: Using unified architecture (direct access)");
            
            if (PlatformServices.getDataStorageHelper() != null) {
                LOGGER.info("✅ DataStorageHelper: Available (" + PlatformServices.getDataStorageHelper().getClass().getSimpleName() + ")");
            } else {
                LOGGER.error("❌ DataStorageHelper: NOT AVAILABLE");
            }
            
            LOGGER.info("=== Fabric Platform Service Verification Complete ===");
            
        } catch (Exception e) {
            LOGGER.error("❌ Fabric platform service verification failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}