package com.quackers29.businesscraft.fabric.init;

/**
 * Fabric block registration - simplified approach with error handling.
 * Uses reflection to avoid compile-time Minecraft dependencies.
 */
public class FabricModBlocks {

    public static void register() {
        System.out.println("DEBUG: FabricModBlocks.register() called - Starting simple block registration");

        try {
            // Try a very simple approach - just register a basic block without complex properties
            ClassLoader classLoader = FabricModBlocks.class.getClassLoader();

            // Load essential classes
            Class<?> registryClass = classLoader.loadClass("net.minecraft.core.Registry");
            Class<?> builtInRegistriesClass = classLoader.loadClass("net.minecraft.core.registries.BuiltInRegistries");
            Class<?> resourceLocationClass = classLoader.loadClass("net.minecraft.resources.ResourceLocation");
            Class<?> blockClass = classLoader.loadClass("net.minecraft.world.level.block.Block");
            Class<?> blockItemClass = classLoader.loadClass("net.minecraft.world.item.BlockItem");
            Class<?> fabricItemSettingsClass = classLoader.loadClass("net.fabricmc.fabric.api.item.v1.FabricItemSettings");

            System.out.println("DEBUG: Essential classes loaded");

            // Create a simple block with default properties
            Object townInterfaceBlock = blockClass.getConstructor().newInstance();

            System.out.println("DEBUG: Simple block created");

            // Create BlockItem with default settings
            Object blockItemSettings = fabricItemSettingsClass.getConstructor().newInstance();
            Object townInterfaceBlockItem = blockItemClass.getConstructor(
                blockClass,
                blockItemSettings.getClass()
            ).newInstance(townInterfaceBlock, blockItemSettings);

            System.out.println("DEBUG: BlockItem created");

            // Register block
            Object blockRegistry = builtInRegistriesClass.getField("BLOCK").get(null);
            Object blockResourceLocation = resourceLocationClass.getConstructor(String.class, String.class)
                .newInstance("businesscraft", "town_interface");
            registryClass.getMethod("register", Object.class, Object.class, Object.class)
                .invoke(null, blockRegistry, blockResourceLocation, townInterfaceBlock);

            System.out.println("DEBUG: Block registered successfully");

            // Register block item
            Object itemRegistry = builtInRegistriesClass.getField("ITEM").get(null);
            Object itemResourceLocation = resourceLocationClass.getConstructor(String.class, String.class)
                .newInstance("businesscraft", "town_interface");
            registryClass.getMethod("register", Object.class, Object.class, Object.class)
                .invoke(null, itemRegistry, itemResourceLocation, townInterfaceBlockItem);

            System.out.println("DEBUG: BlockItem registered successfully");
            System.out.println("DEBUG: Town Interface Block registration completed successfully!");

        } catch (Exception e) {
            System.err.println("Error in simple block registration: " + e.getMessage());
            e.printStackTrace();
            // Don't fail the mod initialization - just log the error
        }
    }
}
