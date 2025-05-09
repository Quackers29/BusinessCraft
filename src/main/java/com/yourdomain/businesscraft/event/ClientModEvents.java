package com.yourdomain.businesscraft.event;

import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.init.ModMenuTypes;
import com.yourdomain.businesscraft.screen.TownInterfaceScreen;
import com.yourdomain.businesscraft.client.TownDebugOverlay;
import com.yourdomain.businesscraft.client.TownDebugNetwork;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = BusinessCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        // Register menu screens
        event.enqueueWork(() -> {
            // Register the TownInterfaceScreen for the TOWN_INTERFACE menu type
            MenuScreens.register(ModMenuTypes.TOWN_INTERFACE.get(), TownInterfaceScreen::new);
            
            // Register network handlers for debug overlay
            TownDebugNetwork.register();
        });
    }
    
    /**
     * Register the town debug overlay 
     */
    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        // Register our overlay with a unique ID
        event.registerAboveAll("town_debug_overlay", new TownDebugOverlay());
    }
}