package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client → Server: Request fresh visit history for the town at a block position.
 */
public class VisitHistoryRequestPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(VisitHistoryRequestPacket.class);

    public VisitHistoryRequestPacket(BlockPos pos) {
        super(pos);
    }

    public VisitHistoryRequestPacket(FriendlyByteBuf buf) {
        super(buf);
    }

    private void write(FriendlyByteBuf buf) {
        super.toBytes(buf);
    }

    public static void encode(VisitHistoryRequestPacket msg, FriendlyByteBuf buf) {
        msg.write(buf);
    }

    public static VisitHistoryRequestPacket decode(FriendlyByteBuf buf) {
        return new VisitHistoryRequestPacket(buf);
    }

    public static void handle(VisitHistoryRequestPacket packet, Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            Object senderObj = PlatformAccess.getNetwork().getSender(context);
            if (!(senderObj instanceof ServerPlayer player)) {
                return;
            }
            if (!(player.level() instanceof ServerLevel serverLevel)) {
                return;
            }

            BlockEntity be = serverLevel.getBlockEntity(packet.getPos());
            if (!(be instanceof TownInterfaceEntity townInterface)) {
                LOGGER.warn("Visit history request for missing town interface at {}", packet.getPos());
                return;
            }

            ITownDataProvider provider = townInterface.getTownDataProvider();
            if (provider == null) {
                PlatformAccess.getNetworkMessages().sendToPlayer(
                        VisitHistoryResponsePacket.empty(packet.getPos()), player);
                return;
            }

            List<ITownDataProvider.VisitHistoryRecord> history = provider.getVisitHistory();
            Map<UUID, String> townNames = new HashMap<>();
            TownManager townManager = TownManager.get(serverLevel);
            for (ITownDataProvider.VisitHistoryRecord record : history) {
                UUID originId = record.getOriginTownId();
                if (originId != null && !townNames.containsKey(originId)) {
                    Town origin = townManager.getTown(originId);
                    townNames.put(originId, origin != null ? origin.getName()
                            : "Town-" + originId.toString().substring(0, 8));
                }
            }

            List<VisitHistoryResponsePacket.Entry> entries = new ArrayList<>(history.size());
            for (ITownDataProvider.VisitHistoryRecord record : history) {
                UUID originId = record.getOriginTownId();
                String name = originId != null ? townNames.getOrDefault(originId, "Unknown") : "Unknown";
                entries.add(new VisitHistoryResponsePacket.Entry(
                        record.getTimestamp(),
                        originId,
                        name,
                        record.getCount(),
                        record.getOriginPos()));
            }

            PlatformAccess.getNetworkMessages().sendToPlayer(
                    new VisitHistoryResponsePacket(packet.getPos(), entries), player);
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}
