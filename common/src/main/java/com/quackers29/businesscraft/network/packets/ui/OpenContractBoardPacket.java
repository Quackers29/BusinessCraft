package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.menu.ContractBoardMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Packet to request the server to open the ContractBoard menu.
 */
public class OpenContractBoardPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenContractBoardPacket.class);

    private final BlockPos blockPos;

    public OpenContractBoardPacket(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public OpenContractBoardPacket(FriendlyByteBuf buf) {
        this.blockPos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
    }

    // Static methods for Forge network registration
    public static void encode(OpenContractBoardPacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }

    public static OpenContractBoardPacket decode(FriendlyByteBuf buf) {
        return new OpenContractBoardPacket(buf);
    }

    public boolean handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            Object senderObj = PlatformAccess.getNetwork().getSender(context);
            if (senderObj instanceof ServerPlayer player) {
                // Get the block entity to ensure all town data is accessible
                BlockEntity entity = player.level().getBlockEntity(blockPos);
                if (entity instanceof TownInterfaceEntity townInterface) {
                    // Open the ContractBoardMenu using PlatformAccess for platform-agnostic screen
                    // opening
                    PlatformAccess.getNetwork().openScreen(player, new MenuProvider() {
                        @Override
                        public Component getDisplayName() {
                            return Component.translatable("menu.businesscraft.contract_board");
                        }

                        @Override
                        public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
                            return new ContractBoardMenu(windowId, inventory, blockPos);
                        }
                    }, blockPos);

                    // Force sync contracts to the player opening the board
                    try {
                        net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) player
                                .level();
                        com.quackers29.businesscraft.contract.ContractBoard board = com.quackers29.businesscraft.contract.ContractBoard
                                .get(level);
                        PlatformAccess.getNetworkMessages().sendToPlayer(
                                new com.quackers29.businesscraft.network.packets.ui.ContractSyncPacket(
                                        board.getContracts()),
                                player);
                        LOGGER.info("Sent initial contract sync to player {}", player.getName().getString());
                    } catch (Exception e) {
                        LOGGER.error("Failed to send initial contract sync", e);
                    }
                } else {
                    LOGGER.error("Failed to get TownInterfaceEntity at position: {}", blockPos);
                }
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
        return true;
    }
}
