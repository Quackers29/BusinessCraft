package com.yourdomain.businesscraft;

import com.yourdomain.businesscraft.block.TownBlock;
import com.yourdomain.businesscraft.block.entity.ModBlockEntities;
import com.yourdomain.businesscraft.command.ClearTownsCommand;
import com.yourdomain.businesscraft.config.ConfigLoader;
import com.yourdomain.businesscraft.menu.ModMenuTypes;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.town.TownManager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
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

@Mod(BusinessCraft.MOD_ID)
public class BusinessCraft {
    public static final String MOD_ID = "businesscraft";
    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessCraft.class);
    
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    
    public static final RegistryObject<Block> TOWN_BLOCK = BLOCKS.register("town_block", 
        () -> new TownBlock());
    
    public static final RegistryObject<Item> TOWN_BLOCK_ITEM = ITEMS.register("town_block",
        () -> new BlockItem(TOWN_BLOCK.get(), new Item.Properties()));

    public BusinessCraft() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the DeferredRegisters
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        
        // Register other mod components
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        // Initialize networking
        ModMessages.register();

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        
        // Register server lifecycle events
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        ConfigLoader.loadConfig();
        LOGGER.info("BusinessCraft initialized. Press F3+K in-game to toggle town debug overlay.");
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
}