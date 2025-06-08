package com.yourdomain.businesscraft.network.packets.platform;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.event.PlatformPathHandler;

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
    
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            Level level = player.level();
            BlockEntity be = level.getBlockEntity(pos);
            
            if (be instanceof TownBlockEntity townBlock) {
                LOGGER.debug("Player {} is setting platform {} path creation mode to {} at {}", 
                    player.getName().getString(), platformId, mode, pos);
                
                townBlock.setPlatformCreationMode(mode, platformId);
                
                // Update the platform path handler state
                if (mode) {
                    PlatformPathHandler.setActivePlatform(pos, platformId);
                } else {
                    PlatformPathHandler.clearActivePlatform();
                }
                
                LOGGER.debug("Successfully set platform {} path creation mode to {} at {}", 
                    platformId, mode, pos);
            }
        });
        
        return true;
    }
} 