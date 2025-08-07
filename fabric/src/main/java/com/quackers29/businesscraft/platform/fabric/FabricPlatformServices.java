package com.quackers29.businesscraft.platform.fabric;

/**
 * Fabric platform services container.
 * Provides a single point of access to all Fabric platform service implementations.
 */
public class FabricPlatformServices {
    
    private final FabricPlatformHelper platformHelper;
    private final FabricRegistryHelper registryHelper;
    private final FabricNetworkHelper networkHelper;
    private final FabricEventHelper eventHelper;
    private final FabricInventoryHelper inventoryHelper;
    private final FabricMenuHelper menuHelper;
    private final FabricBlockEntityHelper blockEntityHelper;
    
    public FabricPlatformServices() {
        this.platformHelper = new FabricPlatformHelper();
        this.registryHelper = new FabricRegistryHelper();
        this.networkHelper = new FabricNetworkHelper();
        this.eventHelper = new FabricEventHelper();
        this.inventoryHelper = new FabricInventoryHelper();
        this.menuHelper = new FabricMenuHelper();
        this.blockEntityHelper = new FabricBlockEntityHelper();
    }
    
    public FabricPlatformHelper getPlatformHelper() {
        return platformHelper;
    }
    
    public FabricRegistryHelper getRegistryHelper() {
        return registryHelper;
    }
    
    public FabricNetworkHelper getNetworkHelper() {
        return networkHelper;
    }
    
    public FabricEventHelper getEventHelper() {
        return eventHelper;
    }
    
    public FabricInventoryHelper getInventoryHelper() {
        return inventoryHelper;
    }
    
    public FabricMenuHelper getMenuHelper() {
        return menuHelper;
    }
    
    public FabricBlockEntityHelper getBlockEntityHelper() {
        return blockEntityHelper;
    }
}