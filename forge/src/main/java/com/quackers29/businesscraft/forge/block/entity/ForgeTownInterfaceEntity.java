package com.quackers29.businesscraft.forge.block.entity;

import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Forge-specific extension of TownInterfaceEntity that properly overrides
 * getCapability.
 * This bridges the platform-agnostic getCapability method to Forge's capability
 * system.
 */
public class ForgeTownInterfaceEntity extends TownInterfaceEntity {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ForgeTownInterfaceEntity.class);

    public ForgeTownInterfaceEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        // Call the platform-agnostic version and cast the result
        Object result = super.getCapabilityCommon(cap, side);
        if (result instanceof LazyOptional) {
            @SuppressWarnings("unchecked")
            LazyOptional<T> castResult = (LazyOptional<T>) result;
            return castResult;
        }
        return LazyOptional.empty();
    }
}
