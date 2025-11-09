package com.quackers29.businesscraft.fabric.init;

import com.quackers29.businesscraft.fabric.block.entity.FabricTownInterfaceEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Fabric block entity registration using direct Fabric API calls.
 */
public class FabricModBlockEntities {
    private static final String MOD_ID = "businesscraft";

    // Store the registered block entity type for later retrieval
    public static BlockEntityType<FabricTownInterfaceEntity> TOWN_INTERFACE_ENTITY_TYPE;
    private static boolean registrationAttempted = false;
    private static boolean registrationSuccessful = false;

    public static Object getTownInterfaceEntityType() {
        return TOWN_INTERFACE_ENTITY_TYPE;
    }


    public static void register() {
        System.out.println("DEBUG: FabricModBlockEntities.register() called");
        if (registrationAttempted && registrationSuccessful) {
            System.out.println("DEBUG: Block entities already registered successfully, skipping");
            return; // Already registered successfully
        }

        registrationAttempted = true;
        System.out.println("DEBUG: FabricModBlockEntities.register() - Attempting block entity registration");

        // Check if blocks are registered first
        net.minecraft.block.Block townInterfaceBlock = Registries.BLOCK.get(new Identifier("businesscraft", "town_interface"));
        if (townInterfaceBlock == null) {
            System.out.println("DEBUG: Town interface block not found in registry - blocks may not be registered yet");
            // Try to register anyway with a placeholder or skip for now
        } else {
            System.out.println("DEBUG: Found town interface block in registry: " + townInterfaceBlock);
        }

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
     * Register block entities using direct Fabric API calls
     */
    private static void registerBlockEntities() {
        try {
            System.out.println("DEBUG: Registering FabricTownInterfaceEntity");

            // Get the town interface block from the registry (registered in FabricModBlocks)
            net.minecraft.block.Block townInterfaceBlock = Registries.BLOCK.get(new Identifier("businesscraft", "town_interface"));

            if (townInterfaceBlock != null) {
                // Create and register the block entity type using Fabric API
                System.out.println("DEBUG: Creating block entity type for town interface block");
                
                // Fabric's factory is called when entities are actually created (not during build)
                // So we can safely reference TOWN_INTERFACE_ENTITY_TYPE in the factory
                // by the time it's called, the type will be set
                TOWN_INTERFACE_ENTITY_TYPE = FabricBlockEntityTypeBuilder.create(
                    (pos, state) -> {
                        // By the time this factory is called, TOWN_INTERFACE_ENTITY_TYPE should be set
                        // But to be safe, we'll use the type parameter if Fabric provides it
                        // For now, use the static field which should be set by the time entities are created
                        return new FabricTownInterfaceEntity(TOWN_INTERFACE_ENTITY_TYPE, pos, state);
                    },
                    townInterfaceBlock
                ).build();

                // Register it in the registry
                Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier("businesscraft", "town_interface_entity"), TOWN_INTERFACE_ENTITY_TYPE);

                System.out.println("DEBUG: Successfully registered FabricTownInterfaceEntity: " + TOWN_INTERFACE_ENTITY_TYPE);
            } else {
                System.out.println("DEBUG: Skipping block entity type registration - town interface block not found");
                // Try to create without specifying the block
                TOWN_INTERFACE_ENTITY_TYPE = FabricBlockEntityTypeBuilder.create(
                    FabricTownInterfaceEntity::new
                    // No blocks specified - this might work for dynamic association
                ).build();

                Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier("businesscraft", "town_interface_entity"), TOWN_INTERFACE_ENTITY_TYPE);
                System.out.println("DEBUG: Registered block entity type without block association: " + TOWN_INTERFACE_ENTITY_TYPE);
            }

        } catch (Exception e) {
            System.err.println("ERROR: Failed to register block entities: " + e.getMessage());
            e.printStackTrace();
        }
    }

}