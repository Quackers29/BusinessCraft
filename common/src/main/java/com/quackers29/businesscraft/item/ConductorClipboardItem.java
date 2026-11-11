package com.quackers29.businesscraft.item;

import com.quackers29.businesscraft.api.ClientHelper;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.init.CommonModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Client-side conductor view: holding this item reveals origin/destination on nearby tourist name tags.
 */
public final class ConductorClipboardItem {

    public static final ResourceLocation ID = new ResourceLocation("businesscraft", "conductor_clipboard");

    private ConductorClipboardItem() {
    }

    public static boolean isStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = resolvedItem();
        return item != null && stack.is(item);
    }

    /**
     * True when the local player holds a conductor clipboard in main or off hand.
     */
    public static boolean clientPlayerHoldingForRouteView() {
        ClientHelper client = PlatformAccess.getClient();
        if (client == null) {
            return false;
        }
        Object playerObj = client.getClientPlayer();
        if (!(playerObj instanceof Player player)) {
            return false;
        }
        return isStack(player.getMainHandItem()) || isStack(player.getOffhandItem());
    }

    private static Item resolvedItem() {
        if (CommonModItems.CONDUCTOR_CLIPBOARD != null) {
            return CommonModItems.CONDUCTOR_CLIPBOARD.get();
        }
        if (PlatformAccess.registry == null) {
            return null;
        }
        return PlatformAccess.getRegistry().getItem(ID);
    }
}
