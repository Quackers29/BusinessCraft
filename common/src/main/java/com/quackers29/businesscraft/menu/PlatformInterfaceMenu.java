package com.quackers29.businesscraft.menu;

import com.quackers29.businesscraft.block.entity.PlatformInterfaceEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Empty menu so right-clicking a Platform Interface opens its screen on the client that clicked.
 * The screen only edits the platform path. Enabled and destinations stay on the Town Centre.
 */
public class PlatformInterfaceMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final Level level;

    public PlatformInterfaceMenu(int windowId, Inventory inventory, FriendlyByteBuf data) {
        this(windowId, inventory, data.readBlockPos());
    }

    public PlatformInterfaceMenu(int windowId, Inventory inventory, BlockPos pos) {
        super(com.quackers29.businesscraft.init.CommonModMenuTypes.PLATFORM_INTERFACE_MENU.get(), windowId);
        this.pos = pos;
        this.level = inventory.player.level();
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    @Override
    public boolean stillValid(Player player) {
        if (level.getBlockEntity(pos) instanceof PlatformInterfaceEntity) {
            return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
