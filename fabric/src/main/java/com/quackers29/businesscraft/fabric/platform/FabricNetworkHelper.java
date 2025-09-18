package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.NetworkHelper;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;

/**
 * Fabric implementation of NetworkHelper using direct Fabric Networking API
 */
public class FabricNetworkHelper implements NetworkHelper {

    private static final String MOD_ID = "businesscraft";

    @Override
    public <T> void registerMessage(int index, Class<T> messageType, Object encoder, Object decoder) {
        // In Fabric, packet registration is typically handled through event registration
        // The actual registration happens in FabricModMessages
    }

    @Override
    public void sendToPlayer(Object message, Object player) {
        if (player instanceof ServerPlayer serverPlayer && message instanceof FriendlyByteBuf buf) {
            // Direct Fabric networking call
            ServerPlayNetworking.send(serverPlayer, new ResourceLocation(MOD_ID, "packet"), buf);
        }
    }

    @Override
    public void sendToAllPlayers(Object message) {
        // Direct Fabric networking call using PlayerLookup
        if (message instanceof FriendlyByteBuf buf) {
            for (ServerPlayer player : PlayerLookup.all(net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.getServer())) {
                ServerPlayNetworking.send(player, new ResourceLocation(MOD_ID, "packet"), buf);
            }
        }
    }

    @Override
    public void sendToAllTrackingChunk(Object message, Object level, Object pos) {
        if (level instanceof ServerLevel serverLevel &&
            pos instanceof BlockPos blockPos &&
            message instanceof FriendlyByteBuf buf) {
            // Direct Fabric networking call using PlayerLookup for chunk tracking
            for (ServerPlayer player : PlayerLookup.tracking(serverLevel, blockPos)) {
                ServerPlayNetworking.send(player, new ResourceLocation(MOD_ID, "packet"), buf);
            }
        }
    }

    @Override
    public void sendToServer(Object message) {
        if (message instanceof FriendlyByteBuf buf) {
            // Direct Fabric client networking call
            ClientPlayNetworking.send(new ResourceLocation(MOD_ID, "packet"), buf);
        }
    }

    @Override
    public boolean isClientSide() {
        // Direct Fabric environment detection
        return net.fabricmc.api.EnvType.CLIENT.equals(net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType());
    }

    @Override
    public Object getCurrentContext() {
        // Fabric doesn't use the same context system as Forge
        // Packets handle their own context through the networking events
        return null;
    }

    @Override
    public void enqueueWork(Object context, Runnable work) {
        // In Fabric, we can run directly since Fabric handles threading differently
        work.run();
    }

    @Override
    public Object getSender(Object context) {
        // Fabric's networking system provides the sender directly in the packet handler
        // This is typically handled by the individual packet classes
        return null;
    }

    @Override
    public void setPacketHandled(Object context) {
        // Fabric doesn't require explicit "packet handled" marking
        // The networking system handles this automatically
    }

    @Override
    public void openScreen(Object player, Object menuProvider) {
        if (player instanceof ServerPlayer serverPlayer &&
            menuProvider instanceof MenuProvider menuProv) {
            // Direct Fabric networking call for opening screens
            serverPlayer.openMenu(menuProv);
        }
    }
}
