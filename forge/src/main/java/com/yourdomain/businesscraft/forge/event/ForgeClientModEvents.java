package com.yourdomain.businesscraft.forge.event;

import com.yourdomain.businesscraft.forge.platform.ForgeMenuTypeHelper;
import com.yourdomain.businesscraft.forge.screen.TownInterfaceScreen;
import com.yourdomain.businesscraft.forge.screen.TradeScreen;
import com.yourdomain.businesscraft.forge.screen.StorageScreen;
import com.yourdomain.businesscraft.forge.screen.PaymentBoardScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterMenuScreensEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge-specific client event handlers
 */
@Mod.EventBusSubscriber(modid = "businesscraft", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ForgeClientModEvents {

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        ForgeMenuTypeHelper menuTypes = (ForgeMenuTypeHelper) com.yourdomain.businesscraft.forge.BusinessCraftForge.MENU_TYPES;

        event.register(menuTypes.getTownInterfaceMenuType(), TownInterfaceScreen::new);
        event.register(menuTypes.getTradeMenuType(), TradeScreen::new);
        event.register(menuTypes.getStorageMenuType(), StorageScreen::new);
        event.register(menuTypes.getPaymentBoardMenuType(), PaymentBoardScreen::new);
    }
}
