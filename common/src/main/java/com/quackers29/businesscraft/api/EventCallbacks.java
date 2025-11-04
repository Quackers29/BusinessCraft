package com.quackers29.businesscraft.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Platform-agnostic event callback interfaces for common module event handlers.
 * These callbacks will be invoked by platform-specific event systems.
 */
public class EventCallbacks {
    
    /**
     * Callback for player tick events (server-side)
     */
    @FunctionalInterface
    public interface PlayerTickCallback {
        void onPlayerTick(ServerPlayer player, ServerLevel level, BlockPos position);
    }
    
    /**
     * Callback for player login events
     */
    @FunctionalInterface
    public interface PlayerLoginCallback {
        void onPlayerLogin(ServerPlayer player, ServerLevel level, BlockPos position);
    }
    
    /**
     * Callback for player logout events
     */
    @FunctionalInterface
    public interface PlayerLogoutCallback {
        void onPlayerLogout(ServerPlayer player);
    }
    
    /**
     * Callback for right-click block events (server-side)
     */
    @FunctionalInterface
    public interface RightClickBlockCallback {
        /**
         * @param player The player who clicked
         * @param level The level
         * @param clickedPos The position that was clicked
         * @return true if the event should be canceled
         */
        boolean onRightClickBlock(Player player, Level level, BlockPos clickedPos);
    }
    
    /**
     * Callback for client tick events
     */
    @FunctionalInterface
    public interface ClientTickCallback {
        void onClientTick();
    }
    
    /**
     * Callback for key input events
     */
    @FunctionalInterface
    public interface KeyInputCallback {
        /**
         * @param keyCode The GLFW key code
         * @param action The action (press/release)
         * @return true if the event should be canceled
         */
        boolean onKeyInput(int keyCode, int action);
    }
    
    /**
     * Callback for mouse scroll events
     */
    @FunctionalInterface
    public interface MouseScrollCallback {
        /**
         * @param scrollDelta The scroll delta
         * @return true if the event should be canceled
         */
        boolean onMouseScroll(double scrollDelta);
    }
    
    /**
     * Callback for level render events
     */
    @FunctionalInterface
    public interface RenderLevelCallback {
        /**
         * @param renderStage The render stage name
         * @param partialTick Partial tick for interpolation
         * @param eventObject The platform-specific render event object (cast to appropriate type in platform code)
         */
        void onRenderLevel(String renderStage, float partialTick, Object eventObject);
    }
    
    /**
     * Callback for level unload events
     */
    @FunctionalInterface
    public interface LevelUnloadCallback {
        void onLevelUnload(Level level);
    }
}
