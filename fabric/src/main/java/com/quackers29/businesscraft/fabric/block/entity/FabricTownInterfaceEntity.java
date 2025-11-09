package com.quackers29.businesscraft.fabric.block.entity;

import com.quackers29.businesscraft.fabric.init.FabricModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric-specific implementation of TownInterfaceEntity.
 * Since the common TownInterfaceEntity extends MenuProvider (Forge-specific),
 * we need a Fabric-compatible implementation that replicates the functionality.
 *
 * This class provides the same business logic as the common TownInterfaceEntity
 * but without Forge-specific dependencies.
 */
public class FabricTownInterfaceEntity extends BlockEntity {
    // Constructor that accepts type - used by FabricBlockEntityTypeBuilder factory
    public FabricTownInterfaceEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    // Constructor that gets type from FabricModBlockEntities - used for manual creation
    public FabricTownInterfaceEntity(BlockPos pos, BlockState state) {
        this(FabricModBlockEntities.TOWN_INTERFACE_ENTITY_TYPE, pos, state);
    }

    // TODO: Implement the full TownInterfaceEntity functionality for Fabric
    // For now, this is a minimal implementation to get the block entity working
}
