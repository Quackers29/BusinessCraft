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

public class TradeMenu extends AbstractContainerMenu {
    // Constants for the number of slots
    private static final int INVENTORY_SIZE = 2; // 1 input slot, 1 output slot
    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;
    
    // ItemHandlers for the trading inventory
    private final ItemStackHandler tradeInventory;
    
    // Slot positions
    public static final int SLOT_INPUT_X = 44;
    public static final int SLOT_INPUT_Y = 36;
    public static final int SLOT_OUTPUT_X = 116;
    public static final int SLOT_OUTPUT_Y = 36;
    
    // Register for the menu type
    public static final DeferredRegister<MenuType<?>> MENUS = 
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, BusinessCraft.MOD_ID);
    
    @SuppressWarnings("unchecked")
    public static final RegistryObject<MenuType<TradeMenu>> TRADE_MENU =
            MENUS.register("trade_menu", () -> {
                return (MenuType<TradeMenu>) net.minecraftforge.common.extensions.IForgeMenuType.create(
                    (windowId, inv, data) -> new TradeMenu(windowId, inv, new ItemStackHandler(INVENTORY_SIZE))
                );
            });
    
    // Constructor for client-side creation
    public TradeMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new ItemStackHandler(INVENTORY_SIZE));
    }
    
    // Constructor for server-side creation
    public TradeMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, new ItemStackHandler(INVENTORY_SIZE));
    }
    
    // Main constructor
    public TradeMenu(int containerId, Inventory playerInventory, IItemHandler tradeInventory) {
        super(TRADE_MENU.get(), containerId);
        
        // Create the trade inventory if it doesn't exist
        if (tradeInventory instanceof ItemStackHandler) {
            this.tradeInventory = (ItemStackHandler) tradeInventory;
        } else {
            this.tradeInventory = new ItemStackHandler(INVENTORY_SIZE);
        }
        
        // Add slots for the trade inventory
        this.addSlot(new InputSlot(this.tradeInventory, SLOT_INPUT, SLOT_INPUT_X, SLOT_INPUT_Y));
        this.addSlot(new OutputSlot(this.tradeInventory, SLOT_OUTPUT, SLOT_OUTPUT_X, SLOT_OUTPUT_Y));
        
        // Add slots for player inventory (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 
                        8 + col * 18, 84 + row * 18));
            }
        }
        
        // Add slots for player hotbar (1 row of 9)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
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
            
            // If shift-clicking from the trade inventory slots (0-1)
            if (index < INVENTORY_SIZE) {
                // Try to move to player inventory (2-38)
                if (!this.moveItemStackTo(slotStack, INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } 
            // If shift-clicking from player inventory
            else {
                // Try to move to the input slot only (0)
                if (!this.moveItemStackTo(slotStack, SLOT_INPUT, SLOT_INPUT + 1, false)) {
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
    
    // Process the trade when requested
    public void processTrade() {
        if (!tradeInventory.getStackInSlot(SLOT_INPUT).isEmpty() && 
            tradeInventory.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
            
            // For now, just move the input to the output
            ItemStack inputStack = tradeInventory.getStackInSlot(SLOT_INPUT).copy();
            tradeInventory.extractItem(SLOT_INPUT, inputStack.getCount(), false);
            tradeInventory.insertItem(SLOT_OUTPUT, inputStack, false);
        }
    }
    
    // Custom input slot that only accepts input items
    private static class InputSlot extends SlotItemHandler {
        public InputSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }
        
        // Allow any item to be placed in the input slot
        @Override
        public boolean mayPlace(ItemStack stack) {
            return true;
        }
    }
    
    // Custom output slot that doesn't accept direct placement
    private static class OutputSlot extends SlotItemHandler {
        public OutputSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }
        
        // Don't allow items to be placed directly in the output slot
        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
} 