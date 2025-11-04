package com.quackers29.businesscraft.fabric.init;

import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Fabric block entity registration using direct API calls.
 * Simplified approach that uses direct Minecraft classes.
 */
public class FabricModBlockEntities {
    private static final String MOD_ID = "businesscraft";
    
    // Store the registered block entity type for later retrieval
    private static Object TOWN_INTERFACE_ENTITY_TYPE;
    private static boolean registrationAttempted = false;
    private static boolean registrationSuccessful = false;

    public static Object getTownInterfaceEntityType() {
        return TOWN_INTERFACE_ENTITY_TYPE;
    }

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
            FabricModBlockEntities.class.getClassLoader(),
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
     * In Fabric, we need Registry and the registry objects
     */
    private static boolean areMinecraftClassesAvailable() {
        try {
            // Load Registry class
            Class<?> registryClass = loadClass("net.minecraft.core.Registry");
            
            // Try to find where BLOCK_ENTITY_TYPE registry is defined
            String[] possibleRegistryLocations = {
                "net.minecraft.core.registries.Registries",
                "net.minecraft.core.registries.BuiltInRegistries",
                "net.minecraft.registry.Registries"
            };
            
            boolean foundRegistries = false;
            for (String location : possibleRegistryLocations) {
                try {
                    Class<?> registriesClass = loadClass(location);
                    // Check if it has BLOCK_ENTITY_TYPE field
                    try {
                        registriesClass.getField("BLOCK_ENTITY_TYPE");
                        foundRegistries = true;
                        break;
                    } catch (NoSuchFieldException e) {
                        continue;
                    }
                } catch (ClassNotFoundException e) {
                    continue;
                }
            }
            
            // Also check Registry class itself
            if (!foundRegistries) {
                try {
                    registryClass.getField("BLOCK_ENTITY_TYPE");
                    foundRegistries = true;
                } catch (NoSuchFieldException e) {
                    // Not found
                }
            }
            
            loadClass("net.minecraft.util.Identifier"); // In Fabric, ResourceLocation is Identifier
            return foundRegistries;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static void register() {
        System.out.println("DEBUG: FabricModBlockEntities.register() called");
        if (registrationAttempted && registrationSuccessful) {
            System.out.println("DEBUG: Block entities already registered successfully, skipping");
            return; // Already registered successfully
        }

        registrationAttempted = true;
        System.out.println("DEBUG: FabricModBlockEntities.register() - Attempting block entity registration");

        // In Fabric, register block entities directly during mod initialization
        try {
            registerBlockEntities();
            registrationSuccessful = true;
            System.out.println("DEBUG: Block entity registration completed successfully!");
        } catch (Exception e) {
            System.err.println("ERROR: Block entity registration failed: " + e.getMessage());
            e.printStackTrace();
            // Try one more time after a short delay
            try {
                Thread.sleep(1000);
                registerBlockEntities();
                registrationSuccessful = true;
                System.out.println("DEBUG: Block entity registration succeeded on second attempt!");
            } catch (Exception e2) {
                System.err.println("ERROR: Block entity registration failed on second attempt: " + e2.getMessage());
                e2.printStackTrace();
                if (e2 instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }


    /**
     * Actual block entity registration logic - simplified test version
     */
    private static void registerBlockEntities() {
        try {
            // Skip block entity registration for now - the common module TownInterfaceEntity
            // uses Forge-specific classes (MenuProvider) that don't exist in Fabric
            // The basic block works without the block entity for now
            System.out.println("DEBUG: Skipping TownInterfaceEntity registration - uses Forge-specific MenuProvider class");
        } catch (Exception e) {
            System.err.println("ERROR: Failed to register block entities: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Object createEntitySupplier(ClassLoader classLoader) throws Exception {
        return (java.util.function.BiFunction<Object, Object, Object>) (blockPos, blockState) -> {
            try {
                // Use the common TownInterfaceEntity class via reflection
                // It's excluded from Fabric source sets but available at runtime via common module JAR
                Class<?> townInterfaceEntityClass = classLoader.loadClass("com.quackers29.businesscraft.block.entity.TownInterfaceEntity");
                Class<?> blockPosClass = classLoader.loadClass("net.minecraft.core.BlockPos");
                Class<?> blockStateClass = classLoader.loadClass("net.minecraft.world.level.block.state.BlockState");
                
                // Create TownInterfaceEntity instance using constructor that takes BlockPos and BlockState
                // The constructor internally calls PlatformAccess.getBlockEntities().getTownInterfaceEntityType()
                return townInterfaceEntityClass.getConstructor(
                    blockPosClass,
                    blockStateClass
                ).newInstance(blockPos, blockState);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create TownInterfaceEntity instance", e);
            }
        };
    }
}