package com.quackers29.businesscraft.init;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.PlatformInterfaceBlock;
import com.quackers29.businesscraft.block.TownInterfaceBlock;
import com.quackers29.businesscraft.platform.PlatformLamp;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;

import java.util.function.Supplier;

public class CommonModBlocks {
    public static Supplier<Block> TOWN_INTERFACE_BLOCK;
    public static Supplier<Block> PLATFORM_INTERFACE_BLOCK;

    public static void register() {
        TOWN_INTERFACE_BLOCK = PlatformAccess.getRegistry().registerBlock("town_interface",
                () -> new TownInterfaceBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.STONE)
                        .strength(50.0f, 1200.0f) // Obsidian-tier baseline when craftableTownInterface = true
                        .sound(SoundType.STONE)
                        .requiresCorrectToolForDrops()));

        PlatformAccess.getRegistry().registerBlockItem("town_interface", TOWN_INTERFACE_BLOCK::get);

        PLATFORM_INTERFACE_BLOCK = PlatformAccess.getRegistry().registerBlock("platform_interface",
                () -> new PlatformInterfaceBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(3.5f, 6.0f)
                        .sound(SoundType.METAL)
                        .lightLevel(state -> PlatformLamp.emission(state.getValue(PlatformInterfaceBlock.LIT)))
                        .requiresCorrectToolForDrops()));

        PlatformAccess.getRegistry().registerBlockItem("platform_interface", PLATFORM_INTERFACE_BLOCK::get);
    }
}
