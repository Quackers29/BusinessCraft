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

        /*
        // COMMENTED OUT: Rely on MC auto-sync via sendBlockUpdated (Forge behavior)
        // In Fabric, we need to explicitly send the update packet to nearby players
        if (level != null && !level.isClientSide() && level instanceof ServerLevel serverLevel) {
            LOGGER.info("[FABRIC] Manually sending update packet to players");

            // Get the update packet
            ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(this);

            if (packet != null) {
                // Send to all players tracking this chunk
                for (ServerPlayer player : serverLevel.players()) {
                    // Check if player is close enough to receive the packet
                    if (player.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) < 4096) { // 64 blocks squared
                        player.connection.send(packet);
                        LOGGER.info("[FABRIC] Sent update packet to player: {}", player.getName().getString());
                    }
                }
            }
        }
        */
    }
}
