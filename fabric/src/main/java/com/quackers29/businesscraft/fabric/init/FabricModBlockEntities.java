package com.quackers29.businesscraft.fabric.init;

/**
 * Fabric block entity registration using direct API calls.
 * Simplified approach that uses direct Minecraft classes.
 */
public class FabricModBlockEntities {

    private static final String MOD_ID = "businesscraft";
    
    // Store the registered block entity type for later retrieval
    private static Object TOWN_INTERFACE_ENTITY_TYPE;

    public static Object getTownInterfaceEntityType() {
        return TOWN_INTERFACE_ENTITY_TYPE;
    }

    public static void register() {
        System.out.println("DEBUG: FabricModBlockEntities.register() called");

        try {
            System.out.println("DEBUG: Starting FabricModBlockEntities registration");

            // Use reflection to access all classes at runtime
            ClassLoader classLoader = FabricModBlockEntities.class.getClassLoader();

            // Load Minecraft classes
            Class<?> registryClass = classLoader.loadClass("net.minecraft.core.Registry");
            Class<?> builtInRegistriesClass = classLoader.loadClass("net.minecraft.core.registries.BuiltInRegistries");
            Class<?> resourceLocationClass = classLoader.loadClass("net.minecraft.resources.ResourceLocation");
            Class<?> blockEntityTypeClass = classLoader.loadClass("net.minecraft.world.level.block.entity.BlockEntityType");
            Class<?> blockEntitySupplierClass = classLoader.loadClass("net.minecraft.world.level.block.entity.BlockEntityType$BlockEntitySupplier");

            // Get the registered TownInterfaceBlock
            Object blockRegistry = builtInRegistriesClass.getField("BLOCK").get(null);
            Object blockResourceLocation = resourceLocationClass.getConstructor(String.class, String.class)
                .newInstance(MOD_ID, "town_interface");
            Object townInterfaceBlock = registryClass.getMethod("get", Object.class, Object.class)
                .invoke(null, blockRegistry, blockResourceLocation);

            if (townInterfaceBlock == null) {
                System.err.println("ERROR: TownInterfaceBlock not found in registry. Block entity registration skipped.");
                return;
            }

            System.out.println("DEBUG: Found TownInterfaceBlock in registry");

            // Create BlockEntityType using Builder pattern
            Class<?> townInterfaceEntityClass = classLoader.loadClass("com.quackers29.businesscraft.fabric.block.entity.TownInterfaceEntity");

            Object blockEntityTypeBuilder = blockEntityTypeClass.getMethod("Builder", Class.class)
                .invoke(null, townInterfaceEntityClass);

            // Create block array
            Object blockArray = java.lang.reflect.Array.newInstance(townInterfaceBlock.getClass(), 1);
            java.lang.reflect.Array.set(blockArray, 0, townInterfaceBlock);

            // Create supplier lambda
            Object supplier = createEntitySupplier(classLoader);

            // Call Builder.of
            blockEntityTypeBuilder = blockEntityTypeBuilder.getClass().getMethod("of",
                blockEntitySupplierClass,
                blockArray.getClass()
            ).invoke(blockEntityTypeBuilder, supplier, blockArray);

            // Build the BlockEntityType
            Object townInterfaceEntityType = blockEntityTypeBuilder.getClass().getMethod("build",
                resourceLocationClass
            ).invoke(blockEntityTypeBuilder, blockResourceLocation);

            System.out.println("DEBUG: BlockEntityType created");

            // Register the block entity type
            Object blockEntityRegistry = builtInRegistriesClass.getField("BLOCK_ENTITY_TYPE").get(null);
            Object entityResourceLocation = resourceLocationClass.getConstructor(String.class, String.class)
                .newInstance(MOD_ID, "town_interface");
            registryClass.getMethod("register", Object.class, Object.class, Object.class)
                .invoke(null, blockEntityRegistry, entityResourceLocation, townInterfaceEntityType);
            
            // Store for later retrieval
            TOWN_INTERFACE_ENTITY_TYPE = townInterfaceEntityType;

            System.out.println("DEBUG: Town Interface Entity BlockEntityType registered successfully");

        } catch (Exception e) {
            System.err.println("Error registering Town Interface Entity: " + e.getMessage());
            e.printStackTrace();
            // Don't fail the mod initialization - just log the error
        }
    }

    private static Object createEntitySupplier(ClassLoader classLoader) throws Exception {
        return (java.util.function.BiFunction<Object, Object, Object>) (blockPos, blockState) -> {
            try {
                Class<?> townInterfaceEntityClass = classLoader.loadClass("com.quackers29.businesscraft.fabric.block.entity.TownInterfaceEntity");
                Class<?> blockPosClass = classLoader.loadClass("net.minecraft.core.BlockPos");
                Class<?> blockStateClass = classLoader.loadClass("net.minecraft.world.level.block.state.BlockState");
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