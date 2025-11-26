package com.quackers29.businesscraft.network.packets.ui;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Packet containing town map data sent from server to client.
 * Contains basic information about all towns for map display.
 */
public class TownMapDataResponsePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownMapDataResponsePacket.class);
    private static final int MAX_STRING_LENGTH = 32767;
    
    // Town data for the map
    private final Map<UUID, TownMapInfo> townData = new HashMap<>();
    
    public TownMapDataResponsePacket() {
    }
    
    /**
     * Add town data to the packet
     */
    public void addTown(UUID id, String name, BlockPos position, int population, int touristCount) {
        townData.put(id, new TownMapInfo(id, name, position, population, touristCount));
    }
    
    /**
     * Get all town data
     */
    public Map<UUID, TownMapInfo> getTownData() {
        return townData;
    }
    
    /**
     * Encode the packet data into the buffer
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(townData.size());
        
        for (TownMapInfo town : townData.values()) {
            buf.writeUUID(town.id);
            buf.writeUtf(town.name, MAX_STRING_LENGTH);
            buf.writeBlockPos(town.position);
            buf.writeInt(town.population);
            buf.writeInt(town.touristCount);
        }
    }
    
    /**
     * Decode the packet data from the buffer
     */
    public static TownMapDataResponsePacket decode(FriendlyByteBuf buf) {
        TownMapDataResponsePacket packet = new TownMapDataResponsePacket();
        
        int townCount = buf.readInt();
        for (int i = 0; i < townCount; i++) {
            UUID id = buf.readUUID();
            String name = buf.readUtf(MAX_STRING_LENGTH);
            BlockPos position = buf.readBlockPos();
            int population = buf.readInt();
            int touristCount = buf.readInt();
            
            packet.addTown(id, name, position, population, touristCount);
        }
        
        return packet;
    }
    
    /**
     * Handle the packet when received on the client
     */
    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            try {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                    "Received town map data response with {} towns", townData.size());
                
                // Store the town data in the client-side cache
                ClientTownMapCache.getInstance().updateTownData(townData);
                
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "Updated client town map cache with {} towns", townData.size());
                
                // Try to refresh any open town map modals
                com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
                if (clientHelper != null) {
                    Object currentScreen = clientHelper.getCurrentScreen();
                    if (currentScreen instanceof com.quackers29.businesscraft.ui.modal.specialized.TownMapModal mapModal) {
                        mapModal.refreshFromCache();
                        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
                            "Refreshed open town map modal with new data");
                    }
                }
                
            } catch (Exception e) {
                LOGGER.error("Error handling TownMapDataResponsePacket", e);
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
    
    /**
     * Simple data class for town information
     */
    public static class TownMapInfo {
        public final UUID id;
        public final String name;
        public final BlockPos position;
        public final int population;
        public final int touristCount;
        
        public TownMapInfo(UUID id, String name, BlockPos position, int population, int touristCount) {
            this.id = id;
            this.name = name;
            this.position = position;
            this.population = population;
            this.touristCount = touristCount;
        }
    }
}
