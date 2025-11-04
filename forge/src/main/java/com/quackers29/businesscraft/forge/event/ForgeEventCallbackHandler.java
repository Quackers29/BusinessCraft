package com.quackers29.businesscraft.forge.event;

import com.quackers29.businesscraft.api.EventCallbacks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Forge event handler wrapper that invokes platform-agnostic callbacks.
 * This class bridges Forge events to common module callbacks.
 */
@Mod.EventBusSubscriber(modid = "businesscraft", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEventCallbackHandler {
    
    // Server-side callbacks
    private static final List<EventCallbacks.PlayerTickCallback> playerTickCallbacks = new ArrayList<>();
    private static final List<EventCallbacks.PlayerLoginCallback> playerLoginCallbacks = new ArrayList<>();
    private static final List<EventCallbacks.PlayerLogoutCallback> playerLogoutCallbacks = new ArrayList<>();
    private static final List<EventCallbacks.RightClickBlockCallback> rightClickBlockCallbacks = new ArrayList<>();
    
    // Client-side callbacks
    private static final List<EventCallbacks.ClientTickCallback> clientTickCallbacks = new ArrayList<>();
    private static final List<EventCallbacks.KeyInputCallback> keyInputCallbacks = new ArrayList<>();
    private static final List<EventCallbacks.MouseScrollCallback> mouseScrollCallbacks = new ArrayList<>();
    private static final List<EventCallbacks.RenderLevelCallback> renderLevelCallbacks = new ArrayList<>();
    private static final List<EventCallbacks.LevelUnloadCallback> levelUnloadCallbacks = new ArrayList<>();
    
    // Server-side event handlers
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isClient() || event.phase != TickEvent.Phase.END) {
            return;
        }
        
        if (event.player instanceof ServerPlayer serverPlayer) {
            net.minecraft.server.level.ServerLevel serverLevel = serverPlayer.serverLevel();
            BlockPos position = serverPlayer.blockPosition();
            
            for (EventCallbacks.PlayerTickCallback callback : playerTickCallbacks) {
                callback.onPlayerTick(serverPlayer, serverLevel, position);
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            net.minecraft.server.level.ServerLevel serverLevel = serverPlayer.serverLevel();
            BlockPos position = serverPlayer.blockPosition();
            
            for (EventCallbacks.PlayerLoginCallback callback : playerLoginCallbacks) {
                callback.onPlayerLogin(serverPlayer, serverLevel, position);
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            
            for (EventCallbacks.PlayerLogoutCallback callback : playerLogoutCallbacks) {
                callback.onPlayerLogout(serverPlayer);
            }
        }
    }
    
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos clickedPos = event.getPos();
        
        for (EventCallbacks.RightClickBlockCallback callback : rightClickBlockCallbacks) {
            if (callback.onRightClickBlock(player, level, clickedPos)) {
                event.setCanceled(true);
                break; // If one callback cancels, cancel the event
            }
        }
    }
    
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            for (EventCallbacks.LevelUnloadCallback callback : levelUnloadCallbacks) {
                callback.onLevelUnload(level);
            }
        }
    }
    
    // Client-side event handlers
    @Mod.EventBusSubscriber(modid = "businesscraft", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class Client {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                for (EventCallbacks.ClientTickCallback callback : clientTickCallbacks) {
                    callback.onClientTick();
                }
            }
        }
        
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            for (EventCallbacks.KeyInputCallback callback : keyInputCallbacks) {
                if (callback.onKeyInput(event.getKey(), event.getAction())) {
                    event.setCanceled(true);
                    break;
                }
            }
        }
        
        @SubscribeEvent
        public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
            for (EventCallbacks.MouseScrollCallback callback : mouseScrollCallbacks) {
                if (callback.onMouseScroll(event.getScrollDelta())) {
                    event.setCanceled(true);
                    break;
                }
            }
        }
        
        @SubscribeEvent
        public static void onRenderLevelStage(RenderLevelStageEvent event) {
            String stageName = event.getStage().toString();
            float partialTick = event.getPartialTick();
            
            // Invoke all registered callbacks for this stage
            for (EventCallbacks.RenderLevelCallback callback : renderLevelCallbacks) {
                callback.onRenderLevel(stageName, partialTick, event);
            }
        }
    }
    
    // Registration methods (called by ForgeEventHelper)
    public static void registerPlayerTickCallback(EventCallbacks.PlayerTickCallback callback) {
        playerTickCallbacks.add(callback);
    }
    
    public static void registerPlayerLoginCallback(EventCallbacks.PlayerLoginCallback callback) {
        playerLoginCallbacks.add(callback);
    }
    
    public static void registerPlayerLogoutCallback(EventCallbacks.PlayerLogoutCallback callback) {
        playerLogoutCallbacks.add(callback);
    }
    
    public static void registerRightClickBlockCallback(EventCallbacks.RightClickBlockCallback callback) {
        rightClickBlockCallbacks.add(callback);
    }
    
    public static void registerClientTickCallback(EventCallbacks.ClientTickCallback callback) {
        clientTickCallbacks.add(callback);
    }
    
    public static void registerKeyInputCallback(EventCallbacks.KeyInputCallback callback) {
        keyInputCallbacks.add(callback);
    }
    
    public static void registerMouseScrollCallback(EventCallbacks.MouseScrollCallback callback) {
        mouseScrollCallbacks.add(callback);
    }
    
    public static void registerRenderLevelCallback(EventCallbacks.RenderLevelCallback callback) {
        renderLevelCallbacks.add(callback);
    }
    
    public static void registerLevelUnloadCallback(EventCallbacks.LevelUnloadCallback callback) {
        levelUnloadCallbacks.add(callback);
    }
}

