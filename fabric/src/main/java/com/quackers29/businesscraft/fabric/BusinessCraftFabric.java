
package com.quackers29.businesscraft.fabric;

import com.quackers29.businesscraft.fabric.platform.*;
import com.quackers29.businesscraft.api.PlatformHelper;
import com.quackers29.businesscraft.api.RegistryHelper;
import com.quackers29.businesscraft.api.EventHelper;
import com.quackers29.businesscraft.api.NetworkHelper;
import com.quackers29.businesscraft.api.MenuHelper;
import com.quackers29.businesscraft.api.EntityHelper;
import com.quackers29.businesscraft.api.BlockEntityHelper;
import com.quackers29.businesscraft.api.MenuTypeHelper;
import com.quackers29.businesscraft.api.ItemHandlerHelper;
import com.quackers29.businesscraft.api.NetworkMessages;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.api.ClientHelper;
import com.quackers29.businesscraft.api.RenderHelper;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.fabric.init.FabricModBlocks;
import com.quackers29.businesscraft.fabric.init.FabricModEntityTypes;
import com.quackers29.businesscraft.fabric.init.FabricModBlockEntities;
import com.quackers29.businesscraft.fabric.init.FabricModMenuTypes;
import com.quackers29.businesscraft.fabric.FabricModMessages;
import com.quackers29.businesscraft.fabric.FabricModEvents;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.resources.ResourceLocation;
import com.quackers29.businesscraft.network.packets.ResourceSyncPacket;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import com.quackers29.businesscraft.town.TownManager;

/**
 * Fabric-specific mod entry point for BusinessCraft.
 * This class contains only Fabric-specific bootstrapping code.
 */
public class BusinessCraftFabric implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft Fabric");

    // Platform helpers - Fabric implementations
    public static final PlatformHelper PLATFORM = new FabricPlatformHelper();
    public static final RegistryHelper REGISTRY = new FabricRegistryHelper();
    public static final EventHelper EVENTS = new FabricEventHelper();
    public static final NetworkHelper NETWORK = new FabricNetworkHelper();
    public static final MenuHelper MENUS = new FabricMenuHelper();
    public static final EntityHelper ENTITIES = new FabricEntityHelper();
    public static final BlockEntityHelper BLOCK_ENTITIES = new FabricBlockEntityHelper();
    public static final MenuTypeHelper MENU_TYPES = new FabricMenuTypeHelper();
    public static final ItemHandlerHelper ITEM_HANDLERS = new FabricItemHandlerHelper();
    public static final NetworkMessages NETWORK_MESSAGES = new FabricNetworkMessages();
    // Client-side only helpers - initialized in client setup
    public static final ClientHelper CLIENT = new FabricClientHelper();
    // RenderHelper uses reflection to avoid compile-time GuiGraphics dependency
    public static final RenderHelper RENDER = new FabricRenderHelper();
    public static final com.quackers29.businesscraft.api.ITouristHelper TOURIST_HELPER = new FabricTouristHelper();

    @Override
    public void onInitialize() {
        LOGGER.info("BusinessCraft Fabric constructor called!");
        System.out.println("DEBUG: BusinessCraft Fabric mod starting up!");

        // Initialize platform abstractions for common code access
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

        // Load config
        com.quackers29.businesscraft.config.ConfigLoader.loadConfig();
        // ClientHelper and RenderHelper will be initialized in clientSetup() - only
        // available on client side

        // Register Fabric-specific registrations
        // Register blocks and items during mod initialization
        LOGGER.info("Registering blocks and items...");
        try {
            FabricModBlocks.register();
            LOGGER.info("Blocks registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register blocks", e);
        }

        try {
            FabricModEntityTypes.register();
            LOGGER.info("Entity types registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register entity types", e);
        }

        try {
            FabricModBlockEntities.register();
            LOGGER.info("Block entities registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register block entities", e);
        }

        try {
            FabricModMenuTypes.register();
            LOGGER.info("Menu types registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register menu types", e);
        }

        // Initialize networking
        FabricModMessages.register();

        // Register client packets if we're on the client side
        // This is a workaround since FabricClientSetup may not be running
        try {
            net.fabricmc.loader.api.FabricLoader fabricLoader = net.fabricmc.loader.api.FabricLoader.getInstance();
            if (fabricLoader.getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                        "Registering client packets from main mod initializer");
                FabricModMessages.registerClientPackets();
            }
        } catch (Exception e) {
            LOGGER.warn("Could not register client packets from main initializer", e);
        }

        // Add to packet registration (find ModMessages or
        // PlatformAccess.getNetworkMessages().registerS2C)
        // Remove this invalid line - registration handled by
        // FabricModMessages.register()
        /*
         * PlatformAccess.getNetworkMessages().registerS2C(new
         * ResourceLocation("businesscraft", "resource_sync"),
         * ResourceSyncPacket::decode, ResourceSyncPacket::handle);
         * LOGGER.info("Registered ResourceSyncPacket for Fabric resource sync");
         */

        // Initialize common event handlers
        com.quackers29.businesscraft.event.PlayerBoundaryTracker.initialize();
        com.quackers29.businesscraft.event.PlatformPathHandler.initialize();

        // Load registries
        com.quackers29.businesscraft.economy.ResourceRegistry.load();
        com.quackers29.businesscraft.production.UpgradeRegistry.load();

        // Register events
        FabricModEvents.register();

        // Register server tick event
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            TownManager.get(level).tick();
            // Tick ContractBoard for each level
            com.quackers29.businesscraft.contract.ContractBoard.get(level).tick(level);
        });

        // Register server lifecycle events to capture server instance
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            FabricModMessages.setServer(server);
        });

        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            FabricModMessages.setServer(null);
            com.quackers29.businesscraft.contract.ContractBoard.clearInstances();
            com.quackers29.businesscraft.town.TownManager.clearInstances();
        });

        LOGGER.info("BusinessCraft Fabric initialized successfully!");
    }
}
