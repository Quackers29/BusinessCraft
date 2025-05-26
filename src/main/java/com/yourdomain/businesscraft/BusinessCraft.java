package com.yourdomain.businesscraft;

import com.yourdomain.businesscraft.block.TownBlock;
import com.yourdomain.businesscraft.block.TownInterfaceBlock;
import com.yourdomain.businesscraft.block.entity.ModBlockEntities;
import com.yourdomain.businesscraft.command.ClearTownsCommand;
import com.yourdomain.businesscraft.config.ConfigLoader;
import com.yourdomain.businesscraft.init.ModMenuTypes;
import com.yourdomain.businesscraft.init.ModBlocks;
import com.yourdomain.businesscraft.menu.TradeMenu;
import com.yourdomain.businesscraft.menu.StorageMenu;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.screen.TradeScreen;
import com.yourdomain.businesscraft.screen.StorageScreen;
import com.yourdomain.businesscraft.town.TownManager;
import net.minecraft.client.gui.screens.MenuScreens;
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
import com.yourdomain.businesscraft.service.TouristVehicleManager;
import net.minecraftforge.event.level.LevelEvent;
import com.yourdomain.businesscraft.entity.ModEntityTypes;

@Mod(BusinessCraft.MOD_ID)
public class BusinessCraft {
    public static final String MOD_ID = "businesscraft";
    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessCraft.class);
    
    // Add a static reference to the manager to use in event handlers
    public static final TouristVehicleManager TOURIST_VEHICLE_MANAGER = new TouristVehicleManager();

    public BusinessCraft() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Register blocks and items
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        
        // Register entity types
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        
        // Register our mod's event handlers
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        
        // Initialize networking
        ModMessages.register();
        
        // Register block entities
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        
        // Register our menus
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        
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
        LOGGER.info("BusinessCraft initialized. Press F3+K in-game to toggle town debug overlay.");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("BusinessCraft client setup starting");
        
        // Register screen factories
        MenuScreens.register(ModMenuTypes.TRADE_MENU.get(), TradeScreen::new);
        MenuScreens.register(ModMenuTypes.STORAGE_MENU.get(), StorageScreen::new);
        // TownInterfaceScreen registration is handled in ClientModEvents
        
        LOGGER.info("BusinessCraft client setup complete");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ClearTownsCommand.register(event.getDispatcher());
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

    // Add a level unload event handler (replacing WorldEvent with LevelEvent)
    private void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel) {
            LOGGER.debug("Clearing tracked vehicles on level unload");
            TOURIST_VEHICLE_MANAGER.clearTrackedVehicles();
        }
    }
}