package com.quackers29.businesscraft.init;

import com.quackers29.businesscraft.api.PlatformAccess;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public final class CommonModItems {
    public static Supplier<Item> CONDUCTOR_CLIPBOARD;

    private CommonModItems() {
    }

    public static void register() {
        CONDUCTOR_CLIPBOARD = PlatformAccess.getRegistry().registerItem("conductor_clipboard",
                () -> new Item(new Item.Properties()));
    }
}
