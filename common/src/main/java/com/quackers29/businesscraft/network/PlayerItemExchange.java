package com.quackers29.businesscraft.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Moves item counts between a player's inventory and town storage.
 * Stacks are matched by item type, which is all the count-based town stores keep.
 */
public final class PlayerItemExchange {
    private PlayerItemExchange() {
    }

    /**
     * Removes {@code count} of {@code item} from the player's inventory and carried
     * cursor stack. Does nothing if they do not have enough.
     */
    public static boolean tryTake(ServerPlayer player, Item item, int count) {
        if (player == null) {
            return false;
        }
        List<ItemStack> stacks = new ArrayList<>();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            stacks.add(inventory.getItem(slot));
        }
        if (player.containerMenu != null) {
            ItemStack carried = player.containerMenu.getCarried();
            if (!carried.isEmpty()) {
                stacks.add(carried);
            }
        }
        return tryTakeFrom(stacks, item, count);
    }

    /**
     * All-or-nothing extract. {@code stacks} entries are mutated only when the
     * full count is available.
     */
    public static boolean tryTakeFrom(List<ItemStack> stacks, Item item, int count) {
        if (item == null || count <= 0 || stacks == null) {
            return false;
        }
        int available = 0;
        for (ItemStack stack : stacks) {
            if (stack != null && stack.is(item)) {
                available += stack.getCount();
            }
        }
        if (available < count) {
            return false;
        }
        int remaining = count;
        for (ItemStack stack : stacks) {
            if (remaining <= 0) {
                break;
            }
            if (stack != null && stack.is(item)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
        return remaining == 0;
    }

    /**
     * Gives {@code count} of {@code item} to the player, splitting into max stacks.
     * Overflow that does not fit is dropped at their feet.
     */
    public static void give(ServerPlayer player, Item item, int count) {
        if (player == null || item == null || count <= 0) {
            return;
        }
        int remaining = count;
        int max = Math.max(1, item.getMaxStackSize());
        while (remaining > 0) {
            int piece = Math.min(max, remaining);
            ItemStack stack = new ItemStack(item, piece);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            remaining -= piece;
        }
    }
}
