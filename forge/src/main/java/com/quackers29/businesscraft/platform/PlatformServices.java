package com.quackers29.businesscraft.platform;

import com.quackers29.businesscraft.platform.forge.ForgePlatformHelper;
import com.quackers29.businesscraft.platform.forge.ForgeRegistryHelper;
import com.quackers29.businesscraft.platform.forge.ForgeNetworkHelper;
import com.quackers29.businesscraft.platform.forge.ForgeEventHelper;
import com.quackers29.businesscraft.platform.forge.ForgeInventoryHelper;
import com.quackers29.businesscraft.platform.forge.ForgeMenuHelper;
import com.quackers29.businesscraft.platform.forge.ForgeBlockEntityHelper;

/**
 * Platform services provider that gives access to platform-specific implementations.
 * This class serves as the main entry point for accessing platform abstraction services.
 */
public class PlatformServices {
    
    private static PlatformHelper platformHelper;
    private static RegistryHelper registryHelper;
    private static NetworkHelper networkHelper;
    private static EventHelper eventHelper;
    private static InventoryHelper inventoryHelper;
    private static MenuHelper menuHelper;
    private static BlockEntityHelper blockEntityHelper;
    
    // Initialize with Forge implementations
    static {
        initializeForge();
    }
    
    /**
     * Initialize platform services for Forge.
     */
    private static void initializeForge() {
        platformHelper = new ForgePlatformHelper();
        registryHelper = new ForgeRegistryHelper("businesscraft");
        networkHelper = new ForgeNetworkHelper("businesscraft", "1.0");
        eventHelper = new ForgeEventHelper();
        inventoryHelper = new ForgeInventoryHelper();
        menuHelper = new ForgeMenuHelper();
        blockEntityHelper = new ForgeBlockEntityHelper();
    }
    
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
     * Initialize platform services with custom implementations.
     * This is primarily for testing or future platform expansion.
     * 
     * @param platformHelper The platform helper implementation
     * @param registryHelper The registry helper implementation
     * @param networkHelper The network helper implementation
     * @param eventHelper The event helper implementation
     */
    public static void initialize(PlatformHelper platformHelper, 
                                RegistryHelper registryHelper,
                                NetworkHelper networkHelper,
                                EventHelper eventHelper) {
        PlatformServices.platformHelper = platformHelper;
        PlatformServices.registryHelper = registryHelper;
        PlatformServices.networkHelper = networkHelper;
        PlatformServices.eventHelper = eventHelper;
    }
    
    /**
     * Initialize platform services with all implementations.
     * This is for complete platform service initialization.
     * 
     * @param platformHelper The platform helper implementation
     * @param registryHelper The registry helper implementation
     * @param networkHelper The network helper implementation
     * @param eventHelper The event helper implementation
     * @param inventoryHelper The inventory helper implementation
     * @param menuHelper The menu helper implementation
     * @param blockEntityHelper The block entity helper implementation
     */
    public static void initialize(PlatformHelper platformHelper, 
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