package com.quackers29.businesscraft.menu;

import com.quackers29.businesscraft.api.PlatformAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.stream.Collectors;

public class ContractBoardMenu extends AbstractContainerMenu {
    private BlockPos townBlockPos;

    // Client-side constructor
    public ContractBoardMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory);
        if (extraData != null && extraData.readableBytes() > 0) {
            this.townBlockPos = extraData.readBlockPos();
        }
    }

    // Server-side constructor
    public ContractBoardMenu(int containerId, Inventory playerInventory, BlockPos townBlockPos) {
        this(containerId, playerInventory);
        this.townBlockPos = townBlockPos;

        // Sync contracts to client
        if (playerInventory.player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.quackers29.businesscraft.contract.ContractBoard board = com.quackers29.businesscraft.contract.ContractBoard
                    .getInstance();
            PlatformAccess.getNetworkMessages().sendToPlayer(
                    new com.quackers29.businesscraft.network.packets.ui.ContractSyncPacket(board.getContracts()),
                    serverPlayer);
        }
    }

    // Common constructor
    public ContractBoardMenu(int containerId, Inventory playerInventory) {
        super((net.minecraft.world.inventory.MenuType<ContractBoardMenu>) PlatformAccess.getMenuTypes()
                .getContractBoardMenuType(), containerId);

        // Add player inventory slots
        // layoutPlayerInventory(playerInventory, 8, 84);
    }

    private void layoutPlayerInventory(Inventory playerInventory, int leftCol, int topRow) {
        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col + row * 9 + 9,
                        leftCol + col * 18, topRow + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col, leftCol + col * 18, topRow + 58));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    public BlockPos getTownBlockPos() {
        return townBlockPos;
    }

    // Client-side contract list
    private java.util.List<com.quackers29.businesscraft.contract.Contract> contracts = new java.util.ArrayList<>();

    public void setContracts(java.util.List<com.quackers29.businesscraft.contract.Contract> contracts) {
        this.contracts = contracts;
    }

    public java.util.List<com.quackers29.businesscraft.contract.Contract> getContracts() {
        return contracts;
    }

    public List<com.quackers29.businesscraft.contract.Contract> getAvailableContracts() {
        return contracts.stream().filter(c -> !c.isExpired() && !c.isCompleted()).collect(Collectors.toList());
    }

    public List<com.quackers29.businesscraft.contract.Contract> getActiveContracts() {
        return contracts.stream().filter(c -> !c.isExpired() && c.isCompleted()).collect(Collectors.toList());
    }

    public List<com.quackers29.businesscraft.contract.Contract> getHistoryContracts() {
        return contracts.stream().filter(c -> c.isExpired() || c.isCompleted()).collect(Collectors.toList());
    }
}
