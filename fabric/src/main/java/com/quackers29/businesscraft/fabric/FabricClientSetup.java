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
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("BusinessCraft Fabric client setup starting");
        
        // Initialize client-side platform helpers
        PlatformAccess.client = BusinessCraftFabric.CLIENT;
        PlatformAccess.render = BusinessCraftFabric.RENDER;
        
        // Register client-side events
        FabricEventCallbackHandler.registerClientEvents();
        
        // Register client-side packet handlers
        registerClientPackets();
        
        // Register screens for menu types
        registerScreens();
        
        // Initialize client-side rendering events
        initializeClientRendering();
        
        // Initialize key handlers
        initializeKeyHandlers();
        
        LOGGER.info("BusinessCraft Fabric client setup complete");
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
            Class<?> screenProviderClass = screenRegistryClass.getClasses()[0]; // ScreenProvider interface
            
            // Get ScreenRegistry.register method
            java.lang.reflect.Method registerMethod = screenRegistryClass.getMethod("register", 
                menuTypeClass, screenProviderClass);
            
            // Get menu types from FabricMenuTypeHelper
            Object townInterfaceMenuType = FabricMenuTypeHelper.getTownInterfaceMenuTypeStatic();
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
