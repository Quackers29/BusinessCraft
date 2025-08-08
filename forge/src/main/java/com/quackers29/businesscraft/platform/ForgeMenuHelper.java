package com.quackers29.businesscraft.platform;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import java.util.function.Supplier;

/**
 * Forge-specific menu helper with menu registration and complex data transfer capabilities.
 * This class provides platform-specific functionality beyond the basic MenuHelper interface.
 */
public interface ForgeMenuHelper extends MenuHelper {
    
    /**
     * Creates a menu type that supports complex data transfer between server and client.
     * This is essential for BusinessCraft's sophisticated UI framework.
     * 
     * @param factory Factory function that creates menu instances with data
     * @param <T> The menu type
     * @return MenuType supplier for registration
     */
    <T extends AbstractContainerMenu> Supplier<MenuType<T>> createDataDrivenMenuType(MenuFactory<T> factory);
    
    /**
     * Creates a simple menu type that doesn't require data transfer.
     * Used for menus that only need basic player inventory access.
     * 
     * @param factory Factory function that creates menu instances  
     * @param <T> The menu type
     * @return MenuType supplier for registration
     */
    <T extends AbstractContainerMenu> Supplier<MenuType<T>> createSimpleMenuType(SimpleMenuFactory<T> factory);
    
    /**
     * Functional interface for menu factories that require custom data transfer.
     * Used with createDataDrivenMenuType for complex UI synchronization.
     */
    @FunctionalInterface
    interface MenuFactory<T extends AbstractContainerMenu> {
        /**
         * Create a menu instance with custom data from the server.
         * 
         * @param containerId Container/window ID for the menu
         * @param playerInventory Player's inventory
         * @param data Custom data buffer containing serialized information
         * @return Created menu instance
         */
        T create(int containerId, Inventory playerInventory, FriendlyByteBuf data);
    }
    
    /**
     * Functional interface for simple menu factories without data transfer.
     * Used with createSimpleMenuType for basic menus.
     */
    @FunctionalInterface
    interface SimpleMenuFactory<T extends AbstractContainerMenu> {
        /**
         * Create a simple menu instance.
         * 
         * @param containerId Container/window ID for the menu
         * @param playerInventory Player's inventory
         * @return Created menu instance
         */
        T create(int containerId, Inventory playerInventory);
    }
    
    /**
     * Helper class for common menu data operations.
     * Provides utilities for serializing/deserializing common data types used in BusinessCraft menus.
     */
    class MenuDataHelper {
        
        /**
         * Write a BlockPos to the data buffer.
         * Used for passing town block positions to menus.
         * 
         * @param buffer Data buffer to write to
         * @param pos BlockPos to serialize
         */
        public static void writeBlockPos(FriendlyByteBuf buffer, BlockPos pos) {
            buffer.writeBlockPos(pos);
        }
        
        /**
         * Read a BlockPos from the data buffer.
         * Used for receiving town block positions in menus.
         * 
         * @param buffer Data buffer to read from
         * @return Deserialized BlockPos
         */
        public static BlockPos readBlockPos(FriendlyByteBuf buffer) {
            return buffer.readBlockPos();
        }
        
        /**
         * Write a string to the data buffer with length prefix.
         * Used for passing town names and other string data.
         * 
         * @param buffer Data buffer to write to
         * @param str String to serialize
         */
        public static void writeString(FriendlyByteBuf buffer, String str) {
            buffer.writeUtf(str);
        }
        
        /**
         * Read a string from the data buffer.
         * Used for receiving town names and other string data.
         * 
         * @param buffer Data buffer to read from
         * @return Deserialized string
         */
        public static String readString(FriendlyByteBuf buffer) {
            return buffer.readUtf();
        }
        
        /**
         * Write an integer to the data buffer.
         * Used for passing numeric data like search radius, population, etc.
         * 
         * @param buffer Data buffer to write to
         * @param value Integer value to serialize
         */
        public static void writeInt(FriendlyByteBuf buffer, int value) {
            buffer.writeInt(value);
        }
        
        /**
         * Read an integer from the data buffer.
         * Used for receiving numeric data.
         * 
         * @param buffer Data buffer to read from
         * @return Deserialized integer
         */
        public static int readInt(FriendlyByteBuf buffer) {
            return buffer.readInt();
        }
        
        /**
         * Write a boolean to the data buffer.
         * Used for passing flag data like tourist spawning enabled.
         * 
         * @param buffer Data buffer to write to
         * @param value Boolean value to serialize
         */
        public static void writeBoolean(FriendlyByteBuf buffer, boolean value) {
            buffer.writeBoolean(value);
        }
        
        /**
         * Read a boolean from the data buffer.
         * Used for receiving flag data.
         * 
         * @param buffer Data buffer to read from
         * @return Deserialized boolean
         */
        public static boolean readBoolean(FriendlyByteBuf buffer) {
            return buffer.readBoolean();
        }
    }
}