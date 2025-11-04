package com.quackers29.businesscraft.network.packets.storage;

import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.network.packets.storage.PersonalStorageResponsePacket;
import com.quackers29.businesscraft.api.PlatformAccess;
import net.minecraft.world.level.Level;

import java.util.UUID;
import com.quackers29.businesscraft.debug.DebugConfig;

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

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            // Get the player who sent the packet
            Object senderObj = PlatformAccess.getNetwork().getSender(context);
            if (!(senderObj instanceof ServerPlayer player)) return;
            
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
            if (!(blockEntity instanceof TownInterfaceEntity townInterfaceEntity)) {
                LOGGER.warn("No town block entity found at position {} for player {}", pos, player.getName().getString());
                return;
            }
            
            // Get the town manager
            TownManager townManager = TownManager.get(serverLevel);
            
            // Get the town from the town block entity
            UUID townId = townInterfaceEntity.getTownId();
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
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                "Sending personal storage data to player {} for town {}", player.getName().getString(), townId);
            PlatformAccess.getNetworkMessages().sendToPlayer(new PersonalStorageResponsePacket(town.getPersonalStorageItems(playerId)), player);
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
} 
