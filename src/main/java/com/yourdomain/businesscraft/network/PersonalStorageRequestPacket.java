package com.yourdomain.businesscraft.network;

import com.yourdomain.businesscraft.town.Town;
import com.yourdomain.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import net.minecraft.world.level.Level;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet to request personal storage data from the server.
 * This is sent when the client needs to refresh personal storage display.
 */
public class PersonalStorageRequestPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersonalStorageRequestPacket.class);
    private final UUID playerId; // Player requesting their storage data

    public PersonalStorageRequestPacket(BlockPos pos, UUID playerId) {
        super(pos);
        this.playerId = playerId;
    }

    public PersonalStorageRequestPacket(FriendlyByteBuf buf) {
        super(buf);
        this.playerId = buf.readUUID();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        super.toBytes(buf);
        buf.writeUUID(playerId);
    }
    
    /**
     * Static encode method needed by ModMessages registration
     */
    public static void encode(PersonalStorageRequestPacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }
    
    /**
     * Static decode method needed by ModMessages registration
     */
    public static PersonalStorageRequestPacket decode(FriendlyByteBuf buf) {
        return new PersonalStorageRequestPacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Get the player who sent the packet
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            
            // Verify player ID matches sender (security check)
            if (!player.getUUID().equals(playerId)) {
                LOGGER.warn("Player {} attempted to request personal storage of another player {}!", 
                    player.getName().getString(), playerId);
                return;
            }
            
            // If position is null, we can't process the request
            if (pos == null) {
                LOGGER.warn("Received null position in personal storage request from player {}", player.getName().getString());
                return;
            }
            
            // Get the level
            Level level = player.level();
            if (!(level instanceof ServerLevel serverLevel)) {
                LOGGER.warn("Player level is not a ServerLevel");
                return;
            }
            
            // Get the town block entity at the position
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof TownBlockEntity townBlockEntity)) {
                LOGGER.warn("No town block entity found at position {} for player {}", pos, player.getName().getString());
                return;
            }
            
            // Get the town manager
            TownManager townManager = TownManager.get(serverLevel);
            
            // Get the town from the town block entity
            UUID townId = townBlockEntity.getTownId();
            if (townId == null) {
                LOGGER.warn("Town block at {} has no town ID", pos);
                return;
            }
            
            // Get the town
            Town town = townManager.getTown(townId);
            if (town == null) {
                LOGGER.warn("No town found with ID {} for personal storage request", townId);
                return;
            }
            
            // Send a response with the player's personal storage data
            LOGGER.debug("Sending personal storage data to player {} for town {}", player.getName().getString(), townId);
            ModMessages.sendToPlayer(new PersonalStorageResponsePacket(town.getPersonalStorageItems(playerId)), player);
        });
        
        ctx.get().setPacketHandled(true);
    }
} 