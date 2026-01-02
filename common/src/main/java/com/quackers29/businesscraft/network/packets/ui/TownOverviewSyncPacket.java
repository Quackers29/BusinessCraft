package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.ui.managers.TownDataCacheManager;
import com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

public class TownOverviewSyncPacket {
    private final float happiness;
    private final String biome;
    private final String currentResearch;
    private final float researchProgress; // days
    private final int dailyTickInterval;
    private final Map<String, Float> activeProductions;
    private final Map<String, Integer> upgradeLevels;
    private final float populationCap;
    private final int totalTouristsArrived;
    private final double totalTouristDistance;

    public TownOverviewSyncPacket(float happiness, String biome, String currentResearch, float researchProgress,
            int dailyTickInterval, Map<String, Float> activeProductions, Map<String, Integer> upgradeLevels,
            float populationCap, int totalTouristsArrived, double totalTouristDistance) {
        this.happiness = happiness;
        this.biome = biome;
        this.currentResearch = currentResearch != null ? currentResearch : "";
        this.researchProgress = researchProgress;
        this.dailyTickInterval = dailyTickInterval;
        this.activeProductions = activeProductions != null ? activeProductions : new HashMap<>();
        this.upgradeLevels = upgradeLevels != null ? new HashMap<>(upgradeLevels) : new HashMap<>();
        this.populationCap = populationCap;
        this.totalTouristsArrived = totalTouristsArrived;
        this.totalTouristDistance = totalTouristDistance;
    }

    public TownOverviewSyncPacket(FriendlyByteBuf buf) {
        this.happiness = buf.readFloat();
        this.biome = buf.readUtf();
        this.currentResearch = buf.readUtf();
        this.researchProgress = buf.readFloat();
        this.dailyTickInterval = buf.readInt();
        this.populationCap = buf.readFloat();
        this.totalTouristsArrived = buf.readInt();
        this.totalTouristDistance = buf.readDouble();

        int size = buf.readInt();
        this.activeProductions = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();
            float val = buf.readFloat();
            this.activeProductions.put(key, val);
        }

        int upgradesSize = buf.readInt();
        this.upgradeLevels = new HashMap<>(upgradesSize);
        for (int i = 0; i < upgradesSize; i++) {
            String key = buf.readUtf();
            int lvl = buf.readInt();
            this.upgradeLevels.put(key, lvl);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(happiness);
        buf.writeUtf(biome != null ? biome : "Unknown");
        buf.writeUtf(currentResearch != null ? currentResearch : "");
        buf.writeFloat(researchProgress);
        buf.writeInt(dailyTickInterval);
        buf.writeFloat(populationCap);
        buf.writeInt(totalTouristsArrived);
        buf.writeDouble(totalTouristDistance);

        buf.writeInt(activeProductions.size());
        for (Map.Entry<String, Float> entry : activeProductions.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeFloat(entry.getValue());
        }

        buf.writeInt(upgradeLevels.size());
        for (Map.Entry<String, Integer> entry : upgradeLevels.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeInt(entry.getValue());
        }
    }

    public static TownOverviewSyncPacket decode(FriendlyByteBuf buf) {
        return new TownOverviewSyncPacket(buf);
    }

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            if (Minecraft.getInstance().screen instanceof TownInterfaceScreen screen) {
                TownDataCacheManager cache = screen.getCacheManager();
                if (cache != null) {
                    cache.updateOverviewData(happiness, biome, currentResearch, researchProgress, dailyTickInterval,
                            activeProductions, upgradeLevels, populationCap, totalTouristsArrived,
                            totalTouristDistance);
                }
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}
