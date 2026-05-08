package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.RenderHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gui.GuiGraphics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fabric implementation of RenderHelper
 * Uses Fabric API directly for overlay and world rendering
 */
public class FabricRenderHelper implements RenderHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricRenderHelper.class);

    private final Map<String, OverlayRenderer> registeredOverlays = new ConcurrentHashMap<>();
    private final Map<String, WorldRenderCallback> worldRenderCallbacks = new ConcurrentHashMap<>();
    private static boolean hudCallbackRegistered = false;

    @Override
    public void renderOverlay(Object guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        // Not used - overlays are registered with Fabric's HudRenderCallback
    }

    @Override
    public void registerOverlay(String overlayId, OverlayRenderer overlay) {
        registeredOverlays.put(overlayId, overlay);

        // Register HUD callback once for all overlays
        if (!hudCallbackRegistered) {
            HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
                int screenWidth = guiGraphics.guiWidth();
                int screenHeight = guiGraphics.guiHeight();

                // Render all registered overlays
                for (Map.Entry<String, OverlayRenderer> entry : registeredOverlays.entrySet()) {
                    entry.getValue().render(guiGraphics, tickDelta, screenWidth, screenHeight);
                }
            });
            hudCallbackRegistered = true;
            LOGGER.debug("Registered Fabric HUD render callback for custom overlays");
        }

        LOGGER.debug("Registered overlay: {}", overlayId);
    }

    @Override
    public void unregisterOverlay(String overlayId) {
        registeredOverlays.remove(overlayId);
        LOGGER.debug("Unregistered overlay: {}", overlayId);
    }

    @Override
    public void registerWorldRenderCallback(String renderStage, WorldRenderCallback callback) {
        worldRenderCallbacks.put(renderStage, callback);

        // Register with Fabric's WorldRenderEvents directly
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            callback.onRender(renderStage, context.tickDelta(), context);
        });

        LOGGER.debug("Registered world render callback for stage: {}", renderStage);
    }

    @Override
    public Object getPoseStack(Object renderEvent) {
        try {
            // Fabric uses matrixStack() while Forge uses poseStack()
            java.lang.reflect.Method getMatrixStackMethod = renderEvent.getClass().getMethod("matrixStack");
            return getMatrixStackMethod.invoke(renderEvent);
        } catch (Exception e) {
            LOGGER.error("Could not get matrix stack from render event", e);
            return null;
        }
    }

    @Override
    public Object getCamera(Object renderEvent) {
        try {
            java.lang.reflect.Method getCameraMethod = renderEvent.getClass().getMethod("camera");
            return getCameraMethod.invoke(renderEvent);
        } catch (Exception e) {
            LOGGER.debug("Could not get camera from render event", e);
            return null;
        }
    }

    @Override
    public float getPartialTick(Object renderEvent) {
        try {
            java.lang.reflect.Method getTickDeltaMethod = renderEvent.getClass().getMethod("tickDelta");
            return ((Number) getTickDeltaMethod.invoke(renderEvent)).floatValue();
        } catch (Exception e) {
            LOGGER.debug("Could not get partial tick from render event", e);
            return 0.0f;
        }
    }

    @Override
    public String getRenderStage(Object renderEvent) {
        try {
            java.lang.reflect.Method getStageMethod = renderEvent.getClass().getMethod("stage");
            Object stageEnum = getStageMethod.invoke(renderEvent);

            String stageStr = stageEnum.toString();
            if (stageStr.contains(":")) {
                stageStr = stageStr.substring(stageStr.indexOf(":") + 1);
            }
            return stageStr.toUpperCase();
        } catch (Exception e) {
            LOGGER.debug("Could not get render stage from render event", e);
            return "";
        }
    }

    @Override
    public boolean isRenderStage(Object renderEvent, String stageName) {
        // Fabric callbacks are already stage-specific, so always return true
        return true;
    }

    /**
     * Get registered overlays (for debugging)
     */
    public Map<String, OverlayRenderer> getRegisteredOverlays() {
        return registeredOverlays;
    }
}
