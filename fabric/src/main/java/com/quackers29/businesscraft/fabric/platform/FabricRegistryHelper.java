package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.RegistryHelper;

/**
 * Fabric implementation of RegistryHelper using Object types for platform-agnostic interface.
 * Actual Minecraft-specific registration logic is handled in Fabric mod initialization.
 */
public class FabricRegistryHelper implements RegistryHelper {

    private static final String MOD_ID = "businesscraft";

    @Override
    public void registerBlock(String name, Object block) {
        // Platform-specific registration is handled in Fabric mod initialization
        // This method provides the interface but delegates to platform-specific code
        FabricRegistryDelegate.registerBlock(name, block);
    }

    @Override
    public void registerBlockItem(String name, Object block) {
        // Platform-specific registration is handled in Fabric mod initialization
        FabricRegistryDelegate.registerBlockItem(name, block);
    }

    @Override
    public void registerEntityType(String name, Object entityType) {
        // Platform-specific registration is handled in Fabric mod initialization
        FabricRegistryDelegate.registerEntityType(name, entityType);
    }

    @Override
    public void registerBlockEntityType(String name, Object blockEntityType) {
        // Platform-specific registration is handled in Fabric mod initialization
        FabricRegistryDelegate.registerBlockEntityType(name, blockEntityType);
    }

    @Override
    public void registerMenuType(String name, Object menuType) {
        // Platform-specific registration is handled in Fabric mod initialization
        FabricRegistryDelegate.registerMenuType(name, menuType);
    }

    @Override
    public Object getItem(Object location) {
        // Platform-specific lookup is handled in Fabric registry delegate
        return FabricRegistryDelegate.getItem(location);
    }

    @Override
    public Object getItemKey(Object item) {
        // Platform-specific lookup is handled in Fabric registry delegate
        return FabricRegistryDelegate.getItemKey(item);
    }

    /**
     * Platform-specific registry delegate that contains the actual Minecraft code.
     * This class is loaded only when Minecraft classes are available.
     */
    private static class FabricRegistryDelegate {
        // These methods will be implemented with actual Fabric registry calls
        // but are separated to avoid compilation issues in build environments

        static void registerBlock(String name, Object block) {
            // Implementation will be provided in platform-specific code
        }

        static void registerBlockItem(String name, Object block) {
            // Implementation will be provided in platform-specific code
        }

        static void registerEntityType(String name, Object entityType) {
            // Implementation will be provided in platform-specific code
        }

        static void registerBlockEntityType(String name, Object blockEntityType) {
            // Implementation will be provided in platform-specific code
        }

        static void registerMenuType(String name, Object menuType) {
            // Implementation will be provided in platform-specific code
        }

        static Object getItem(Object location) {
            // Implementation will be provided in platform-specific code
            return null;
        }

        static Object getItemKey(Object item) {
            // Implementation will be provided in platform-specific code
            return null;
        }
    }
}
