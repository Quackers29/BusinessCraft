package com.yourdomain.businesscraft.menu;

import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.town.Town;
import com.yourdomain.businesscraft.town.TownManager;
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
import org.slf4j.LoggerFactory;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public class TownBlockMenu extends AbstractContainerMenu {
    private final TownBlockEntity blockEntity;
    private final ContainerData data;
    private static final Logger LOGGER = LogManager.getLogger("BusinessCraft/TownBlockMenu");
    private Town cachedTown;
    private UUID townId;

    public TownBlockMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public TownBlockMenu(int id, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.TOWN_BLOCK_MENU.get(), id);
        this.blockEntity = entity instanceof TownBlockEntity ? 
            (TownBlockEntity) entity : null;
        this.data = blockEntity != null ? 
            blockEntity.getContainerData() : new SimpleContainerData(4);
        
        if (blockEntity != null) {
            blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .ifPresent(handler -> {
                    addSlot(new SlotItemHandler(handler, 0, 152, 20));
                });
            addDataSlots(data);
        }
        
        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    public int getBreadCount() {
        return data.get(0);
    }

    public int getPopulation() {
        return data.get(1);
    }

    public String getTownName() {
        if (blockEntity != null) {
            String name = blockEntity.getTownName();
            if (!name.isEmpty()) {
                return name;
            }
            return "Unregistered";
        }
        return "Invalid";
    }

    public boolean isTouristSpawningEnabled() {
        if (blockEntity != null) {
            int state = data.get(2);
            return state == 1;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index < 36) {
                if (!this.moveItemStackTo(itemstack1, 36, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 36, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
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
        return blockEntity;
    }

    public Town getTown() {
        if (blockEntity != null) {
            UUID townId = blockEntity.getTownId();
            if (townId != null) {
                Level level = blockEntity.getLevel();
                if (level instanceof ServerLevel sLevel) {
                    return TownManager.get(sLevel).getTown(townId);
                }
            }
        }
        return null;
    }

    public ContainerData getData() {
        return data;
    }
}