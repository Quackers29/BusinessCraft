package com.quackers29.businesscraft.fabric.event;

import com.quackers29.businesscraft.api.EventCallbacks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
    
    /**
     * Initialize Fabric event handlers
     */
    public static void initialize() {
        LOGGER.info("Initializing Fabric event callback handlers...");
        
        try {
            // Register server-side events
            registerServerEvents();
            
            // Register client-side events (will be called from FabricClientSetup)
            // Client events are registered separately in FabricClientSetup
            
            LOGGER.info("Fabric event callback handlers initialized successfully");
        } catch (Exception e) {
            LOGGER.error("Error initializing Fabric event callback handlers", e);
            e.printStackTrace();
        }
    }
    
    /**
     * Register server-side events using Fabric's event API
     */
    private static void registerServerEvents() {
        try {
            ClassLoader classLoader = FabricEventCallbackHandler.class.getClassLoader();
            
            // Load Fabric event classes
            Class<?> serverTickEventsClass = classLoader.loadClass("net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents");
            Class<?> serverPlayerEventsClass = classLoader.loadClass("net.fabricmc.fabric.api.event.lifecycle.v1.ServerPlayerEvents");
            Class<?> serverWorldEventsClass = classLoader.loadClass("net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents");
            
            // Load Minecraft classes for type checking (but not for compile-time use)
            Class<?> serverPlayerClass = classLoader.loadClass("net.minecraft.server.level.ServerPlayer");
            Class<?> serverLevelClass = classLoader.loadClass("net.minecraft.server.level.ServerLevel");
            Class<?> levelClass = classLoader.loadClass("net.minecraft.world.level.Level");
            Class<?> playerClass = classLoader.loadClass("net.minecraft.world.entity.player.Player");
            Class<?> blockPosClass = classLoader.loadClass("net.minecraft.core.BlockPos");
            
            // Register player tick callback - END_SERVER_TICK is a static field
            java.lang.reflect.Field endTickField = serverTickEventsClass.getField("END_SERVER_TICK");
            Object endTickEvent = endTickField.get(null);
            java.lang.reflect.Method registerTickMethod = endTickEvent.getClass().getMethod("register", java.util.function.Consumer.class);
            registerTickMethod.invoke(endTickEvent, (java.util.function.Consumer<Object>) (server) -> {
                // Tick each player
                try {
                    java.lang.reflect.Method getPlayerListMethod = server.getClass().getMethod("getPlayerList");
                    Object playerList = getPlayerListMethod.invoke(server);
                    java.lang.reflect.Method getPlayersMethod = playerList.getClass().getMethod("getPlayers");
                    @SuppressWarnings("unchecked")
                    java.util.List<Object> players = (java.util.List<Object>) getPlayersMethod.invoke(playerList);
                    
                    for (Object player : players) {
                        // Use reflection to check if player is ServerPlayer and extract data
                        try {
                            if (serverPlayerClass.isInstance(player)) {
                                // Use reflection to get serverLevel and blockPosition
                                java.lang.reflect.Method getServerLevelMethod = serverPlayerClass.getMethod("serverLevel");
                                Object serverLevel = getServerLevelMethod.invoke(player);
                                
                                java.lang.reflect.Method getBlockPositionMethod = serverPlayerClass.getMethod("blockPosition");
                                Object position = getBlockPositionMethod.invoke(player);
                                
                                for (EventCallbacks.PlayerTickCallback callback : playerTickCallbacks) {
                                    // Use unchecked cast - objects are actually correct types from common module
                                    @SuppressWarnings("unchecked")
                                    com.quackers29.businesscraft.api.EventCallbacks.PlayerTickCallback cb = callback;
                                    // Cast Objects to expected types - they are actually ServerPlayer, ServerLevel, BlockPos
                                    Object castPlayer = serverPlayerClass.cast(player);
                                    Object castLevel = serverLevelClass.cast(serverLevel);
                                    Object castPos = blockPosClass.cast(position);
                                    // Invoke using reflection since we can't use types directly
                                    java.lang.reflect.Method method = callback.getClass().getMethod("onPlayerTick", 
                                        serverPlayerClass, serverLevelClass, blockPosClass);
                                    method.invoke(callback, castPlayer, castLevel, castPos);
                                }
                            }
                        } catch (Exception ex) {
                            LOGGER.error("Error in player tick handler", ex);
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.error("Error in player tick handler", ex);
                }
            });
            
            // Register player login callback - JOIN is a static field for initial login
            // Note: Fabric uses JOIN for initial login and AFTER_RESPAWN for respawn
            try {
                java.lang.reflect.Field joinField = serverPlayerEventsClass.getField("JOIN");
                Object joinEvent = joinField.get(null);
                java.lang.reflect.Method registerLoginMethod = joinEvent.getClass().getMethod("register", java.util.function.Consumer.class);
                registerLoginMethod.invoke(joinEvent, (java.util.function.Consumer<Object>) (player) -> {
                    try {
                        if (serverPlayerClass.isInstance(player)) {
                            java.lang.reflect.Method getServerLevelMethod = serverPlayerClass.getMethod("serverLevel");
                            Object serverLevel = getServerLevelMethod.invoke(player);
                            
                            java.lang.reflect.Method getBlockPositionMethod = serverPlayerClass.getMethod("blockPosition");
                            Object position = getBlockPositionMethod.invoke(player);
                            
                            for (EventCallbacks.PlayerLoginCallback callback : playerLoginCallbacks) {
                                // Use reflection to invoke callback - cast Objects to expected types
                                Object castPlayer = serverPlayerClass.cast(player);
                                Object castLevel = serverLevelClass.cast(serverLevel);
                                Object castPos = blockPosClass.cast(position);
                                java.lang.reflect.Method method = callback.getClass().getMethod("onPlayerLogin", 
                                    serverPlayerClass, serverLevelClass, blockPosClass);
                                method.invoke(callback, castPlayer, castLevel, castPos);
                            }
                        }
                    } catch (Exception ex) {
                        LOGGER.error("Error in player login handler", ex);
                    }
                });
            } catch (Exception e) {
                LOGGER.warn("Could not register player login event (JOIN), trying AFTER_RESPAWN", e);
                // Fallback to AFTER_RESPAWN if JOIN doesn't exist
                try {
                    java.lang.reflect.Field respawnField = serverPlayerEventsClass.getField("AFTER_RESPAWN");
                    Object respawnEvent = respawnField.get(null);
                    java.lang.reflect.Method registerRespawnMethod = respawnEvent.getClass().getMethod("register", java.util.function.BiConsumer.class);
                    registerRespawnMethod.invoke(respawnEvent, (java.util.function.BiConsumer<Object, Object>) (oldPlayer, newPlayer) -> {
                        try {
                            if (serverPlayerClass.isInstance(newPlayer)) {
                                java.lang.reflect.Method getServerLevelMethod = serverPlayerClass.getMethod("serverLevel");
                                Object serverLevel = getServerLevelMethod.invoke(newPlayer);
                                
                                java.lang.reflect.Method getBlockPositionMethod = serverPlayerClass.getMethod("blockPosition");
                                Object position = getBlockPositionMethod.invoke(newPlayer);
                                
                                for (EventCallbacks.PlayerLoginCallback callback : playerLoginCallbacks) {
                                    // Use reflection to invoke callback - cast Objects to expected types
                                    Object castPlayer = serverPlayerClass.cast(newPlayer);
                                    Object castLevel = serverLevelClass.cast(serverLevel);
                                    Object castPos = blockPosClass.cast(position);
                                    java.lang.reflect.Method method = callback.getClass().getMethod("onPlayerLogin", 
                                        serverPlayerClass, serverLevelClass, blockPosClass);
                                    method.invoke(callback, castPlayer, castLevel, castPos);
                                }
                            }
                        } catch (Exception ex) {
                            LOGGER.error("Error in player respawn handler", ex);
                        }
                    });
                } catch (Exception e2) {
                    LOGGER.error("Could not register player login event at all", e2);
                }
            }
            
            // Register player logout callback - DISCONNECT is a static field
            try {
                java.lang.reflect.Field disconnectField = serverPlayerEventsClass.getField("DISCONNECT");
                Object disconnectEvent = disconnectField.get(null);
                java.lang.reflect.Method registerDisconnectMethod = disconnectEvent.getClass().getMethod("register", java.util.function.Consumer.class);
                registerDisconnectMethod.invoke(disconnectEvent, (java.util.function.Consumer<Object>) (player) -> {
                    try {
                        if (serverPlayerClass.isInstance(player)) {
                            for (EventCallbacks.PlayerLogoutCallback callback : playerLogoutCallbacks) {
                                // Use reflection to invoke callback - cast Object to expected type
                                Object castPlayer = serverPlayerClass.cast(player);
                                java.lang.reflect.Method method = callback.getClass().getMethod("onPlayerLogout", serverPlayerClass);
                                method.invoke(callback, castPlayer);
                            }
                        }
                    } catch (Exception ex) {
                        LOGGER.error("Error in player logout handler", ex);
                    }
                });
            } catch (Exception e) {
                LOGGER.warn("Could not register player logout event", e);
            }
            
            // Register level unload callback - UNLOAD is a static field
            try {
                java.lang.reflect.Field unloadField = serverWorldEventsClass.getField("UNLOAD");
                Object unloadEvent = unloadField.get(null);
                java.lang.reflect.Method registerUnloadMethod = unloadEvent.getClass().getMethod("register", java.util.function.Consumer.class);
                registerUnloadMethod.invoke(unloadEvent, (java.util.function.Consumer<Object>) (world) -> {
                    try {
                        if (levelClass.isInstance(world)) {
                            for (EventCallbacks.LevelUnloadCallback callback : levelUnloadCallbacks) {
                                // Use reflection to invoke callback - cast Object to expected type
                                Object castWorld = levelClass.cast(world);
                                java.lang.reflect.Method method = callback.getClass().getMethod("onLevelUnload", levelClass);
                                method.invoke(callback, castWorld);
                            }
                        }
                    } catch (Exception ex) {
                        LOGGER.error("Error in level unload handler", ex);
                    }
                });
            } catch (Exception e) {
                LOGGER.warn("Could not register level unload event", e);
            }
            
            // Register right-click block callback
            // Fabric uses UseBlockCallback for this
            try {
                Class<?> useBlockCallbackClass = classLoader.loadClass("net.fabricmc.fabric.api.event.player.UseBlockCallback");
                java.lang.reflect.Field eventField = useBlockCallbackClass.getField("EVENT");
                Object useBlockEvent = eventField.get(null);
                java.lang.reflect.Method registerUseBlockMethod = useBlockEvent.getClass().getMethod("register", useBlockCallbackClass);
                
                Object callback = java.lang.reflect.Proxy.newProxyInstance(
                    classLoader,
                    new Class[]{useBlockCallbackClass},
                    (proxy, proxyMethod, args) -> {
                        if (proxyMethod.getName().equals("interact")) {
                            Object player = args[0];
                            Object world = args[1];
                            Object hand = args[2];
                            Object hitResult = args[3];
                            
                            try {
                                // Use the levelClass and playerClass already declared at method level
                                if (levelClass.isInstance(world) && playerClass.isInstance(player)) {
                                    // Extract BlockPos from hit result
                                    Object clickedPos = null;
                                    try {
                                        java.lang.reflect.Method getBlockPosMethod = hitResult.getClass().getMethod("getBlockPos");
                                        clickedPos = getBlockPosMethod.invoke(hitResult);
                                    } catch (Exception e) {
                                        LOGGER.error("Error extracting BlockPos from hit result", e);
                                    }
                                    
                                    // Check if server-side using reflection
                                    boolean isClientSide = false;
                                    try {
                                        java.lang.reflect.Method isClientSideMethod = levelClass.getMethod("isClientSide");
                                        isClientSide = (Boolean) isClientSideMethod.invoke(world);
                                    } catch (Exception e) {
                                        LOGGER.error("Error checking if client side", e);
                                    }
                                    
                                    if (clickedPos != null && !isClientSide) {
                                        for (EventCallbacks.RightClickBlockCallback cb : rightClickBlockCallbacks) {
                                            // Use reflection to invoke callback - cast Objects to expected types
                                            Object castPlayer = playerClass.cast(player);
                                            Object castWorld = levelClass.cast(world);
                                            Object castPos = blockPosClass.cast(clickedPos);
                                            java.lang.reflect.Method callbackMethod = cb.getClass().getMethod("onRightClickBlock", 
                                                playerClass, levelClass, blockPosClass);
                                            boolean cancel = (Boolean) callbackMethod.invoke(cb, castPlayer, castWorld, castPos);
                                            if (cancel) {
                                                // Return ActionResult.FAIL to cancel
                                                try {
                                                    Class<?> actionResultClass = classLoader.loadClass("net.minecraft.world.InteractionResult");
                                                    java.lang.reflect.Field failField = actionResultClass.getField("FAIL");
                                                    return failField.get(null);
                                                } catch (Exception e) {
                                                    return null; // Can't cancel, return null
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                LOGGER.error("Error in right-click block handler", e);
                            }
                            
                            // Continue with normal interaction
                            try {
                                Class<?> actionResultClass = classLoader.loadClass("net.minecraft.world.InteractionResult");
                                java.lang.reflect.Field passField = actionResultClass.getField("PASS");
                                return passField.get(null);
                            } catch (Exception e) {
                                return null;
                            }
                        }
                        return null;
                    }
                );
                
                registerUseBlockMethod.invoke(null, callback);
            } catch (Exception e) {
                LOGGER.warn("Could not register UseBlockCallback, right-click block events may not work", e);
            }
            
            LOGGER.info("Server-side Fabric events registered successfully");
        } catch (Exception e) {
            LOGGER.error("Error registering server events", e);
            e.printStackTrace();
        }
    }
    
    /**
     * Register client-side events (called from FabricClientSetup)
     */
    public static void registerClientEvents() {
        try {
            ClassLoader classLoader = FabricEventCallbackHandler.class.getClassLoader();
            
            // Load Fabric client event classes
            Class<?> clientTickEventsClass = classLoader.loadClass("net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents");
            Class<?> clientWorldEventsClass = classLoader.loadClass("net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents");
            
            // Load Minecraft Level class for type checking (client-side)
            Class<?> clientLevelClass = classLoader.loadClass("net.minecraft.world.level.Level");
            
            // Register client tick callback - END_CLIENT_TICK is a static field
            try {
                java.lang.reflect.Field endTickField = clientTickEventsClass.getField("END_CLIENT_TICK");
                Object endTickEvent = endTickField.get(null);
                java.lang.reflect.Method registerTickMethod = endTickEvent.getClass().getMethod("register", java.util.function.Consumer.class);
                registerTickMethod.invoke(endTickEvent, (java.util.function.Consumer<Object>) (client) -> {
                    for (EventCallbacks.ClientTickCallback callback : clientTickCallbacks) {
                        callback.onClientTick();
                    }
                });
            } catch (Exception e) {
                LOGGER.warn("Could not register client tick event", e);
            }
            
            // Register world unload callback (client-side) - UNLOAD is a static field
            try {
                java.lang.reflect.Field unloadField = clientWorldEventsClass.getField("UNLOAD");
                Object unloadEvent = unloadField.get(null);
                java.lang.reflect.Method registerUnloadMethod = unloadEvent.getClass().getMethod("register", java.util.function.Consumer.class);
                registerUnloadMethod.invoke(unloadEvent, (java.util.function.Consumer<Object>) (world) -> {
                    try {
                        if (clientLevelClass.isInstance(world)) {
                            for (EventCallbacks.LevelUnloadCallback callback : levelUnloadCallbacks) {
                                // Use reflection to invoke callback - cast Object to expected type
                                Object castWorld = clientLevelClass.cast(world);
                                java.lang.reflect.Method method = callback.getClass().getMethod("onLevelUnload", clientLevelClass);
                                method.invoke(callback, castWorld);
                            }
                        }
                    } catch (Exception ex) {
                        LOGGER.error("Error in client level unload handler", ex);
                    }
                });
            } catch (Exception e) {
                LOGGER.warn("Could not register client world unload event", e);
            }
            
            // Register key input callback - Fabric uses KeyBinding callbacks
            // This will be handled in FabricClientSetup with key binding registration
            
            // Register mouse scroll callback - Fabric uses mouse callback events
            // This will be handled in FabricClientSetup
            
            // Register render level callback - Fabric uses world render events
            // This will be handled in FabricClientSetup or FabricRenderHelper
            
            LOGGER.info("Client-side Fabric events registered successfully");
        } catch (Exception e) {
            LOGGER.error("Error registering client events", e);
            e.printStackTrace();
        }
    }
    
    // Registration methods (called by FabricEventHelper)
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

