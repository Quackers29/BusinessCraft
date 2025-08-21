package com.quackers29.businesscraft.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Placeholder/Base type for TownInterfaceEntity in common module.
 * This class provides the minimum required structure to allow
 * common module code to compile with type references.
 * 
 * This version can be instantiated but will use null for BlockEntityType.
 * Platform modules should override this to provide the correct BlockEntityType.
 */
public class TownInterfaceEntityStub extends BlockEntity {
    
    /**
     * Constructor required by Minecraft BlockEntity system.
     * This constructor signature must match what platform modules use.
     */
    public TownInterfaceEntityStub(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }
    
    // Constructor overload that platform modules use for instantiation
    public TownInterfaceEntityStub(BlockPos blockPos, BlockState blockState) {
        // Call parent constructor with null - this is a fallback implementation
        // Platform modules should provide proper BlockEntityType through their own constructors
        super(null, blockPos, blockState);
    }
}