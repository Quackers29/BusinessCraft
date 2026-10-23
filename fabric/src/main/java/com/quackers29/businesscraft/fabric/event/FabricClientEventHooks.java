package com.quackers29.businesscraft.fabric.event;

import com.quackers29.businesscraft.api.EventCallbacks;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.client.showcase.Showcase;
import com.quackers29.businesscraft.network.packets.ui.OpenTownInterfacePacket;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-only Fabric event wiring. Kept out of {@link FabricEventCallbackHandler}
 * so a dedicated server never loads client classes.
 */
public final class FabricClientEventHooks {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricClientEventHooks.class);
    private static final Map<Integer, Boolean> previousKeyStates = new HashMap<>();
    private static boolean scrollCallbackRegistered;

    private FabricClientEventHooks() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Showcase.tick();
            for (EventCallbacks.ClientTickCallback callback : FabricEventCallbackHandler.clientTickCallbacks) {
                callback.onClientTick();
            }

            if (!FabricEventCallbackHandler.keyInputCallbacks.isEmpty() && client.getWindow() != null) {
                long windowHandle = client.getWindow().getWindow();
                int keyCode = GLFW.GLFW_KEY_F4;
                int currentState = GLFW.glfwGetKey(windowHandle, keyCode);
                boolean previousState = previousKeyStates.getOrDefault(keyCode, false);
                if (currentState == GLFW.GLFW_PRESS && !previousState) {
                    for (EventCallbacks.KeyInputCallback callback : FabricEventCallbackHandler.keyInputCallbacks) {
                        if (callback.onKeyInput(keyCode, GLFW.GLFW_PRESS)) {
                            break;
                        }
                    }
                }
                previousKeyStates.put(keyCode, currentState == GLFW.GLFW_PRESS);
            }
        });

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!scrollCallbackRegistered && client.getWindow() != null) {
                long windowHandle = client.getWindow().getWindow();
                GLFW.glfwSetScrollCallback(windowHandle, (window, xOffset, yOffset) -> {
                    for (EventCallbacks.MouseScrollCallback callback : FabricEventCallbackHandler.mouseScrollCallbacks) {
                        if (callback.onMouseScroll(yOffset)) {
                            break;
                        }
                    }
                });
                scrollCallbackRegistered = true;
                LOGGER.debug("Registered GLFW scroll callback");
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (client.level != null) {
                for (EventCallbacks.LevelUnloadCallback callback : FabricEventCallbackHandler.levelUnloadCallbacks) {
                    callback.onLevelUnload(client.level);
                }
            }
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            BlockPos pos = hitResult.getBlockPos();
            if (world.getBlockState(pos).getBlock() instanceof com.quackers29.businesscraft.block.TownInterfaceBlock) {
                PlatformAccess.getNetworkMessages().sendToServer(new OpenTownInterfacePacket(pos));
            }
            return InteractionResult.PASS;
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            for (EventCallbacks.RenderLevelCallback callback : FabricEventCallbackHandler.renderLevelCallbacks) {
                callback.onRenderLevel("AFTER_TRANSLUCENT_BLOCKS", context.tickDelta(), context);
            }
        });
    }
}
