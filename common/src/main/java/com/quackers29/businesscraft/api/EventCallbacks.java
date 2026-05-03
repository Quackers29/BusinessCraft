package com.quackers29.businesscraft.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class EventCallbacks {
    @FunctionalInterface
    public interface PlayerTickCallback {
        void onPlayerTick(ServerPlayer player, ServerLevel level, BlockPos position);
    }

    @FunctionalInterface
    public interface PlayerLoginCallback {
        void onPlayerLogin(ServerPlayer player, ServerLevel level, BlockPos position);
    }

    @FunctionalInterface
    public interface PlayerLogoutCallback {
        void onPlayerLogout(ServerPlayer player);
    }

    @FunctionalInterface
    public interface RightClickBlockCallback {
        boolean onRightClickBlock(Player player, Level level, BlockPos clickedPos);
    }

    @FunctionalInterface
    public interface ClientTickCallback {
        void onClientTick();
    }

    @FunctionalInterface
    public interface KeyInputCallback {
        boolean onKeyInput(int keyCode, int action);
    }

    @FunctionalInterface
    public interface MouseScrollCallback {
        boolean onMouseScroll(double scrollDelta);
    }

    @FunctionalInterface
    public interface RenderLevelCallback {
        void onRenderLevel(String renderStage, float partialTick, Object eventObject);
    }

    @FunctionalInterface
    public interface LevelUnloadCallback {
        void onLevelUnload(Level level);
    }
}
