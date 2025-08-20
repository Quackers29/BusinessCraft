package com.quackers29.businesscraft.network.packets.misc;

import com.quackers29.businesscraft.client.TownDebugOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Packet to send town debug data to client
 */
public class TownDebugDataResponsePacket {
    private final List<TownDebugOverlay.TownDebugData> townData;
    
    public TownDebugDataResponsePacket(List<TownDebugOverlay.TownDebugData> townData) {
        this.townData = townData;
    }
    
    public TownDebugDataResponsePacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        townData = new ArrayList<>(size);
        
        for (int i = 0; i < size; i++) {
            String id = buf.readUtf();
            String name = buf.readUtf();
            String position = buf.readUtf();
            int population = buf.readInt();
            int breadCount = buf.readInt();
            boolean touristSpawningEnabled = buf.readBoolean();
            boolean canSpawnTourists = buf.readBoolean();
            String pathStart = buf.readBoolean() ? buf.readUtf() : null;
            String pathEnd = buf.readBoolean() ? buf.readUtf() : null;
            int searchRadius = buf.readInt();
            int totalVisitors = buf.readInt();
            
            townData.add(new TownDebugOverlay.TownDebugData(
                id, name, position, population, breadCount, 
                touristSpawningEnabled, canSpawnTourists, 
                pathStart, pathEnd, searchRadius, totalVisitors
            ));
        }
    }
    
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(townData.size());
        
        for (TownDebugOverlay.TownDebugData data : townData) {
            buf.writeUtf(data.id);
            buf.writeUtf(data.name);
            buf.writeUtf(data.position);
            buf.writeInt(data.population);
            buf.writeInt(data.breadCount);
            buf.writeBoolean(data.touristSpawningEnabled);
            buf.writeBoolean(data.canSpawnTourists);
            buf.writeBoolean(data.pathStart != null);
            if (data.pathStart != null) buf.writeUtf(data.pathStart);
            buf.writeBoolean(data.pathEnd != null);
            if (data.pathEnd != null) buf.writeUtf(data.pathEnd);
            buf.writeInt(data.searchRadius);
            buf.writeInt(data.totalVisitors);
        }
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            // Update the overlay with the received data
            TownDebugOverlay.setTownData(townData);
        });
        return true;
    }
}