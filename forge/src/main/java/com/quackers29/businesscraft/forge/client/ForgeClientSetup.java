package com.quackers29.businesscraft.forge.client;

import com.quackers29.businesscraft.client.CommonClientSetup;
import net.minecraft.client.gui.screens.MenuScreens;
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

    @SubscribeEvent
    public static void init(FMLClientSetupEvent event) {
        // Initialize client helper only on client side
        com.quackers29.businesscraft.api.PlatformAccess.client = new com.quackers29.businesscraft.forge.platform.ForgeClientHelper();
        com.quackers29.businesscraft.api.PlatformAccess.render = new com.quackers29.businesscraft.forge.platform.ForgeRenderHelper();

        // Register render helper for overlay handling
        com.quackers29.businesscraft.forge.platform.ForgeRenderHelper.ForgeOverlayRegistry.setRenderHelper(
                (com.quackers29.businesscraft.forge.platform.ForgeRenderHelper) com.quackers29.businesscraft.api.PlatformAccess.render);

        // Initialize common client setup (key handlers, render events)
        event.enqueueWork(() -> {
            CommonClientSetup.init();

            // Register screens
            CommonClientSetup.registerScreens();
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Register renderers using common setup
        CommonClientSetup.registerRenderers((type, provider) -> {
            event.registerEntityRenderer((net.minecraft.world.entity.EntityType) type, provider);
        });
    }
}
