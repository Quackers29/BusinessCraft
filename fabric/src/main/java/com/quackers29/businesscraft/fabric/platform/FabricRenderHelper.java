package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.RenderHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fabric implementation of RenderHelper
 * Uses reflection to avoid compile-time dependency on GuiGraphics
 * Note: Method signatures match the interface but use Object internally
 */
public class FabricRenderHelper implements RenderHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricRenderHelper.class);
    
    private final Map<String, OverlayRenderer> registeredOverlays = new ConcurrentHashMap<>();
    private final Map<String, WorldRenderCallback> worldRenderCallbacks = new ConcurrentHashMap<>();
    
    // Cache reflected classes for performance - lazy initialization
    private static Class<?> guiGraphicsClass;
    private static boolean initialized = false;
    
    /**
     * Lazy initialization of reflection classes
     */
    private static synchronized void ensureInitialized() {
        if (initialized) return;
        
        try {
            ClassLoader classLoader = FabricRenderHelper.class.getClassLoader();
            guiGraphicsClass = classLoader.loadClass("net.minecraft.client.gui.GuiGraphics");
            initialized = true;
        } catch (Exception e) {
            LOGGER.warn("Could not load GuiGraphics class - overlay rendering will be disabled", e);
            initialized = false;
        }
    }
    
    @Override
    public void renderOverlay(Object guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        // This method is not used directly - overlays are registered with Fabric's InGameHud
        // The actual rendering happens via Fabric's overlay registration system
        // Cast handled internally via reflection
    }
    
    @Override
    public void registerOverlay(String overlayId, OverlayRenderer overlay) {
        ensureInitialized();
        registeredOverlays.put(overlayId, overlay);
        // Register with Fabric's InGameHud using reflection
        registerOverlayWithFabric(overlayId, overlay);
    }
    
    @Override
    public void unregisterOverlay(String overlayId) {
        registeredOverlays.remove(overlayId);
        // Unregister from Fabric's InGameHud using reflection
        unregisterOverlayFromFabric(overlayId);
    }
    
    @Override
    public void registerWorldRenderCallback(String renderStage, WorldRenderCallback callback) {
        worldRenderCallbacks.put(renderStage, callback);
        // Register with Fabric's WorldRenderEvents using reflection
        registerWorldRenderCallbackWithFabric(renderStage, callback);
    }
    
    @Override
    public Object getPoseStack(Object renderEvent) {
        try {
            // Fabric's render event has a poseStack() method
            java.lang.reflect.Method getPoseStackMethod = renderEvent.getClass().getMethod("poseStack");
            return getPoseStackMethod.invoke(renderEvent);
        } catch (Exception e) {
            LOGGER.debug("Could not get pose stack from render event", e);
            return null;
        }
    }
    
    @Override
    public Object getCamera(Object renderEvent) {
        try {
            // Fabric's render event has a camera() method
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
            // Fabric's render event has a tickDelta() method
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
            // Fabric's render event has a stage() method that returns a Stage enum
            java.lang.reflect.Method getStageMethod = renderEvent.getClass().getMethod("stage");
            Object stageEnum = getStageMethod.invoke(renderEvent);
            
            // Convert enum to string and normalize
            String stageStr = stageEnum.toString();
            // Remove namespace prefix if present and convert to uppercase
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
        String eventStage = getRenderStage(renderEvent);
        return eventStage.equals(stageName.toUpperCase());
    }
    
    /**
     * Register overlay with Fabric's InGameHud system using reflection
     */
    private void registerOverlayWithFabric(String overlayId, OverlayRenderer overlay) {
        if (!initialized) {
            LOGGER.warn("GuiGraphics not available - cannot register overlay: {}", overlayId);
            return;
        }
        
        try {
            ClassLoader classLoader = FabricRenderHelper.class.getClassLoader();
            // Fabric uses HudRenderCallback for overlays
            Class<?> hudRenderCallbackClass = classLoader.loadClass("net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback");
            
            // Register the overlay callback
            java.lang.reflect.Method registerMethod = hudRenderCallbackClass.getMethod("register", hudRenderCallbackClass);
            
            // Create a callback that wraps our overlay renderer
            Object fabricCallback = java.lang.reflect.Proxy.newProxyInstance(
                classLoader,
                new Class[]{hudRenderCallbackClass},
                (proxy, method, args) -> {
                    if (method.getName().equals("onHudRender")) {
                        // args[0] = guiGraphics (Object), args[1] = tickDelta (float)
                        Object guiGraphics = args[0];
                        float tickDelta = ((Number) args[1]).floatValue();
                        
                        // Get screen dimensions using reflection
                        Object minecraft = getMinecraftInstance();
                        if (minecraft != null) {
                            java.lang.reflect.Method getWindowMethod = minecraft.getClass().getMethod("getWindow");
                            Object window = getWindowMethod.invoke(minecraft);
                            java.lang.reflect.Method getGuiScaledWidthMethod = window.getClass().getMethod("getGuiScaledWidth");
                            java.lang.reflect.Method getGuiScaledHeightMethod = window.getClass().getMethod("getGuiScaledHeight");
                            int screenWidth = (Integer) getGuiScaledWidthMethod.invoke(window);
                            int screenHeight = (Integer) getGuiScaledHeightMethod.invoke(window);
                            
                            // Cast guiGraphics and call overlay renderer directly (OverlayRenderer now accepts Object)
                            if (guiGraphicsClass.isInstance(guiGraphics)) {
                                overlay.render(guiGraphics, tickDelta, screenWidth, screenHeight);
                            }
                        }
                        return null;
                    }
                    return null;
                }
            );
            
            registerMethod.invoke(null, fabricCallback);
            LOGGER.debug("Registered overlay with Fabric: {}", overlayId);
        } catch (Exception e) {
            LOGGER.error("Error registering overlay with Fabric: {}", overlayId, e);
        }
    }
    
    /**
     * Unregister overlay from Fabric (note: Fabric doesn't have direct unregister, but we track it)
     */
    private void unregisterOverlayFromFabric(String overlayId) {
        // Fabric doesn't have a direct unregister method for HudRenderCallback
        // We just remove it from our tracking map
        LOGGER.debug("Unregistered overlay (tracking only): {}", overlayId);
    }
    
    /**
     * Register world render callback with Fabric's WorldRenderEvents using reflection
     */
    private void registerWorldRenderCallbackWithFabric(String renderStage, WorldRenderCallback callback) {
        try {
            ClassLoader classLoader = FabricRenderHelper.class.getClassLoader();
            Class<?> worldRenderEventsClass = classLoader.loadClass("net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents");
            
            // Get the appropriate stage event (e.g., AFTER_TRANSLUCENT)
            java.lang.reflect.Method getStageMethod = getStageMethodForFabric(worldRenderEventsClass, renderStage);
            if (getStageMethod == null) {
                LOGGER.warn("Could not find Fabric render stage for: {}", renderStage);
                return;
            }
            
            // Get the callback interface
            Class<?> worldRenderContextClass = classLoader.loadClass("net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext");
            
            // Register the callback
            Object fabricCallback = java.lang.reflect.Proxy.newProxyInstance(
                classLoader,
                new Class[]{getCallbackInterface(worldRenderEventsClass)},
                (proxy, method, args) -> {
                    if (method.getName().equals("onWorldRender")) {
                        Object context = args[0];
                        // Invoke our callback with the normalized stage name
                        callback.onRender(renderStage, getPartialTick(context), context);
                        return null;
                    }
                    return null;
                }
            );
            
            getStageMethod.invoke(null, fabricCallback);
            LOGGER.debug("Registered world render callback for stage: {}", renderStage);
        } catch (Exception e) {
            LOGGER.error("Error registering world render callback for stage: {}", renderStage, e);
        }
    }
    
    /**
     * Get the appropriate Fabric render stage method
     */
    private java.lang.reflect.Method getStageMethodForFabric(Class<?> worldRenderEventsClass, String renderStage) {
        try {
            // Map our stage names to Fabric's method names
            String methodName = mapStageToFabricMethod(renderStage);
            if (methodName == null) return null;
            
            Class<?> callbackInterface = getCallbackInterface(worldRenderEventsClass);
            return worldRenderEventsClass.getMethod(methodName, callbackInterface);
        } catch (Exception e) {
            LOGGER.debug("Could not find Fabric render stage method for: {}", renderStage, e);
            return null;
        }
    }
    
    /**
     * Map our platform-agnostic stage names to Fabric's method names
     */
    private String mapStageToFabricMethod(String renderStage) {
        // Fabric uses methods like AFTER_TRANSLUCENT, AFTER_ENTITIES, etc.
        switch (renderStage.toUpperCase()) {
            case "AFTER_TRANSLUCENT_BLOCKS":
                return "afterTranslucent";
            case "AFTER_ENTITIES":
                return "afterEntities";
            case "AFTER_PARTICLES":
                return "afterParticles";
            case "AFTER_WEATHER":
                return "afterWeather";
            case "AFTER_SKY":
                return "afterSky";
            case "AFTER_SOLID_BLOCKS":
                return "afterSolid";
            default:
                return null;
        }
    }
    
    /**
     * Get the callback interface from WorldRenderEvents
     */
    private Class<?> getCallbackInterface(Class<?> worldRenderEventsClass) {
        try {
            // Fabric's callback interfaces are typically inner classes
            for (Class<?> innerClass : worldRenderEventsClass.getClasses()) {
                if (innerClass.getSimpleName().contains("Callback") || innerClass.getSimpleName().contains("After")) {
                    return innerClass;
                }
            }
            // Fallback: try to find by name pattern
            return Class.forName("net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents$AfterTranslucent");
        } catch (Exception e) {
            LOGGER.error("Could not find Fabric render callback interface", e);
            return null;
        }
    }
    
    /**
     * Get Minecraft instance using reflection
     */
    private Object getMinecraftInstance() {
        try {
            ClassLoader classLoader = FabricRenderHelper.class.getClassLoader();
            Class<?> minecraftClientClass = classLoader.loadClass("net.minecraft.client.MinecraftClient");
            java.lang.reflect.Method getInstanceMethod = minecraftClientClass.getMethod("getInstance");
            return getInstanceMethod.invoke(null);
        } catch (Exception e) {
            LOGGER.debug("Could not get Minecraft instance", e);
            return null;
        }
    }
    
    /**
     * Get registered overlays (for debugging)
     */
    public Map<String, OverlayRenderer> getRegisteredOverlays() {
        return registeredOverlays;
    }
}
