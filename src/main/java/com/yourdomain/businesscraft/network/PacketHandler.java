package com.yourdomain.businesscraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import com.yourdomain.businesscraft.BusinessCraft;

/**
 * Handles registration and dispatch of network packets
 * 
 * @deprecated This class is deprecated and will be removed in a future update.
 * Use {@link ModMessages} instead for all packet registration and sending.
 * This class contains duplicate functionality and is being maintained only for backward compatibility.
 */
@Deprecated
public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(BusinessCraft.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }
    
    /**
     * @deprecated Use {@link ModMessages#register()} instead
     */
    @Deprecated
    public static void register() {
        // Register existing packets
        // ... existing packet registrations ...
        
        // Register platform-related packets
        INSTANCE.registerMessage(id(), AddPlatformPacket.class, 
            AddPlatformPacket::encode, 
            AddPlatformPacket::decode, 
            AddPlatformPacket::handle);
        
        INSTANCE.registerMessage(id(), DeletePlatformPacket.class,
            DeletePlatformPacket::encode,
            DeletePlatformPacket::decode,
            DeletePlatformPacket::handle);
        
        INSTANCE.registerMessage(id(), SetPlatformEnabledPacket.class,
            SetPlatformEnabledPacket::encode,
            SetPlatformEnabledPacket::decode,
            SetPlatformEnabledPacket::handle);
        
        INSTANCE.registerMessage(id(), SetPlatformPathPacket.class,
            SetPlatformPathPacket::encode,
            SetPlatformPathPacket::decode,
            SetPlatformPathPacket::handle);
        
        INSTANCE.registerMessage(id(), RefreshPlatformsPacket.class,
            RefreshPlatformsPacket::encode,
            buf -> new RefreshPlatformsPacket(buf),
            RefreshPlatformsPacket::handle);
    }
    
    /**
     * Sends a packet to all clients tracking a specific chunk
     * 
     * @deprecated Use {@link ModMessages#sendToAllTrackingChunk(Object, Level, BlockPos)} instead
     */
    @Deprecated
    public static void sendToAllTracking(Object packet, ServerLevel level, BlockPos pos) {
        INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> 
            level.getChunkAt(pos)), packet);
    }
    
    /**
     * Sends a packet to a specific player
     * 
     * @deprecated Use {@link ModMessages#sendToPlayer(Object, ServerPlayer)} instead
     */
    @Deprecated
    public static void sendToPlayer(Object packet, ServerPlayer player) {
        INSTANCE.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
} 