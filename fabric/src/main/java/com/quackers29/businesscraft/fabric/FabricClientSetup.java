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
        
        // Initialize client-side rendering events (with delayed retry)
        scheduleDelayedRenderingInitialization();
        
        // Initialize key handlers (with delayed retry)
        scheduleDelayedKeyHandlerInitialization();

        // Register block interaction callback directly (simpler approach)
        registerBlockInteractionCallback();

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
                        LOGGER.info("Retrying screen registration (attempt {})... Menu type available: {}", retryCount, menuType != null);
                        if (menuType != null) {
                            try {
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
                            LOGGER.warn("Full exception details:", e);
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
                            LOGGER.warn("Full exception details:", e);
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
     * Block interactions are now handled directly in the block's onUse method
     * No need for additional callback registration
     */
    private void registerBlockInteractionCallback() {
        LOGGER.info("Block interactions handled by block's onUse method - no additional callback needed");
    }

    /**
     * Register screens for menu types using Fabric's HandledScreens API
     */
    private void registerScreens() {
        try {
            LOGGER.info("Registering screens for menu types...");
            
            ClassLoader classLoader = FabricClientSetup.class.getClassLoader();
            
            // Load Fabric's HandledScreens class
            Class<?> handledScreensClass = classLoader.loadClass("net.minecraft.client.gui.screen.ingame.HandledScreens");
            Class<?> screenHandlerTypeClass = classLoader.loadClass("net.minecraft.screen.ScreenHandlerType");
            LOGGER.info("Loaded HandledScreens class: {}", handledScreensClass.getName());
            
            // Check if menu types are registered yet
            Object townInterfaceMenuType = FabricMenuTypeHelper.getTownInterfaceMenuTypeStatic();
            LOGGER.info("Retrieved townInterfaceMenuType: {}", townInterfaceMenuType != null ? townInterfaceMenuType.getClass().getName() : "null");
            if (townInterfaceMenuType == null) {
                LOGGER.warn("Menu types not registered yet - screens will be registered when menu types become available");
                throw new IllegalStateException("Menu types not registered yet - screens will be registered when menu types become available");
            }
            
            // Get HandledScreens.register method
            // In Minecraft 1.20.1, HandledScreens.register() takes a BiFunction or similar functional interface
            // Let's find the actual register method and see what it expects
            java.lang.reflect.Method[] registerMethods = handledScreensClass.getDeclaredMethods();
            java.lang.reflect.Method registerMethod = null;
            Class<?> screenFactoryClass = null;
            
            LOGGER.info("Looking for register method in HandledScreens...");
            for (java.lang.reflect.Method m : registerMethods) {
                if ("register".equals(m.getName()) && java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                    Class<?>[] paramTypes = m.getParameterTypes();
                    LOGGER.info("Found register method: {} with parameters: {}", m.getName(), java.util.Arrays.toString(paramTypes));
                    if (paramTypes.length == 2 && paramTypes[0] == screenHandlerTypeClass) {
                        registerMethod = m;
                        screenFactoryClass = paramTypes[1];
                        LOGGER.info("Using register method with ScreenFactory type: {}", screenFactoryClass.getName());
                        break;
                    }
                }
            }
            
            if (registerMethod == null || screenFactoryClass == null) {
                LOGGER.error("Could not find HandledScreens.register() method. Available methods:");
                for (java.lang.reflect.Method m : registerMethods) {
                    if (java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                        LOGGER.error("  - {} ({})", m.getName(), java.util.Arrays.toString(m.getParameterTypes()));
                    }
                }
                throw new RuntimeException("Could not find HandledScreens.register() method");
            }
            
            // Register FabricTownInterfaceScreen
            if (townInterfaceMenuType != null) {
                registerFabricScreen(registerMethod, townInterfaceMenuType, screenHandlerTypeClass, screenFactoryClass,
                    "com.quackers29.businesscraft.fabric.client.FabricTownInterfaceScreen", classLoader);
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
            LOGGER.info("Attempting to register Fabric screen: {}", screenClassName);
            
            // Load screen class (Fabric-specific, extends HandledScreen)
            Class<?> screenClass = classLoader.loadClass(screenClassName);
            LOGGER.info("Loaded screen class: {}", screenClass.getName());
            
            // Load required classes for screen constructor
            Class<?> playerInventoryClass = classLoader.loadClass("net.minecraft.entity.player.PlayerInventory");
            Class<?> textClass = classLoader.loadClass("net.minecraft.text.Text");
            
            // Find the screen constructor dynamically
            // HandledScreen<T> constructor takes (T handler, PlayerInventory inventory, Text title)
            // So we need to find a constructor that takes (some ScreenHandler subclass, PlayerInventory, Text)
            java.lang.reflect.Constructor<?> screenConstructor = null;
            LOGGER.info("Looking for screen constructor in: {}", screenClass.getName());
            for (java.lang.reflect.Constructor<?> c : screenClass.getConstructors()) {
                Class<?>[] paramTypes = c.getParameterTypes();
                LOGGER.debug("Found constructor: {}", java.util.Arrays.toString(paramTypes));
                if (paramTypes.length == 3) {
                    // Check if first parameter extends ScreenHandler, second is PlayerInventory, third is Text
                    Class<?> screenHandlerClass = classLoader.loadClass("net.minecraft.screen.ScreenHandler");
                    if (screenHandlerClass.isAssignableFrom(paramTypes[0]) && 
                        paramTypes[1] == playerInventoryClass && 
                        paramTypes[2] == textClass) {
                        screenConstructor = c;
                        LOGGER.info("Found matching screen constructor: {}", screenConstructor);
                        break;
                    }
                }
            }
            
            if (screenConstructor == null) {
                throw new RuntimeException("Could not find screen constructor with signature (ScreenHandler, PlayerInventory, Text)");
            }
            
            // Create ScreenFactory lambda using Proxy
            // ScreenFactory is a functional interface, so we need to implement its single abstract method
            // Find the abstract method (should be something like create(handler, inventory, title))
            java.lang.reflect.Method factoryMethod = null;
            LOGGER.info("Looking for ScreenFactory method in: {}", screenFactoryClass.getName());
            for (java.lang.reflect.Method m : screenFactoryClass.getMethods()) {
                LOGGER.debug("Checking method: {} - abstract: {}, declaring class: {}", 
                    m.getName(), java.lang.reflect.Modifier.isAbstract(m.getModifiers()), m.getDeclaringClass().getName());
                if (java.lang.reflect.Modifier.isAbstract(m.getModifiers()) || 
                    (m.getDeclaringClass() == screenFactoryClass && !m.isDefault())) {
                    factoryMethod = m;
                    LOGGER.info("Found factory method: {}", m);
                    break;
                }
            }
            
            if (factoryMethod == null) {
                // Fallback: try common method names
                LOGGER.info("Factory method not found via inspection, trying common names...");
                // Provider interface likely has a method that takes (ScreenHandler, PlayerInventory, Text)
                Class<?> screenHandlerClass = classLoader.loadClass("net.minecraft.screen.ScreenHandler");
                try {
                    factoryMethod = screenFactoryClass.getMethod("create", screenHandlerClass, playerInventoryClass, textClass);
                    LOGGER.info("Found factory method via getMethod(create): {}", factoryMethod);
                } catch (NoSuchMethodException e) {
                    try {
                        factoryMethod = screenFactoryClass.getMethod("apply", screenHandlerClass, playerInventoryClass, textClass);
                        LOGGER.info("Found factory method via getMethod(apply): {}", factoryMethod);
                    } catch (NoSuchMethodException e2) {
                        // Try to find any method that takes 3 parameters
                        for (java.lang.reflect.Method m : screenFactoryClass.getMethods()) {
                            if (m.getParameterCount() == 3) {
                                factoryMethod = m;
                                LOGGER.info("Found factory method with 3 parameters: {}", m);
                                break;
                            }
                        }
                        if (factoryMethod == null) {
                            LOGGER.error("Could not find ScreenFactory method. Available methods:");
                            for (java.lang.reflect.Method m : screenFactoryClass.getMethods()) {
                                LOGGER.error("  - {} ({})", m.getName(), java.util.Arrays.toString(m.getParameterTypes()));
                            }
                            throw new RuntimeException("Could not find ScreenFactory method");
                        }
                    }
                }
            }
            
            final java.lang.reflect.Method finalFactoryMethod = factoryMethod;
            final java.lang.reflect.Constructor<?> finalScreenConstructor = screenConstructor;
            Object screenFactory = java.lang.reflect.Proxy.newProxyInstance(
                classLoader,
                new Class[]{screenFactoryClass},
                (proxy, method, args) -> {
                    LOGGER.debug("ScreenFactory proxy invoked: method={}, args.length={}", method.getName(), args != null ? args.length : 0);
                    // Invoke the screen constructor when the factory method is called
                    if (method.equals(finalFactoryMethod)) {
                        Object handler = args[0];
                        Object inventory = args[1];
                        Object title = args.length > 2 ? args[2] : null;
                        
                        LOGGER.info("Creating screen instance with handler={}, inventory={}, title={}", 
                            handler != null ? handler.getClass().getName() : "null",
                            inventory != null ? inventory.getClass().getName() : "null",
                            title);
                        return finalScreenConstructor.newInstance(handler, inventory, title);
                    }
                    // Handle Object methods
                    if (method.getDeclaringClass() == Object.class) {
                        if (method.getName().equals("toString")) {
                            return "ScreenFactory for " + screenClassName;
                        }
                        if (method.getName().equals("equals")) {
                            return proxy == args[0];
                        }
                        if (method.getName().equals("hashCode")) {
                            return System.identityHashCode(proxy);
                        }
                    }
                    return null;
                }
            );
            LOGGER.info("Created ScreenFactory proxy: {}", screenFactory.getClass().getName());
            
            // Register the screen using the provided register method
            LOGGER.info("Invoking register method with menuType={}, screenFactory={}", 
                menuType != null ? menuType.getClass().getName() : "null",
                screenFactory.getClass().getName());
            registerMethod.invoke(null, menuType, screenFactory);
            LOGGER.info("Successfully registered Fabric screen: {}", screenClassName);
        } catch (Exception e) {
            LOGGER.error("Error registering Fabric screen: {}", screenClassName, e);
            e.printStackTrace();
        }
    }
}
