package com.yourdomain.businesscraft.platform.forge;

import com.yourdomain.businesscraft.platform.NetworkHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Forge implementation of the NetworkHelper interface using SimpleChannel.
 */
public class ForgeNetworkHelper implements NetworkHelper {
    
    private final SimpleChannel channel;
    private final Map<Class<?>, Integer> packetIds = new HashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(0);
    
    public ForgeNetworkHelper(String modId, String channelVersion) {
        this.channel = NetworkRegistry.newSimpleChannel(
            new net.minecraft.resources.ResourceLocation(modId, "main"),
            () -> channelVersion,
            channelVersion::equals,
            channelVersion::equals
        );
    }
    
    @Override
    public <T> void registerClientToServerPacket(Class<T> messageClass, 
                                                PacketEncoder<T> encoder,
                                                PacketDecoder<T> decoder, 
                                                PacketHandler<T> handler) {
        int id = nextId.getAndIncrement();
        packetIds.put(messageClass, id);
        
        channel.registerMessage(id, messageClass,
            encoder::encode,
            decoder::decode,
            (packet, ctx) -> {
                ctx.get().enqueueWork(() -> {
                    ServerPlayer player = ctx.get().getSender();
                    if (player != null) {
                        handler.handle(packet, player);
                    }
                });
                ctx.get().setPacketHandled(true);
            },
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }
    
    @Override
    public <T> void registerServerToClientPacket(Class<T> messageClass,
                                                PacketEncoder<T> encoder,
                                                PacketDecoder<T> decoder,
                                                PacketHandler<T> handler) {
        int id = nextId.getAndIncrement();
        packetIds.put(messageClass, id);
        
        channel.registerMessage(id, messageClass,
            encoder::encode,
            decoder::decode,
            (packet, ctx) -> {
                ctx.get().enqueueWork(() -> {
                    Player player = net.minecraft.client.Minecraft.getInstance().player;
                    if (player != null) {
                        handler.handle(packet, player);
                    }
                });
                ctx.get().setPacketHandled(true);
            },
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }
    
    @Override
    public void sendToServer(Object packet) {
        channel.sendToServer(packet);
    }
    
    @Override
    public void sendToClient(Object packet, ServerPlayer player) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    
    @Override
    public void sendToAllClients(Object packet) {
        channel.send(PacketDistributor.ALL.noArg(), packet);
    }
    
    /**
     * Gets the underlying SimpleChannel for Forge-specific operations.
     * This is Forge-specific functionality.
     */
    public SimpleChannel getChannel() {
        return channel;
    }
    
    /**
     * Gets the packet ID for a registered packet class.
     * This is Forge-specific functionality.
     */
    public Integer getPacketId(Class<?> packetClass) {
        return packetIds.get(packetClass);
    }
}