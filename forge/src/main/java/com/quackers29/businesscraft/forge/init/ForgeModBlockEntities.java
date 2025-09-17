package com.quackers29.businesscraft.forge.init;

import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.forge.platform.ForgeRegistryHelper;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

/**
 * Forge-specific block entity registrations
 */
public class ForgeModBlockEntities {
    public static RegistryObject<BlockEntityType<TownInterfaceEntity>> TOWN_INTERFACE_ENTITY = ForgeRegistryHelper.BLOCK_ENTITY_TYPES.register("town_interface",
            () -> BlockEntityType.Builder.of(TownInterfaceEntity::new, ForgeModBlocks.TOWN_INTERFACE_BLOCK.get()).build(null));

    public static void register() {
        // Block entity registration is handled by RegistryObject above
    }
}
