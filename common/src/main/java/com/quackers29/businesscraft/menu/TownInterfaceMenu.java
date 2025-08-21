package com.quackers29.businesscraft.menu;

import com.quackers29.businesscraft.platform.PlatformServices;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * UNIFIED ARCHITECTURE STUB: Minimal TownInterfaceMenu for compilation.
 * 
 * This is a temporary bridge class that allows the common module TownInterfaceBlock to compile
 * while menu implementations use platform-specific registration.
 * 
 * Platform-specific menu types are obtained through PlatformServices to avoid direct 
 * dependencies on ModMenuTypes or similar platform-specific registration classes.
 */
public class TownInterfaceMenu extends AbstractContainerMenu {
    
    private final BlockPos pos;
    
    public TownInterfaceMenu(int windowId, Inventory inventory, BlockPos pos) {
        super(getMenuType(), windowId);
        this.pos = pos;
        // Platform-specific implementations will handle the actual menu setup
    }
    
    private static MenuType<?> getMenuType() {
        // Use platform services to get the appropriate menu type
        // This avoids direct dependency on ModMenuTypes
        Object menuType = PlatformServices.getMenuHelper();
        if (menuType instanceof MenuType<?> type) {
            return type;
        }
        // Fallback - platform implementations should override this
        return null;
    }
    
    @Override
    public boolean stillValid(Player player) {
        // Basic validation - platform implementations will override
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
    
    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        // Stub implementation - platform-specific implementations will override
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
}