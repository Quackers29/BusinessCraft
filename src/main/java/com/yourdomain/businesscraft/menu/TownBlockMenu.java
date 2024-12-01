package com.yourdomain.businesscraft.menu;

import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TownBlockMenu extends AbstractContainerMenu {
    private final TownBlockEntity blockEntity;
    private final ContainerData data;
    private static final Logger LOGGER = LogManager.getLogger();

    public TownBlockMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        super(ModMenuTypes.TOWN_BLOCK_MENU.get(), id);
        BlockPos pos = extraData.readBlockPos();
        TownBlockEntity blockEntity = (TownBlockEntity) inv.player.level().getBlockEntity(pos);
        this.blockEntity = blockEntity;
        this.data = blockEntity != null ? blockEntity.getContainerData() : new SimpleContainerData(2);

        // Initialize slots and data
        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        if (blockEntity != null) {
            blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                addSlot(new SlotItemHandler(handler, 0, 44, 36)); // Input slot for bread
            });
        }

        addDataSlots(data);
    }

    public int getBreadCount() {
        return data.get(0);
    }

    public int getPopulation() {
        return data.get(1);
    }

    public String getTownName() {
        int index = data.get(2); // Get town name index
        String[] townNames = TownBlockEntity.getTownNames();
        return index >= 0 && index < townNames.length ? townNames[index] : "Unknown";
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

    public TownBlockEntity getBlockEntity() {
        return this.blockEntity;
    }
}