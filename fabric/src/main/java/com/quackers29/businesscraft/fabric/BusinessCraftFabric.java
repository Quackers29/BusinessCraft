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
import com.quackers29.businesscraft.fabric.init.FabricModBlocks;
import com.quackers29.businesscraft.fabric.init.FabricModEntityTypes;
import com.quackers29.businesscraft.fabric.init.FabricModBlockEntities;
import com.quackers29.businesscraft.fabric.init.FabricModMenuTypes;
import com.quackers29.businesscraft.fabric.FabricModMessages;
import com.quackers29.businesscraft.fabric.FabricModEvents;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        // ClientHelper and RenderHelper will be initialized in clientSetup() - only available on client side

        // Register Fabric-specific registrations
        FabricModBlocks.register();
        FabricModEntityTypes.register();
        FabricModBlockEntities.register();
        FabricModMenuTypes.register(); // Menu types registration

        // Initialize networking
        FabricModMessages.register();

        // Register events
        FabricModEvents.register();

        LOGGER.info("BusinessCraft Fabric initialized successfully!");
    }
}
