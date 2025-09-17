package com.yourdomain.businesscraft.client;

import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.client.renderer.TouristRenderer;
import com.yourdomain.businesscraft.init.ModEntityTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = BusinessCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Client-side setup code
    }
    
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Register the tourist renderer
        event.registerEntityRenderer(ModEntityTypes.TOURIST.get(), TouristRenderer::new);
    }
} 