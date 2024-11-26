package com.yourdomain.businesscraft.block.entity;

import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.registry.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(ForgeRegistries.BLOCK_ENTITY_TYPES, BusinessCraft.MOD_ID);

    public static final RegistryObject<BlockEntityType<CompanyBlockEntity>> COMPANY_BLOCK_ENTITY = BLOCK_ENTITIES
            .register("company_block",
                    () -> BlockEntityType.Builder.of(CompanyBlockEntity::new,
                            ModBlocks.COMPANY_BLOCK.get()).build(null));
}