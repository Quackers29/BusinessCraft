package com.quackers29.businesscraft.platform.fabric;

/**
 * Fabric platform services container.
 * Provides a single point of access to all Fabric platform service implementations.
 */
public class FabricPlatformServices {
    
    private final FabricPlatformHelper platformHelper;
    // TODO: Add other service implementations
    
    public FabricPlatformServices() {
        this.platformHelper = new FabricPlatformHelper();
        // TODO: Initialize other services
    }
    
    public FabricPlatformHelper getPlatformHelper() {
        return platformHelper;
    }
    
    // TODO: Add other getters - temporary null implementations for now
    public Object getRegistryHelper() {
        return null; // TODO: Implement FabricRegistryHelper
    }
    
    public Object getNetworkHelper() {
        return null; // TODO: Implement FabricNetworkHelper
    }
    
    public Object getEventHelper() {
        return null; // TODO: Implement FabricEventHelper
    }
    
    public Object getInventoryHelper() {
        return null; // TODO: Implement FabricInventoryHelper
    }
    
    public Object getMenuHelper() {
        return null; // TODO: Implement FabricMenuHelper
    }
    
    public Object getBlockEntityHelper() {
        return null; // TODO: Implement FabricBlockEntityHelper
    }
}