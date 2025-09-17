package com.yourdomain.businesscraft.forge.event;

import com.yourdomain.businesscraft.forge.platform.ForgeMenuTypeHelper;
import com.yourdomain.businesscraft.ui.screens.town.TownInterfaceScreen;
import com.yourdomain.businesscraft.ui.screens.town.TradeScreen;
import com.yourdomain.businesscraft.ui.screens.town.StorageScreen;
import com.yourdomain.businesscraft.ui.screens.town.PaymentBoardScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Forge-specific client event handlers
 */
@Mod.EventBusSubscriber(modid = "businesscraft", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ForgeClientModEvents {

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        // TODO: Fix screen registration - need to find correct MenuScreens package for Forge 1.20.1
        // For now, screens will need to be registered manually or through another method

        com.yourdomain.businesscraft.forge.platform.ForgeMenuTypeHelper menuTypes =
            (com.yourdomain.businesscraft.forge.platform.ForgeMenuTypeHelper) com.yourdomain.businesscraft.forge.BusinessCraftForge.MENU_TYPES;

        // Temporarily commented out - need correct API
        // event.enqueueWork(() -> {
        //     // Register menu screens here
        // });
    }
}
