package com.quackers29.businesscraft.network.packets.misc;

import com.quackers29.businesscraft.client.TownDebugOverlay;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Packet to request town debug data from server
 */
public class RequestTownDebugDataPacket {
    
    public RequestTownDebugDataPacket() {
    }
    
    public RequestTownDebugDataPacket(FriendlyByteBuf buf) {
        // No data to read
    }
    
    public void toBytes(FriendlyByteBuf buf) {
        // No data to write
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                List<TownDebugOverlay.TownDebugData> townDataList = new ArrayList<>();
                
                // Gather data from all loaded levels
                for (ServerLevel level : player.getServer().getAllLevels()) {
                    TownManager townManager = TownManager.get(level);
                    Collection<Town> towns = townManager.getAllTowns();
                    
                    // Convert Town objects to TownDebugData
                    for (Town town : towns) {
                        townDataList.add(new TownDebugOverlay.TownDebugData(
                            town.getId().toString(),
                            town.getName(),
                            town.getPosition().toShortString(),
                            town.getPopulation(),
                            town.getBreadCount(),
                            town.isTouristSpawningEnabled(),
                            town.canSpawnTourists(),
                            town.getPathStart() != null ? town.getPathStart().toShortString() : null,
                            town.getPathEnd() != null ? town.getPathEnd().toShortString() : null,
                            town.getSearchRadius(),
                            town.getTotalVisitors()
                        ));
                    }
                }
                
                // Send response packet back to client
                com.quackers29.businesscraft.network.ModMessages.sendToPlayer(
                    new TownDebugDataResponsePacket(townDataList), player
                );
            }
        });
        return true;
    }
}