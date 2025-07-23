package com.yourdomain.businesscraft.network.packets.ui;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourdomain.businesscraft.town.Town;
import com.yourdomain.businesscraft.town.TownManager;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.debug.DebugConfig;

/**
 * Packet for requesting town map data from the server.
 * This is sent by the client when opening the town map modal.
 */
public class RequestTownMapDataPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestTownMapDataPacket.class);
    
    // No data needed for the request
    public RequestTownMapDataPacket() {
    }
    
    /**
     * Encode the packet data into the buffer
     */
    public void encode(FriendlyByteBuf buf) {
        // No data to encode
    }
    
    /**
     * Decode the packet data from the buffer
     */
    public static RequestTownMapDataPacket decode(FriendlyByteBuf buf) {
        return new RequestTownMapDataPacket();
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
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Received RequestTownMapDataPacket from null player");
                    return;
                }
                
                ServerLevel serverLevel = (ServerLevel) player.level();
                TownManager townManager = TownManager.get(serverLevel);
                
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "Processing town map data request for player: {}", player.getName().getString());
                
                // Get all towns data
                Map<UUID, Town> allTowns = townManager.getAllTowns();
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "Server has {} towns to send", allTowns.size());
                
                // Create and send response packet
                TownMapDataResponsePacket responsePacket = new TownMapDataResponsePacket();
                for (Map.Entry<UUID, Town> entry : allTowns.entrySet()) {
                    Town town = entry.getValue();
                    responsePacket.addTown(
                        town.getId(),
                        town.getName(),
                        town.getPosition(),
                        town.getPopulation(),
                        town.getTouristCount()
                    );
                }
                
                ModMessages.sendToPlayer(responsePacket, player);
                
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "Sent town map data response with {} towns to player: {}", 
                    allTowns.size(), player.getName().getString());
                
            } catch (Exception e) {
                LOGGER.error("Error handling RequestTownMapDataPacket", e);
            }
        });
        context.setPacketHandled(true);
    }
}