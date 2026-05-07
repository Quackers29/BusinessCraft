package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.town.data.TownLeaderboardData;
import com.quackers29.businesscraft.ui.screens.town.TownLeaderboardScreen;
import com.quackers29.businesscraft.api.PlatformAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server → Client: Send leaderboard data for all towns.
 */
public class LeaderboardDataResponsePacket {
    private final List<TownLeaderboardData> towns;
    private final String currentTownName;

    public LeaderboardDataResponsePacket(List<TownLeaderboardData> towns, String currentTownName) {
        this.towns = towns;
        this.currentTownName = currentTownName != null ? currentTownName : "";
    }

    public LeaderboardDataResponsePacket(FriendlyByteBuf buf) {
        this.currentTownName = buf.readUtf();
        int size = buf.readInt();
        this.towns = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            UUID townId = buf.readUUID();
            String name = buf.readUtf();
            BlockPos position = buf.readBlockPos();
            long population = buf.readLong();
            long money = buf.readLong();
            float happiness = buf.readFloat();
            this.towns.add(new TownLeaderboardData(townId, name, position, population, money, happiness));
        }
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeUtf(currentTownName);
        buf.writeInt(towns.size());
        for (TownLeaderboardData town : towns) {
            buf.writeUUID(town.townId());
            buf.writeUtf(town.name());
            buf.writeBlockPos(town.position());
            buf.writeLong(town.population());
            buf.writeLong(town.money());
            buf.writeFloat(town.happiness());
        }
    }

    public static void encode(LeaderboardDataResponsePacket msg, FriendlyByteBuf buf) {
        msg.write(buf);
    }

    public static LeaderboardDataResponsePacket decode(FriendlyByteBuf buf) {
        return new LeaderboardDataResponsePacket(buf);
    }

    public static void handle(LeaderboardDataResponsePacket packet, Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            Screen currentScreen = minecraft.screen;

            // Open the ranking screen with the received data
            if (minecraft.player != null) {
                TownLeaderboardScreen rankingScreen = new TownLeaderboardScreen(
                    Component.literal("Town Ranking"),
                    currentScreen,
                    minecraft.player.blockPosition(),
                    packet.currentTownName
                );
                rankingScreen.setTownData(packet.towns);
                minecraft.setScreen(rankingScreen);
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }

    public List<TownLeaderboardData> getTowns() {
        return towns;
    }
}
