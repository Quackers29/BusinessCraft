package com.quackers29.businesscraft.forge.network;

import com.quackers29.businesscraft.forge.platform.ForgeNetworkHelper;
import com.quackers29.businesscraft.network.PacketRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Forge-specific network message registration and handling
 */
public class ForgeModMessages {
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        ForgeNetworkHelper networkHelper = (ForgeNetworkHelper) com.quackers29.businesscraft.forge.BusinessCraftForge.NETWORK;
        SimpleChannel net = networkHelper.getChannel();

        for (PacketRegistry.PacketDefinition<?> packet : PacketRegistry.getPackets()) {
            registerPacket(net, packet);
        }
    }

    private static <MSG> void registerPacket(SimpleChannel net, PacketRegistry.PacketDefinition<MSG> packetDef) {
        NetworkDirection direction = packetDef.direction() == PacketRegistry.NetworkDirection.PLAY_TO_SERVER
                ? NetworkDirection.PLAY_TO_SERVER
                : NetworkDirection.PLAY_TO_CLIENT;

        net.messageBuilder(packetDef.packetClass(), id(), direction)
                .decoder(packetDef.decoder())
                .encoder(packetDef.encoder())
                .consumerMainThread((msg, ctxSupplier) -> {
                    // Adapt the handler to match Forge's expectation
                    // We need to pass the context as an Object because our common handler expects
                    // Object
                    // In the common handler, we'll need to cast it back if we need it,
                    // but most handlers just use the player which we can get here.

                    NetworkEvent.Context ctx = ctxSupplier.get();
                    ServerPlayer sender = ctx.getSender(); // Null for client-bound packets

                    // For client-bound packets, we might need a way to get the client player or
                    // context
                    // But our common handler signature is (MSG, Object context)

                    packetDef.handler().accept(msg, ctx);

                    // Mark packet as handled
                    ctx.setPacketHandled(true);
                })
                .add();
    }

    public static void sendToServer(Object message) {
        ForgeNetworkHelper networkHelper = (ForgeNetworkHelper) com.quackers29.businesscraft.forge.BusinessCraftForge.NETWORK;
        networkHelper.sendToServer(message);
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        ForgeNetworkHelper networkHelper = (ForgeNetworkHelper) com.quackers29.businesscraft.forge.BusinessCraftForge.NETWORK;
        networkHelper.sendToPlayer(message, player);
    }

    public static void sendToAllPlayers(Object message) {
        ForgeNetworkHelper networkHelper = (ForgeNetworkHelper) com.quackers29.businesscraft.forge.BusinessCraftForge.NETWORK;
        networkHelper.sendToAllPlayers(message);
    }

    public static void sendToAllTrackingChunk(Object message, net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos) {
        ForgeNetworkHelper networkHelper = (ForgeNetworkHelper) com.quackers29.businesscraft.forge.BusinessCraftForge.NETWORK;
        networkHelper.sendToAllTrackingChunk(message, level, pos);
    }
}
