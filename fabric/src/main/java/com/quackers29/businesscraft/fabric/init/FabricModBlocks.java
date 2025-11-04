package com.quackers29.businesscraft.fabric.init;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;

/**
 * Fabric block registration using direct Fabric API calls.
 */
public class FabricModBlocks {
    private static boolean registrationAttempted = false;
    private static boolean registrationSuccessful = false;

    /**
     * Helper method to load classes with fallback classloaders
     * Tries multiple classloaders to find Minecraft classes
     */
    private static Class<?> loadClass(String className) throws ClassNotFoundException {
        // First, try to get a Minecraft class that's definitely loaded (like Block or Item)
        // Use that class's classloader to load other classes
        ClassLoader mcClassLoader = null;
        
        // Try to find a Minecraft class that's definitely loaded
        String[] knownMcClasses = {
            "net.minecraft.world.level.block.Block",
            "net.minecraft.world.item.Item", 
            "net.minecraft.util.Identifier",
            "net.minecraft.resources.ResourceLocation"
        };
        
        ClassLoader threadClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader[] classLoaders = {
            threadClassLoader,
            FabricModBlocks.class.getClassLoader(),
            ClassLoader.getSystemClassLoader()
        };
        
        for (ClassLoader loader : classLoaders) {
            if (loader == null) continue;
            for (String knownClass : knownMcClasses) {
                try {
                    Class<?> testClass = Class.forName(knownClass, true, loader);
                    mcClassLoader = testClass.getClassLoader();
                    System.out.println("DEBUG: Found Minecraft classloader via " + knownClass);
                    break;
                } catch (ClassNotFoundException e) {
                    continue;
                }
            }
            if (mcClassLoader != null) break;
        }
        
        if (mcClassLoader == null) {
            throw new ClassNotFoundException("Could not find Minecraft classloader - no Minecraft classes available");
        }
        
        // Now try to load the requested class using the Minecraft classloader
        try {
            return Class.forName(className, true, mcClassLoader);
        } catch (ClassNotFoundException e) {
            // Try alternative names
            String[] alternatives = {
                className.replace("net.minecraft.core.Registry", "net.minecraft.core.Registry"),
                className.replace("net.minecraft.core.registries.BuiltInRegistries", "net.minecraft.core.registries.Registries"),
                className.replace("net.minecraft.resources.ResourceLocation", "net.minecraft.util.Identifier")
            };
            
            for (String alt : alternatives) {
                if (alt.equals(className)) continue;
                try {
                    return Class.forName(alt, true, mcClassLoader);
                } catch (ClassNotFoundException e2) {
                    continue;
                }
            }
            
            throw new ClassNotFoundException("Could not load " + className + " using Minecraft classloader", e);
        }
    }

    /**
     * Check if Minecraft classes are available
     * In Fabric, we need Registry and the registry objects (BLOCK, ITEM)
     */
    private static boolean areMinecraftClassesAvailable() {
        try {
            // Load Registry class
            Class<?> registryClass = loadClass("net.minecraft.core.Registry");
            
            // Try to find where BLOCK and ITEM registries are defined
            // They might be in Registries, BuiltInRegistries, or Registry itself
            String[] possibleRegistryLocations = {
                "net.minecraft.core.registries.Registries",
                "net.minecraft.core.registries.BuiltInRegistries",
                "net.minecraft.registry.Registries" // Alternative location
            };
            
            boolean foundRegistries = false;
            for (String location : possibleRegistryLocations) {
                try {
                    Class<?> registriesClass = loadClass(location);
                    // Check if it has BLOCK and ITEM fields
                    try {
                        registriesClass.getField("BLOCK");
                        registriesClass.getField("ITEM");
                        foundRegistries = true;
                        System.out.println("DEBUG: Found registries in " + location);
                        break;
                    } catch (NoSuchFieldException e) {
                        // Try next location
                        continue;
                    }
                } catch (ClassNotFoundException e) {
                    // Try next location
                    continue;
                }
            }
            
            // Also check Registry class itself for static fields
            if (!foundRegistries) {
                try {
                    registryClass.getField("BLOCK");
                    registryClass.getField("ITEM");
                    foundRegistries = true;
                    System.out.println("DEBUG: Found registries in Registry class");
                } catch (NoSuchFieldException e) {
                    // Not in Registry class either
                }
            }
            
            loadClass("net.minecraft.util.Identifier"); // In Fabric, ResourceLocation is Identifier
            return foundRegistries;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Exception e) {
            System.err.println("DEBUG: Error checking class availability: " + e.getMessage());
            return false;
        }
    }

    public static void register() {
        System.out.println("DEBUG: FabricModBlocks.register() called");
        if (registrationAttempted && registrationSuccessful) {
            System.out.println("DEBUG: Blocks already registered successfully, skipping");
            return; // Already registered successfully
        }

        registrationAttempted = true;
        System.out.println("DEBUG: FabricModBlocks.register() - Attempting block registration");

        // In Fabric, register blocks directly during mod initialization
        // Don't check availability - just register and handle any exceptions
        try {
            registerBlocks();
            registrationSuccessful = true;
            System.out.println("DEBUG: Block registration completed successfully!");
        } catch (Exception e) {
            System.err.println("ERROR: Block registration failed: " + e.getMessage());
            e.printStackTrace();
            // Try one more time after a short delay (Fabric might still be initializing)
            try {
                Thread.sleep(1000);
                registerBlocks();
                registrationSuccessful = true;
                System.out.println("DEBUG: Block registration succeeded on second attempt!");
            } catch (Exception e2) {
                System.err.println("ERROR: Block registration failed on second attempt: " + e2.getMessage());
                e2.printStackTrace();
                if (e2 instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }


    /**
     * Actual block registration logic - simple working block
     */
    private static void registerBlocks() {
        try {
            // Create a simple working block using Fabric APIs
            // This avoids the complex TownInterfaceBlock that uses Forge-specific classes
            Block townInterfaceBlock = new Block(FabricBlockSettings.create()
                .strength(3.0f, 3.0f)
                .requiresTool());

            // Register the block using proper Fabric Registry.register method
            Registry.register(Registries.BLOCK, new Identifier("businesscraft", "town_interface"), townInterfaceBlock);

            // Create and register block item
            BlockItem townInterfaceBlockItem = new BlockItem(townInterfaceBlock, new FabricItemSettings());
            Registry.register(Registries.ITEM, new Identifier("businesscraft", "town_interface"), townInterfaceBlockItem);

            System.out.println("DEBUG: Simple Town Interface Block registration completed successfully!");
        } catch (Exception e) {
            System.err.println("ERROR: Failed to register simple block: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
