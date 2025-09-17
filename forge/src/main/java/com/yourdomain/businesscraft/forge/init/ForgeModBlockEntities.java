package com.yourdomain.businesscraft.forge.init;

import com.yourdomain.businesscraft.block.entity.TownInterfaceEntity;
import com.yourdomain.businesscraft.forge.platform.ForgeRegistryHelper;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Forge-specific block entity registrations
 */
public class ForgeModBlockEntities {
    public static final BlockEntityType<TownInterfaceEntity> TOWN_INTERFACE_ENTITY =
        BlockEntityType.Builder.of(TownInterfaceEntity::new, ForgeModBlocks.TOWN_INTERFACE_BLOCK).build(null);

    public static void register() {
        ForgeRegistryHelper registry = (ForgeRegistryHelper) com.yourdomain.businesscraft.forge.BusinessCraftForge.REGISTRY;
        registry.registerBlockEntityType("town_interface", TOWN_INTERFACE_ENTITY);
    }
}
