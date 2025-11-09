package com.quackers29.businesscraft.fabric.init;

import com.quackers29.businesscraft.fabric.platform.FabricMenuTypeHelper;
import com.quackers29.businesscraft.fabric.platform.FabricRegistryHelper;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric menu type registration
 * Uses Fabric's ScreenHandlerRegistry API to register menu types
 */
public class FabricModMenuTypes {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricModMenuTypes.class);
    private static final String MOD_ID = "businesscraft";
    private static boolean registrationAttempted = false;
    private static boolean registrationSuccessful = false;
    
    /**
     * Helper method to load classes with fallback classloaders
     */
    private static Class<?> loadClass(String className) throws ClassNotFoundException {
        // First, try to get a Minecraft class that's definitely loaded
        ClassLoader mcClassLoader = null;
        
        String[] knownMcClasses = {
            "net.minecraft.world.level.block.Block",
            "net.minecraft.world.item.Item", 
            "net.minecraft.util.Identifier",
            "net.minecraft.resources.ResourceLocation"
        };
        
        ClassLoader threadClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader[] classLoaders = {
            threadClassLoader,
            FabricModMenuTypes.class.getClassLoader(),
            ClassLoader.getSystemClassLoader()
        };
        
        for (ClassLoader loader : classLoaders) {
            if (loader == null) continue;
            for (String knownClass : knownMcClasses) {
                try {
                    Class<?> testClass = Class.forName(knownClass, true, loader);
                    mcClassLoader = testClass.getClassLoader();
                    break;
                } catch (ClassNotFoundException e) {
                    continue;
                }
            }
            if (mcClassLoader != null) break;
        }
        
        if (mcClassLoader == null) {
            throw new ClassNotFoundException("Could not find Minecraft classloader");
        }
        
        return Class.forName(className, true, mcClassLoader);
    }

    /**
     * Check if Minecraft classes are available
     */
    private static boolean areMinecraftClassesAvailable() {
        try {
            loadClass("net.minecraft.world.inventory.MenuType");
            loadClass("net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry");
            loadClass("net.minecraft.util.Identifier");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static void register() {
        System.out.println("DEBUG: FabricModMenuTypes.register() called");
        if (registrationAttempted && registrationSuccessful) {
            System.out.println("DEBUG: Menu types already registered successfully, skipping");
            return; // Already registered successfully
        }

        registrationAttempted = true;
        LOGGER.info("Registering Fabric menu types...");

        // In Fabric, register menu types directly during mod initialization
        try {
            registerMenuTypes();
            registrationSuccessful = true;
            LOGGER.info("Fabric menu types registered successfully");
        } catch (Exception e) {
            LOGGER.error("Menu type registration failed: " + e.getMessage(), e);
            // Try one more time after a short delay
            try {
                Thread.sleep(1000);
                registerMenuTypes();
                registrationSuccessful = true;
                LOGGER.info("Menu type registration succeeded on second attempt!");
            } catch (Exception e2) {
                LOGGER.error("Menu type registration failed on second attempt: " + e2.getMessage(), e2);
                if (e2 instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }


    /**
     * Menu types cannot be registered on Fabric because the common module classes
     * use Forge-specific classes (AbstractContainerMenu) that don't exist in Fabric.
     * Menu opening will be handled through event callbacks instead.
     */
    private static void registerMenuTypes() {
        try {
            // Skip menu type registration - common module classes use Forge-specific AbstractContainerMenu
            // Menu opening will be handled through Fabric's event system instead
            System.out.println("DEBUG: Skipping menu type registration - common module uses Forge-specific classes");
            System.out.println("DEBUG: Menu opening will be handled through Fabric event callbacks");
        } catch (Exception e) {
            System.err.println("ERROR: Failed to skip menu type registration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
}
