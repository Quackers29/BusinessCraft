package com.quackers29.businesscraft.fabric.network.packets.ui;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric implementation of OpenTownInterfacePacket using Fabric networking APIs.
 */
public class OpenTownInterfacePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenTownInterfacePacket.class);

    private final BlockPos blockPos;

    public OpenTownInterfacePacket(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
    }

    public static OpenTownInterfacePacket decode(FriendlyByteBuf buf) {
        BlockPos blockPos = buf.readBlockPos();
        return new OpenTownInterfacePacket(blockPos);
    }

    public void handle(ServerPlayer player) {
        // Open the town interface menu for the player
        // This would need to be implemented with the actual menu opening logic
        LOGGER.info("Opening town interface menu at position: {} for player: {}",
                   blockPos, player.getName().getString());

        // TODO: Implement the actual menu opening logic
        // This would typically involve:
        // 1. Getting the block entity at the position
        // 2. Creating and opening the town interface menu
    }

    // Static methods for Fabric network registration
    public static void encode(OpenTownInterfacePacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }
}
