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
        
        // Register client-side events (with retry if classes not available)
        try {
            FabricEventCallbackHandler.registerClientEvents();
        } catch (Exception e) {
            LOGGER.warn("Could not register client events immediately, will retry", e);
            scheduleDelayedClientEvents();
        }
        
        // Register client-side packet handlers
        registerClientPackets();
        
        // Register screens for menu types (with delayed retry)
        scheduleDelayedScreenRegistration();
        
        // Initialize client-side rendering events (with delayed retry)
        scheduleDelayedRenderingInitialization();
        
        // Initialize key handlers (with delayed retry)
        scheduleDelayedKeyHandlerInitialization();
        
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
                        LOGGER.info("Retrying screen registration (attempt {})...", retryCount);
                        try {
                            registerScreens();
                            screensRegistered = true;
                            LOGGER.info("Screen registration successful on retry!");
                            return;
                        } catch (Exception e) {
                            LOGGER.warn("Screen registration failed on retry {}: {}", retryCount, e.getMessage());
                            delayMs = Math.min(delayMs * 2, 5000);
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
     * Schedule delayed rendering initialization
     */
    private void scheduleDelayedRenderingInitialization() {
        new Thread(() -> {
            int maxRetries = 10;
            int retryCount = 0;
            int delayMs = 500;
            
            while (retryCount < maxRetries && !renderingInitialized) {
                try {
                    Thread.sleep(delayMs);
                    retryCount++;
                    
                    if (areMinecraftClassesAvailable()) {
                        LOGGER.info("Retrying rendering initialization (attempt {})...", retryCount);
                        try {
                            initializeClientRendering();
                            renderingInitialized = true;
                            LOGGER.info("Rendering initialization successful on retry!");
                            return;
                        } catch (Exception e) {
                            LOGGER.warn("Rendering initialization failed on retry {}: {}", retryCount, e.getMessage());
                            delayMs = Math.min(delayMs * 2, 5000);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            
            if (!renderingInitialized) {
                LOGGER.error("Rendering initialization failed after {} retries.", maxRetries);
            }
        }).start();
    }
    
    /**
     * Schedule delayed key handler initialization
     */
    private void scheduleDelayedKeyHandlerInitialization() {
        new Thread(() -> {
            int maxRetries = 10;
            int retryCount = 0;
            int delayMs = 500;
            
            while (retryCount < maxRetries && !keyHandlersInitialized) {
                try {
                    Thread.sleep(delayMs);
                    retryCount++;
                    
                    if (areMinecraftClassesAvailable()) {
                        LOGGER.info("Retrying key handler initialization (attempt {})...", retryCount);
                        try {
                            initializeKeyHandlers();
                            keyHandlersInitialized = true;
                            LOGGER.info("Key handler initialization successful on retry!");
                            return;
                        } catch (Exception e) {
                            LOGGER.warn("Key handler initialization failed on retry {}: {}", retryCount, e.getMessage());
                            delayMs = Math.min(delayMs * 2, 5000);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            
            if (!keyHandlersInitialized) {
                LOGGER.error("Key handler initialization failed after {} retries.", maxRetries);
            }
        }).start();
    }
    
    /**
     * Schedule delayed client events registration
     */
    private void scheduleDelayedClientEvents() {
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                try {
                    FabricEventCallbackHandler.registerClientEvents();
                    LOGGER.info("Client events registered successfully on retry!");
                } catch (Exception e) {
                    LOGGER.warn("Client events registration still failed on retry: {}", e.getMessage());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
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
     * Check if Minecraft classes are available
     */
    private boolean areMinecraftClassesAvailable() {
        try {
            ClassLoader classLoader = FabricClientSetup.class.getClassLoader();
            classLoader.loadClass("net.minecraft.core.Vec3i");
            classLoader.loadClass("net.minecraft.core.BlockPos");
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
            throw new RuntimeException("Failed to initialize client rendering", e);
        }
    }
    
    /**
     * Initialize key handlers for debug overlay and platform path creation
     */
    private void initializeKeyHandlers() {
        try {
            // Initialize TownDebugKeyHandler (F4 key for debug overlay)
            Class<?> townDebugKeyHandlerClass = Class.forName("com.quackers29.businesscraft.client.TownDebugKeyHandler");
            java.lang.reflect.Method initializeMethod = townDebugKeyHandlerClass.getMethod("initialize");
            initializeMethod.invoke(null);
            LOGGER.info("Town debug key handler initialized");
            
            // Initialize PlatformPathKeyHandler (platform path creation keys)
            Class<?> platformPathKeyHandlerClass = Class.forName("com.quackers29.businesscraft.client.PlatformPathKeyHandler");
            java.lang.reflect.Method platformPathInitMethod = platformPathKeyHandlerClass.getMethod("initialize");
            platformPathInitMethod.invoke(null);
            LOGGER.info("Platform path key handler initialized");
            
            // Initialize TownDebugOverlay (overlay registration will work now that RenderHelper is enabled)
            Class<?> townDebugOverlayClass = Class.forName("com.quackers29.businesscraft.client.TownDebugOverlay");
            java.lang.reflect.Method overlayInitMethod = townDebugOverlayClass.getMethod("initialize");
            overlayInitMethod.invoke(null);
            LOGGER.info("Town debug overlay initialized");
        } catch (Exception e) {
            LOGGER.warn("Could not initialize key handlers", e);
            throw new RuntimeException("Failed to initialize key handlers", e);
        }
    }
    
    /**
     * Register client-side packet handlers
     */
    private void registerClientPackets() {
        try {
            // Register client packets from FabricModMessages
            Class<?> fabricModMessagesClass = Class.forName("com.quackers29.businesscraft.fabric.FabricModMessages");
            java.lang.reflect.Method registerClientPacketsMethod = fabricModMessagesClass.getMethod("registerClientPackets");
            registerClientPacketsMethod.invoke(null);
            LOGGER.info("Client packet handlers registered");
        } catch (Exception e) {
            LOGGER.warn("Could not register client packet handlers", e);
        }
    }
    
    /**
     * Register screens for menu types using Fabric's ScreenRegistry API
     * Uses reflection to access screen classes (excluded from Fabric build)
     */
    private void registerScreens() {
        try {
            LOGGER.info("Registering screens for menu types...");
            
            ClassLoader classLoader = FabricClientSetup.class.getClassLoader();
            
            // Load Fabric's ScreenRegistry class
            Class<?> screenRegistryClass = classLoader.loadClass("net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry");
            Class<?> menuTypeClass = classLoader.loadClass("net.minecraft.world.inventory.MenuType");
            
            // Check if menu types are registered yet
            Object townInterfaceMenuType = FabricMenuTypeHelper.getTownInterfaceMenuTypeStatic();
            if (townInterfaceMenuType == null) {
                throw new IllegalStateException("Menu types not registered yet - screens will be registered when menu types become available");
            }
            Class<?> screenProviderClass = screenRegistryClass.getClasses()[0]; // ScreenProvider interface
            
            // Get ScreenRegistry.register method
            java.lang.reflect.Method registerMethod = screenRegistryClass.getMethod("register", 
                menuTypeClass, screenProviderClass);
            
            // Get menu types from FabricMenuTypeHelper
            Object tradeMenuType = FabricMenuTypeHelper.getTradeMenuTypeStatic();
            Object storageMenuType = FabricMenuTypeHelper.getStorageMenuTypeStatic();
            Object paymentBoardMenuType = FabricMenuTypeHelper.getPaymentBoardMenuTypeStatic();
            
            // Register TownInterfaceScreen
            if (townInterfaceMenuType != null) {
                registerScreen(registerMethod, townInterfaceMenuType, menuTypeClass, screenProviderClass,
                    "com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen", classLoader);
            }
            
            // Register TradeScreen
            if (tradeMenuType != null) {
                registerScreen(registerMethod, tradeMenuType, menuTypeClass, screenProviderClass,
                    "com.quackers29.businesscraft.ui.screens.town.TradeScreen", classLoader);
            }
            
            // Register StorageScreen
            if (storageMenuType != null) {
                registerScreen(registerMethod, storageMenuType, menuTypeClass, screenProviderClass,
                    "com.quackers29.businesscraft.ui.screens.town.StorageScreen", classLoader);
            }
            
            // Register PaymentBoardScreen
            if (paymentBoardMenuType != null) {
                registerScreen(registerMethod, paymentBoardMenuType, menuTypeClass, screenProviderClass,
                    "com.quackers29.businesscraft.ui.screens.town.PaymentBoardScreen", classLoader);
            }
            
            LOGGER.info("Screen registration complete");
        } catch (Exception e) {
            LOGGER.error("Error registering screens", e);
            e.printStackTrace();
        }
    }
    
    /**
     * Register a single screen using reflection
     */
    private void registerScreen(java.lang.reflect.Method registerMethod, Object menuType, 
                                Class<?> menuTypeClass, Class<?> screenProviderClass,
                                String screenClassName, ClassLoader classLoader) {
        try {
            // Load screen class
            Class<?> screenClass = classLoader.loadClass(screenClassName);
            
            // Load required classes for screen constructor
            Class<?> abstractContainerMenuClass = classLoader.loadClass("net.minecraft.world.inventory.AbstractContainerMenu");
            Class<?> playerInventoryClass = classLoader.loadClass("net.minecraft.world.entity.player.Inventory");
            Class<?> componentClass = classLoader.loadClass("net.minecraft.network.chat.Component");
            
            // Create ScreenProvider using Proxy
            Object screenProvider = java.lang.reflect.Proxy.newProxyInstance(
                classLoader,
                new Class[]{screenProviderClass},
                (proxy, method, args) -> {
                    if (method.getName().equals("create") || method.getName().equals("apply")) {
                        // args[0] = menu, args[1] = inventory, args[2] = title
                        Object menu = args[0];
                        Object inventory = args[1];
                        Object title = args.length > 2 ? args[2] : null;
                        
                        // Create screen instance using constructor
                        // Screen constructors typically take: (AbstractContainerMenu menu, Inventory inventory, Component title)
                        java.lang.reflect.Constructor<?> constructor = screenClass.getConstructor(
                            abstractContainerMenuClass,
                            playerInventoryClass,
                            componentClass
                        );
                        return constructor.newInstance(menu, inventory, title);
                    }
                    return null;
                }
            );
            
            // Register the screen
            registerMethod.invoke(null, menuType, screenProvider);
            LOGGER.debug("Registered screen: {}", screenClassName);
        } catch (Exception e) {
            LOGGER.error("Error registering screen: {}", screenClassName, e);
            e.printStackTrace();
        }
    }
}
