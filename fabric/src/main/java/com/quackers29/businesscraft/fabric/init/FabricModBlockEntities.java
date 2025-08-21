package com.quackers29.businesscraft.fabric.init;

import com.quackers29.businesscraft.block.entity.FabricTownInterfaceEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Fabric-specific block entity registration for BusinessCraft.
 * Registers the Fabric TownInterfaceEntity implementation.
 */
public class FabricModBlockEntities {
    
    // Registered block entities
    public static final BlockEntityType<FabricTownInterfaceEntity> TOWN_INTERFACE_ENTITY = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        new ResourceLocation("businesscraft", "town_interface"),
        FabricBlockEntityTypeBuilder.create(FabricTownInterfaceEntity::new, FabricModBlocks.TOWN_INTERFACE).build()
    );
    
    /**
     * Initialize all Fabric block entity registrations.
     * This should be called during Fabric mod initialization.
     */
    public static void initialize() {
        // Registration happens during class loading due to static initialization
        // This method exists for explicit initialization if needed
    }
}