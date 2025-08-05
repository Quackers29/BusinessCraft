package com.quackers29.businesscraft.platform;

import com.quackers29.businesscraft.platform.forge.ForgePlatformHelper;
import com.quackers29.businesscraft.platform.forge.ForgeRegistryHelper;
import com.quackers29.businesscraft.platform.forge.ForgeNetworkHelper;
import com.quackers29.businesscraft.platform.forge.ForgeEventHelper;

/**
 * Platform services provider that gives access to platform-specific implementations.
 * This class serves as the main entry point for accessing platform abstraction services.
 */
public class PlatformServices {
    
    private static PlatformHelper platformHelper;
    private static RegistryHelper registryHelper;
    private static NetworkHelper networkHelper;
    private static EventHelper eventHelper;
    
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
}