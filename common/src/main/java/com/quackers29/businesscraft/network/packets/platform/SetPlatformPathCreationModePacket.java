package com.quackers29.businesscraft.network.packets.platform;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.event.PlatformPathHandler;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Packet sent from client to server to set platform path creation mode
 */
public class SetPlatformPathCreationModePacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final BlockPos pos;
    private final UUID platformId;
    private final boolean mode;
    
    public SetPlatformPathCreationModePacket(BlockPos pos, UUID platformId, boolean mode) {
        this.pos = pos;
        this.platformId = platformId;
        this.mode = mode;
    }
    
    public SetPlatformPathCreationModePacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.platformId = buf.readUUID();
        this.mode = buf.readBoolean();
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUUID(platformId);
        buf.writeBoolean(mode);
    }

    /**
     * Serialize packet data for Fabric networking
     */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUUID(platformId);
        buf.writeBoolean(mode);
    }

    // Static decode method for Forge network registration
    public static SetPlatformPathCreationModePacket decode(FriendlyByteBuf buf) {
        return new SetPlatformPathCreationModePacket(buf);
    }
    
    public boolean handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            Object senderObj = PlatformAccess.getNetwork().getSender(context);
            if (!(senderObj instanceof ServerPlayer player)) return;
            
            Level level = player.level();
            BlockEntity be = level.getBlockEntity(pos);
            
            if (be instanceof TownInterfaceEntity townInterface) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "Player {} is setting platform {} path creation mode to {} at {}", 
                    player.getName().getString(), platformId, mode, pos);
                
                townInterface.setPlatformCreationMode(mode, platformId);
                
                // Update the platform path handler state
                if (mode) {
                    PlatformPathHandler.setActivePlatform(pos, platformId);
                } else {
                    PlatformPathHandler.clearActivePlatform();
                }
                
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "Successfully set platform {} path creation mode to {} at {}", 
                    platformId, mode, pos);
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
        return true;
    }
} 
