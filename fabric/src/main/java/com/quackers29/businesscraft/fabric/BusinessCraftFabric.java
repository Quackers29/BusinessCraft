package com.quackers29.businesscraft.fabric;

import com.quackers29.businesscraft.init.*;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.event.ModEvents;
import com.quackers29.businesscraft.event.PlayerBoundaryTracker;
import com.quackers29.businesscraft.network.ModMessages;
import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.platform.PlatformServiceProvider;
import com.quackers29.businesscraft.platform.fabric.FabricPlatformServices;
import com.quackers29.businesscraft.service.TouristVehicleManager;
import com.quackers29.businesscraft.town.TownManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusinessCraftFabric implements ModInitializer {
    public static final String MOD_ID = "businesscraft";
    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessCraftFabric.class);
    
    // Static reference to the manager to use in event handlers
    public static final TouristVehicleManager TOURIST_VEHICLE_MANAGER = new TouristVehicleManager();

    @Override
    public void onInitialize() {
        LOGGER.info("BusinessCraft Fabric initializing...");
        
        // Initialize Fabric platform services using Enhanced MultiLoader approach
        FabricPlatformServices fabricServices = new FabricPlatformServices();
        PlatformServices.setPlatform(
            fabricServices.getPlatformHelper(),
            fabricServices.getRegistryHelper(),
            fabricServices.getNetworkHelper(),
            fabricServices.getEventHelper(),
            fabricServices.getInventoryHelper(),
            fabricServices.getMenuHelper(),
            fabricServices.getBlockEntityHelper()
        );
        
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
        
        // Initialize platform-agnostic registration
        ModBlocks.initialize();
        ModBlockEntities.initialize();
        ModEntityTypes.initialize();
        ModMenuTypes.initialize();
        
        // Load configuration
        ConfigLoader.loadConfig();
        
        // Initialize networking
        ModMessages.register();
        
        // Initialize platform-agnostic event handlers
        ModEvents.initialize();
        PlayerBoundaryTracker.initialize();
        
        // Register server lifecycle events
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Server started, initializing town data...");
            server.getAllLevels().forEach(level -> {
                TownManager townManager = TownManager.get(level);
                int townCount = townManager.getAllTowns().size();
                LOGGER.info("Loaded {} towns for level: {}", townCount, level.dimension().location());
            });
        });
        
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Server stopping, saving town data...");
            server.getAllLevels().forEach(level -> {
                TownManager.get(level).onServerStopping();
            });
            
            // Clear all instances after saving to ensure a clean slate on next load
            TownManager.clearInstances();
            
            // Clear tracked vehicles
            LOGGER.info("Clearing tracked vehicles on server stopping");
            TOURIST_VEHICLE_MANAGER.clearTrackedVehicles();
        });
        
        // Register world unload event
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            DebugConfig.debug(LOGGER, DebugConfig.MOD_INITIALIZATION, "Clearing tracked vehicles on world unload");
            TOURIST_VEHICLE_MANAGER.clearTrackedVehicles();
        });
        
        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            com.quackers29.businesscraft.command.ClearTownsCommand.register(dispatcher);
        });
        
        // Report active debug logging configuration
        DebugConfig.logActiveDebuggers();
        
        LOGGER.info("BusinessCraft Fabric initialized. Press F3+K in-game to toggle town debug overlay.");
    }
}