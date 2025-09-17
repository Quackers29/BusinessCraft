package com.quackers29.businesscraft.forge.platform;

import com.quackers29.businesscraft.api.NetworkHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
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
                                   FriendlyByteBuf.Reader<T> decoder) {
        // Register without handler - packets will be handled by their own handle methods
        // This is handled by ForgeModMessages directly
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
        channel.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)), message);
    }

    @Override
    public boolean isClientSide() {
        return net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT;
    }

    @Override
    public Object getCurrentContext() {
        // This method is not typically used in packet handling
        // Each packet handles its own context
        return null;
    }

    @Override
    public void enqueueWork(Object context, Runnable work) {
        if (context instanceof NetworkEvent.Context ctx) {
            ctx.enqueueWork(work);
        }
    }

    @Override
    public ServerPlayer getSender(Object context) {
        if (context instanceof NetworkEvent.Context ctx) {
            return ctx.getSender();
        }
        return null;
    }

    @Override
    public void setPacketHandled(Object context) {
        if (context instanceof NetworkEvent.Context ctx) {
            ctx.setPacketHandled(true);
        }
    }

    @Override
    public void openScreen(ServerPlayer player, MenuProvider menuProvider) {
        NetworkHooks.openScreen(player, menuProvider);
    }
}
