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

/**
 * Packet sent when a player exits the Town Block UI
 * Used to trigger extended particle displays
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
            handlePacket(context, (player, townBlock) -> {
                LOGGER.debug("Player {} exited Town UI at {}", player.getUUID(), pos);
                townBlock.registerPlayerExitUI(player.getUUID());
            });
        });
        context.setPacketHandled(true);
        return true;
    }
} 