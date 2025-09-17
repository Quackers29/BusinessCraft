package com.quackers29.businesscraft.fabric.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric networking setup
 */
public class FabricModMessages {
    public static void register() {
        // TODO: Register network messages using Fabric networking API
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        // TODO: Implement sending messages to specific player
    }

    public static void sendToAllPlayers(Object message) {
        // TODO: Implement sending messages to all players
    }

    public static void sendToAllTrackingChunk(Object message, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        // TODO: Implement sending messages to players tracking a chunk
    }

    public static void sendToServer(Object message) {
        // TODO: Implement client to server communication
    }
}
