package com.yourdomain.businesscraft.forge.platform;

import com.yourdomain.businesscraft.api.NetworkHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Forge implementation of NetworkHelper
 */
public class ForgeNetworkHelper implements NetworkHelper {
    private static final String PROTOCOL_VERSION = "1";
    private static final ResourceLocation CHANNEL_NAME = new ResourceLocation("businesscraft", "main");

    private SimpleChannel channel;

    public ForgeNetworkHelper() {
        this.channel = NetworkRegistry.newSimpleChannel(
            CHANNEL_NAME,
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
        );
    }

    public SimpleChannel getChannel() {
        return channel;
    }

    @Override
    public <T> void registerMessage(int index, Class<T> messageType, FriendlyByteBuf.Writer<T> encoder,
                                   FriendlyByteBuf.Reader<T> decoder, NetworkEvent.Context.Handler<T> handler) {
        channel.registerMessage(index, messageType, encoder, decoder, handler);
    }

    @Override
    public void sendToPlayer(Object message, ServerPlayer player) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    @Override
    public void sendToAllPlayers(Object message) {
        channel.send(PacketDistributor.ALL.noArg(), message);
    }

    @Override
    public void sendToServer(Object message) {
        channel.sendToServer(message);
    }

    public void sendToAllTrackingChunk(Object message, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)).send(message);
    }
}
