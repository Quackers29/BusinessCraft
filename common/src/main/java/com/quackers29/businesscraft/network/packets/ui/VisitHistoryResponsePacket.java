package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.network.ClientPacketDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server → Client: Visit history rows for a town interface block.
 */
public class VisitHistoryResponsePacket {

    public record Entry(long timestamp, UUID originTownId, String originTownName, int count, BlockPos originPos) {
    }

    private final BlockPos blockPos;
    private final List<Entry> entries;

    public VisitHistoryResponsePacket(BlockPos blockPos, List<Entry> entries) {
        this.blockPos = blockPos;
        this.entries = entries != null ? List.copyOf(entries) : List.of();
    }

    public static VisitHistoryResponsePacket empty(BlockPos blockPos) {
        return new VisitHistoryResponsePacket(blockPos, List.of());
    }

    public VisitHistoryResponsePacket(FriendlyByteBuf buf) {
        this.blockPos = buf.readBlockPos();
        int size = buf.readInt();
        List<Entry> loaded = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            long timestamp = buf.readLong();
            UUID originId = buf.readUUID();
            String originName = buf.readUtf();
            int count = buf.readInt();
            BlockPos originPos = buf.readBlockPos();
            loaded.add(new Entry(timestamp, originId, originName, count, originPos));
        }
        this.entries = List.copyOf(loaded);
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeInt(entries.size());
        for (Entry entry : entries) {
            buf.writeLong(entry.timestamp());
            buf.writeUUID(entry.originTownId() != null ? entry.originTownId() : new UUID(0, 0));
            buf.writeUtf(entry.originTownName() != null ? entry.originTownName() : "Unknown");
            buf.writeInt(entry.count());
            BlockPos pos = entry.originPos() != null ? entry.originPos() : BlockPos.ZERO;
            buf.writeBlockPos(pos);
        }
    }

    public static void encode(VisitHistoryResponsePacket msg, FriendlyByteBuf buf) {
        msg.write(buf);
    }

    public static VisitHistoryResponsePacket decode(FriendlyByteBuf buf) {
        return new VisitHistoryResponsePacket(buf);
    }

    public static void handle(VisitHistoryResponsePacket packet, Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> ClientPacketDispatcher.dispatch(packet));
        PlatformAccess.getNetwork().setPacketHandled(context);
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public List<Entry> getEntries() {
        return entries;
    }
}
