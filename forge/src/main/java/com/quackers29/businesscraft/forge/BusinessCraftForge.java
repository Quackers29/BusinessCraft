package com.quackers29.businesscraft.forge;

import com.quackers29.businesscraft.forge.platform.ForgePlatformHelper;
import com.quackers29.businesscraft.forge.platform.ForgeRegistryHelper;
import com.quackers29.businesscraft.forge.platform.ForgeEventHelper;
import com.quackers29.businesscraft.forge.platform.ForgeNetworkHelper;
import com.quackers29.businesscraft.forge.platform.ForgeMenuHelper;
import com.quackers29.businesscraft.forge.platform.ForgeEntityHelper;
import com.quackers29.businesscraft.forge.platform.ForgeBlockEntityHelper;
import com.quackers29.businesscraft.forge.platform.ForgeMenuTypeHelper;
import com.quackers29.businesscraft.forge.platform.ForgeItemHandlerHelper;
import com.quackers29.businesscraft.forge.platform.ForgeNetworkMessages;
import com.quackers29.businesscraft.forge.platform.ForgeClientHelper;
import com.quackers29.businesscraft.forge.platform.ForgeRenderHelper;
import com.quackers29.businesscraft.forge.platform.ForgeTouristHelper;
import com.quackers29.businesscraft.forge.init.ForgeModBlocks;
import com.quackers29.businesscraft.forge.init.ForgeModEntityTypes;
import com.quackers29.businesscraft.forge.init.ForgeModBlockEntities;
import com.quackers29.businesscraft.forge.init.ForgeModMenuTypes;
import com.quackers29.businesscraft.forge.network.ForgeModMessages;
import com.quackers29.businesscraft.forge.event.ForgeModEvents;
import com.quackers29.businesscraft.forge.client.ForgeClientSetup;
import com.quackers29.businesscraft.api.PlatformHelper;
import com.quackers29.businesscraft.api.RegistryHelper;
import com.quackers29.businesscraft.api.EventHelper;
import com.quackers29.businesscraft.api.NetworkHelper;
import com.quackers29.businesscraft.api.MenuHelper;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.api.ClientHelper;
import com.quackers29.businesscraft.api.RenderHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.service.TouristVehicleManager;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.command.ClearTownsCommand;

/**
 * Forge-specific mod entry point for BusinessCraft.
 * This class contains only Forge-specific bootstrapping code.
 * All business logic is in the common module.
 */
@Mod(BusinessCraftForge.MOD_ID)
public class BusinessCraftForge {
    public static final String MOD_ID = "businesscraft";
    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessCraftForge.class);

    // Platform abstractions - these will be used by the common code
    public static final PlatformHelper PLATFORM = new ForgePlatformHelper();
    public static final RegistryHelper REGISTRY = new ForgeRegistryHelper();
    public static final EventHelper EVENTS = new ForgeEventHelper();
    public static final NetworkHelper NETWORK = new ForgeNetworkHelper();
    public static final MenuHelper MENUS = new ForgeMenuHelper();
    public static final com.quackers29.businesscraft.api.EntityHelper ENTITIES = new ForgeEntityHelper();
    public static final com.quackers29.businesscraft.api.BlockEntityHelper BLOCK_ENTITIES = new ForgeBlockEntityHelper();
    public static final com.quackers29.businesscraft.api.MenuTypeHelper MENU_TYPES = new ForgeMenuTypeHelper();
    public static final com.quackers29.businesscraft.api.ItemHandlerHelper ITEM_HANDLERS = new ForgeItemHandlerHelper();
    public static final com.quackers29.businesscraft.api.NetworkMessages NETWORK_MESSAGES = new ForgeNetworkMessages();
    public static final ClientHelper CLIENT = new ForgeClientHelper(); // Client-side only
    public static final RenderHelper RENDER = new ForgeRenderHelper(); // Client-side only
    public static final com.quackers29.businesscraft.api.ITouristHelper TOURIST_HELPER = new ForgeTouristHelper();

    static {
        // Initialize platform abstractions for common code access early
        PlatformAccess.platform = PLATFORM;
        PlatformAccess.registry = REGISTRY;
        PlatformAccess.events = EVENTS;
        PlatformAccess.network = NETWORK;
        PlatformAccess.menus = MENUS;
        PlatformAccess.entities = ENTITIES;
        PlatformAccess.blockEntities = BLOCK_ENTITIES;
        PlatformAccess.menuTypes = MENU_TYPES;
        PlatformAccess.itemHandlers = ITEM_HANDLERS;
        PlatformAccess.networkMessages = NETWORK_MESSAGES;
        PlatformAccess.touristHelper = TOURIST_HELPER;
    }

    // Add a static reference to the manager to use in event handlers
    public static final TouristVehicleManager TOURIST_VEHICLE_MANAGER = new TouristVehicleManager();

    public BusinessCraftForge() {
        LOGGER.info("BusinessCraft Forge constructor called!");
        System.out.println("DEBUG: BusinessCraft Forge mod starting up!");
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // ClientHelper and RenderHelper will be initialized in clientSetup() - only
        // available on client side
        // But we need to set the renderHelper reference early for overlay registration
        ForgeRenderHelper.ForgeOverlayRegistry.setRenderHelper((ForgeRenderHelper) RENDER);

        // Register DeferredRegisters with the mod event bus
        System.out.println("DEBUG: About to register DeferredRegisters with mod event bus");
        ((ForgeRegistryHelper) REGISTRY).register(modEventBus);
        System.out.println("DEBUG: DeferredRegisters registered with mod event bus");

        // Initialize Forge-specific registrations
        ForgeModBlocks.register();
        ForgeModEntityTypes.register();
        ForgeModBlockEntities.register();
        ForgeModMenuTypes.register();

        // Register our mod's event handlers
        System.out.println("DEBUG: Registering event listeners");
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        System.out.println("DEBUG: Event listeners registered");

        // Initialize networking
        ForgeModMessages.register();

        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);

        // Register server lifecycle events
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);

        // Register the world unload event listener
        MinecraftForge.EVENT_BUS.addListener(this::onLevelUnload);

        LOGGER.info("BusinessCraft Forge initialized. Press F3+K in-game to toggle town debug overlay.");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        System.out.println("DEBUG: commonSetup method called!");
        LOGGER.info("BusinessCraft Forge common setup starting");

        ConfigLoader.loadConfig();
        com.quackers29.businesscraft.economy.ResourceRegistry.load();
        com.quackers29.businesscraft.production.UpgradeRegistry.load();
        com.quackers29.businesscraft.contract.ContractBoard.getInstance().load();

        // Report active debug logging configuration
        DebugConfig.logActiveDebuggers();

        // Block items are now registered via RegistryObject
        LOGGER.info("Block items registered via RegistryObject");

        // Initialize server-side event handlers
        com.quackers29.businesscraft.event.PlayerBoundaryTracker.initialize();
        com.quackers29.businesscraft.event.PlatformPathHandler.initialize();

        LOGGER.info("BusinessCraft Forge common setup complete.");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("BusinessCraft Forge client setup starting");

        // Initialize client helper only on client side
        PlatformAccess.client = CLIENT;
        PlatformAccess.render = RENDER;

        // RenderHelper reference already set in constructor for overlay registration

        // Client-side setup handled by ForgeClientSetup
        ForgeClientSetup.init(event);

        LOGGER.info("BusinessCraft Forge client setup complete");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ClearTownsCommand.register(event.getDispatcher());
        com.quackers29.businesscraft.command.AddContractCommand.register(event.getDispatcher());
    }

    private void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Server stopping, saving town data...");
        ServerLifecycleHooks.getCurrentServer().getAllLevels().forEach(level -> {
            TownManager.get((ServerLevel) level).onServerStopping();
        });

        // Clear all instances after saving to ensure a clean slate on next load
        TownManager.clearInstances();

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

    @SubscribeEvent
    public void onCreativeModeTabBuildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS) {
            // Add our Town Interface block item to the Building Blocks creative tab
            if (com.quackers29.businesscraft.init.CommonModBlocks.TOWN_INTERFACE_BLOCK != null) {
                event.accept(com.quackers29.businesscraft.init.CommonModBlocks.TOWN_INTERFACE_BLOCK.get());
            }
        }
    }

    // Add a level unload event handler
    private void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel) {
            DebugConfig.debug(LOGGER, DebugConfig.MOD_INITIALIZATION, "Clearing tracked vehicles on level unload");
            TOURIST_VEHICLE_MANAGER.clearTrackedVehicles();
        }
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel) {
            TownManager.get((ServerLevel) event.level).tick();
            // Only tick ContractBoard once per server tick, not per level
            if (event.level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
                com.quackers29.businesscraft.contract.ContractBoard.getInstance().tick();
            }
        }
    }
}
