package com.quackers29.businesscraft.client;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.client.renderer.TouristRenderer;
import com.quackers29.businesscraft.event.ClientRenderEvents;
import com.quackers29.businesscraft.init.CommonModEntityTypes;
import com.quackers29.businesscraft.init.CommonModMenuTypes;
import com.quackers29.businesscraft.menu.PaymentBoardMenu;
import com.quackers29.businesscraft.menu.StorageMenu;
import com.quackers29.businesscraft.menu.TownInterfaceMenu;
import com.quackers29.businesscraft.menu.TradeMenu;
import com.quackers29.businesscraft.ui.screens.town.PaymentBoardScreen;
import com.quackers29.businesscraft.ui.screens.town.StorageScreen;
import com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen;
import com.quackers29.businesscraft.ui.screens.town.TradeScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

public class CommonClientSetup {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft Client");

    public static void init() {
        LOGGER.info("Initializing Common Client Setup...");

        // Initialize key handlers
        TownDebugKeyHandler.initialize();
        PlatformPathKeyHandler.initialize();
        TownDebugOverlay.initialize();

        // Initialize client render events
        ClientRenderEvents.initialize();
    }

    public static void registerScreens() {
        LOGGER.info("Registering screens...");

        MenuScreens.register(CommonModMenuTypes.TOWN_INTERFACE_MENU.get(), TownInterfaceScreen::new);
        MenuScreens.register(CommonModMenuTypes.TRADE_MENU.get(), TradeScreen::new);
        MenuScreens.register(CommonModMenuTypes.STORAGE_MENU.get(), StorageScreen::new);
        MenuScreens.register(CommonModMenuTypes.PAYMENT_BOARD_MENU.get(), PaymentBoardScreen::new);
    }

    @SuppressWarnings("unchecked")
    public static void registerRenderers(BiConsumer<EntityType<?>, EntityRendererProvider<?>> rendererRegistrar) {
        LOGGER.info("Registering renderers...");
        rendererRegistrar.accept(CommonModEntityTypes.TOURIST.get(), (EntityRendererProvider) TouristRenderer::new);
    }
}
