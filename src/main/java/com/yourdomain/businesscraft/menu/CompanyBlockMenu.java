package com.yourdomain.businesscraft.menu;

import com.yourdomain.businesscraft.block.entity.CompanyBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

public class CompanyBlockMenu extends AbstractContainerMenu {
    private final CompanyBlockEntity blockEntity;
    private final ContainerData data;

    public CompanyBlockMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(2));
    }

    public CompanyBlockMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.COMPANY_BLOCK_MENU.get(), id);
        checkContainerSize(inv, 2);
        blockEntity = (CompanyBlockEntity) entity;
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        if (blockEntity != null) {
            blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                addSlot(new SlotItemHandler(handler, 0, 44, 36));  // Input slot
                addSlot(new SlotItemHandler(handler, 1, 116, 36)); // Output slot
            });
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Implementation for shift-clicking items
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, blockEntity.getBlockState().getBlock());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
} 