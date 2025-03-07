package com.yourdomain.businesscraft.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import com.yourdomain.businesscraft.init.ModMenuTypes;

/**
 * Menu container for the Town Interface block.
 * This class manages the data displayed in the Town Interface UI.
 */
public class TownInterfaceMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    
    // Town properties for UI display
    private String townName = "New Town";
    private String mayorName = "Mayor";
    private int townLevel = 99;
    private int townPopulation = 0;
    private int townReputation = 50;
    
    // Economy data
    private int goldCoins = 0;
    private int silverCoins = 0;
    private int bronzeCoins = 0;
    
    // Settings
    private boolean autoCollectEnabled = false;
    private boolean taxesEnabled = false;

    /**
     * Constructor for server-side menu creation
     */
    public TownInterfaceMenu(int windowId, Inventory inv, BlockPos pos) {
        super(ModMenuTypes.TOWN_INTERFACE.get(), windowId);
        this.pos = pos;
        
        // Initialize with demo data
        this.townName = "Riverside";
        this.mayorName = inv.player.getName().getString();
        this.townLevel = 2;
        this.townPopulation = 8;
        this.townReputation = 75;
        this.goldCoins = 2;
        this.silverCoins = 15;
        this.bronzeCoins = 42;
    }
    
    /**
     * Constructor for client-side menu creation
     */
    public TownInterfaceMenu(int windowId, Inventory inv, FriendlyByteBuf data) {
        this(windowId, inv, data.readBlockPos());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // No inventory slots to manage
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64;
    }
    
    // Getters and setters for UI data
    
    public String getTownName() {
        return townName;
    }
    
    public void setTownName(String townName) {
        this.townName = townName;
        // In a real implementation, this would trigger a server-side update
    }
    
    public String getMayorName() {
        return mayorName;
    }
    
    public void setMayorName(String mayorName) {
        this.mayorName = mayorName;
        // In a real implementation, this would trigger a server-side update
    }
    
    public int getTownLevel() {
        return townLevel;
    }
    
    public int getTownPopulation() {
        return townPopulation;
    }
    
    public int getTownReputation() {
        return townReputation;
    }
    
    public int getGoldCoins() {
        return goldCoins;
    }
    
    public int getSilverCoins() {
        return silverCoins;
    }
    
    public int getBronzeCoins() {
        return bronzeCoins;
    }
    
    public boolean isAutoCollectEnabled() {
        return autoCollectEnabled;
    }
    
    public void setAutoCollectEnabled(boolean enabled) {
        this.autoCollectEnabled = enabled;
        // In a real implementation, this would trigger a server-side update
    }
    
    public boolean isTaxesEnabled() {
        return taxesEnabled;
    }
    
    public void setTaxesEnabled(boolean enabled) {
        this.taxesEnabled = enabled;
        // In a real implementation, this would trigger a server-side update
    }
} 