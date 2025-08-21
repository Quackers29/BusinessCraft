package com.quackers29.businesscraft.fabric.init;

import com.quackers29.businesscraft.block.TownInterfaceBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * Fabric-specific block registration for BusinessCraft.
 * Registers the Fabric TownInterfaceBlock implementation.
 */
public class FabricModBlocks {
    
    // Registered blocks
    public static final Block TOWN_INTERFACE = Registry.register(
        BuiltInRegistries.BLOCK,
        new ResourceLocation("businesscraft", "town_interface"),
        new TownInterfaceBlock(FabricBlockSettings.create()
            .mapColor(MapColor.STONE)
            .strength(3.0f, 3.0f)
            .sounds(SoundType.STONE)
            .requiresTool()
        )
    );
    
    // Registered block items
    public static final Item TOWN_INTERFACE_ITEM = Registry.register(
        BuiltInRegistries.ITEM,
        new ResourceLocation("businesscraft", "town_interface"),
        new BlockItem(TOWN_INTERFACE, new Item.Properties())
    );
    
    /**
     * Initialize all Fabric block registrations.
     * This should be called during Fabric mod initialization.
     */
    public static void initialize() {
        // Registration happens during class loading due to static initialization
        // This method exists for explicit initialization if needed
    }
}