package com.yourdomain.businesscraft.network.packets.ui;

import com.yourdomain.businesscraft.block.entity.TownInterfaceEntity;
import com.yourdomain.businesscraft.debug.DebugConfig;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.yourdomain.businesscraft.town.Town;
import com.yourdomain.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet sent from client to server to request current boundary data
 * for boundary visualization updates during the active visualization window.
 */
public class BoundarySyncRequestPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(BoundarySyncRequestPacket.class);

    public BoundarySyncRequestPacket(BlockPos pos) {
        super(pos);
    }

    public BoundarySyncRequestPacket(FriendlyByteBuf buf) {
        super(buf);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        super.toBytes(buf);
    }
    
    /**
     * Static encode method needed by ModMessages registration
     */
    public static void encode(BoundarySyncRequestPacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }
    
    /**
     * Static decode method needed by ModMessages registration
     */
    public static BoundarySyncRequestPacket decode(FriendlyByteBuf buf) {
        return new BoundarySyncRequestPacket(buf);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Server-side handling
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            Level level = player.level();
            if (!(level instanceof ServerLevel serverLevel)) return;
            
            // Get town population and send response
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TownInterfaceEntity townInterface) {
                UUID townId = townInterface.getTownId();
                if (townId != null) {
                    TownManager townManager = TownManager.get(serverLevel);
                    Town town = townManager.getTown(townId);
                    if (town != null) {
                        // Use town's boundary calculation method (single source of truth)
                        int currentBoundaryRadius = town.getBoundaryRadius();
                        
                        // Send boundary update back to client
                        ModMessages.sendToPlayer(new BoundarySyncResponsePacket(pos, currentBoundaryRadius), player);
                        
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                            "Boundary sync request for town at {}: boundary radius={}", pos, currentBoundaryRadius);
                    }
                }
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}