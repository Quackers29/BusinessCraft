package com.yourdomain.businesscraft.init;

import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.block.entity.TownInterfaceEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
        public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
                        .create(ForgeRegistries.BLOCK_ENTITY_TYPES, BusinessCraft.MOD_ID);

        public static final RegistryObject<BlockEntityType<TownInterfaceEntity>> TOWN_INTERFACE_ENTITY = BLOCK_ENTITIES
                        .register("town_interface",
                                        () -> BlockEntityType.Builder.of(TownInterfaceEntity::new,
                                                        ModBlocks.TOWN_INTERFACE.get()).build(null));
} 