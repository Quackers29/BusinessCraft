package com.quackers29.businesscraft.api;

import net.minecraft.core.BlockPos;

/**
 * Platform-agnostic interface for client-side operations.
 * Provides access to Minecraft client instance and common client operations.
 */
public interface ClientHelper {
        /**
         * Get the Minecraft client instance (platform-specific)
         * 
         * @return The Minecraft client instance as Object
         */
        Object getMinecraft();

        /**
         * Get the current client level
         * 
         * @return The client level, or null if not in a world
         */
        Object getClientLevel();

        /**
         * Get the current screen
         * 
         * @return The current screen, or null if none is open
         */
        Object getCurrentScreen();

        /**
         * Get the font renderer
         * 
         * @return The font renderer instance
         */
        Object getFont();

        /**
         * Execute code on the client thread
         * 
         * @param runnable The code to execute
         */
        void executeOnClientThread(Runnable runnable);

        /**
         * Check if we're currently on the client thread
         * 
         * @return true if on client thread
         */
        boolean isOnClientThread();

        /**
         * Get the sound manager
         * 
         * @return The sound manager instance, or null if not available
         */
        Object getSoundManager();

        /**
         * Get the client player
         * 
         * @return The client player, or null if not in a world
         */
        Object getClientPlayer();

        /**
         * Get the current player (alias for getClientPlayer)
         * 
         * @return The player instance, or null if not in a world
         */
        default Object getPlayer() {
                return getClientPlayer();
        }

        void openDestinationsScreen(
                        BlockPos pos,
                        java.util.UUID platformId,
                        String platformName,
                        java.util.Map<java.util.UUID, String> townNames,
                        java.util.Map<java.util.UUID, Boolean> enabledState,
                        java.util.Map<java.util.UUID, Integer> townDistances,
                        java.util.Map<java.util.UUID, String> townDirections);

        void invalidateTownScreenCache();

        void refreshPlatformScreen(boolean force);

        void updateTownOverviewData(
                        float happiness, String biome, String biomeVariant, String currentResearch,
                        float researchProgress, int dailyTickInterval,
                        java.util.Map<String, Float> activeProductions,
                        java.util.Map<String, Integer> upgradeLevels,
                        float populationCap, int totalTouristsArrived,
                        double totalTouristDistance, float borderRadius,
                        java.util.Map<String, Float> aiScores);

        void updateContractBoard(java.util.List<com.quackers29.businesscraft.contract.Contract> contracts);
}
