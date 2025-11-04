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
            if (!(senderObj instanceof ServerPlayer player)) return;
            
            Level level = player.level();
            if (level == null) return;
            
            // First try to handle as a TownInterfaceEntity (original behavior)
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TownInterfaceEntity townInterface) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "Player {} exited TownInterfaceEntity UI at {}", player.getUUID(), pos);
                townInterface.registerPlayerExitUI(player.getUUID());
                
                // Send visualization enable packet to client
                PlatformAccess.getNetworkMessages().sendToPlayer(new PlatformVisualizationPacket(pos), player);
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
