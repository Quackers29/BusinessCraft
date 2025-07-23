package com.yourdomain.businesscraft.network.packets.ui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourdomain.businesscraft.debug.DebugConfig;
import com.yourdomain.businesscraft.town.TownManager;
import com.yourdomain.businesscraft.town.Town;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.platform.Platform;

import java.util.UUID;
import java.util.List;
import java.util.function.Supplier;

/**
 * Packet sent from client to server to request detailed platform data for a specific town.
 * This is sent when the user clicks on a town in the map view.
 */
public class RequestTownPlatformDataPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestTownPlatformDataPacket.class);
    
    private final UUID townId;
    
    public RequestTownPlatformDataPacket(UUID townId) {
        this.townId = townId;
    }
    
    /**
     * Encode the packet data into the buffer
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(townId);
    }
    

    
    /**
     * Decode the packet data from the buffer
     */
    public static RequestTownPlatformDataPacket decode(FriendlyByteBuf buf) {
        UUID townId = buf.readUUID();
        return new RequestTownPlatformDataPacket(townId);
    }
    
    /**
     * Handle the packet when received on the server
     */
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            try {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Received RequestTownPlatformDataPacket with null sender");
                    return;
                }
                
                ServerLevel level = player.serverLevel();
                TownManager townManager = TownManager.get(level);
                Town town = townManager.getTown(townId);
                
                if (town == null) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                        "Town not found for platform data request: {}", townId);
                    return;
                }
                
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "Processing platform data request for town: {} ({})", town.getName(), townId);
                
                // Find the town block entity to get platform data
                if (level.getBlockEntity(town.getPosition()) instanceof TownBlockEntity townEntity) {
                    List<Platform> platforms = townEntity.getPlatforms();
                    
                                    // Create response packet with platform data and town info
                TownPlatformDataResponsePacket response = new TownPlatformDataResponsePacket(townId);
                
                // Add current town information
                response.setTownInfo(town.getName(), town.getPopulation(), town.getTouristCount());
                
                for (Platform platform : platforms) {
                    if (platform.isComplete()) { // Only send platforms with both start and end positions
                        response.addPlatform(
                            platform.getId(),
                            platform.getName(),
                            platform.isEnabled(),
                            platform.getStartPos(),
                            platform.getEndPos(),
                            platform.getEnabledDestinations()
                        );
                    }
                }
                    
                    // Send response back to client
                    ModMessages.sendToPlayer(response, player);
                    
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                        "Sent platform data response for town {} with {} platforms", 
                        town.getName(), platforms.size());
                } else {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                        "No town block entity found at position {} for town {}", 
                        town.getPosition(), town.getName());
                }
                
            } catch (Exception e) {
                LOGGER.error("Error handling RequestTownPlatformDataPacket", e);
            }
        });
        context.setPacketHandled(true);
    }
} 