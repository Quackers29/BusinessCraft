package com.quackers29.businesscraft.fabric.init;

import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
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
            // Create a block that implements EntityBlock to indicate it has a block entity
            class TownInterfaceBlock extends Block implements net.minecraft.world.level.block.EntityBlock {
                public TownInterfaceBlock(Properties settings) {
                    super(settings);
                }

                @Override
                public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(
                        net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
                    System.out.println("DEBUG: newBlockEntity called for block at " + pos);
                    if (FabricModBlockEntities.TOWN_INTERFACE_ENTITY_TYPE != null) {
                        return new TownInterfaceEntity(pos, state);
                    } else {
                        System.err.println("ERROR: Block entity type is null when creating block entity");
                        return null;
                    }
                }

                @Override
                public net.minecraft.world.InteractionResult use(net.minecraft.world.level.block.state.BlockState state,
                        net.minecraft.world.level.Level world,
                        net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player,
                        net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {

                    if (world.isClientSide) {
                        // On client side, just return success to indicate interaction was handled
                        return net.minecraft.world.InteractionResult.SUCCESS;
                    }

                    // On server side, try to open the menu
                    try {
                        // Get the block entity
                        net.minecraft.world.level.block.entity.BlockEntity blockEntity = world.getBlockEntity(pos);
                        if (blockEntity instanceof TownInterfaceEntity townEntity) {
                            System.out.println(
                                    "DEBUG: Found block entity at " + pos + ": " + blockEntity.getClass().getName());

                            // Open the menu directly using PlatformAccess
                            com.quackers29.businesscraft.api.PlatformAccess.getNetwork().openScreen(player, townEntity,
                                    pos);
                            System.out.println("DEBUG: Opened town interface menu via PlatformAccess");
                        } else {
                            System.out.println("DEBUG: No TownInterfaceEntity found at " + pos);
                        }

                        return net.minecraft.world.InteractionResult.SUCCESS;
                    } catch (Exception e) {
                        System.err.println("Error in block onUse: " + e.getMessage());
                        e.printStackTrace();
                        return net.minecraft.world.InteractionResult.FAIL;
                    }
                }
            }

            // Instantiate the block
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

            System.out.println("DEBUG: Town Interface Block with BlockEntity registration completed successfully!");
        } catch (Exception e) {
            System.err.println("ERROR: Failed to register block with block entity: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
