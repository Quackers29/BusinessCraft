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
     * Check if we're running on a server (as opposed to client-only)
     */
    private static boolean isServerSide() {
        try {
            ClassLoader classLoader = FabricEventCallbackHandler.class.getClassLoader();
            // Try to load server-only classes
            classLoader.loadClass("net.minecraft.server.level.ServerPlayer");
            classLoader.loadClass("net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Initialize Fabric event handlers
     */
    public static void initialize() {
        LOGGER.info("Initializing Fabric event callback handlers...");
        
        try {
            // Only register server-side events if we're on a server
            if (isServerSide()) {
                registerServerEvents();
            } else {
                LOGGER.info("Skipping server event registration - running on client");
            }
            
            // Register client-side events (will be called from FabricClientSetup)
            // Client events are registered separately in FabricClientSetup
            
            LOGGER.info("Fabric event callback handlers initialized successfully");
        } catch (Exception e) {
            LOGGER.warn("Error initializing Fabric event callback handlers (non-fatal)", e);
            // Don't print stack trace for expected errors on client
            if (isServerSide()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Register server-side events using Fabric's event API
     */
    private static void registerServerEvents() {
        // Double-check we're on server side
        if (!isServerSide()) {
            LOGGER.debug("Skipping server event registration - not on server");
            return;
        }
        
        try {
            ClassLoader classLoader = FabricEventCallbackHandler.class.getClassLoader();
            
            // Load Fabric event classes with fallback
            Class<?> serverTickEventsClass = loadClassWithFallback(classLoader, "net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents");
            Class<?> serverPlayerEventsClass = loadClassWithFallback(classLoader, "net.fabricmc.fabric.api.event.lifecycle.v1.ServerPlayerEvents");
            Class<?> serverWorldEventsClass = loadClassWithFallback(classLoader, "net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents");
            
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
                                        // Check if path creation mode is active
                                        Object activePos = com.quackers29.businesscraft.fabric.event.FabricModEvents.getActiveTownBlockPos();
                                        if (activePos != null) {
                                            // Handle path creation mode (similar to ForgeModEvents)
                                            try {
                                                long currentTime = System.currentTimeMillis();
                                                long lastClickTime = com.quackers29.businesscraft.fabric.event.FabricModEvents.getLastClickTime();
                                                
                                                // Debounce clicks
                                                if (currentTime - lastClickTime < 500) {
                                                    LOGGER.debug("Ignoring click due to debounce");
                                                    return null;
                                                }
                                                com.quackers29.businesscraft.fabric.event.FabricModEvents.setLastClickTime(currentTime);
                                                
                                                // Get the town interface entity
                                                Class<?> blockEntityClass = classLoader.loadClass("net.minecraft.world.level.block.entity.BlockEntity");
                                                java.lang.reflect.Method getBlockEntityMethod = levelClass.getMethod("getBlockEntity", blockPosClass);
                                                Object blockEntity = getBlockEntityMethod.invoke(world, activePos);
                                                
                                                if (blockEntity != null) {
                                                    Class<?> townInterfaceEntityClass = classLoader.loadClass("com.quackers29.businesscraft.block.entity.TownInterfaceEntity");
                                                    if (townInterfaceEntityClass.isInstance(blockEntity)) {
                                                        // Check if in path creation mode
                                                        java.lang.reflect.Method isInPathCreationModeMethod = townInterfaceEntityClass.getMethod("isInPathCreationMode");
                                                        boolean inPathMode = (Boolean) isInPathCreationModeMethod.invoke(blockEntity);
                                                        
                                                        if (inPathMode) {
                                                            // Load Component class and methods for messages (used multiple times)
                                                            Class<?> componentClass = classLoader.loadClass("net.minecraft.network.chat.Component");
                                                            java.lang.reflect.Method literalMethod = componentClass.getMethod("literal", String.class);
                                                            java.lang.reflect.Method sendSystemMessageMethod = playerClass.getMethod("sendSystemMessage", componentClass);
                                                            
                                                            // Check valid distance
                                                            java.lang.reflect.Method isValidPathDistanceMethod = townInterfaceEntityClass.getMethod("isValidPathDistance", blockPosClass);
                                                            boolean validDistance = (Boolean) isValidPathDistanceMethod.invoke(blockEntity, clickedPos);
                                                            
                                                            if (!validDistance) {
                                                                // Send message to player
                                                                Object message = literalMethod.invoke(null, "Point too far from town!");
                                                                sendSystemMessageMethod.invoke(player, message);
                                                                
                                                                // Cancel event
                                                                Class<?> actionResultClass = classLoader.loadClass("net.minecraft.world.InteractionResult");
                                                                java.lang.reflect.Field failField = actionResultClass.getField("FAIL");
                                                                return failField.get(null);
                                                            }
                                                            
                                                            boolean awaitingSecondClick = com.quackers29.businesscraft.fabric.event.FabricModEvents.isAwaitingSecondClick();
                                                            
                                                            if (!awaitingSecondClick) {
                                                                // First click - set start point
                                                                java.lang.reflect.Method setPathStartMethod = townInterfaceEntityClass.getMethod("setPathStart", blockPosClass);
                                                                setPathStartMethod.invoke(blockEntity, clickedPos);
                                                                com.quackers29.businesscraft.fabric.event.FabricModEvents.setAwaitingSecondClick(true);
                                                                
                                                                // Send message
                                                                Object yellowMessage = literalMethod.invoke(null, "First point set! Now click to set the end point.");
                                                                sendSystemMessageMethod.invoke(player, yellowMessage);
                                                            } else {
                                                                // Second click - set end point
                                                                java.lang.reflect.Method setPathEndMethod = townInterfaceEntityClass.getMethod("setPathEnd", blockPosClass);
                                                                setPathEndMethod.invoke(blockEntity, clickedPos);
                                                                java.lang.reflect.Method setPathCreationModeMethod = townInterfaceEntityClass.getMethod("setPathCreationMode", boolean.class);
                                                                setPathCreationModeMethod.invoke(blockEntity, false);
                                                                
                                                                // Get path start for provider update
                                                                java.lang.reflect.Method getPathStartMethod = townInterfaceEntityClass.getMethod("getPathStart");
                                                                Object pathStart = getPathStartMethod.invoke(blockEntity);
                                                                
                                                                // Update provider
                                                                java.lang.reflect.Method getTownDataProviderMethod = townInterfaceEntityClass.getMethod("getTownDataProvider");
                                                                Object provider = getTownDataProviderMethod.invoke(blockEntity);
                                                                if (provider != null) {
                                                                    Class<?> providerClass = classLoader.loadClass("com.quackers29.businesscraft.api.ITownDataProvider");
                                                                    java.lang.reflect.Method setPathStartProviderMethod = providerClass.getMethod("setPathStart", blockPosClass);
                                                                    java.lang.reflect.Method setPathEndProviderMethod = providerClass.getMethod("setPathEnd", blockPosClass);
                                                                    java.lang.reflect.Method markDirtyMethod = providerClass.getMethod("markDirty");
                                                                    
                                                                    setPathStartProviderMethod.invoke(provider, pathStart);
                                                                    setPathEndProviderMethod.invoke(provider, clickedPos);
                                                                    markDirtyMethod.invoke(provider);
                                                                }
                                                                
                                                                // Send success message
                                                                Object greenMessage = literalMethod.invoke(null, "Path created!");
                                                                sendSystemMessageMethod.invoke(player, greenMessage);
                                                                
                                                                // Reset state
                                                                com.quackers29.businesscraft.fabric.event.FabricModEvents.setAwaitingSecondClick(false);
                                                                com.quackers29.businesscraft.fabric.event.FabricModEvents.clearActiveTownBlock();
                                                            }
                                                            
                                                            // Cancel event
                                                            Class<?> actionResultClass = classLoader.loadClass("net.minecraft.world.InteractionResult");
                                                            java.lang.reflect.Field failField = actionResultClass.getField("FAIL");
                                                            return failField.get(null);
                                                        }
                                                    }
                                                }
                                            } catch (Exception ex) {
                                                LOGGER.error("Error handling path creation mode", ex);
                                            }
                                        }
                                        
                                        // Call registered callbacks
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
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Fabric API server event classes not available. Server events will not be registered.");
            LOGGER.warn("This is expected on client-side - server events will be registered when running on a server.");
            // Don't fail - this is expected on client
        } catch (Exception e) {
            LOGGER.error("Error registering server events", e);
            // Don't fail initialization - events can be registered later if needed
        }
    }
    
    /**
     * Register client-side events (called from FabricClientSetup)
     */
    public static void registerClientEvents() {
        try {
            ClassLoader classLoader = FabricEventCallbackHandler.class.getClassLoader();

            // Load Fabric client event classes with fallback
            Class<?> clientTickEventsClass = loadClassWithFallback(classLoader, "net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents");
            Class<?> clientWorldEventsClass = loadClassWithFallback(classLoader, "net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents");
            Class<?> useBlockCallbackClass = loadClassWithFallback(classLoader, "net.fabricmc.fabric.api.event.player.UseBlockCallback");

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
            
            // Register key input callback - Fabric uses ClientTickEvents to check key states
            try {
                // Reuse classLoader and clientTickEventsClass from above
                Class<?> keyBindingClass = classLoader.loadClass("net.minecraft.client.KeyMapping");
                
                // Get END_CLIENT_TICK event
                java.lang.reflect.Field endTickField = clientTickEventsClass.getField("END_CLIENT_TICK");
                Object endTickEvent = endTickField.get(null);
                java.lang.reflect.Method registerTickMethod = endTickEvent.getClass().getMethod("register", java.util.function.Consumer.class);
                
                // Register a tick callback that checks key states
                registerTickMethod.invoke(endTickEvent, (java.util.function.Consumer<Object>) (minecraftClient) -> {
                    try {
                        if (!keyInputCallbacks.isEmpty()) {
                            // Get GLFW window handle
                            java.lang.reflect.Method getWindowMethod = minecraftClient.getClass().getMethod("getWindow");
                            Object window = getWindowMethod.invoke(minecraftClient);
                            java.lang.reflect.Method getWindowMethod2 = window.getClass().getMethod("getWindow");
                            long windowHandle = ((Number) getWindowMethod2.invoke(window)).longValue();
                            
                            // Check for key presses using GLFW (via reflection)
                            Class<?> glfwClass = classLoader.loadClass("org.lwjgl.glfw.GLFW");
                            java.lang.reflect.Method glfwGetKeyMethod = glfwClass.getMethod("glfwGetKey", long.class, int.class);
                            
                            // Check keys that are registered (F4 is the main one)
                            int[] keysToCheck = {293}; // GLFW_KEY_F4
                            
                            for (int keyCode : keysToCheck) {
                                int currentState = ((Number) glfwGetKeyMethod.invoke(null, windowHandle, keyCode)).intValue();
                                Boolean previousState = previousKeyStates.getOrDefault(keyCode, false);
                                
                                // Detect key press (transition from not pressed to pressed)
                                if (currentState == 1 && !previousState) {
                                    // Key was just pressed
                                    for (EventCallbacks.KeyInputCallback callback : keyInputCallbacks) {
                                        boolean handled = callback.onKeyInput(keyCode, 1); // GLFW_PRESS
                                        if (handled) {
                                            break; // Stop if handled
                                        }
                                    }
                                }
                                
                                // Update previous state
                                previousKeyStates.put(keyCode, currentState == 1);
                            }
                        }
                    } catch (Exception ex) {
                        LOGGER.error("Error in key input handler", ex);
                    }
                });
                
                LOGGER.info("Key input callback registered");
            } catch (Exception e) {
                LOGGER.warn("Could not register key input event", e);
            }
            
            // Register mouse scroll callback - Mouse scroll is handled at screen level in common module screens
            // This is a placeholder - mouse scroll is typically handled in Screen classes
            LOGGER.info("Mouse scroll callback registration - handled at screen level");

            // Register block interaction callback for menu opening (Fabric equivalent of right-click)
            try {
                // UseBlockCallback has an EVENT field, not a register method
                java.lang.reflect.Field eventField = useBlockCallbackClass.getField("EVENT");
                Object eventObj = eventField.get(null);

                // Register the callback using the EVENT.register() method
                java.lang.reflect.Method registerMethod = eventObj.getClass().getMethod("register", java.util.function.Function.class);
                registerMethod.invoke(eventObj, (java.util.function.Function<Object, Object>) (interactionEvent) -> {
                    try {
                        // interactionEvent is a UseBlockCallback.ItemUsageContext
                        // Check if this is a town interface block
                        Object world = interactionEvent.getClass().getMethod("getWorld").invoke(interactionEvent);
                        Object blockPos = interactionEvent.getClass().getMethod("getBlockPos").invoke(interactionEvent);
                        Object blockState = interactionEvent.getClass().getMethod("getBlockState").invoke(interactionEvent);
                        Object player = interactionEvent.getClass().getMethod("getPlayer").invoke(interactionEvent);

                        // Check if this is our town interface block
                        Object block = blockState.getClass().getMethod("getBlock").invoke(blockState);
                        String blockId = block.getClass().getMethod("toString").invoke(block).toString();

                        if (blockId.contains("businesscraft:town_interface")) {
                            // This is a town interface block - open the menu
                            LOGGER.info("Right-clicked on town interface block - opening menu");

                            // Send packet to server to open menu
                            // Use the PlatformAccess network helper
                            try {
                                Class<?> platformAccessClass = classLoader.loadClass("com.quackers29.businesscraft.api.PlatformAccess");
                                Object network = platformAccessClass.getMethod("getNetwork").invoke(null);
                                java.lang.reflect.Method sendToServerMethod = network.getClass().getMethod("sendToServer", Object.class);

                                // Create OpenTownInterfacePacket
                                Class<?> packetClass = classLoader.loadClass("com.quackers29.businesscraft.network.packets.ui.OpenTownInterfacePacket");
                                Object packet = packetClass.getConstructor(blockPos.getClass()).newInstance(blockPos);

                                sendToServerMethod.invoke(network, packet);
                                LOGGER.info("Sent OpenTownInterfacePacket to server");
                            } catch (Exception packetEx) {
                                LOGGER.error("Error sending menu open packet", packetEx);
                            }

                            // Return PASS to allow normal interaction
                            Class<?> actionResultClass = classLoader.loadClass("net.minecraft.util.ActionResult");
                            return actionResultClass.getField("PASS").get(null);
                        }

                        // Not our block, continue with normal interaction
                        return null;
                    } catch (Exception ex) {
                        LOGGER.error("Error in block interaction callback", ex);
                        return null;
                    }
                });
                LOGGER.info("Registered UseBlockCallback for menu opening");
            } catch (Exception e) {
                LOGGER.warn("Could not register UseBlockCallback", e);
            }

            // Register render level callback - Fabric uses WorldRenderEvents (handled by RenderHelper)
            // Render level callbacks are already handled by FabricRenderHelper.registerWorldRenderCallback()
            // ClientRenderEvents registers its callback via PlatformAccess.getEvents().registerRenderLevelCallback()
            // which calls FabricRenderHelper.registerWorldRenderCallback()
            // So no additional registration needed here
            LOGGER.info("Render level callbacks handled via RenderHelper");

            LOGGER.info("Client-side Fabric events registered successfully");
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Fabric API client event classes not available. Client events will not be registered.");
            LOGGER.warn("This may indicate a timing issue - client events will be registered when classes become available.");
            // Schedule retry
            scheduleDelayedClientEvents();
        } catch (Exception e) {
            LOGGER.error("Error registering client events", e);
            // Schedule retry
            scheduleDelayedClientEvents();
        }
    }
    
    /**
     * Schedule delayed client events registration
     */
    private static void scheduleDelayedClientEvents() {
        new Thread(() -> {
            int maxRetries = 10;
            int retryCount = 0;
            int delayMs = 500;
            
            while (retryCount < maxRetries) {
                try {
                    Thread.sleep(delayMs);
                    retryCount++;
                    
                    try {
                        ClassLoader classLoader = FabricEventCallbackHandler.class.getClassLoader();
                        classLoader.loadClass("net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents");
                        classLoader.loadClass("net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents");
                        
                        LOGGER.info("Retrying client event registration (attempt {})...", retryCount);
                        // Retry registration - but avoid infinite recursion by calling internal method
                        registerClientEventsInternal();
                        LOGGER.info("Client events registered successfully on retry!");
                        return;
                    } catch (ClassNotFoundException e) {
                        delayMs = Math.min(delayMs * 2, 5000);
                        continue;
                    } catch (Exception e) {
                        LOGGER.warn("Client event registration failed on retry {}: {}", retryCount, e.getMessage());
                        delayMs = Math.min(delayMs * 2, 5000);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            
            LOGGER.warn("Client event registration failed after {} retries.", maxRetries);
        }).start();
    }
    
    /**
     * Internal method to register client events (used by retry mechanism)
     */
    private static void registerClientEventsInternal() {
        // Same logic as registerClientEvents but without retry scheduling
        try {
            ClassLoader classLoader = FabricEventCallbackHandler.class.getClassLoader();
            
            // Load Fabric client event classes with fallback
            Class<?> clientTickEventsClass = loadClassWithFallback(classLoader, "net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents");
            Class<?> clientWorldEventsClass = loadClassWithFallback(classLoader, "net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents");
            Class<?> useBlockCallbackClass = loadClassWithFallback(classLoader, "net.fabricmc.fabric.api.event.player.UseBlockCallback");

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
            
            // Register key input callback - Fabric uses ClientTickEvents to check key states
            try {
                // Key input is handled via registerKeyInputCallback and tracked in client tick
                // This is implemented in registerKeyInputCallback method
                LOGGER.info("Key input callbacks registered via registerKeyInputCallback");
            } catch (Exception e) {
                LOGGER.warn("Could not register key input callback", e);
            }
            
            // Register block interaction callback for menu opening (Fabric equivalent of right-click)
            try {
                // Register UseBlockCallback for handling right-click on blocks
                java.lang.reflect.Method registerMethod = useBlockCallbackClass.getMethod("register",
                    java.util.function.Function.class);
                registerMethod.invoke(null, (java.util.function.Function<Object, Object>) (event) -> {
                    try {
                        // event is a UseBlockCallback.ItemUsageContext
                        // Check if this is a town interface block
                        Object world = event.getClass().getMethod("getWorld").invoke(event);
                        Object blockPos = event.getClass().getMethod("getBlockPos").invoke(event);
                        Object blockState = event.getClass().getMethod("getBlockState").invoke(event);
                        Object player = event.getClass().getMethod("getPlayer").invoke(event);

                        // Check if this is our town interface block
                        Object block = blockState.getClass().getMethod("getBlock").invoke(blockState);
                        String blockId = block.getClass().getMethod("toString").invoke(block).toString();

                        if (blockId.contains("businesscraft:town_interface")) {
                            // This is a town interface block - open the menu
                            LOGGER.info("Right-clicked on town interface block - opening menu");

                            // Send packet to server to open menu
                            // Use the PlatformAccess network helper
                            try {
                                Class<?> platformAccessClass = classLoader.loadClass("com.quackers29.businesscraft.api.PlatformAccess");
                                Object network = platformAccessClass.getMethod("getNetwork").invoke(null);
                                java.lang.reflect.Method sendToServerMethod = network.getClass().getMethod("sendToServer", Object.class);

                                // Create OpenTownInterfacePacket
                                Class<?> packetClass = classLoader.loadClass("com.quackers29.businesscraft.network.packets.ui.OpenTownInterfacePacket");
                                Object packet = packetClass.getConstructor(blockPos.getClass()).newInstance(blockPos);

                                sendToServerMethod.invoke(network, packet);
                                LOGGER.info("Sent OpenTownInterfacePacket to server");
                            } catch (Exception packetEx) {
                                LOGGER.error("Error sending menu open packet", packetEx);
                            }

                            // Return PASS to allow normal interaction
                            Class<?> actionResultClass = classLoader.loadClass("net.minecraft.util.ActionResult");
                            return actionResultClass.getField("PASS").get(null);
                        }

                        // Not our block, continue with normal interaction
                        return null;
                    } catch (Exception ex) {
                        LOGGER.error("Error in block interaction callback", ex);
                        return null;
                    }
                });
                LOGGER.info("Registered UseBlockCallback for menu opening");
            } catch (Exception e) {
                LOGGER.warn("Could not register UseBlockCallback", e);
            }

            // Render level callbacks are handled via RenderHelper.registerRenderLevelCallback
            // So no additional registration needed here
            LOGGER.info("Render level callbacks handled via RenderHelper");

            LOGGER.info("Client-side Fabric events registered successfully");
        } catch (Exception e) {
            throw new RuntimeException("Failed to register client events", e);
        }
    }
    
    // Key state tracking for key input callbacks
    private static final java.util.Map<Integer, Boolean> previousKeyStates = new java.util.HashMap<>();
    
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
    
    /**
     * Load class with fallback classloaders
     */
    private static Class<?> loadClassWithFallback(ClassLoader classLoader, String className) throws ClassNotFoundException {
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e1) {
            try {
                ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                if (contextClassLoader != null) {
                    return contextClassLoader.loadClass(className);
                }
            } catch (ClassNotFoundException e2) {
                // Fall through
            }
            throw new ClassNotFoundException("Could not load " + className + " from any classloader", e1);
        }
    }
}

