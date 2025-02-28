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
 */
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
    
    public static void register() {
        // Register existing packets
        // ... existing packet registrations ...
        
        // Register platform-related packets
        INSTANCE.registerMessage(id(), AddPlatformPacket.class, 
            AddPlatformPacket::encode, 
            AddPlatformPacket::new, 
            AddPlatformPacket::handle);
        
        INSTANCE.registerMessage(id(), DeletePlatformPacket.class,
            DeletePlatformPacket::encode,
            DeletePlatformPacket::new,
            DeletePlatformPacket::handle);
        
        INSTANCE.registerMessage(id(), SetPlatformEnabledPacket.class,
            SetPlatformEnabledPacket::encode,
            SetPlatformEnabledPacket::new,
            SetPlatformEnabledPacket::handle);
        
        INSTANCE.registerMessage(id(), SetPlatformPathPacket.class,
            SetPlatformPathPacket::encode,
            SetPlatformPathPacket::new,
            SetPlatformPathPacket::handle);
        
        INSTANCE.registerMessage(id(), RefreshPlatformsPacket.class,
            RefreshPlatformsPacket::encode,
            RefreshPlatformsPacket::new,
            RefreshPlatformsPacket::handle);
    }
    
    /**
     * Sends a packet to all clients tracking a specific chunk
     */
    public static void sendToAllTracking(Object packet, ServerLevel level, BlockPos pos) {
        INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> 
            level.getChunkAt(pos)), packet);
    }
    
    /**
     * Sends a packet to a specific player
     */
    public static void sendToPlayer(Object packet, ServerPlayer player) {
        INSTANCE.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
} 