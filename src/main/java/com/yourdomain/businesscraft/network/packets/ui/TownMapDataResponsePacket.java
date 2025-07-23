package com.yourdomain.businesscraft.network.packets.ui;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourdomain.businesscraft.debug.DebugConfig;

/**
 * Packet containing town map data sent from server to client.
 * Contains basic information about all towns for map display.
 */
public class TownMapDataResponsePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownMapDataResponsePacket.class);
    
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
            buf.writeUtf(town.name, 32767); // Max string length
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
            String name = buf.readUtf(32767);
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
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            try {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "Received town map data response with {} towns", townData.size());
                
                // Store the town data in the client-side cache
                ClientTownMapCache.getInstance().updateTownData(townData);
                
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "Updated client town map cache with {} towns", townData.size());
                
                // Try to refresh any open town map modals
                if (net.minecraft.client.Minecraft.getInstance().screen instanceof 
                    com.yourdomain.businesscraft.ui.modal.specialized.TownMapModal) {
                    ((com.yourdomain.businesscraft.ui.modal.specialized.TownMapModal) 
                     net.minecraft.client.Minecraft.getInstance().screen).refreshFromCache();
                    DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
                        "Refreshed open town map modal with new data");
                }
                
            } catch (Exception e) {
                LOGGER.error("Error handling TownMapDataResponsePacket", e);
            }
        });
        context.setPacketHandled(true);
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