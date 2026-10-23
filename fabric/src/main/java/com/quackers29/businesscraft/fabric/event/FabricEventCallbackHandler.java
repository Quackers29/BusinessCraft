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

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    // Client-side callbacks. Package-visible so FabricClientEventHooks can run them
    // without this server-loaded class referencing client types.
    static final List<EventCallbacks.ClientTickCallback> clientTickCallbacks = new ArrayList<>();
    static final List<EventCallbacks.KeyInputCallback> keyInputCallbacks = new ArrayList<>();
    static final List<EventCallbacks.MouseScrollCallback> mouseScrollCallbacks = new ArrayList<>();
    static final List<EventCallbacks.RenderLevelCallback> renderLevelCallbacks = new ArrayList<>();
    static final List<EventCallbacks.LevelUnloadCallback> levelUnloadCallbacks = new ArrayList<>();

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

                    com.quackers29.businesscraft.command.ClearContractsCommand.register(dispatcher);
                    com.quackers29.businesscraft.command.DumpTownDataCommand.register(dispatcher);
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
