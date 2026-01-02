package com.quackers29.businesscraft.network.packets;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.town.data.ClientSyncHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ResourceSyncPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceSyncPacket.class);

    private final BlockPos pos;
    private final Map<Item, Integer> resources;
    private final Map<Item, float[]> resourceStats; // [production, consumption, capacity]

    public ResourceSyncPacket(BlockPos pos, Map<Item, Integer> resources, Map<Item, float[]> resourceStats) {
        this.pos = pos;
        this.resources = new HashMap<>(resources);
        this.resourceStats = resourceStats != null ? new HashMap<>(resourceStats) : new HashMap<>();
    }

    public ResourceSyncPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        int size = buf.readInt();
        this.resources = new HashMap<>();
        for (int i = 0; i < size; i++) {
            var rl = buf.readResourceLocation();
            var item = (Item) PlatformAccess.getRegistry().getItem(rl);
            int count = buf.readInt();
            if (item != null) {
                this.resources.put(item, count);
            }
        }

        int statsSize = buf.readInt();
        this.resourceStats = new HashMap<>();
        for (int i = 0; i < statsSize; i++) {
            var rl = buf.readResourceLocation();
            var item = (Item) PlatformAccess.getRegistry().getItem(rl);
            float prod = buf.readFloat();
            float cons = buf.readFloat();
            float cap = buf.readFloat();
            if (item != null) {
                this.resourceStats.put(item, new float[] { prod, cons, cap });
            }
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(resources.size());
        resources.forEach((item, count) -> {
            var key = (net.minecraft.resources.ResourceLocation) PlatformAccess.getRegistry().getItemKey(item);
            buf.writeResourceLocation(key);
            buf.writeInt(count);
        });

        buf.writeInt(resourceStats.size());
        resourceStats.forEach((item, stats) -> {
            var key = (net.minecraft.resources.ResourceLocation) PlatformAccess.getRegistry().getItemKey(item);
            buf.writeResourceLocation(key);
            buf.writeFloat(stats[0]); // Production
            buf.writeFloat(stats[1]); // Consumption
            buf.writeFloat(stats[2]); // Capacity
        });
    }

    public static void encode(ResourceSyncPacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }

    public static ResourceSyncPacket decode(FriendlyByteBuf buf) {
        return new ResourceSyncPacket(buf);
    }

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            // LOGGER.info("[CLIENT] ResourceSyncPacket received for pos {}", pos); //
            // Reduced logging
            var mc = Minecraft.getInstance();
            var level = mc.level;
            if (level == null)
                return;

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TownInterfaceEntity entity) {
                var clientResources = entity.getClientSyncHelper().getClientResources();
                clientResources.clear();
                clientResources.putAll(resources);

                // Update stats
                entity.getClientSyncHelper().updateClientResourceStats(resourceStats);

                // Refresh open UI if TownInterfaceScreen is active
                if (mc.screen instanceof com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen screen) {
                    screen.getMenu().refreshDataSlots();
                }
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}
