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

    public LeaderboardDataResponsePacket(List<TownLeaderboardData> towns) {
        this.towns = towns;
    }

    public LeaderboardDataResponsePacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        this.towns = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            UUID townId = buf.readUUID();
            String name = buf.readUtf();
            BlockPos position = buf.readBlockPos();
            long population = buf.readLong();
            long money = buf.readLong();
            this.towns.add(new TownLeaderboardData(townId, name, position, population, money));
        }
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeInt(towns.size());
        for (TownLeaderboardData town : towns) {
            buf.writeUUID(town.townId());
            buf.writeUtf(town.name());
            buf.writeBlockPos(town.position());
            buf.writeLong(town.population());
            buf.writeLong(town.money());
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

            // Open the leaderboard screen with the received data
            if (minecraft.player != null) {
                TownLeaderboardScreen leaderboardScreen = new TownLeaderboardScreen(
                    Component.literal("Town Leaderboard"),
                    currentScreen,
                    minecraft.player.blockPosition()
                );
                leaderboardScreen.setTownData(packet.towns);
                minecraft.setScreen(leaderboardScreen);
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }

    public List<TownLeaderboardData> getTowns() {
        return towns;
    }
}
