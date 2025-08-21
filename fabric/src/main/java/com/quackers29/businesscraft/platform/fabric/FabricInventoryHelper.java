package com.quackers29.businesscraft.platform.fabric;

import com.quackers29.businesscraft.platform.InventoryHelper;
import net.minecraft.item.ItemStack;

/**
 * Fabric implementation of InventoryHelper using Yarn mappings.
 * Simplified to match common interface pattern for unified architecture.
 */
public class FabricInventoryHelper implements InventoryHelper {
    
    @Override
    public boolean isItemStackValid(Object itemStack) {
        if (itemStack instanceof ItemStack stack) {
            return !stack.isEmpty();
        }
        return false;
    }
}