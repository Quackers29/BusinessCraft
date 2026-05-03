package com.quackers29.businesscraft.api;

import net.minecraft.core.BlockPos;

public interface ClientHelper {
    Object getMinecraft();

    Object getClientLevel();

    Object getCurrentScreen();

    Object getFont();

    void executeOnClientThread(Runnable runnable);

    boolean isOnClientThread();

    Object getSoundManager();

    Object getClientPlayer();

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
