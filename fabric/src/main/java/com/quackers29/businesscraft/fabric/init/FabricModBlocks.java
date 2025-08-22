package com.quackers29.businesscraft.fabric.init;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Fabric-specific block registration for BusinessCraft.
 * Ready for unified architecture - will reference common module blocks when migration completes.
 */
public class FabricModBlocks {
    
    private static final String MOD_ID = "businesscraft";
    
    // Placeholder for unified architecture migration
    // Will be replaced with reference to common module TownInterfaceBlock
    public static Block TOWN_INTERFACE;
    public static Item TOWN_INTERFACE_ITEM;
    
    /**
     * Initialize all Fabric block registrations.
     * Currently creates placeholder blocks - will use unified architecture classes after migration.
     */
    public static void initialize() {
        // TODO: Replace with reference to common module TownInterfaceBlock after unified architecture migration
        // For now, create minimal stub to enable compilation
        
        // Quick fix: Create basic interactable block to prevent crash
        TOWN_INTERFACE = Registry.register(Registries.BLOCK, 
            new Identifier(MOD_ID, "town_interface"),
            new Block(FabricBlockSettings.create()
                .mapColor(MapColor.STONE_GRAY)
                .strength(3.0f, 3.0f)
                .sounds(BlockSoundGroup.STONE)
                .requiresTool()
            ) {
                @Override
                public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
                    if (!world.isClient) {
                        player.sendMessage(net.minecraft.text.Text.literal("Fabric Town Interface - Menu system not yet implemented"), false);
                    }
                    return ActionResult.SUCCESS;
                }
            }
        );
        
        TOWN_INTERFACE_ITEM = Registry.register(Registries.ITEM,
            new Identifier(MOD_ID, "town_interface"),
            new BlockItem(TOWN_INTERFACE, new Item.Settings())
        );
    }
}