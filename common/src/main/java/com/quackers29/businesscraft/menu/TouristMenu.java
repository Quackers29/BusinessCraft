package com.quackers29.businesscraft.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.MenuType;
import com.quackers29.businesscraft.init.CommonModMenuTypes;

public class TouristMenu extends AbstractContainerMenu {

    private final net.minecraft.world.inventory.ContainerData data;

    // Constructor for server-side
    public TouristMenu(int containerId, Inventory playerInventory, net.minecraft.world.inventory.ContainerData data) {
        super(CommonModMenuTypes.TOURIST_MENU.get(), containerId);
        this.data = data;
        addDataSlots(data);

        // Player inventory placement
        layoutPlayerInventorySlots(playerInventory, 108, 84);
    }

    // Constructor for client-side
    public TouristMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, new net.minecraft.world.inventory.SimpleContainerData(2));
    }

    // Helper for layout
    private void layoutPlayerInventorySlots(Inventory playerInventory, int leftCol, int topRow) {
        // Player inventory
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, leftCol + j * 18, topRow + i * 18));
            }
        }

        // Hotbar
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, leftCol + i * 18, 142));
        }
    }

    public int getJourneyAge() {
        return this.data.get(0);
    }

    public int getTimeLeft() {
        return this.data.get(1);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // Simplified
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
