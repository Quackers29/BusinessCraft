package com.quackers29.businesscraft.network.packets.ui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.quackers29.businesscraft.api.PlatformAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.block.TownInterfaceBlock;
import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Packet sent when a player exits a Town UI
 * Used to trigger extended particle displays for platforms
 * Works with both TownBlockEntity and TownInterfaceBlock
 */
public class PlayerExitUIPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerExitUIPacket.class);

    public PlayerExitUIPacket(BlockPos pos) {
        super(pos);
    }

    public PlayerExitUIPacket(FriendlyByteBuf buf) {
        super(buf);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        super.toBytes(buf);
    }

    /**
     * Static encode method needed by ModMessages registration
     */
    public static void encode(PlayerExitUIPacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }

    /**
     * Static decode method needed by ModMessages registration
     */
    public static PlayerExitUIPacket decode(FriendlyByteBuf buf) {
        return new PlayerExitUIPacket(buf);
    }

    public boolean handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            // Get player and level
            Object senderObj = PlatformAccess.getNetwork().getSender(context);
            if (!(senderObj instanceof ServerPlayer player))
                return;

            Level level = player.level();
            if (level == null)
                return;

            // First try to handle as a TownInterfaceEntity (original behavior)
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TownInterfaceEntity townInterface) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                        "Player {} exited TownInterfaceEntity UI at {}", player.getUUID(), pos);
                townInterface.registerPlayerExitUI(player.getUUID());

                // Send visualization enable packet to client
                PlatformAccess.getNetworkMessages().sendToPlayer(new PlatformVisualizationPacket(pos), player);

                // Also send platform data to the client so it can render them
                java.util.UUID townId = townInterface.getTownId();
                if (townId != null && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    com.quackers29.businesscraft.town.Town town = com.quackers29.businesscraft.town.TownManager
                            .get(serverLevel).getTown(townId);
                    if (town != null) {
                        TownPlatformDataResponsePacket platformPacket = new TownPlatformDataResponsePacket(townId);

                        // Add all platforms from the town interface entity
                        for (com.quackers29.businesscraft.platform.Platform platform : townInterface.getPlatforms()) {
                            platformPacket.addPlatform(
                                    platform.getId(),
                                    platform.getName(),
                                    platform.isEnabled(),
                                    platform.getStartPos(),
                                    platform.getEndPos(),
                                    platform.getEnabledDestinations());
                        }

                        // Set town info
                        platformPacket.setTownInfo(town.getName(), town.getPopulation(), town.getTouristCount(),
                                town.getBoundaryRadius());

                        PlatformAccess.getNetworkMessages().sendToPlayer(platformPacket, player);

                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                                "Sent {} platforms to client for visualization", townInterface.getPlatforms().size());
                    }
                }
            }
            // Then check if it's a TownInterfaceBlock
            else {
                BlockState blockState = level.getBlockState(pos);
                if (blockState.getBlock() instanceof TownInterfaceBlock townInterfaceBlock) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                            "Player {} exited TownInterfaceBlock UI at {}", player.getUUID(), pos);
                    townInterfaceBlock.registerPlayerExitUI(player.getUUID(), level, pos);

                    // Send visualization enable packet to client
                    PlatformAccess.getNetworkMessages().sendToPlayer(new PlatformVisualizationPacket(pos), player);
                }
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
        return true;
    }
}
