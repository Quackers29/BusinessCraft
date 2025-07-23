package com.yourdomain.businesscraft.network.packets.ui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourdomain.businesscraft.debug.DebugConfig;

import java.util.UUID;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Supplier;

/**
 * Packet sent from server to client containing detailed platform data for a specific town.
 * This includes platform positions, paths, and enabled destinations.
 */
public class TownPlatformDataResponsePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownPlatformDataResponsePacket.class);
    private static final int MAX_STRING_LENGTH = 32767;
    
    private final UUID townId;
    private final Map<UUID, PlatformInfo> platforms = new HashMap<>();
    private TownInfo townInfo;
    
    public TownPlatformDataResponsePacket(UUID townId) {
        this.townId = townId;
    }
    
    /**
     * Add platform data to the packet
     */
    public void addPlatform(UUID platformId, String name, boolean enabled, 
                           BlockPos startPos, BlockPos endPos, Set<UUID> enabledDestinations) {
        platforms.put(platformId, new PlatformInfo(platformId, name, enabled, startPos, endPos, enabledDestinations));
    }
    
    /**
     * Set town information
     */
    public void setTownInfo(String name, int population, int touristCount) {
        this.townInfo = new TownInfo(name, population, touristCount);
    }
    
    /**
     * Get the town ID this data is for
     */
    public UUID getTownId() {
        return townId;
    }
    
    /**
     * Get all platform data
     */
    public Map<UUID, PlatformInfo> getPlatforms() {
        return platforms;
    }
    
    /**
     * Get town information
     */
    public TownInfo getTownInfo() {
        return townInfo;
    }
    
    /**
     * Encode the packet data into the buffer
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(townId);
        
        // Write town info
        buf.writeBoolean(townInfo != null);
        if (townInfo != null) {
            buf.writeUtf(townInfo.name, MAX_STRING_LENGTH);
            buf.writeInt(townInfo.population);
            buf.writeInt(townInfo.touristCount);
        }
        
        buf.writeInt(platforms.size());
        
        for (PlatformInfo platform : platforms.values()) {
            buf.writeUUID(platform.id);
            buf.writeUtf(platform.name, MAX_STRING_LENGTH);
            buf.writeBoolean(platform.enabled);
            buf.writeBlockPos(platform.startPos);
            buf.writeBlockPos(platform.endPos);
            
            // Write enabled destinations
            buf.writeInt(platform.enabledDestinations.size());
            for (UUID destId : platform.enabledDestinations) {
                buf.writeUUID(destId);
            }
        }
    }
    
    /**
     * Decode the packet data from the buffer
     */
    public static TownPlatformDataResponsePacket decode(FriendlyByteBuf buf) {
        UUID townId = buf.readUUID();
        TownPlatformDataResponsePacket packet = new TownPlatformDataResponsePacket(townId);
        
        // Read town info
        boolean hasTownInfo = buf.readBoolean();
        if (hasTownInfo) {
            String townName = buf.readUtf(MAX_STRING_LENGTH);
            int population = buf.readInt();
            int touristCount = buf.readInt();
            packet.setTownInfo(townName, population, touristCount);
        }
        
        int platformCount = buf.readInt();
        for (int i = 0; i < platformCount; i++) {
            UUID platformId = buf.readUUID();
            String name = buf.readUtf(MAX_STRING_LENGTH);
            boolean enabled = buf.readBoolean();
            BlockPos startPos = buf.readBlockPos();
            BlockPos endPos = buf.readBlockPos();
            
            // Read enabled destinations
            int destCount = buf.readInt();
            Set<UUID> enabledDestinations = new java.util.HashSet<>();
            for (int j = 0; j < destCount; j++) {
                enabledDestinations.add(buf.readUUID());
            }
            
            packet.addPlatform(platformId, name, enabled, startPos, endPos, enabledDestinations);
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
                    "Received platform data response for town {} with {} platforms", 
                    townId, platforms.size());
                
                // Update the client-side town map cache with platform data
                ClientTownMapCache.getInstance().updateTownPlatformData(townId, platforms);
                
                // Also update town information in cache if available
                if (townInfo != null) {
                    ClientTownMapCache.getInstance().updateTownInfo(townId, townInfo.name, townInfo.population, townInfo.touristCount);
                }
                
                // Try to refresh any open town map modals
                var currentScreen = net.minecraft.client.Minecraft.getInstance().screen;
                if (currentScreen instanceof com.yourdomain.businesscraft.ui.modal.specialized.TownMapModal mapModal) {
                    mapModal.refreshPlatformData(townId, platforms);
                    
                    // Also update town info if available
                    if (townInfo != null) {
                        mapModal.refreshTownData(townId, townInfo);
                    }
                    
                    DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
                        "Refreshed open town map modal with platform and town data for town {}", townId);
                }
                
            } catch (Exception e) {
                LOGGER.error("Error handling TownPlatformDataResponsePacket", e);
            }
        });
        context.setPacketHandled(true);
    }
    
    /**
     * Data class for platform information
     */
    public static class PlatformInfo {
        public final UUID id;
        public final String name;
        public final boolean enabled;
        public final BlockPos startPos;
        public final BlockPos endPos;
        public final Set<UUID> enabledDestinations;
        
        public PlatformInfo(UUID id, String name, boolean enabled, BlockPos startPos, BlockPos endPos, Set<UUID> enabledDestinations) {
            this.id = id;
            this.name = name;
            this.enabled = enabled;
            this.startPos = startPos;
            this.endPos = endPos;
            this.enabledDestinations = new java.util.HashSet<>(enabledDestinations);
        }
    }
    
    /**
     * Data class for town information
     */
    public static class TownInfo {
        public final String name;
        public final int population;
        public final int touristCount;
        
        public TownInfo(String name, int population, int touristCount) {
            this.name = name;
            this.population = population;
            this.touristCount = touristCount;
        }
    }
} 