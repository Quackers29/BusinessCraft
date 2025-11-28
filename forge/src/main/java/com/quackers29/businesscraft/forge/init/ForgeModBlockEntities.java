package com.quackers29.businesscraft.forge.init;

import com.quackers29.businesscraft.forge.block.entity.ForgeTownInterfaceEntity;
import com.quackers29.businesscraft.forge.platform.ForgeRegistryHelper;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

/**
 * Forge-specific block entity registrations
 */
public class ForgeModBlockEntities {
        public static RegistryObject<BlockEntityType<ForgeTownInterfaceEntity>> TOWN_INTERFACE_ENTITY = ForgeRegistryHelper.BLOCK_ENTITY_TYPES
                        .register("town_interface",
                                        () -> BlockEntityType.Builder.of(ForgeTownInterfaceEntity::new,
                                                        com.quackers29.businesscraft.init.CommonModBlocks.TOWN_INTERFACE_BLOCK
                                                                        .get())
                                                        .build(null));

        public static void register() {
                // Block entity registration is handled by RegistryObject above
        }
}
