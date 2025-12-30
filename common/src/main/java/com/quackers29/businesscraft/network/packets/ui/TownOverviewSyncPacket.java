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
    private final List<String> unlockedNodes;
    private final float populationCap;

    public TownOverviewSyncPacket(float happiness, String biome, String currentResearch, float researchProgress,
            int dailyTickInterval, Map<String, Float> activeProductions, Collection<String> unlockedNodes,
            float populationCap) {
        this.happiness = happiness;
        this.biome = biome;
        this.currentResearch = currentResearch != null ? currentResearch : "";
        this.researchProgress = researchProgress;
        this.dailyTickInterval = dailyTickInterval;
        this.activeProductions = activeProductions != null ? activeProductions : new HashMap<>();
        this.unlockedNodes = unlockedNodes != null ? new ArrayList<>(unlockedNodes) : new ArrayList<>();
        this.populationCap = populationCap;
    }

    public TownOverviewSyncPacket(FriendlyByteBuf buf) {
        this.happiness = buf.readFloat();
        this.biome = buf.readUtf();
        this.currentResearch = buf.readUtf();
        this.researchProgress = buf.readFloat();
        this.dailyTickInterval = buf.readInt();
        this.populationCap = buf.readFloat();

        int size = buf.readInt();
        this.activeProductions = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();
            float val = buf.readFloat();
            this.activeProductions.put(key, val);
        }

        int unlockedSize = buf.readInt();
        this.unlockedNodes = new ArrayList<>(unlockedSize);
        for (int i = 0; i < unlockedSize; i++) {
            this.unlockedNodes.add(buf.readUtf());
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(happiness);
        buf.writeUtf(biome != null ? biome : "Unknown");
        buf.writeUtf(currentResearch != null ? currentResearch : "");
        buf.writeFloat(researchProgress);
        buf.writeInt(dailyTickInterval);
        buf.writeFloat(populationCap);

        buf.writeInt(activeProductions.size());
        for (Map.Entry<String, Float> entry : activeProductions.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeFloat(entry.getValue());
        }

        buf.writeInt(unlockedNodes.size());
        for (String node : unlockedNodes) {
            buf.writeUtf(node);
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
                            activeProductions, unlockedNodes, populationCap);
                }
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}
