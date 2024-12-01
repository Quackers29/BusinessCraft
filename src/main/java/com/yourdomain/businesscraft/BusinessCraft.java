package com.yourdomain.businesscraft;

import com.yourdomain.businesscraft.registry.ModBlocks;
import com.yourdomain.businesscraft.block.entity.ModBlockEntities;
import com.yourdomain.businesscraft.menu.ModMenuTypes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.yourdomain.businesscraft.config.ConfigLoader;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.yourdomain.businesscraft.network.ModMessages;

@Mod(BusinessCraft.MOD_ID)
public class BusinessCraft {
    public static final String MOD_ID = "businesscraft";

    public BusinessCraft() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register our blocks and items
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::setup);

        ModMessages.register();
    }

    @SubscribeEvent
    public void setup(FMLCommonSetupEvent event) {
        ConfigLoader.loadConfig();
    }
}