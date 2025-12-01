package com.quackers29.businesscraft.fabric.event;

import com.quackers29.businesscraft.api.EventCallbacks;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.fabric.FabricModMessages;
import com.quackers29.businesscraft.network.packets.ui.OpenTownInterfacePacket;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.event.TownEventHandler;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Fabric event handler wrapper that invokes platform-agnostic callbacks.
 * This class bridges Fabric events to common module callbacks.
 */
public class FabricEventCallbackHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricEventCallbackHandler.class);

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

    // Key state tracking for key input callbacks
    private static final Map<Integer, Boolean> previousKeyStates = new HashMap<>();

    /**
     * Check if we're running on a server (as opposed to client-only)
     */
    private static boolean isServerSide() {
        return true;
    }

    /**
     * Initialize Fabric event handlers
     */
    public static void initialize() {
        LOGGER.info("Initializing Fabric event callback handlers...");

        try {
            registerServerEvents();
            LOGGER.info("Fabric event callback handlers initialized successfully");
        } catch (Exception e) {
            LOGGER.warn("Error initializing Fabric event callback handlers", e);
        }
    }

    /**
     * Register server-side events using Fabric's event API
     */
    private static void registerServerEvents() {
        // Player Tick
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                for (EventCallbacks.PlayerTickCallback callback : playerTickCallbacks) {
                    callback.onPlayerTick(player, player.serverLevel(), player.blockPosition());
                }
            }
        });

        // Player Login
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            for (EventCallbacks.PlayerLoginCallback callback : playerLoginCallbacks) {
                callback.onPlayerLogin(player, player.serverLevel(), player.blockPosition());
            }
        });

        // Player Respawn
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            for (EventCallbacks.PlayerLoginCallback callback : playerLoginCallbacks) {
                callback.onPlayerLogin(newPlayer, newPlayer.serverLevel(), newPlayer.blockPosition());
            }
        });

        // Player Logout
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            for (EventCallbacks.PlayerLogoutCallback callback : playerLogoutCallbacks) {
                callback.onPlayerLogout(player);
            }
        });

        // Level Unload
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            for (EventCallbacks.LevelUnloadCallback callback : levelUnloadCallbacks) {
                callback.onLevelUnload(world);
            }
        });

        // Right Click Block
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND)
                return InteractionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();

            // Path creation mode logic
            if (TownEventHandler.onRightClickBlock(player, world, pos)) {
                return InteractionResult.FAIL;
            }

            // Call registered callbacks
            for (EventCallbacks.RightClickBlockCallback callback : rightClickBlockCallbacks) {
                if (callback.onRightClickBlock(player, world, pos)) {
                    return InteractionResult.FAIL;
                }
            }

            return InteractionResult.PASS;
        });

        // Register Commands
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT
                .register((dispatcher, registryAccess, environment) -> {
                    com.quackers29.businesscraft.command.ClearTownsCommand.register(dispatcher);
                    com.quackers29.businesscraft.command.AddContractCommand.register(dispatcher);
                });
    }

    /**
     * Register client-side events (called from FabricClientSetup)
     */
    public static void registerClientEvents() {
        // Client Tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            for (EventCallbacks.ClientTickCallback callback : clientTickCallbacks) {
                callback.onClientTick();
            }

            // Key Input handling (simulated via tick)
            if (!keyInputCallbacks.isEmpty() && client.getWindow() != null) {
                long windowHandle = client.getWindow().getWindow();

                // Check F4 (debug)
                int keyCode = GLFW.GLFW_KEY_F4;
                int currentState = GLFW.glfwGetKey(windowHandle, keyCode);
                boolean previousState = previousKeyStates.getOrDefault(keyCode, false);

                if (currentState == GLFW.GLFW_PRESS && !previousState) {
                    for (EventCallbacks.KeyInputCallback callback : keyInputCallbacks) {
                        if (callback.onKeyInput(keyCode, GLFW.GLFW_PRESS)) {
                            break;
                        }
                    }
                }
                previousKeyStates.put(keyCode, currentState == GLFW.GLFW_PRESS);
            }
        });

        // Client World Unload (via Disconnect)
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (client.level != null) {
                for (EventCallbacks.LevelUnloadCallback callback : levelUnloadCallbacks) {
                    callback.onLevelUnload(client.level);
                }
            }
        });

        // UseBlockCallback for menu opening (Client side check to send packet)
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND)
                return InteractionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            if (world.getBlockState(pos).getBlock() instanceof com.quackers29.businesscraft.block.TownInterfaceBlock) {
                LOGGER.info("Right-clicked on town interface block - opening menu");
                PlatformAccess.getNetworkMessages().sendToServer(new OpenTownInterfacePacket(pos));
                return InteractionResult.PASS; // Allow interaction to proceed (or should we consume it?)
            }
            return InteractionResult.PASS;
        });

        // World Render Event (After Translucent) - Critical for platform/boundary
        // visualization
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            for (EventCallbacks.RenderLevelCallback callback : renderLevelCallbacks) {
                // Pass "AFTER_TRANSLUCENT_BLOCKS" to match the common RenderHelper.RenderStage
                // constant
                callback.onRenderLevel("AFTER_TRANSLUCENT_BLOCKS", context.tickDelta(), context);
            }
        });
    }

    // Registration methods
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
