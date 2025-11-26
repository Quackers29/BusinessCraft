package com.quackers29.businesscraft.fabric;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.fabric.event.FabricEventCallbackHandler;
import com.quackers29.businesscraft.fabric.platform.FabricMenuTypeHelper;
import com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen;
import com.quackers29.businesscraft.client.TownDebugKeyHandler;
import com.quackers29.businesscraft.client.PlatformPathKeyHandler;
import com.quackers29.businesscraft.client.TownDebugOverlay;
import com.quackers29.businesscraft.event.ClientRenderEvents;
import com.quackers29.businesscraft.fabric.FabricModMessages;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import com.quackers29.businesscraft.client.renderer.TouristRenderer;
import com.quackers29.businesscraft.fabric.init.FabricModEntityTypes;

/**
 * Fabric client-side initialization
 */
public class FabricClientSetup implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft Fabric");
    private static boolean screensRegistered = false;


    @Override
    public void onInitializeClient() {
        try {
            // CRITICAL: Register client-side packet handlers FIRST
            registerClientPackets();

            // Initialize client-side platform helpers
            PlatformAccess.client = BusinessCraftFabric.CLIENT;
            PlatformAccess.render = BusinessCraftFabric.RENDER;

            // Register client-side events
            try {
                FabricEventCallbackHandler.registerClientEvents();
            } catch (Exception e) {
                LOGGER.warn("Could not register client events", e);
            }

            // Register screens for menu types
            // Try to register immediately, schedule retry if menu types not ready yet
            LOGGER.info("Attempting initial screen registration...");
            try {
                registerScreens();
                screensRegistered = true;
                LOGGER.info("Screen registration completed successfully on first attempt");
            } catch (Exception e) {
                LOGGER.warn("Screen registration failed on first attempt: " + e.getMessage(), e);
                scheduleDelayedScreenRegistration();
            }

            // Initialize client-side rendering events
            initializeClientRendering();

            // Initialize key handlers
            initializeKeyHandlers();

            // Register entity renderers
            EntityRendererRegistry.register(FabricModEntityTypes.TOURIST, TouristRenderer::new);

            LOGGER.info("BusinessCraft Fabric client setup complete");
        } catch (Exception e) {
            LOGGER.error("CRITICAL: Exception in onInitializeClient", e);
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Schedule delayed screen registration
     */
    private void scheduleDelayedScreenRegistration() {
        new Thread(() -> {
            int maxRetries = 10;
            int retryCount = 0;
            int delayMs = 500;

            while (retryCount < maxRetries && !screensRegistered) {
                try {
                    Thread.sleep(delayMs);
                    retryCount++;

                    // Check if menu type is available before retrying
                    Object menuType = FabricMenuTypeHelper.getTownInterfaceMenuTypeStatic();
                    if (menuType != null) {
                        try {
                            // We need to run this on the main thread if possible, or ensure thread safety
                            // But MenuScreens.register is usually safe to call during init
                            registerScreens();
                            screensRegistered = true;
                            LOGGER.info("Screen registration successful on retry!");
                            return;
                        } catch (Exception e) {
                            LOGGER.warn("Screen registration failed on retry {}: {}", retryCount, e.getMessage(), e);
                            delayMs = Math.min(delayMs * 2, 5000);
                        }
                    } else {
                        LOGGER.debug("Menu type not available yet, will retry...");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            if (!screensRegistered) {
                LOGGER.error("Screen registration failed after {} retries.", maxRetries);
            }
        }).start();
    }

    /**
     * Initialize client-side rendering events
     */
    private void initializeClientRendering() {
        try {
            ClientRenderEvents.initialize();
            LOGGER.info("Client rendering events initialized");
        } catch (Exception e) {
            LOGGER.warn("Could not initialize client rendering events", e);
        }
    }

    /**
     * Initialize key handlers for debug overlay and platform path creation
     */
    private void initializeKeyHandlers() {
        try {
            TownDebugKeyHandler.initialize();
            PlatformPathKeyHandler.initialize();
            TownDebugOverlay.initialize();
            LOGGER.info("Key handlers initialized");
        } catch (Exception e) {
            LOGGER.warn("Could not initialize key handlers", e);
        }
    }

    /**
     * Register client-side packet handlers
     */
    private void registerClientPackets() {
        try {
            FabricModMessages.registerClientPackets();
            LOGGER.info("Client packet handlers registered");
        } catch (Exception e) {
            LOGGER.warn("Could not register client packet handlers", e);
        }
    }

    /**
     * Register screens for menu types using Mojang mappings
     */
    private void registerScreens() {
        try {
            LOGGER.info("Registering screens for menu types...");

            // Get the registered menu types
            MenuType<?> townInterfaceMenuType = (MenuType<?>) FabricMenuTypeHelper.getTownInterfaceMenuTypeStatic();
            MenuType<?> paymentBoardMenuType = (MenuType<?>) FabricMenuTypeHelper.getPaymentBoardMenuTypeStatic();

            if (townInterfaceMenuType == null) {
                LOGGER.warn("Menu types not registered yet");
                throw new IllegalStateException("Menu types not registered yet");
            }

            // Register TownInterfaceScreen
            @SuppressWarnings("unchecked")
            MenuType<com.quackers29.businesscraft.menu.TownInterfaceMenu> typedMenuType = (MenuType<com.quackers29.businesscraft.menu.TownInterfaceMenu>) townInterfaceMenuType;
            MenuScreens.register(typedMenuType, TownInterfaceScreen::new);
            LOGGER.info("Registered TownInterfaceScreen");

            // Register PaymentBoardScreen
            if (paymentBoardMenuType != null) {
                @SuppressWarnings("unchecked")
                MenuType<com.quackers29.businesscraft.menu.PaymentBoardMenu> typedPaymentBoardMenuType = (MenuType<com.quackers29.businesscraft.menu.PaymentBoardMenu>) paymentBoardMenuType;
                MenuScreens.register(typedPaymentBoardMenuType,
                        com.quackers29.businesscraft.ui.screens.town.PaymentBoardScreen::new);
                LOGGER.info("Registered PaymentBoardScreen");
            } else {
                LOGGER.warn("PaymentBoardMenuType not registered yet");
            }

            LOGGER.info("Screen registration complete");
        } catch (Exception e) {
            LOGGER.error("Error registering screens", e);
            throw new RuntimeException("Failed to register screens", e);
        }
    }
}
