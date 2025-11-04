package com.quackers29.businesscraft.api;

/**
 * Platform-agnostic interface for client-side operations.
 * Provides access to Minecraft client instance and common client operations.
 */
public interface ClientHelper {
    /**
     * Get the Minecraft client instance (platform-specific)
     * @return The Minecraft client instance as Object
     */
    Object getMinecraft();

    /**
     * Get the current client level
     * @return The client level, or null if not in a world
     */
    Object getClientLevel();

    /**
     * Get the current screen
     * @return The current screen, or null if none is open
     */
    Object getCurrentScreen();

    /**
     * Get the font renderer
     * @return The font renderer instance
     */
    Object getFont();

    /**
     * Execute code on the client thread
     * @param runnable The code to execute
     */
    void executeOnClientThread(Runnable runnable);

    /**
     * Check if we're currently on the client thread
     * @return true if on client thread
     */
    boolean isOnClientThread();
}

