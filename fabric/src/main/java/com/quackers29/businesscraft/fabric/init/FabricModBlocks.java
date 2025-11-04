package com.quackers29.businesscraft.fabric.init;

/**
 * Fabric block registration using reflection to access common module classes.
 * Uses reflection to avoid compile-time Minecraft dependencies.
 */
public class FabricModBlocks {

    public static void register() {
        System.out.println("DEBUG: FabricModBlocks.register() called - Starting block registration");

        try {
            ClassLoader classLoader = FabricModBlocks.class.getClassLoader();

            // Load essential Minecraft classes
            Class<?> registryClass = classLoader.loadClass("net.minecraft.core.Registry");
            Class<?> builtInRegistriesClass = classLoader.loadClass("net.minecraft.core.registries.BuiltInRegistries");
            Class<?> resourceLocationClass = classLoader.loadClass("net.minecraft.resources.ResourceLocation");
            Class<?> blockPropertiesClass = classLoader.loadClass("net.minecraft.world.level.block.state.BlockBehaviour$Properties");
            
            // Load common module TownInterfaceBlock class (available at runtime via common module JAR)
            Class<?> townInterfaceBlockClass = classLoader.loadClass("com.quackers29.businesscraft.block.TownInterfaceBlock");
            Class<?> blockItemClass = classLoader.loadClass("net.minecraft.world.item.BlockItem");
            Class<?> itemPropertiesClass = classLoader.loadClass("net.minecraft.world.item.Item$Properties");
            
            System.out.println("DEBUG: Essential classes loaded");

            // Create block properties using reflection
            Object blockProperties = blockPropertiesClass.getMethod("of").invoke(null);
            java.lang.reflect.Method mapColorMethod = blockPropertiesClass.getMethod("mapColor", 
                classLoader.loadClass("net.minecraft.world.level.material.MapColor"));
            java.lang.reflect.Method strengthMethod = blockPropertiesClass.getMethod("strength", float.class, float.class);
            java.lang.reflect.Method soundMethod = blockPropertiesClass.getMethod("sound", 
                classLoader.loadClass("net.minecraft.world.level.block.SoundType"));
            java.lang.reflect.Method requiresCorrectToolMethod = blockPropertiesClass.getMethod("requiresCorrectToolForDrops");
            
            // Configure block properties
            Object stoneMapColor = classLoader.loadClass("net.minecraft.world.level.material.MapColor").getField("STONE").get(null);
            Object stoneSound = classLoader.loadClass("net.minecraft.world.level.block.SoundType").getField("STONE").get(null);
            blockProperties = mapColorMethod.invoke(blockProperties, stoneMapColor);
            blockProperties = strengthMethod.invoke(blockProperties, 3.0f, 3.0f);
            blockProperties = soundMethod.invoke(blockProperties, stoneSound);
            blockProperties = requiresCorrectToolMethod.invoke(blockProperties);
            
            System.out.println("DEBUG: Block properties created");

            // Create TownInterfaceBlock instance using constructor that takes BlockBehaviour.Properties
            Object townInterfaceBlock = townInterfaceBlockClass.getConstructor(blockPropertiesClass)
                .newInstance(blockProperties);

            System.out.println("DEBUG: TownInterfaceBlock created");

            // Create BlockItem with default settings
            Object itemProperties = itemPropertiesClass.getConstructor().newInstance();
            Object townInterfaceBlockItem = blockItemClass.getConstructor(
                classLoader.loadClass("net.minecraft.world.level.block.Block"),
                itemPropertiesClass
            ).newInstance(townInterfaceBlock, itemProperties);

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
            System.err.println("Error in block registration: " + e.getMessage());
            e.printStackTrace();
            // Don't fail the mod initialization - just log the error
        }
    }
}
