package com.yourdomain.businesscraft.forge.event;

import com.yourdomain.businesscraft.forge.platform.ForgeMenuTypeHelper;
import com.yourdomain.businesscraft.ui.screens.town.TownInterfaceScreen;
import com.yourdomain.businesscraft.ui.screens.town.TradeScreen;
import com.yourdomain.businesscraft.ui.screens.town.StorageScreen;
import com.yourdomain.businesscraft.ui.screens.town.PaymentBoardScreen;
import net.minecraftforge.api.distmarker.Dist;
// RegisterMenuScreensEvent import - may need to check Forge version
// import net.minecraftforge.client.event.RegisterMenuScreensEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge-specific client event handlers
 */
@Mod.EventBusSubscriber(modid = "businesscraft", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ForgeClientModEvents {

    // Temporarily disabled - need to check correct Forge event for menu screen registration
    // @SubscribeEvent
    // public static void registerMenuScreens(RegisterMenuScreensEvent event) {
    //     ForgeMenuTypeHelper menuTypes = (ForgeMenuTypeHelper) com.yourdomain.businesscraft.forge.BusinessCraftForge.MENU_TYPES;
    //
    //     event.register(menuTypes.getTownInterfaceMenuType(), TownInterfaceScreen::new);
    //     event.register(menuTypes.getTradeMenuType(), TradeScreen::new);
    //     event.register(menuTypes.getStorageMenuType(), StorageScreen::new);
    //     event.register(menuTypes.getPaymentBoardMenuType(), PaymentBoardScreen::new);
    // }
}
