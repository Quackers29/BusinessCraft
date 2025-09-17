package com.yourdomain.businesscraft.menu;

import com.yourdomain.businesscraft.api.PlatformAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraft.network.FriendlyByteBuf;
import com.yourdomain.businesscraft.api.PlatformAccess;
import com.yourdomain.businesscraft.network.packets.storage.TradeResourcePacket;
import net.minecraft.core.BlockPos;

public class TradeMenu extends AbstractContainerMenu {
    // Constants for the number of slots
    private static final int INVENTORY_SIZE = 2; // 1 input slot, 1 output slot
    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;
    
    // ItemHandlers for the trading inventory
    private final ItemStackHandler tradeInventory;
    
    // Store the position of the town block
    private BlockPos townBlockPos;
    
    // Slot positions
    public static final int SLOT_INPUT_X = 44;
    public static final int SLOT_INPUT_Y = 36;
    public static final int SLOT_OUTPUT_X = 116;
    public static final int SLOT_OUTPUT_Y = 36;
    
    // Constructor for client-side creation
    public TradeMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new ItemStackHandler(INVENTORY_SIZE));
    }
    
    // Constructor for server-side creation
    public TradeMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, new ItemStackHandler(INVENTORY_SIZE));
        // Read the BlockPos from the extra data if available
        if (extraData != null && extraData.readableBytes() > 0) {
            this.townBlockPos = extraData.readBlockPos();
        }
    }
    
    // Constructor with BlockPos
    public TradeMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, new ItemStackHandler(INVENTORY_SIZE));
        this.townBlockPos = pos;
    }
    
    // Main constructor
    public TradeMenu(int containerId, Inventory playerInventory, IItemHandler tradeInventory) {
        super(PlatformAccess.getMenuTypes().getTradeMenuType(), containerId);
        
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
        return true; // Allow interaction regardless of position
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
    public boolean processTrade() {
        ItemStack stack = this.tradeInventory.getStackInSlot(SLOT_INPUT);
        if (stack.isEmpty()) {
            return false;
        }
        
        // Don't move the item directly - instead send it to the server for processing
        // The server will decide if the town accepts the trade and sends back emeralds
        PlatformAccess.getNetworkMessages().sendToServer(new TradeResourcePacket(townBlockPos, stack.copy(), SLOT_INPUT));
        
        // Remove the input item now, payment will be handled by server response
        this.tradeInventory.extractItem(SLOT_INPUT, stack.getCount(), false);
        
        return true;
    }
    
    /**
     * Sets an item in the output slot directly (used for server-side trade responses)
     */
    public void setOutputItem(ItemStack itemStack) {
        if (!itemStack.isEmpty() && tradeInventory.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
            // Place the item in the output slot
            tradeInventory.insertItem(SLOT_OUTPUT, itemStack, false);
        }
    }
    
    /**
     * Get the position of the town block
     * @return The BlockPos of the town block
     */
    public BlockPos getTownBlockPos() {
        return this.townBlockPos;
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