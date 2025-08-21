package com.quackers29.businesscraft.platform;

/**
 * Platform-agnostic service provider for accessing platform-specific implementations.
 * 
 * Enhanced MultiLoader approach: This common module class provides access to platform services
 * without depending on Minecraft classes. Platform modules (Forge/Fabric) register their
 * service implementations during mod initialization.
 */
public class PlatformServiceProvider {
    
    private static Object platformHelper;
    private static Object registryHelper;
    private static Object networkHelper;
    private static Object eventHelper;
    private static Object inventoryHelper;
    private static Object menuHelper;
    private static Object blockEntityHelper;
    
    /**
     * Set platform services from platform-specific implementations.
     * Called by platform modules during initialization.
     * 
     * @param platformHelper Platform helper implementation
     * @param registryHelper Registry helper implementation
     * @param networkHelper Network helper implementation
     * @param eventHelper Event helper implementation
     * @param inventoryHelper Inventory helper implementation
     * @param menuHelper Menu helper implementation
     * @param blockEntityHelper Block entity helper implementation
     */
    public static void setPlatform(Object platformHelper,
                                 Object registryHelper,
                                 Object networkHelper,
                                 Object eventHelper,
                                 Object inventoryHelper,
                                 Object menuHelper,
                                 Object blockEntityHelper) {
        PlatformServiceProvider.platformHelper = platformHelper;
        PlatformServiceProvider.registryHelper = registryHelper;
        PlatformServiceProvider.networkHelper = networkHelper;
        PlatformServiceProvider.eventHelper = eventHelper;
        PlatformServiceProvider.inventoryHelper = inventoryHelper;
        PlatformServiceProvider.menuHelper = menuHelper;
        PlatformServiceProvider.blockEntityHelper = blockEntityHelper;
    }
    
    /**
     * Get the platform helper instance (cast to platform-specific type in platform modules).
     */
    public static Object getPlatformHelper() {
        return platformHelper;
    }
    
    /**
     * Get the registry helper instance (cast to platform-specific type in platform modules).
     */
    public static Object getRegistryHelper() {
        return registryHelper;
    }
    
    /**
     * Get the network helper instance (cast to platform-specific type in platform modules).
     */
    public static Object getNetworkHelper() {
        return networkHelper;
    }
    
    /**
     * Get the event helper instance (cast to platform-specific type in platform modules).
     */
    public static Object getEventHelper() {
        return eventHelper;
    }
    
    /**
     * Get the inventory helper instance (cast to platform-specific type in platform modules).
     */
    public static Object getInventoryHelper() {
        return inventoryHelper;
    }
    
    /**
     * Get the menu helper instance (cast to platform-specific type in platform modules).
     */
    public static Object getMenuHelper() {
        return menuHelper;
    }
    
    /**
     * Get the block entity helper instance (cast to platform-specific type in platform modules).
     */
    public static Object getBlockEntityHelper() {
        return blockEntityHelper;
    }
}