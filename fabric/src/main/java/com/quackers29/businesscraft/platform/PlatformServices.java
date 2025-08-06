package com.quackers29.businesscraft.platform;

/**
 * Platform services provider for Fabric that gives access to platform-specific implementations.
 * This class serves as the main entry point for accessing platform abstraction services.
 * 
 * Enhanced MultiLoader approach: Platform implementations are provided by Fabric mod loader
 * during initialization. This Fabric module contains Fabric-specific implementations.
 */
public class PlatformServices {
    
    private static PlatformHelper platformHelper;
    private static RegistryHelper registryHelper;
    private static NetworkHelper networkHelper;
    private static EventHelper eventHelper;
    private static InventoryHelper inventoryHelper;
    private static MenuHelper menuHelper;
    private static BlockEntityHelper blockEntityHelper;
    
    /**
     * Get the platform helper instance.
     * @return The platform helper
     */
    public static PlatformHelper getPlatformHelper() {
        return platformHelper;
    }
    
    /**
     * Get the registry helper instance.
     * @return The registry helper
     */
    public static RegistryHelper getRegistryHelper() {
        return registryHelper;
    }
    
    /**
     * Get the network helper instance.
     * @return The network helper
     */
    public static NetworkHelper getNetworkHelper() {
        return networkHelper;
    }
    
    /**
     * Get the event helper instance.
     * @return The event helper
     */
    public static EventHelper getEventHelper() {
        return eventHelper;
    }
    
    /**
     * Get the inventory helper instance.
     * @return The inventory helper
     */
    public static InventoryHelper getInventoryHelper() {
        return inventoryHelper;
    }
    
    /**
     * Get the menu helper instance.
     * @return The menu helper
     */
    public static MenuHelper getMenuHelper() {
        return menuHelper;
    }
    
    /**
     * Get the block entity helper instance.
     * @return The block entity helper
     */
    public static BlockEntityHelper getBlockEntityHelper() {
        return blockEntityHelper;
    }
    
    /**
     * Set platform services from individual service implementations.
     * This provides maximum flexibility for platform initialization.
     * 
     * @param platformHelper Platform helper implementation
     * @param registryHelper Registry helper implementation
     * @param networkHelper Network helper implementation
     * @param eventHelper Event helper implementation
     * @param inventoryHelper Inventory helper implementation
     * @param menuHelper Menu helper implementation
     * @param blockEntityHelper Block entity helper implementation
     */
    public static void setPlatform(PlatformHelper platformHelper,
                                 RegistryHelper registryHelper,
                                 NetworkHelper networkHelper,
                                 EventHelper eventHelper,
                                 InventoryHelper inventoryHelper,
                                 MenuHelper menuHelper,
                                 BlockEntityHelper blockEntityHelper) {
        PlatformServices.platformHelper = platformHelper;
        PlatformServices.registryHelper = registryHelper;
        PlatformServices.networkHelper = networkHelper;
        PlatformServices.eventHelper = eventHelper;
        PlatformServices.inventoryHelper = inventoryHelper;
        PlatformServices.menuHelper = menuHelper;
        PlatformServices.blockEntityHelper = blockEntityHelper;
    }
}