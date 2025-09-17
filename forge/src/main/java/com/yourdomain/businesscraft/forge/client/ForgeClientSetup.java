package com.yourdomain.businesscraft.forge.client;

import com.yourdomain.businesscraft.client.renderer.TouristRenderer;
import com.yourdomain.businesscraft.forge.init.ForgeModEntityTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Forge-specific client setup
 */
@Mod.EventBusSubscriber(modid = "businesscraft", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ForgeClientSetup {

    public static void init() {
        // Any additional client initialization can go here
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Client-side setup code
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Register the tourist renderer
        event.registerEntityRenderer(ForgeModEntityTypes.TOURIST, TouristRenderer::new);
    }
}
