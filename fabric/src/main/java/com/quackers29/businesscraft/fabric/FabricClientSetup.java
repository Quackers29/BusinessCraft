package com.quackers29.businesscraft.fabric;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.client.CommonClientSetup;
import com.quackers29.businesscraft.fabric.event.FabricEventCallbackHandler;
import com.quackers29.businesscraft.fabric.FabricModMessages;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

            // Initialize common client setup (key handlers, render events)
            CommonClientSetup.init();

            // Register entity renderers
            CommonClientSetup.registerRenderers((type, provider) -> {
                EntityRendererRegistry.register((net.minecraft.world.entity.EntityType) type,
                        (context) -> provider.create(context));
            });

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
            CommonClientSetup.registerScreens();
            LOGGER.info("Screen registration complete");
        } catch (Exception e) {
            LOGGER.error("Error registering screens", e);
            throw new RuntimeException("Failed to register screens", e);
        }
    }
}
