package com.yourdomain.businesscraft.menu;

import com.yourdomain.businesscraft.BusinessCraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.network.FriendlyByteBuf;

public class StorageMenu extends AbstractContainerMenu {
    // Constants for the number of slots
    private static final int STORAGE_ROWS = 2;
    private static final int STORAGE_COLS = 9;
    private static final int INVENTORY_SIZE = STORAGE_ROWS * STORAGE_COLS; // 18 slots for storage
    
    // ItemHandlers for the storage inventory
    private final ItemStackHandler storageInventory;
    
    // Storage grid positions
    private static final int STORAGE_START_X = 8;
    private static final int STORAGE_START_Y = 28;
    private static final int SLOT_SIZE = 18;
    
    // Register the menu type
    public static final DeferredRegister<MenuType<?>> MENUS = 
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, BusinessCraft.MOD_ID);
    
    @SuppressWarnings("unchecked")
    public static final RegistryObject<MenuType<StorageMenu>> STORAGE_MENU =
            MENUS.register("storage_menu", () -> {
                return (MenuType<StorageMenu>) net.minecraftforge.common.extensions.IForgeMenuType.create(
                    (windowId, inv, data) -> new StorageMenu(windowId, inv, new ItemStackHandler(INVENTORY_SIZE))
                );
            });
    
    // Constructor for client-side creation
    public StorageMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new ItemStackHandler(INVENTORY_SIZE));
    }
    
    // Constructor for server-side creation
    public StorageMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, new ItemStackHandler(INVENTORY_SIZE));
    }
    
    // Main constructor
    public StorageMenu(int containerId, Inventory playerInventory, IItemHandler storageInventory) {
        super(STORAGE_MENU.get(), containerId);
        
        // Create the storage inventory if it doesn't exist
        if (storageInventory instanceof ItemStackHandler) {
            this.storageInventory = (ItemStackHandler) storageInventory;
        } else {
            this.storageInventory = new ItemStackHandler(INVENTORY_SIZE);
        }
        
        // Add slots for the storage inventory (2 rows of 9)
        for (int row = 0; row < STORAGE_ROWS; row++) {
            for (int col = 0; col < STORAGE_COLS; col++) {
                int index = col + row * STORAGE_COLS;
                int x = STORAGE_START_X + col * SLOT_SIZE;
                int y = STORAGE_START_Y + row * SLOT_SIZE;
                this.addSlot(new StorageSlot(this.storageInventory, index, x, y));
            }
        }
        
        // Add slots for player inventory (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 
                        8 + col * SLOT_SIZE, 84 + row * SLOT_SIZE));
            }
        }
        
        // Add slots for player hotbar (1 row of 9)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * SLOT_SIZE, 142));
        }
    }
    
    // Check if the player can interact with the menu
    @Override
    public boolean stillValid(Player player) {
        return true; // Always valid as this is a UI-only menu
    }
    
    // Handle shift-clicking items between slots
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();
            
            // If shift-clicking from the storage inventory
            if (index < INVENTORY_SIZE) {
                // Try to move to player inventory
                if (!this.moveItemStackTo(slotStack, INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } 
            // If shift-clicking from player inventory
            else {
                // Try to move to storage inventory
                if (!this.moveItemStackTo(slotStack, 0, INVENTORY_SIZE, false)) {
                    return ItemStack.EMPTY;
                }
            }
            
            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            
            slot.onTake(player, slotStack);
        }
        
        return itemstack;
    }
    
    // Custom slot implementation for storage
    private static class StorageSlot extends SlotItemHandler {
        public StorageSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }
        
        // Allow any item to be placed in storage slots
        @Override
        public boolean mayPlace(ItemStack stack) {
            return true;
        }
    }
} 