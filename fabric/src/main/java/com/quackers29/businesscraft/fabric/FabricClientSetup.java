package com.quackers29.businesscraft.fabric;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.fabric.event.FabricEventCallbackHandler;
import com.quackers29.businesscraft.fabric.platform.FabricMenuTypeHelper;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric client-side initialization
 */
public class FabricClientSetup implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft Fabric Client");
    private static boolean screensRegistered = false;
    private static boolean renderingInitialized = false;
    private static boolean keyHandlersInitialized = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("BusinessCraft Fabric client setup starting");

        // Initialize client-side platform helpers
        PlatformAccess.client = BusinessCraftFabric.CLIENT;
        PlatformAccess.render = BusinessCraftFabric.RENDER;

        // Register client-side events
        try {
            FabricEventCallbackHandler.registerClientEvents();
        } catch (Exception e) {
            LOGGER.warn("Could not register client events", e);
        }

        // Register client-side packet handlers
        registerClientPackets();

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

        LOGGER.info("BusinessCraft Fabric client setup complete");
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

                    if (areClientClassesAvailable()) {
                        // Check if menu type is available before retrying
                        Object menuType = FabricMenuTypeHelper.getTownInterfaceMenuTypeStatic();
                        LOGGER.info("Retrying screen registration (attempt {})... Menu type available: {}", retryCount,
                                menuType != null);
                        if (menuType != null) {
                            try {
                                registerScreens();
                                screensRegistered = true;
                                LOGGER.info("Screen registration successful on retry!");
                                return;
                            } catch (Exception e) {
                                LOGGER.warn("Screen registration failed on retry {}: {}", retryCount, e.getMessage(),
                                        e);
                                delayMs = Math.min(delayMs * 2, 5000);
                            }
                        } else {
                            LOGGER.debug("Menu type not available yet, will retry...");
                        }
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
     * Check if client classes are available
     */
    private boolean areClientClassesAvailable() {
        try {
            ClassLoader classLoader = FabricClientSetup.class.getClassLoader();
            classLoader.loadClass("net.minecraft.world.inventory.MenuType");
            classLoader.loadClass("net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Initialize client-side rendering events
     */
    private void initializeClientRendering() {
        try {
            // Initialize ClientRenderEvents (platform and boundary visualization)
            Class<?> clientRenderEventsClass = Class.forName("com.quackers29.businesscraft.event.ClientRenderEvents");
            java.lang.reflect.Method initializeMethod = clientRenderEventsClass.getMethod("initialize");
            initializeMethod.invoke(null);
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
            // Initialize TownDebugKeyHandler (F4 key for debug overlay)
            Class<?> townDebugKeyHandlerClass = Class
                    .forName("com.quackers29.businesscraft.client.TownDebugKeyHandler");
            java.lang.reflect.Method initializeMethod = townDebugKeyHandlerClass.getMethod("initialize");
            initializeMethod.invoke(null);

            // Initialize PlatformPathKeyHandler (platform path creation keys)
            Class<?> platformPathKeyHandlerClass = Class
                    .forName("com.quackers29.businesscraft.client.PlatformPathKeyHandler");
            java.lang.reflect.Method platformPathInitMethod = platformPathKeyHandlerClass.getMethod("initialize");
            platformPathInitMethod.invoke(null);

            // Initialize TownDebugOverlay
            Class<?> townDebugOverlayClass = Class.forName("com.quackers29.businesscraft.client.TownDebugOverlay");
            java.lang.reflect.Method overlayInitMethod = townDebugOverlayClass.getMethod("initialize");
            overlayInitMethod.invoke(null);

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
            // Register client packets from FabricModMessages
            Class<?> fabricModMessagesClass = Class.forName("com.quackers29.businesscraft.fabric.FabricModMessages");
            java.lang.reflect.Method registerClientPacketsMethod = fabricModMessagesClass
                    .getMethod("registerClientPackets");
            registerClientPacketsMethod.invoke(null);
            LOGGER.info("Client packet handlers registered");
        } catch (Exception e) {
            LOGGER.warn("Could not register client packet handlers", e);
        }
    }

    /**
     * Register screens for menu types using Fabric's HandledScreens API
     */
    private void registerScreens() {
        try {
            LOGGER.info("Registering screens for menu types...");

            ClassLoader classLoader = FabricClientSetup.class.getClassLoader();

            // Load Fabric's HandledScreens class
            Class<?> handledScreensClass = classLoader
                    .loadClass("net.minecraft.client.gui.screen.ingame.HandledScreens");
            Class<?> screenHandlerTypeClass = classLoader.loadClass("net.minecraft.screen.ScreenHandlerType");
            LOGGER.info("Loaded HandledScreens class: {}", handledScreensClass.getName());

            // Check if menu types are registered yet
            Object townInterfaceMenuType = FabricMenuTypeHelper.getTownInterfaceMenuTypeStatic();
            LOGGER.info("Retrieved townInterfaceMenuType: {}",
                    townInterfaceMenuType != null ? townInterfaceMenuType.getClass().getName() : "null");
            if (townInterfaceMenuType == null) {
                LOGGER.warn(
                        "Menu types not registered yet - screens will be registered when menu types become available");
                throw new IllegalStateException(
                        "Menu types not registered yet - screens will be registered when menu types become available");
            }

            // Get HandledScreens.register method
            java.lang.reflect.Method[] registerMethods = handledScreensClass.getDeclaredMethods();
            java.lang.reflect.Method registerMethod = null;
            Class<?> screenFactoryClass = null;

            LOGGER.info("Looking for register method in HandledScreens...");
            for (java.lang.reflect.Method m : registerMethods) {
                if ("register".equals(m.getName()) && java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                    Class<?>[] paramTypes = m.getParameterTypes();
                    if (paramTypes.length == 2 && paramTypes[0] == screenHandlerTypeClass) {
                        registerMethod = m;
                        screenFactoryClass = paramTypes[1];
                        LOGGER.info("Using register method with ScreenFactory type: {}", screenFactoryClass.getName());
                        break;
                    }
                }
            }

            if (registerMethod == null || screenFactoryClass == null) {
                throw new RuntimeException("Could not find HandledScreens.register() method");
            }

            // Register TownInterfaceScreen (from common module)
            if (townInterfaceMenuType != null) {
                registerFabricScreen(registerMethod, townInterfaceMenuType, screenHandlerTypeClass, screenFactoryClass,
                        "com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen", classLoader);
            }

            LOGGER.info("Screen registration complete");
        } catch (Exception e) {
            LOGGER.error("Error registering screens", e);
            e.printStackTrace();
        }
    }

    /**
     * Register a Fabric-specific screen using HandledScreens.register()
     */
    private void registerFabricScreen(java.lang.reflect.Method registerMethod, Object menuType,
            Class<?> screenHandlerTypeClass, Class<?> screenFactoryClass,
            String screenClassName, ClassLoader classLoader) {
        try {
            LOGGER.info("Attempting to register screen: {}", screenClassName);

            // Load screen class
            Class<?> screenClass = classLoader.loadClass(screenClassName);
            LOGGER.info("Loaded screen class: {}", screenClass.getName());

            // Load required classes for screen constructor
            Class<?> playerInventoryClass = classLoader.loadClass("net.minecraft.entity.player.PlayerInventory");
            Class<?> textClass = classLoader.loadClass("net.minecraft.text.Text");

            // Find the screen constructor dynamically
            java.lang.reflect.Constructor<?> screenConstructor = null;
            LOGGER.info("Looking for screen constructor in: {}", screenClass.getName());
            for (java.lang.reflect.Constructor<?> c : screenClass.getConstructors()) {
                Class<?>[] paramTypes = c.getParameterTypes();
                if (paramTypes.length == 3) {
                    // Check if first parameter is compatible (ScreenHandler/AbstractContainerMenu)
                    // We use Object.class check or just assume it's the right one if other params
                    // match
                    if (paramTypes[1] == playerInventoryClass && paramTypes[2] == textClass) {
                        screenConstructor = c;
                        LOGGER.info("Found matching screen constructor: {}", screenConstructor);
                        break;
                    }
                }
            }

            if (screenConstructor == null) {
                throw new RuntimeException(
                        "Could not find screen constructor with signature (Menu, PlayerInventory, Text)");
            }

            // Create ScreenFactory lambda using Proxy
            java.lang.reflect.Method factoryMethod = null;
            for (java.lang.reflect.Method m : screenFactoryClass.getMethods()) {
                if (m.getParameterCount() == 3) {
                    factoryMethod = m;
                    break;
                }
            }

            if (factoryMethod == null) {
                throw new RuntimeException("Could not find ScreenFactory method");
            }

            final java.lang.reflect.Method finalFactoryMethod = factoryMethod;
            final java.lang.reflect.Constructor<?> finalScreenConstructor = screenConstructor;
            Object screenFactory = java.lang.reflect.Proxy.newProxyInstance(
                    classLoader,
                    new Class[] { screenFactoryClass },
                    (proxy, method, args) -> {
                        if (method.equals(finalFactoryMethod)) {
                            Object handler = args[0];
                            Object inventory = args[1];
                            Object title = args.length > 2 ? args[2] : null;
                            return finalScreenConstructor.newInstance(handler, inventory, title);
                        }
                        return null;
                    });

            // Register the screen
            registerMethod.invoke(null, menuType, screenFactory);
            LOGGER.info("Successfully registered screen: {}", screenClassName);
        } catch (Exception e) {
            LOGGER.error("Error registering screen: {}", screenClassName, e);
            e.printStackTrace();
        }
    }
}
