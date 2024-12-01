package com.yourdomain.businesscraft.network;

import com.yourdomain.businesscraft.BusinessCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

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

        net.messageBuilder(SetPathCreationModePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(SetPathCreationModePacket::decode)
            .encoder(SetPathCreationModePacket::encode)
            .consumerMainThread(SetPathCreationModePacket::handle)
            .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
} 