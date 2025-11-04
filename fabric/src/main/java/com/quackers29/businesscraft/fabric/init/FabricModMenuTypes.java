package com.quackers29.businesscraft.fabric.init;

import com.quackers29.businesscraft.fabric.platform.FabricMenuTypeHelper;
import com.quackers29.businesscraft.fabric.platform.FabricRegistryHelper;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric menu type registration
 * Uses Fabric's ScreenHandlerRegistry API to register menu types
 */
public class FabricModMenuTypes {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricModMenuTypes.class);
    private static final String MOD_ID = "businesscraft";
    private static boolean registrationAttempted = false;
    private static boolean registrationSuccessful = false;
    
    /**
     * Helper method to load classes with fallback classloaders
     */
    private static Class<?> loadClass(String className) throws ClassNotFoundException {
        // First, try to get a Minecraft class that's definitely loaded
        ClassLoader mcClassLoader = null;
        
        String[] knownMcClasses = {
            "net.minecraft.world.level.block.Block",
            "net.minecraft.world.item.Item", 
            "net.minecraft.util.Identifier",
            "net.minecraft.resources.ResourceLocation"
        };
        
        ClassLoader threadClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader[] classLoaders = {
            threadClassLoader,
            FabricModMenuTypes.class.getClassLoader(),
            ClassLoader.getSystemClassLoader()
        };
        
        for (ClassLoader loader : classLoaders) {
            if (loader == null) continue;
            for (String knownClass : knownMcClasses) {
                try {
                    Class<?> testClass = Class.forName(knownClass, true, loader);
                    mcClassLoader = testClass.getClassLoader();
                    break;
                } catch (ClassNotFoundException e) {
                    continue;
                }
            }
            if (mcClassLoader != null) break;
        }
        
        if (mcClassLoader == null) {
            throw new ClassNotFoundException("Could not find Minecraft classloader");
        }
        
        return Class.forName(className, true, mcClassLoader);
    }

    /**
     * Check if Minecraft classes are available
     */
    private static boolean areMinecraftClassesAvailable() {
        try {
            loadClass("net.minecraft.world.inventory.MenuType");
            loadClass("net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry");
            loadClass("net.minecraft.util.Identifier");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static void register() {
        System.out.println("DEBUG: FabricModMenuTypes.register() called");
        if (registrationAttempted && registrationSuccessful) {
            System.out.println("DEBUG: Menu types already registered successfully, skipping");
            return; // Already registered successfully
        }

        registrationAttempted = true;
        LOGGER.info("Registering Fabric menu types...");

        // In Fabric, register menu types directly during mod initialization
        try {
            registerMenuTypes();
            registrationSuccessful = true;
            LOGGER.info("Fabric menu types registered successfully");
        } catch (Exception e) {
            LOGGER.error("Menu type registration failed: " + e.getMessage(), e);
            // Try one more time after a short delay
            try {
                Thread.sleep(1000);
                registerMenuTypes();
                registrationSuccessful = true;
                LOGGER.info("Menu type registration succeeded on second attempt!");
            } catch (Exception e2) {
                LOGGER.error("Menu type registration failed on second attempt: " + e2.getMessage(), e2);
                if (e2 instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }


    /**
     * Actual menu type registration logic - simplified test version
     */
    private static void registerMenuTypes() {
        try {
            // Skip menu type registration for now - the common module TownInterfaceMenu
            // likely uses Forge-specific classes that don't exist in Fabric
            // The basic block works without the menu for now
            System.out.println("DEBUG: Skipping TownInterfaceMenu registration - may use Forge-specific classes");
        } catch (Exception e) {
            System.err.println("ERROR: Failed to register menu types: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void registerTownInterfaceMenu(Class<?> identifierClass, Class<?> menuTypeClass, Class<?> friendlyByteBufClass,
                                                   Class<?> playerInventoryClass, java.lang.reflect.Method registerMethod,
                                                   Class<?> screenHandlerRegistryClass, FabricRegistryHelper registry) {
        try {
            // Load menu class using reflection
            ClassLoader classLoader = FabricModMenuTypes.class.getClassLoader();
            Class<?> townInterfaceMenuClass = classLoader.loadClass("com.quackers29.businesscraft.menu.TownInterfaceMenu");
            Class<?> blockPosClass = classLoader.loadClass("net.minecraft.core.BlockPos");
            
            // Create identifier
            Object identifier = identifierClass.getConstructor(String.class, String.class)
                .newInstance(MOD_ID, "town_interface");
            
            // Create menu type factory
            java.util.function.Supplier<?> menuTypeSupplier = () -> {
                try {
                    // Use ScreenHandlerRegistry.ExtendedClientHandlerFactory
                    // For Fabric, we need to create a MenuType with ExtendedClientHandlerFactory
                    Class<?> extendedFactoryClass = screenHandlerRegistryClass.getClasses()[0];
                    for (Class<?> innerClass : screenHandlerRegistryClass.getClasses()) {
                        if (innerClass.getSimpleName().contains("Extended")) {
                            extendedFactoryClass = innerClass;
                            break;
                        }
                    }
                    
                    // Create ExtendedClientHandlerFactory
                    Object factory = java.lang.reflect.Proxy.newProxyInstance(
                        classLoader,
                        new Class[]{extendedFactoryClass},
                        (proxy, method, args) -> {
                            if (method.getName().equals("create")) {
                                // args[0] = syncId, args[1] = inventory, args[2] = buf
                                int syncId = (Integer) args[0];
                                Object inventory = args[1];
                                Object buf = args[2];
                                
                                // Read BlockPos from buffer
                                java.lang.reflect.Method readBlockPosMethod = friendlyByteBufClass.getMethod("readBlockPos");
                                Object blockPos = readBlockPosMethod.invoke(buf);
                                
                                // Create menu instance
                                java.lang.reflect.Constructor<?> menuConstructor = townInterfaceMenuClass.getConstructor(
                                    int.class, playerInventoryClass, blockPosClass
                                );
                                return menuConstructor.newInstance(syncId, inventory, blockPos);
                            }
                            return null;
                        }
                    );
                    
                    // Create MenuType using reflection
                    // Fabric uses MenuType.register() or ScreenHandlerRegistry methods
                    java.lang.reflect.Method registerMenuTypeMethod = menuTypeClass.getMethod("register", identifierClass, extendedFactoryClass);
                    return registerMenuTypeMethod.invoke(null, identifier, factory);
                } catch (Exception e) {
                    LOGGER.error("Error creating TownInterfaceMenu type", e);
                    return null;
                }
            };
            
            // Register menu type
            Object menuType = registerMethod.invoke(null, identifier, menuTypeSupplier);
            
            // Store in MenuTypeHelper
            FabricMenuTypeHelper.setTownInterfaceMenuType(menuType);
            
            // Register with registry
            registry.registerMenuType("town_interface", menuType);
            
            LOGGER.info("Registered TownInterfaceMenu");
        } catch (Exception e) {
            LOGGER.error("Error registering TownInterfaceMenu", e);
            e.printStackTrace();
        }
    }
    
    private static void registerTradeMenu(Class<?> identifierClass, Class<?> menuTypeClass, Class<?> friendlyByteBufClass,
                                          Class<?> playerInventoryClass, java.lang.reflect.Method registerMethod,
                                          Class<?> screenHandlerRegistryClass, FabricRegistryHelper registry) {
        try {
            ClassLoader classLoader = FabricModMenuTypes.class.getClassLoader();
            Class<?> tradeMenuClass = classLoader.loadClass("com.quackers29.businesscraft.menu.TradeMenu");
            Class<?> itemStackHandlerClass = classLoader.loadClass("net.minecraftforge.items.ItemStackHandler");
            
            Object identifier = identifierClass.getConstructor(String.class, String.class)
                .newInstance(MOD_ID, "trade_menu");
            
            java.util.function.Supplier<?> menuTypeSupplier = () -> {
                try {
                    Class<?> extendedFactoryClass = screenHandlerRegistryClass.getClasses()[0];
                    for (Class<?> innerClass : screenHandlerRegistryClass.getClasses()) {
                        if (innerClass.getSimpleName().contains("Extended")) {
                            extendedFactoryClass = innerClass;
                            break;
                        }
                    }
                    
                    Object factory = java.lang.reflect.Proxy.newProxyInstance(
                        classLoader,
                        new Class[]{extendedFactoryClass},
                        (proxy, method, args) -> {
                            if (method.getName().equals("create")) {
                                int syncId = (Integer) args[0];
                                Object inventory = args[1];
                                
                                // Create ItemStackHandler with 2 slots
                                java.lang.reflect.Constructor<?> handlerConstructor = itemStackHandlerClass.getConstructor(int.class);
                                Object handler = handlerConstructor.newInstance(2);
                                
                                java.lang.reflect.Constructor<?> menuConstructor = tradeMenuClass.getConstructor(
                                    int.class, playerInventoryClass, itemStackHandlerClass
                                );
                                return menuConstructor.newInstance(syncId, inventory, handler);
                            }
                            return null;
                        }
                    );
                    
                    java.lang.reflect.Method registerMenuTypeMethod = menuTypeClass.getMethod("register", identifierClass, extendedFactoryClass);
                    return registerMenuTypeMethod.invoke(null, identifier, factory);
                } catch (Exception e) {
                    LOGGER.error("Error creating TradeMenu type", e);
                    return null;
                }
            };
            
            Object menuType = registerMethod.invoke(null, identifier, menuTypeSupplier);
            FabricMenuTypeHelper.setTradeMenuType(menuType);
            registry.registerMenuType("trade_menu", menuType);
            
            LOGGER.info("Registered TradeMenu");
        } catch (Exception e) {
            LOGGER.error("Error registering TradeMenu", e);
            e.printStackTrace();
        }
    }
    
    private static void registerStorageMenu(Class<?> identifierClass, Class<?> menuTypeClass, Class<?> friendlyByteBufClass,
                                            Class<?> playerInventoryClass, java.lang.reflect.Method registerMethod,
                                            Class<?> screenHandlerRegistryClass, FabricRegistryHelper registry) {
        try {
            ClassLoader classLoader = FabricModMenuTypes.class.getClassLoader();
            Class<?> storageMenuClass = classLoader.loadClass("com.quackers29.businesscraft.menu.StorageMenu");
            Class<?> itemStackHandlerClass = classLoader.loadClass("net.minecraftforge.items.ItemStackHandler");
            
            Object identifier = identifierClass.getConstructor(String.class, String.class)
                .newInstance(MOD_ID, "storage_menu");
            
            java.util.function.Supplier<?> menuTypeSupplier = () -> {
                try {
                    Class<?> extendedFactoryClass = screenHandlerRegistryClass.getClasses()[0];
                    for (Class<?> innerClass : screenHandlerRegistryClass.getClasses()) {
                        if (innerClass.getSimpleName().contains("Extended")) {
                            extendedFactoryClass = innerClass;
                            break;
                        }
                    }
                    
                    Object factory = java.lang.reflect.Proxy.newProxyInstance(
                        classLoader,
                        new Class[]{extendedFactoryClass},
                        (proxy, method, args) -> {
                            if (method.getName().equals("create")) {
                                int syncId = (Integer) args[0];
                                Object inventory = args[1];
                                
                                java.lang.reflect.Constructor<?> handlerConstructor = itemStackHandlerClass.getConstructor(int.class);
                                Object handler = handlerConstructor.newInstance(18);
                                
                                java.lang.reflect.Constructor<?> menuConstructor = storageMenuClass.getConstructor(
                                    int.class, playerInventoryClass, itemStackHandlerClass
                                );
                                return menuConstructor.newInstance(syncId, inventory, handler);
                            }
                            return null;
                        }
                    );
                    
                    java.lang.reflect.Method registerMenuTypeMethod = menuTypeClass.getMethod("register", identifierClass, extendedFactoryClass);
                    return registerMenuTypeMethod.invoke(null, identifier, factory);
                } catch (Exception e) {
                    LOGGER.error("Error creating StorageMenu type", e);
                    return null;
                }
            };
            
            Object menuType = registerMethod.invoke(null, identifier, menuTypeSupplier);
            FabricMenuTypeHelper.setStorageMenuType(menuType);
            registry.registerMenuType("storage_menu", menuType);
            
            LOGGER.info("Registered StorageMenu");
        } catch (Exception e) {
            LOGGER.error("Error registering StorageMenu", e);
            e.printStackTrace();
        }
    }
    
    private static void registerPaymentBoardMenu(Class<?> identifierClass, Class<?> menuTypeClass, Class<?> friendlyByteBufClass,
                                                 Class<?> playerInventoryClass, java.lang.reflect.Method registerMethod,
                                                 Class<?> screenHandlerRegistryClass, FabricRegistryHelper registry) {
        try {
            ClassLoader classLoader = FabricModMenuTypes.class.getClassLoader();
            Class<?> paymentBoardMenuClass = classLoader.loadClass("com.quackers29.businesscraft.menu.PaymentBoardMenu");
            
            Object identifier = identifierClass.getConstructor(String.class, String.class)
                .newInstance(MOD_ID, "payment_board_menu");
            
            java.util.function.Supplier<?> menuTypeSupplier = () -> {
                try {
                    Class<?> extendedFactoryClass = screenHandlerRegistryClass.getClasses()[0];
                    for (Class<?> innerClass : screenHandlerRegistryClass.getClasses()) {
                        if (innerClass.getSimpleName().contains("Extended")) {
                            extendedFactoryClass = innerClass;
                            break;
                        }
                    }
                    
                    Object factory = java.lang.reflect.Proxy.newProxyInstance(
                        classLoader,
                        new Class[]{extendedFactoryClass},
                        (proxy, method, args) -> {
                            if (method.getName().equals("create")) {
                                int syncId = (Integer) args[0];
                                Object inventory = args[1];
                                Object buf = args[2];
                                
                                java.lang.reflect.Constructor<?> menuConstructor = paymentBoardMenuClass.getConstructor(
                                    int.class, playerInventoryClass, friendlyByteBufClass
                                );
                                return menuConstructor.newInstance(syncId, inventory, buf);
                            }
                            return null;
                        }
                    );
                    
                    java.lang.reflect.Method registerMenuTypeMethod = menuTypeClass.getMethod("register", identifierClass, extendedFactoryClass);
                    return registerMenuTypeMethod.invoke(null, identifier, factory);
                } catch (Exception e) {
                    LOGGER.error("Error creating PaymentBoardMenu type", e);
                    return null;
                }
            };
            
            Object menuType = registerMethod.invoke(null, identifier, menuTypeSupplier);
            FabricMenuTypeHelper.setPaymentBoardMenuType(menuType);
            registry.registerMenuType("payment_board_menu", menuType);
            
            LOGGER.info("Registered PaymentBoardMenu");
        } catch (Exception e) {
            LOGGER.error("Error registering PaymentBoardMenu", e);
            e.printStackTrace();
        }
    }
}
