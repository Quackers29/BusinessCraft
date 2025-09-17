package com.yourdomain.businesscraft.forge.platform;

import com.yourdomain.businesscraft.api.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Forge implementation of ItemHandlerHelper
 */
public class ForgeItemHandlerHelper implements ItemHandlerHelper {
    @Override
    public Object createItemStackHandler(int size) {
        return new ItemStackHandler(size);
    }
}
