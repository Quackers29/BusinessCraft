package com.quackers29.businesscraft.init;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.TownInterfaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;

import java.util.function.Supplier;

public class CommonModBlocks {
    public static Supplier<Block> TOWN_INTERFACE_BLOCK;

    public static void register() {
        TOWN_INTERFACE_BLOCK = PlatformAccess.getRegistry().registerBlock("town_interface",
                () -> new TownInterfaceBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.STONE)
                        .strength(50.0f, 1200.0f) // Obsidian-tier baseline when craftableTownInterface = true
                        .sound(SoundType.STONE)
                        .requiresCorrectToolForDrops()));

        PlatformAccess.getRegistry().registerBlockItem("town_interface", TOWN_INTERFACE_BLOCK::get);
    }
}
