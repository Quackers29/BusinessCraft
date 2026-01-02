package com.quackers29.businesscraft.forge.platform;

import com.quackers29.businesscraft.api.NetworkMessages;
import net.minecraft.server.level.ServerPlayer;

/**
 * Forge implementation of NetworkMessages
 */
public class ForgeNetworkMessages implements NetworkMessages {
    @Override
    public void sendToPlayer(Object message, Object player) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.quackers29.businesscraft.forge.network.ForgeModMessages.sendToPlayer(message, serverPlayer);
        }
    }

    @Override
    public void sendToAllPlayers(Object message) {
        com.quackers29.businesscraft.forge.network.ForgeModMessages.sendToAllPlayers(message);
    }

    @Override
    public void sendToAllTrackingChunk(Object message, Object level, Object pos) {
        if (level instanceof net.minecraft.world.level.Level mcLevel &&
                pos instanceof net.minecraft.core.BlockPos mcPos) {
            com.quackers29.businesscraft.forge.network.ForgeModMessages.sendToAllTrackingChunk(message, mcLevel, mcPos);
        }
    }

    @Override
    public void sendToServer(Object message) {
        com.quackers29.businesscraft.forge.network.ForgeModMessages.sendToServer(message);
    }
}
