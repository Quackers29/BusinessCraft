package com.yourdomain.businesscraft.event;

import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.init.ModMenuTypes;
import com.yourdomain.businesscraft.ui.screens.town.TownInterfaceScreen;
import com.yourdomain.businesscraft.ui.screens.town.TradeScreen;
import com.yourdomain.businesscraft.ui.screens.town.StorageScreen;
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
        // Register all menu screens in one place for consistency
        event.enqueueWork(() -> {
            // Register the TownInterfaceScreen for the TOWN_INTERFACE menu type
            MenuScreens.register(ModMenuTypes.TOWN_INTERFACE.get(), TownInterfaceScreen::new);
            
            // Register the TradeScreen for the TRADE_MENU menu type
            MenuScreens.register(ModMenuTypes.TRADE_MENU.get(), TradeScreen::new);
            
            // Register the StorageScreen for the STORAGE_MENU menu type
            MenuScreens.register(ModMenuTypes.STORAGE_MENU.get(), StorageScreen::new);
            
            // Note: TOWN_BLOCK menu type is used internally only and doesn't need a screen registration
            
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