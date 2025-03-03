package com.yourdomain.businesscraft.network;

import com.yourdomain.businesscraft.BusinessCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Handles registration and sending of network packets
 */
public class ModMessages {
    private static SimpleChannel INSTANCE;

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(BusinessCraft.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        // Register all packets using a consistent pattern
        net.messageBuilder(ToggleTouristSpawningPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ToggleTouristSpawningPacket::new)
                .encoder(ToggleTouristSpawningPacket::toBytes)
                .consumerMainThread(ToggleTouristSpawningPacket::handle)
                .add();
                
        net.messageBuilder(SetSearchRadiusPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetSearchRadiusPacket::new)
                .encoder(SetSearchRadiusPacket::toBytes)
                .consumerMainThread(SetSearchRadiusPacket::handle)
                .add();
        
        // Register the path creation packet
        // This packet has static encode/decode methods that delegate to instance methods
        net.messageBuilder(SetPathCreationModePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetPathCreationModePacket::decode)
                .encoder(SetPathCreationModePacket::encode)
                .consumerMainThread(SetPathCreationModePacket::handle)
                .add();
                
        // Register the town name change packet
        net.messageBuilder(SetTownNamePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetTownNamePacket::decode)
                .encoder(SetTownNamePacket::encode)
                .consumerMainThread(SetTownNamePacket::handle)
                .add();
                
        // Register platform-related packets
        net.messageBuilder(AddPlatformPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(AddPlatformPacket::new)
                .encoder(AddPlatformPacket::encode)
                .consumerMainThread(AddPlatformPacket::handle)
                .add();
                
        net.messageBuilder(DeletePlatformPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(DeletePlatformPacket::new)
                .encoder(DeletePlatformPacket::encode)
                .consumerMainThread(DeletePlatformPacket::handle)
                .add();
                
        net.messageBuilder(SetPlatformEnabledPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetPlatformEnabledPacket::new)
                .encoder(SetPlatformEnabledPacket::encode)
                .consumerMainThread(SetPlatformEnabledPacket::handle)
                .add();
                
        net.messageBuilder(SetPlatformPathPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetPlatformPathPacket::new)
                .encoder(SetPlatformPathPacket::encode)
                .consumerMainThread(SetPlatformPathPacket::handle)
                .add();
                
        net.messageBuilder(SetPlatformPathCreationModePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetPlatformPathCreationModePacket::new)
                .encoder(SetPlatformPathCreationModePacket::encode)
                .consumerMainThread(SetPlatformPathCreationModePacket::handle)
                .add();
                
        net.messageBuilder(RefreshPlatformsPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(RefreshPlatformsPacket::new)
                .encoder(RefreshPlatformsPacket::encode)
                .consumerMainThread(RefreshPlatformsPacket::handle)
                .add();
                
        // Register platform destination packets
        net.messageBuilder(OpenDestinationsUIPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(OpenDestinationsUIPacket::new)
                .encoder(OpenDestinationsUIPacket::encode)
                .consumerMainThread(OpenDestinationsUIPacket::handle)
                .add();
                
        net.messageBuilder(SetPlatformDestinationPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetPlatformDestinationPacket::new)
                .encoder(SetPlatformDestinationPacket::encode)
                .consumerMainThread(SetPlatformDestinationPacket::handle)
                .add();
                
        net.messageBuilder(RefreshDestinationsPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(RefreshDestinationsPacket::new)
                .encoder(RefreshDestinationsPacket::encode)
                .consumerMainThread(RefreshDestinationsPacket::handle)
                .add();

        // Register player exit UI packet
        net.messageBuilder(PlayerExitUIPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PlayerExitUIPacket::decode)
                .encoder(PlayerExitUIPacket::encode)
                .consumerMainThread(PlayerExitUIPacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
    
    /**
     * Sends a packet to all clients tracking a specific chunk
     */
    public static <MSG> void sendToAllTrackingChunk(MSG message, Level level, BlockPos pos) {
        INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> 
            level.getChunkAt(pos)), message);
    }
} 