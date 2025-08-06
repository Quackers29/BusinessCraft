package com.quackers29.businesscraft.platform.forge;

/**
 * Forge platform services container.
 * Provides a single point of access to all Forge platform service implementations.
 */
public class ForgePlatformServices {
    
    private final ForgePlatformHelper platformHelper;
    private final ForgeRegistryHelper registryHelper;
    private final ForgeNetworkHelper networkHelper;
    private final ForgeEventHelper eventHelper;
    private final ForgeInventoryHelper inventoryHelper;
    private final ForgeMenuHelper menuHelper;
    private final ForgeBlockEntityHelper blockEntityHelper;
    
    public ForgePlatformServices() {
        this.platformHelper = new ForgePlatformHelper();
        this.registryHelper = new ForgeRegistryHelper("businesscraft");
        this.networkHelper = new ForgeNetworkHelper("businesscraft", "1.0");
        this.eventHelper = new ForgeEventHelper();
        this.inventoryHelper = new ForgeInventoryHelper();
        this.menuHelper = new ForgeMenuHelper();
        this.blockEntityHelper = new ForgeBlockEntityHelper();
    }
    
    public ForgePlatformHelper getPlatformHelper() {
        return platformHelper;
    }
    
    public ForgeRegistryHelper getRegistryHelper() {
        return registryHelper;
    }
    
    public ForgeNetworkHelper getNetworkHelper() {
        return networkHelper;
    }
    
    public ForgeEventHelper getEventHelper() {
        return eventHelper;
    }
    
    public ForgeInventoryHelper getInventoryHelper() {
        return inventoryHelper;
    }
    
    public ForgeMenuHelper getMenuHelper() {
        return menuHelper;
    }
    
    public ForgeBlockEntityHelper getBlockEntityHelper() {
        return blockEntityHelper;
    }
}