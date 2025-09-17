package com.yourdomain.businesscraft.client;

// BusinessCraft moved to platform-specific module
import com.yourdomain.businesscraft.api.PlatformAccess;
import com.yourdomain.businesscraft.town.Town;
import com.yourdomain.businesscraft.town.TownManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Handles network communication for the town debug overlay
 */
public class TownDebugNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static SimpleChannel INSTANCE;
    
    private static int packetId = 0;
    private static int nextId() {
        return packetId++;
    }
    
    public static void register() {
        // Create a separate network channel for debug packets
        INSTANCE = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(PlatformAccess.getPlatform().getModId(), "town_debug"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );
        
        // Register the request packet (client -> server)
        INSTANCE.messageBuilder(RequestTownDataPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(buf -> new RequestTownDataPacket())
                .encoder((msg, buf) -> {})
                .consumerMainThread(RequestTownDataPacket::handle)
                .add();
                
        // Register the response packet (server -> client)
        INSTANCE.messageBuilder(TownDataResponsePacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(TownDataResponsePacket::new)
                .encoder(TownDataResponsePacket::encode)
                .consumerMainThread(TownDataResponsePacket::handle)
                .add();
    }
    
    /**
     * Client-side method to request town data from the server
     */
    @OnlyIn(Dist.CLIENT)
    public static void requestTownData() {
        INSTANCE.sendToServer(new RequestTownDataPacket());
    }
    
    /**
     * Request packet (client -> server)
     */
    public static class RequestTownDataPacket {
        public RequestTownDataPacket() {}
        
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    List<TownDebugOverlay.TownDebugData> townDataList = new ArrayList<>();
                    
                    // Gather data from all loaded levels
                    for (ServerLevel level : player.getServer().getAllLevels()) {
                        TownManager townManager = TownManager.get(level);
                        Map<UUID, Town> towns = townManager.getAllTowns();
                        
                        // Convert Town objects to TownDebugData
                        for (Map.Entry<UUID, Town> entry : towns.entrySet()) {
                            Town town = entry.getValue();
                            townDataList.add(new TownDebugOverlay.TownDebugData(
                                town.getId().toString(),
                                town.getName(),
                                town.getPosition().toShortString(),
                                town.getPopulation(),
                                town.getBreadCount(),
                                town.isTouristSpawningEnabled(),
                                town.canSpawnTourists(),
                                town.getPathStart() != null ? town.getPathStart().toShortString() : null,
                                town.getPathEnd() != null ? town.getPathEnd().toShortString() : null,
                                town.getSearchRadius(),
                                town.getTotalVisitors()
                            ));
                        }
                    }
                    
                    // Send the data back to the client
                    INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new TownDataResponsePacket(townDataList)
                    );
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Response packet (server -> client)
     */
    public static class TownDataResponsePacket {
        private final List<TownDebugOverlay.TownDebugData> townData;
        
        public TownDataResponsePacket(List<TownDebugOverlay.TownDebugData> townData) {
            this.townData = townData;
        }
        
        public TownDataResponsePacket(FriendlyByteBuf buf) {
            int size = buf.readInt();
            townData = new ArrayList<>(size);
            
            for (int i = 0; i < size; i++) {
                String id = buf.readUtf();
                String name = buf.readUtf();
                String position = buf.readUtf();
                int population = buf.readInt();
                int breadCount = buf.readInt();
                boolean touristSpawningEnabled = buf.readBoolean();
                boolean canSpawnTourists = buf.readBoolean();
                String pathStart = buf.readBoolean() ? buf.readUtf() : null;
                String pathEnd = buf.readBoolean() ? buf.readUtf() : null;
                int searchRadius = buf.readInt();
                int totalVisitors = buf.readInt();
                
                townData.add(new TownDebugOverlay.TownDebugData(
                    id, name, position, population, breadCount, 
                    touristSpawningEnabled, canSpawnTourists, 
                    pathStart, pathEnd, searchRadius, totalVisitors
                ));
            }
        }
        
        public void encode(FriendlyByteBuf buf) {
            buf.writeInt(townData.size());
            
            for (TownDebugOverlay.TownDebugData data : townData) {
                buf.writeUtf(data.id);
                buf.writeUtf(data.name);
                buf.writeUtf(data.position);
                buf.writeInt(data.population);
                buf.writeInt(data.breadCount);
                buf.writeBoolean(data.touristSpawningEnabled);
                buf.writeBoolean(data.canSpawnTourists);
                buf.writeBoolean(data.pathStart != null);
                if (data.pathStart != null) buf.writeUtf(data.pathStart);
                buf.writeBoolean(data.pathEnd != null);
                if (data.pathEnd != null) buf.writeUtf(data.pathEnd);
                buf.writeInt(data.searchRadius);
                buf.writeInt(data.totalVisitors);
            }
        }
        
        @OnlyIn(Dist.CLIENT)
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                // Update the overlay with the received data
                TownDebugOverlay.setTownData(townData);
                
                // No notification message - silently update the data
            });
            ctx.get().setPacketHandled(true);
        }
    }
} 