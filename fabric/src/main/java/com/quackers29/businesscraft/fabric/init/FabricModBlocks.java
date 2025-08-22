package com.quackers29.businesscraft.fabric.init;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

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
        
        TOWN_INTERFACE = Registry.register(Registries.BLOCK, 
            new Identifier(MOD_ID, "town_interface"),
            new Block(FabricBlockSettings.create()
                .mapColor(MapColor.STONE_GRAY)
                .strength(3.0f, 3.0f)
                .sounds(BlockSoundGroup.STONE)
                .requiresTool()
            )
        );
        
        TOWN_INTERFACE_ITEM = Registry.register(Registries.ITEM,
            new Identifier(MOD_ID, "town_interface"),
            new BlockItem(TOWN_INTERFACE, new Item.Settings())
        );
    }
}