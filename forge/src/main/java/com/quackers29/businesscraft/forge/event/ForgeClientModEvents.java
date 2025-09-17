package com.quackers29.businesscraft.forge.event;

import com.quackers29.businesscraft.forge.platform.ForgeMenuTypeHelper;
import com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen;
import com.quackers29.businesscraft.ui.screens.town.TradeScreen;
import com.quackers29.businesscraft.ui.screens.town.StorageScreen;
import com.quackers29.businesscraft.ui.screens.town.PaymentBoardScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.gui.screens.MenuScreens;
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
        com.quackers29.businesscraft.forge.platform.ForgeMenuTypeHelper menuTypes =
            (com.quackers29.businesscraft.forge.platform.ForgeMenuTypeHelper) com.quackers29.businesscraft.forge.BusinessCraftForge.MENU_TYPES;

        // Register menu screens
        event.enqueueWork(() -> {
            MenuScreens.register((net.minecraft.world.inventory.MenuType<com.quackers29.businesscraft.menu.TownInterfaceMenu>) menuTypes.getTownInterfaceMenuType(), TownInterfaceScreen::new);
            MenuScreens.register((net.minecraft.world.inventory.MenuType<com.quackers29.businesscraft.menu.TradeMenu>) menuTypes.getTradeMenuType(), TradeScreen::new);
            MenuScreens.register((net.minecraft.world.inventory.MenuType<com.quackers29.businesscraft.menu.StorageMenu>) menuTypes.getStorageMenuType(), StorageScreen::new);
            MenuScreens.register((net.minecraft.world.inventory.MenuType<com.quackers29.businesscraft.menu.PaymentBoardMenu>) menuTypes.getPaymentBoardMenuType(), PaymentBoardScreen::new);
        });
    }
}
