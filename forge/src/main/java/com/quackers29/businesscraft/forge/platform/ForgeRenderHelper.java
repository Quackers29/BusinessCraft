package com.quackers29.businesscraft.forge.platform;

import com.quackers29.businesscraft.api.RenderHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forge implementation of RenderHelper
 */
public class ForgeRenderHelper implements RenderHelper {
    private final Map<String, OverlayRenderer> registeredOverlays = new ConcurrentHashMap<>();
    
    @Override
    public void renderOverlay(Object guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        // This method is not used directly - overlays are registered with ForgeGui
        // Cast handled by Forge implementation
    }
    
    @Override
    public void registerOverlay(String overlayId, OverlayRenderer overlay) {
        registeredOverlays.put(overlayId, overlay);
        // Overlay will be rendered during RenderGameOverlayEvent
    }
    
    @Override
    public void unregisterOverlay(String overlayId) {
        registeredOverlays.remove(overlayId);
        // Note: Forge doesn't have a direct unregister method, but we can track it
    }
    
    @Override
    public void registerWorldRenderCallback(String renderStage, WorldRenderCallback callback) {
        // Register with Forge's event system via the static handler
        ForgeWorldRenderHandler.registerCallback(renderStage, callback);
    }
    
    @Override
    public Object getPoseStack(Object renderEvent) {
        if (renderEvent instanceof RenderLevelStageEvent event) {
            return event.getPoseStack();
        }
        return null;
    }
    
    @Override
    public Object getCamera(Object renderEvent) {
        if (renderEvent instanceof RenderLevelStageEvent event) {
            return event.getCamera();
        }
        return null;
    }
    
    @Override
    public float getPartialTick(Object renderEvent) {
        if (renderEvent instanceof RenderLevelStageEvent event) {
            return event.getPartialTick();
        }
        return 0.0f;
    }
    
    @Override
    public String getRenderStage(Object renderEvent) {
        if (renderEvent instanceof RenderLevelStageEvent event) {
            // Convert Forge's Stage enum to our platform-agnostic string constant
            // Forge returns "minecraft:after_translucent_blocks", we need "AFTER_TRANSLUCENT_BLOCKS"
            String stageStr = event.getStage().toString();
            // Remove "minecraft:" prefix if present and convert to uppercase
            if (stageStr.startsWith("minecraft:")) {
                stageStr = stageStr.substring("minecraft:".length());
            }
            // Convert to uppercase and replace underscores to match our constants
            return stageStr.toUpperCase().replace("_", "_");
        }
        return "";
    }
    
    @Override
    public boolean isRenderStage(Object renderEvent, String stageName) {
        if (renderEvent instanceof RenderLevelStageEvent event) {
            // Normalize both stage names for comparison
            String eventStage = event.getStage().toString();
            if (eventStage.startsWith("minecraft:")) {
                eventStage = eventStage.substring("minecraft:".length());
            }
            eventStage = eventStage.toUpperCase();
            
            // Compare with normalized stage name
            return eventStage.equals(stageName.toUpperCase());
        }
        return false;
    }
    
    /**
     * Get the map of registered overlays (for use by event handler)
     */
    public Map<String, OverlayRenderer> getRegisteredOverlays() {
        return registeredOverlays;
    }
    
    /**
     * Static handler for world render events
     */
    @Mod.EventBusSubscriber(modid = "businesscraft", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeWorldRenderHandler {
        private static final Map<String, WorldRenderCallback> callbacks = new ConcurrentHashMap<>();
        
        @SubscribeEvent
        public static void onRenderLevelStage(RenderLevelStageEvent event) {
            String stageName = event.getStage().toString();
            float partialTick = event.getPartialTick();
            
            // Invoke all registered callbacks for this stage
            WorldRenderCallback callback = callbacks.get(stageName);
            if (callback != null) {
                callback.onRender(stageName, partialTick, event);
            }
        }
        
        public static void registerCallback(String renderStage, WorldRenderCallback callback) {
            callbacks.put(renderStage, callback);
        }
    }
    
    /**
     * Static handler for registering overlays
     */
    @Mod.EventBusSubscriber(modid = "businesscraft", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ForgeOverlayRegistry {
        private static ForgeRenderHelper renderHelper;
        private static final Set<String> registeredOverlayIds = new HashSet<>();
        
        public static void setRenderHelper(ForgeRenderHelper helper) {
            renderHelper = helper;
        }
        
        @SubscribeEvent
        public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
            if (renderHelper != null) {
                Map<String, OverlayRenderer> overlays = renderHelper.getRegisteredOverlays();
                for (Map.Entry<String, OverlayRenderer> entry : overlays.entrySet()) {
                    String overlayId = "businesscraft:" + entry.getKey();
                    event.registerAboveAll(overlayId, (gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
                        entry.getValue().render(guiGraphics, partialTick, screenWidth, screenHeight);
                    });
                    registeredOverlayIds.add(entry.getKey()); // Mark as registered
                }
            }
        }
        
        public static boolean isOverlayRegistered(String overlayId) {
            return registeredOverlayIds.contains(overlayId);
        }
    }
}

