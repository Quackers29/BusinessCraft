package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.network.ClientPacketDispatcher;
import com.quackers29.businesscraft.platform.PlatformInterfaceSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client snapshot for one Town Interface block.
 */
public class PlatformInterfaceSyncPacket {
    private final PlatformInterfaceSnapshot snapshot;

    public PlatformInterfaceSyncPacket(PlatformInterfaceSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public PlatformInterfaceSyncPacket(FriendlyByteBuf buf) {
        BlockPos kioskPos = buf.readBlockPos();
        boolean active = buf.readBoolean();
        String inactiveReason = buf.readUtf();
        String townName = buf.readUtf();
        String platformName = buf.readUtf();
        boolean enabled = buf.readBoolean();
        boolean pathSet = buf.readBoolean();
        String pathText = buf.readUtf();
        String waitingText = buf.readUtf();
        boolean townInReach = buf.readBoolean();
        int count = buf.readInt();
        List<PlatformInterfaceSnapshot.Destination> destinations = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            destinations.add(new PlatformInterfaceSnapshot.Destination(
                    buf.readUUID(), buf.readUtf(), buf.readBoolean(), buf.readInt()));
        }
        this.snapshot = new PlatformInterfaceSnapshot(
                kioskPos, active, inactiveReason, townName, platformName, enabled, pathSet, pathText, waitingText,
                townInReach, destinations);
    }

    public static PlatformInterfaceSyncPacket decode(FriendlyByteBuf buf) {
        return new PlatformInterfaceSyncPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(snapshot.kioskPos());
        buf.writeBoolean(snapshot.active());
        buf.writeUtf(snapshot.inactiveReason());
        buf.writeUtf(snapshot.townName());
        buf.writeUtf(snapshot.platformName());
        buf.writeBoolean(snapshot.enabled());
        buf.writeBoolean(snapshot.pathSet());
        buf.writeUtf(snapshot.pathText());
        buf.writeUtf(snapshot.waitingText());
        buf.writeBoolean(snapshot.townInReach());
        buf.writeInt(snapshot.destinations().size());
        for (PlatformInterfaceSnapshot.Destination destination : snapshot.destinations()) {
            buf.writeUUID(destination.townId());
            buf.writeUtf(destination.name());
            buf.writeBoolean(destination.enabled());
            buf.writeInt(destination.distance());
        }
    }

    public PlatformInterfaceSnapshot snapshot() {
        return snapshot;
    }

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> ClientPacketDispatcher.dispatch(this));
    }
}
