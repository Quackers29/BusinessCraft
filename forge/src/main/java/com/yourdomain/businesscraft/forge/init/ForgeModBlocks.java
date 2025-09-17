package com.yourdomain.businesscraft.forge.init;

import com.yourdomain.businesscraft.block.TownInterfaceBlock;
import com.yourdomain.businesscraft.forge.platform.ForgeRegistryHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

/**
 * Forge-specific block registrations
 */
public class ForgeModBlocks {
    // Town Interface Block - our primary town management block
    public static final Block TOWN_INTERFACE_BLOCK = new TownInterfaceBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .strength(3.0f, 3.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
    );

    public static void register() {
        ForgeRegistryHelper registry = (ForgeRegistryHelper) com.yourdomain.businesscraft.forge.BusinessCraftForge.REGISTRY;

        registry.registerBlock("town_interface", TOWN_INTERFACE_BLOCK);
    }
}
