package com.quackers29.businesscraft.fabric.init;

import com.quackers29.businesscraft.block.TownInterfaceBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.BlockItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;

/**
 * Fabric block registration using direct Fabric API calls.
 */
public class FabricModBlocks {
    private static boolean registrationAttempted = false;
    private static boolean registrationSuccessful = false;

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
     * Actual block registration logic - block with block entity
     */
    private static void registerBlocks() {
        try {
            // Instantiate the common TownInterfaceBlock
            // This ensures we use the shared logic for town creation (setPlacedBy) and UI
            // opening (use)
            Block townInterfaceBlock = new TownInterfaceBlock(FabricBlockSettings.create()
                    .strength(3.0f, 3.0f)
                    .requiresTool());

            // Register the block using proper Fabric Registry.register method
            Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation("businesscraft", "town_interface"),
                    townInterfaceBlock);

            // Create and register block item
            BlockItem townInterfaceBlockItem = new BlockItem(townInterfaceBlock, new FabricItemSettings());
            Registry.register(BuiltInRegistries.ITEM, new ResourceLocation("businesscraft", "town_interface"),
                    townInterfaceBlockItem);

            System.out.println("DEBUG: Town Interface Block registration completed successfully!");
        } catch (Exception e) {
            System.err.println("ERROR: Failed to register block: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
