package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.ClientHelper;
import net.minecraft.client.Minecraft;

/**
 * Fabric implementation of ClientHelper
 * Uses direct Mojang mappings for client access
 */
public class FabricClientHelper implements ClientHelper {
    @Override
    public Object getMinecraft() {
        return Minecraft.getInstance();
    }

    @Override
    public Object getClientLevel() {
        return Minecraft.getInstance().level;
    }

    @Override
    public Object getCurrentScreen() {
        return Minecraft.getInstance().screen;
    }

    @Override
    public Object getFont() {
        return Minecraft.getInstance().font;
    }

    @Override
    public void executeOnClientThread(Runnable runnable) {
        Minecraft.getInstance().execute(runnable);
    }

    @Override
    public boolean isOnClientThread() {
        return Minecraft.getInstance().isSameThread();
    }

    @Override
    public Object getSoundManager() {
        return Minecraft.getInstance().getSoundManager();
    }

    @Override
    public Object getClientPlayer() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player;
    }

    @Override
    public void openDestinationsScreen(
            net.minecraft.core.BlockPos pos,
            java.util.UUID platformId,
            String platformName,
            java.util.Map<java.util.UUID, String> townNames,
            java.util.Map<java.util.UUID, Boolean> enabledState,
            java.util.Map<java.util.UUID, Integer> townDistances,
            java.util.Map<java.util.UUID, String> townDirections) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new com.quackers29.businesscraft.ui.screens.platform.DestinationsScreenV2(
                pos,
                platformId,
                platformName,
                townNames,
                enabledState,
                townDistances,
                townDirections));
    }

    @Override
    public void invalidateTownScreenCache() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen screen) {
            screen.invalidateCache();
        }
    }

    @Override
    public void refreshPlatformScreen(boolean force) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof com.quackers29.businesscraft.ui.screens.platform.PlatformManagementScreenV2 screen) {
            screen.refreshPlatformData(force);
        }
    }

    @Override
    public void updateTownOverviewData(
            float happiness, String biome, String biomeVariant, String currentResearch,
            float researchProgress, int dailyTickInterval,
            java.util.Map<String, Float> activeProductions,
            java.util.Map<String, Integer> upgradeLevels,
            float populationCap, int totalTouristsArrived,
            double totalTouristDistance, float borderRadius,
            java.util.Map<String, Float> aiScores) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen screen) {
            com.quackers29.businesscraft.ui.managers.TownDataCacheManager cache = screen.getCacheManager();
            if (cache != null) {
                cache.updateOverviewData(happiness, biome, biomeVariant, currentResearch, researchProgress,
                        dailyTickInterval, activeProductions, upgradeLevels, populationCap,
                        totalTouristsArrived, totalTouristDistance, borderRadius, aiScores);
            }
        }
    }

    @Override
    public void updateContractBoard(java.util.List<com.quackers29.businesscraft.contract.Contract> contracts) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof com.quackers29.businesscraft.ui.screens.town.ContractBoardScreen screen) {
            screen.updateContracts(contracts);
        }
    }
}
