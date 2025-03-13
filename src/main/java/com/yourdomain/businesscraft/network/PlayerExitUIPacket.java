package com.yourdomain.businesscraft.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.block.TownInterfaceBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Get player and level
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            Level level = player.level();
            if (level == null) return;
            
            // First try to handle as a TownBlockEntity (original behavior)
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TownBlockEntity townBlock) {
                LOGGER.debug("Player {} exited TownBlockEntity UI at {}", player.getUUID(), pos);
                townBlock.registerPlayerExitUI(player.getUUID());
            } 
            // Then check if it's a TownInterfaceBlock
            else {
                BlockState blockState = level.getBlockState(pos);
                if (blockState.getBlock() instanceof TownInterfaceBlock townInterfaceBlock) {
                    LOGGER.debug("Player {} exited TownInterfaceBlock UI at {}", player.getUUID(), pos);
                    townInterfaceBlock.registerPlayerExitUI(player.getUUID(), level, pos);
                }
            }
        });
        context.setPacketHandled(true);
        return true;
    }
} 