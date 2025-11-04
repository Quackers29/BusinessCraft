package com.quackers29.businesscraft.fabric.init;

import com.quackers29.businesscraft.fabric.platform.FabricMenuTypeHelper;
import com.quackers29.businesscraft.fabric.platform.FabricRegistryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric menu type registration
 * Uses Fabric's ScreenHandlerRegistry API to register menu types
 */
public class FabricModMenuTypes {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricModMenuTypes.class);
    private static final String MOD_ID = "businesscraft";
    
    public static void register() {
        LOGGER.info("Registering Fabric menu types...");
        
        try {
            ClassLoader classLoader = FabricModMenuTypes.class.getClassLoader();
            
            // Load Fabric's ScreenHandlerRegistry
            Class<?> screenHandlerRegistryClass = classLoader.loadClass("net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry");
            Class<?> menuTypeClass = classLoader.loadClass("net.minecraft.world.inventory.MenuType");
            Class<?> identifierClass = classLoader.loadClass("net.minecraft.util.Identifier");
            Class<?> playerInventoryClass = classLoader.loadClass("net.minecraft.world.entity.player.Inventory");
            Class<?> friendlyByteBufClass = classLoader.loadClass("net.minecraft.network.FriendlyByteBuf");
            
            // Get ScreenHandlerRegistry.SimpleClientHandlerFactory
            Class<?> simpleClientHandlerFactoryClass = screenHandlerRegistryClass.getClasses()[0]; // Usually the first inner class
            
            // Get ScreenHandlerRegistry.register method
            java.lang.reflect.Method registerMethod = screenHandlerRegistryClass.getMethod(
                "register",
                identifierClass,
                java.util.function.Supplier.class
            );
            
            // Get RegistryHelper for registration
            FabricRegistryHelper registry = (FabricRegistryHelper) com.quackers29.businesscraft.fabric.BusinessCraftFabric.REGISTRY;
            
            // Register TownInterfaceMenu
            registerTownInterfaceMenu(identifierClass, menuTypeClass, friendlyByteBufClass, playerInventoryClass, registerMethod, screenHandlerRegistryClass, registry);
            
            // Register TradeMenu
            registerTradeMenu(identifierClass, menuTypeClass, friendlyByteBufClass, playerInventoryClass, registerMethod, screenHandlerRegistryClass, registry);
            
            // Register StorageMenu
            registerStorageMenu(identifierClass, menuTypeClass, friendlyByteBufClass, playerInventoryClass, registerMethod, screenHandlerRegistryClass, registry);
            
            // Register PaymentBoardMenu
            registerPaymentBoardMenu(identifierClass, menuTypeClass, friendlyByteBufClass, playerInventoryClass, registerMethod, screenHandlerRegistryClass, registry);
            
            LOGGER.info("Fabric menu types registered successfully");
        } catch (Exception e) {
            LOGGER.error("Error registering Fabric menu types", e);
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
