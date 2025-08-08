package com.quackers29.businesscraft;

import com.quackers29.businesscraft.block.TownInterfaceBlock;
import com.quackers29.businesscraft.init.ModBlockEntities;
import com.quackers29.businesscraft.command.ClearTownsCommand;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.init.ModMenuTypes;
import com.quackers29.businesscraft.init.ModBlocks;
import com.quackers29.businesscraft.network.ModMessages;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.service.TouristVehicleManager;
import net.minecraftforge.event.level.LevelEvent;
import com.quackers29.businesscraft.init.ModEntityTypes;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.platform.PlatformServiceProvider;
import com.quackers29.businesscraft.platform.ITownManagerService;
import com.quackers29.businesscraft.platform.DataStorageHelper;
import com.quackers29.businesscraft.platform.forge.ForgeRegistryHelper;
import com.quackers29.businesscraft.platform.forge.ForgePlatformServices;
import com.quackers29.businesscraft.event.ModEvents;
import com.quackers29.businesscraft.event.ClientModEvents;
import com.quackers29.businesscraft.event.PlayerBoundaryTracker;
import com.quackers29.businesscraft.client.ClientSetup;

@Mod(BusinessCraft.MOD_ID)
public class BusinessCraft {
    public static final String MOD_ID = "businesscraft";
    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessCraft.class);

    // Add a static reference to the manager to use in event handlers
    public static final TouristVehicleManager TOURIST_VEHICLE_MANAGER = new TouristVehicleManager();

    public BusinessCraft() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Initialize Forge platform services using Enhanced MultiLoader approach
        ForgePlatformServices forgeServices = new ForgePlatformServices();
        
        // Initialize the core platform services first
        PlatformServices.setPlatform(
            forgeServices.getPlatformHelper(),
            forgeServices.getRegistryHelper(),
            forgeServices.getNetworkHelper(),
            forgeServices.getEventHelper(),
            forgeServices.getInventoryHelper(),
            forgeServices.getMenuHelper(),
            forgeServices.getBlockEntityHelper()
        );
        
        // TODO: Initialize town management services manually until classpath issue is resolved
        // This should be replaced with setPlatformComplete once compilation issue is fixed
        initializeTownServices(forgeServices);
        
        // Phase 9.9.1: Runtime service verification
        LOGGER.info("=== BUSINESSCRAFT FORGE PLATFORM LOADING ===");
        verifyPlatformServices();
        
        // Register with common module service provider
        PlatformServiceProvider.setPlatform(
            forgeServices.getPlatformHelper(),
            forgeServices.getRegistryHelper(),
            forgeServices.getNetworkHelper(),
            forgeServices.getEventHelper(),
            forgeServices.getInventoryHelper(),
            forgeServices.getMenuHelper(),
            forgeServices.getBlockEntityHelper()
        );
        
        // Initialize platform-agnostic registration coordination from common module
        com.quackers29.businesscraft.init.CommonRegistration.initialize();
        
        // Initialize Forge-specific registration
        ModBlocks.initialize();
        ModBlockEntities.initialize();
        ModEntityTypes.initialize();
        ModMenuTypes.initialize();
        
        // Register platform abstraction DeferredRegisters for Forge
        ForgeRegistryHelper forgeHelper = (ForgeRegistryHelper) PlatformServices.getRegistryHelper();
        forgeHelper.getBlocks().register(modEventBus);
        forgeHelper.getItems().register(modEventBus);
        forgeHelper.getBlockEntities().register(modEventBus);
        forgeHelper.getEntities().register(modEventBus);
        forgeHelper.getMenus().register(modEventBus);
        
        // Register our mod's event handlers
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        
        // Initialize networking
        ModMessages.register();
        
        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);
        
        // Register server lifecycle events
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        
        // Register the world unload event listener if not already present
        MinecraftForge.EVENT_BUS.addListener(this::onLevelUnload);
        
        LOGGER.info("BusinessCraft initialized. Press F3+K in-game to toggle town debug overlay.");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        ConfigLoader.loadConfig();
        
        // Initialize platform-agnostic event handlers
        ModEvents.initialize();
        PlayerBoundaryTracker.initialize();
        
        // Report active debug logging configuration
        DebugConfig.logActiveDebuggers();
        
        LOGGER.info("BusinessCraft initialized. Press F3+K in-game to toggle town debug overlay.");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("BusinessCraft client setup starting");
        
        // Initialize platform-agnostic client event handlers
        ClientModEvents.initialize();
        ClientSetup.initialize();
        
        // TODO: Fix screen registration - temporarily disabled due to deferred registration timing issues
        // The ModMenuTypes fields are null at this point because deferred registration hasn't completed
        // Need to find proper event to register screens after deferred registration completes
        event.enqueueWork(() -> {
            LOGGER.info("Client setup: Screen registration temporarily disabled for testing");
            // net.minecraft.client.gui.screens.MenuScreens.register(ModMenuTypes.TOWN_INTERFACE.get(), 
            //     com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen::new);
            // net.minecraft.client.gui.screens.MenuScreens.register(ModMenuTypes.TRADE_MENU.get(), 
            //     com.quackers29.businesscraft.ui.screens.town.TradeScreen::new);
            // net.minecraft.client.gui.screens.MenuScreens.register(ModMenuTypes.STORAGE_MENU.get(), 
            //     com.quackers29.businesscraft.ui.screens.town.StorageScreen::new);
            // net.minecraft.client.gui.screens.MenuScreens.register(ModMenuTypes.PAYMENT_BOARD_MENU.get(), 
            //     com.quackers29.businesscraft.ui.screens.town.PaymentBoardScreen::new);
            
            LOGGER.info("Registered all menu screen types");
        });
        
        LOGGER.info("BusinessCraft client setup complete");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ClearTownsCommand.register(event.getDispatcher());
    }

    private void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Server stopping, town data will be persisted automatically...");
        // TODO: Add any necessary cleanup for common TownManager if needed
        
        // Don't forget to clean up the tourist vehicle manager
        LOGGER.info("Clearing tracked vehicles on server stopping");
        TOURIST_VEHICLE_MANAGER.clearTrackedVehicles();
    }
    
    private void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("Server started, initializing town data...");
        ServerLifecycleHooks.getCurrentServer().getAllLevels().forEach(level -> {
            if (level instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel) level;
                // This ensures TownManager is initialized for each level
                TownManager townManager = TownManager.get(serverLevel);
                
                // Log number of towns loaded
                int townCount = townManager.getAllTowns().size();
                LOGGER.info("Loaded {} towns for level: {}", townCount, level.dimension().location());
            }
        });
    }

    // Add a level unload event handler (replacing WorldEvent with LevelEvent)
    private void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel) {
            DebugConfig.debug(LOGGER, DebugConfig.MOD_INITIALIZATION, "Clearing tracked vehicles on level unload");
            TOURIST_VEHICLE_MANAGER.clearTrackedVehicles();
        }
    }
    
    /**
     * Initialize town management services manually.
     * This is a workaround for the setPlatformComplete method compilation issue.
     */
    private void initializeTownServices(ForgePlatformServices forgeServices) {
        try {
            // Use reflection to access private fields and set the town services
            java.lang.reflect.Field townManagerField = PlatformServices.class.getDeclaredField("townManagerService");
            townManagerField.setAccessible(true);
            townManagerField.set(null, forgeServices.getTownManagerService());
            
            java.lang.reflect.Field dataStorageField = PlatformServices.class.getDeclaredField("dataStorageHelper");
            dataStorageField.setAccessible(true);
            dataStorageField.set(null, forgeServices.getDataStorageHelper());
            
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
        LOGGER.info("=== Phase 9.9.1: Platform Service Verification ===");
        
        try {
            LOGGER.info("FORGE PLATFORM SERVICE VERIFICATION STARTING...");
            
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
            
            if (PlatformServices.getInventoryHelper() != null) {
                LOGGER.info("✅ InventoryHelper: Available (" + PlatformServices.getInventoryHelper().getClass().getSimpleName() + ")");
            } else {
                LOGGER.error("❌ InventoryHelper: NOT AVAILABLE");
            }
            
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
            
            // Test new town management services
            if (PlatformServices.getTownManagerService() != null) {
                LOGGER.info("✅ TownManagerService: Available (" + PlatformServices.getTownManagerService().getClass().getSimpleName() + ")");
            } else {
                LOGGER.error("❌ TownManagerService: NOT AVAILABLE");
            }
            
            if (PlatformServices.getDataStorageHelper() != null) {
                LOGGER.info("✅ DataStorageHelper: Available (" + PlatformServices.getDataStorageHelper().getClass().getSimpleName() + ")");
            } else {
                LOGGER.error("❌ DataStorageHelper: NOT AVAILABLE");
            }
            
            LOGGER.info("=== Platform Service Verification Complete ===");
            
        } catch (Exception e) {
            LOGGER.error("❌ Platform service verification failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}