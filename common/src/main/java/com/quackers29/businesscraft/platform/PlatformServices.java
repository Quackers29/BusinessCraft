package com.quackers29.businesscraft.platform;

/**
 * Platform services provider that gives access to platform-specific implementations.
 * This class serves as the main entry point for accessing platform abstraction services.
 * 
 * Enhanced MultiLoader approach: Platform implementations are provided by each mod loader
 * during initialization. This common module contains no platform-specific code.
 */
public class PlatformServices {
    
    private static PlatformHelper platformHelper;
    private static RegistryHelper registryHelper;
    private static NetworkHelper networkHelper;
    private static EventHelper eventHelper;
    private static InventoryHelper inventoryHelper;
    private static MenuHelper menuHelper;
    private static BusinessCraftMenuProvider menuProvider;
    private static BlockEntityHelper blockEntityHelper;
    private static ITownManagerService townManagerService;
    private static DataStorageHelper dataStorageHelper;
    private static TownInterfaceEntityService townInterfaceEntityService;
    
    // Static initialization is handled by platform-specific modules
    // Each platform (Forge/Fabric) calls setPlatform or initialize during mod loading
    
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
     * Get the menu provider instance.
     * @return The menu provider
     */
    public static BusinessCraftMenuProvider getMenuProvider() {
        return menuProvider;
    }

    /**
     * Get the block entity helper instance.
     * @return The block entity helper
     */
    public static BlockEntityHelper getBlockEntityHelper() {
        return blockEntityHelper;
    }
    
    /**
     * Get the town manager service instance.
     * @return The town manager service
     */
    public static ITownManagerService getTownManagerService() {
        return townManagerService;
    }
    
    /**
     * Get the data storage helper instance.
     * @return The data storage helper
     */
    public static DataStorageHelper getDataStorageHelper() {
        return dataStorageHelper;
    }

    /**
     * Get the town interface entity service instance.
     * @return The town interface entity service
     */
    public static TownInterfaceEntityService getTownInterfaceEntityService() {
        return townInterfaceEntityService;
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
     * @param menuProvider The menu provider implementation
     * @param blockEntityHelper The block entity helper implementation
     */
    public static void initialize(PlatformHelper platformHelper,
                                RegistryHelper registryHelper,
                                NetworkHelper networkHelper,
                                EventHelper eventHelper,
                                InventoryHelper inventoryHelper,
                                MenuHelper menuHelper,
                                BusinessCraftMenuProvider menuProvider,
                                BlockEntityHelper blockEntityHelper) {
        PlatformServices.platformHelper = platformHelper;
        PlatformServices.registryHelper = registryHelper;
        PlatformServices.networkHelper = networkHelper;
        PlatformServices.eventHelper = eventHelper;
        PlatformServices.inventoryHelper = inventoryHelper;
        PlatformServices.menuHelper = menuHelper;
        PlatformServices.menuProvider = menuProvider;
        PlatformServices.blockEntityHelper = blockEntityHelper;
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
     * @param menuProvider Menu provider implementation
     * @param blockEntityHelper Block entity helper implementation
     */
    public static void setPlatform(PlatformHelper platformHelper,
                                 RegistryHelper registryHelper,
                                 NetworkHelper networkHelper,
                                 EventHelper eventHelper,
                                 InventoryHelper inventoryHelper,
                                 MenuHelper menuHelper,
                                 BusinessCraftMenuProvider menuProvider,
                                 BlockEntityHelper blockEntityHelper) {
        PlatformServices.platformHelper = platformHelper;
        PlatformServices.registryHelper = registryHelper;
        PlatformServices.networkHelper = networkHelper;
        PlatformServices.eventHelper = eventHelper;
        PlatformServices.inventoryHelper = inventoryHelper;
        PlatformServices.menuHelper = menuHelper;
        PlatformServices.menuProvider = menuProvider;
        PlatformServices.blockEntityHelper = blockEntityHelper;
    }
    
    /**
     * Set platform services including town management and data storage.
     * Complete platform service initialization for full functionality.
     *
     * @param platformHelper Platform helper implementation
     * @param registryHelper Registry helper implementation
     * @param networkHelper Network helper implementation
     * @param eventHelper Event helper implementation
     * @param inventoryHelper Inventory helper implementation
     * @param menuHelper Menu helper implementation
     * @param menuProvider Menu provider implementation
     * @param blockEntityHelper Block entity helper implementation
     * @param townManagerService Town manager service implementation
     * @param dataStorageHelper Data storage helper implementation
     * @param townInterfaceEntityService Town interface entity service implementation
     */
    public static void setPlatformComplete(PlatformHelper platformHelper,
                                         RegistryHelper registryHelper,
                                         NetworkHelper networkHelper,
                                         EventHelper eventHelper,
                                         InventoryHelper inventoryHelper,
                                         MenuHelper menuHelper,
                                         BusinessCraftMenuProvider menuProvider,
                                         BlockEntityHelper blockEntityHelper,
                                         ITownManagerService townManagerService,
                                         DataStorageHelper dataStorageHelper,
                                         TownInterfaceEntityService townInterfaceEntityService) {
        PlatformServices.platformHelper = platformHelper;
        PlatformServices.registryHelper = registryHelper;
        PlatformServices.networkHelper = networkHelper;
        PlatformServices.eventHelper = eventHelper;
        PlatformServices.inventoryHelper = inventoryHelper;
        PlatformServices.menuHelper = menuHelper;
        PlatformServices.menuProvider = menuProvider;
        PlatformServices.blockEntityHelper = blockEntityHelper;
        PlatformServices.townManagerService = townManagerService;
        PlatformServices.dataStorageHelper = dataStorageHelper;
        PlatformServices.townInterfaceEntityService = townInterfaceEntityService;
    }
}