package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.data.TownLeaderboardData;
import com.quackers29.businesscraft.api.PlatformAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client → Server: Request leaderboard data for all towns.
 */
public class LeaderboardDataRequestPacket {

    public LeaderboardDataRequestPacket() {
        // Empty packet - just a request signal
    }

    public LeaderboardDataRequestPacket(FriendlyByteBuf buf) {
        // Empty constructor for decoding
    }

    private void write(FriendlyByteBuf buf) {
        // Empty packet - no data to write
    }

    public static void encode(LeaderboardDataRequestPacket msg, FriendlyByteBuf buf) {
        msg.write(buf);
    }

    public static LeaderboardDataRequestPacket decode(FriendlyByteBuf buf) {
        return new LeaderboardDataRequestPacket(buf);
    }

    public static void handle(LeaderboardDataRequestPacket packet, Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            Object senderObj = PlatformAccess.getNetwork().getSender(context);
            if (!(senderObj instanceof ServerPlayer player)) return;

            // Get all towns from the server
            TownManager townManager = TownManager.get(player.serverLevel());
            Map<UUID, Town> allTowns = townManager.getAllTowns();

            // Convert to leaderboard data
            List<TownLeaderboardData> leaderboardData = new ArrayList<>();
            for (Town town : allTowns.values()) {
                TownLeaderboardData data = new TownLeaderboardData(
                    town.getId(),
                    town.getName(),
                    town.getPosition(),
                    town.getPopulation(),
                    town.getResourceCount(Items.EMERALD)
                );
                leaderboardData.add(data);
            }

            // Send response back to client
            LeaderboardDataResponsePacket responsePacket = new LeaderboardDataResponsePacket(leaderboardData);
            PlatformAccess.getNetworkMessages().sendToPlayer(responsePacket, player);
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}
