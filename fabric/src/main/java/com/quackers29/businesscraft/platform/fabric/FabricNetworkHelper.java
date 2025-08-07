package com.quackers29.businesscraft.platform.fabric;

import com.quackers29.businesscraft.platform.NetworkHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Fabric implementation of NetworkHelper using Yarn mappings.
 * Implements cross-platform networking using Fabric Networking API.
 */
public class FabricNetworkHelper implements NetworkHelper {
    
    @Override
    public <T> void registerServerBoundPacket(Identifier packetId, PacketDecoder<T> decoder, PacketHandler<T> handler) {
        ServerPlayNetworking.registerGlobalReceiver(packetId, (server, player, handler1, buf, responseSender) -> {
            T packet = decoder.decode(buf);
            server.execute(() -> handler.handle(packet, player));
        });
    }
    
    @Override
    public <T> void registerClientBoundPacket(Identifier packetId, PacketDecoder<T> decoder, ClientPacketHandler<T> handler) {
        ClientPlayNetworking.registerGlobalReceiver(packetId, (client, handler1, buf, responseSender) -> {
            T packet = decoder.decode(buf);
            client.execute(() -> handler.handle(packet));
        });
    }
    
    @Override
    public <T> void sendToPlayer(ServerPlayerEntity player, Identifier packetId, T packet, PacketEncoder<T> encoder) {
        PacketByteBuf buf = PacketByteBufs.create();
        encoder.encode(packet, buf);
        ServerPlayNetworking.send(player, packetId, buf);
    }
    
    @Override
    public <T> void sendToAll(Identifier packetId, T packet, PacketEncoder<T> encoder) {
        // Send to all players on server - we need to get the server instance differently
        // This method would typically be called from a server context where we have access to server
        // For now, we'll implement this as a helper that requires the server to be passed
        // This is a limitation that may need to be addressed in the interface design
        throw new UnsupportedOperationException("sendToAll requires server context - use sendToPlayer for each player instead");
    }
    
    @Override
    public <T> void sendToServer(Identifier packetId, T packet, PacketEncoder<T> encoder) {
        PacketByteBuf buf = PacketByteBufs.create();
        encoder.encode(packet, buf);
        ClientPlayNetworking.send(packetId, buf);
    }
}