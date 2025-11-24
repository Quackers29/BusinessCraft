package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.NetworkHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.core.BlockPos;

/**
 * Fabric implementation of NetworkHelper using direct Mojang mappings.
 */
public class FabricNetworkHelper implements NetworkHelper {

    private static final String MOD_ID = "businesscraft";

    @Override
    public <T> void registerMessage(int index, Class<T> messageType, Object encoder, Object decoder) {
        // Platform-specific registration is handled in Fabric mod networking setup
        // TODO: Implement proper networking registration if needed
    }

    @Override
    public void sendToPlayer(Object message, Object player) {
        // TODO: Implement proper networking
    }

    @Override
    public void sendToAllPlayers(Object message) {
        // TODO: Implement proper networking
    }

    @Override
    public void sendToAllTrackingChunk(Object message, Object level, Object pos) {
        // TODO: Implement proper networking
    }

    @Override
    public void sendToServer(Object message) {
        // TODO: Implement proper networking
    }

    @Override
    public boolean isClientSide() {
        // Simple environment check
        try {
            // In Fabric, we can check the environment type safely
            return net.fabricmc.loader.api.FabricLoader.getInstance()
                    .getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT;
        } catch (Exception e) {
            // Fallback if FabricLoader is not available (shouldn't happen in Fabric env)
            return true;
        }
    }

    @Override
    public Object getCurrentContext() {
        return null;
    }

    @Override
    public void enqueueWork(Object context, Runnable work) {
        work.run();
    }

    @Override
    public Object getSender(Object context) {
        return null;
    }

    @Override
    public void setPacketHandled(Object context) {
    }

    @Override
    public void openScreen(Object player, Object menuProvider) {
        openScreen(player, menuProvider, null);
    }

    @Override
    public void openScreen(Object player, Object menuProvider, Object blockPos) {
        if (player instanceof ServerPlayer serverPlayer && menuProvider instanceof MenuProvider provider) {
            if (provider instanceof net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory) {
                serverPlayer.openMenu(provider);
            } else {
                net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory factory = new net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory() {
                    @Override
                    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int syncId,
                            net.minecraft.world.entity.player.Inventory inv,
                            net.minecraft.world.entity.player.Player player) {
                        return provider.createMenu(syncId, inv, player);
                    }

                    @Override
                    public net.minecraft.network.chat.Component getDisplayName() {
                        return provider.getDisplayName();
                    }

                    @Override
                    public void writeScreenOpeningData(ServerPlayer player, net.minecraft.network.FriendlyByteBuf buf) {
                        if (blockPos instanceof BlockPos pos) {
                            buf.writeBlockPos(pos);
                        }
                    }
                };
                serverPlayer.openMenu(factory);
            }
        }
    }
}
