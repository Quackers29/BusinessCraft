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
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
} 