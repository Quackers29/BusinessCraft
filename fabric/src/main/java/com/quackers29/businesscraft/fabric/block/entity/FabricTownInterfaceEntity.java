package com.quackers29.businesscraft.fabric.block.entity;

import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric-specific extension of TownInterfaceEntity that ensures proper syncing.
 * On Fabric, we need to explicitly send the packet to nearby players.
 */
public class FabricTownInterfaceEntity extends TownInterfaceEntity {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricTownInterfaceEntity.class);

    public FabricTownInterfaceEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public void setChanged() {
        super.setChanged();

        // Note: Fabric's sendBlockUpdated() DOES send packets to nearby players
        // No manual packet sending needed (tested and confirmed working)
    }
}
