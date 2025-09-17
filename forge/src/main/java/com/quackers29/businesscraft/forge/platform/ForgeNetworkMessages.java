package com.quackers29.businesscraft.forge.platform;

import com.quackers29.businesscraft.api.NetworkMessages;
import net.minecraft.server.level.ServerPlayer;

/**
 * Forge implementation of NetworkMessages
 */
public class ForgeNetworkMessages implements NetworkMessages {
    @Override
    public void sendToPlayer(Object message, ServerPlayer player) {
        com.quackers29.businesscraft.forge.network.ForgeModMessages.sendToPlayer(message, player);
    }

    @Override
    public void sendToAllPlayers(Object message) {
        com.quackers29.businesscraft.forge.network.ForgeModMessages.sendToAllPlayers(message);
    }

    @Override
    public void sendToAllTrackingChunk(Object message, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        com.quackers29.businesscraft.forge.network.ForgeModMessages.sendToAllTrackingChunk(message, level, pos);
    }

    @Override
    public void sendToServer(Object message) {
        com.quackers29.businesscraft.forge.network.ForgeModMessages.sendToServer(message);
    }
}
