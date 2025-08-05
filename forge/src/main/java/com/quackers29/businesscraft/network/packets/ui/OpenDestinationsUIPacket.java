package com.quackers29.businesscraft.network.packets.ui;

import java.util.UUID;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.network.ModMessages;
import com.quackers29.businesscraft.util.PositionConverter;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Packet for opening the destinations UI for a platform
 */
public class OpenDestinationsUIPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final BlockPos blockPos;
    private final UUID platformId;
    
    public OpenDestinationsUIPacket(BlockPos blockPos, UUID platformId) {
        this.blockPos = blockPos;
        this.platformId = platformId;
    }
    
    /**
     * Encode the packet data into the buffer
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeUUID(platformId);
    }
    
    /**
     * Decode the packet data from the buffer
     */
    public static OpenDestinationsUIPacket decode(FriendlyByteBuf buf) {
        return new OpenDestinationsUIPacket(buf.readBlockPos(), buf.readUUID());
    }
    
    /**
     * Handle the packet on the receiving side
     */
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            // Get player and world from context
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            Level level = player.level();
            
            // Check if the block entity is valid
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof TownInterfaceEntity townInterface) {
                // Find the platform
                Platform platform = townInterface.getPlatform(platformId);
                if (platform != null) {
                    // Get town manager to find all towns
                    TownManager townManager = TownManager.get((ServerLevel) level);
                    
                    // Create a response packet with town information
                    RefreshDestinationsPacket responsePacket = new RefreshDestinationsPacket(
                        blockPos, platformId
                    );
                    
                    // Get the current town's position for distance calculations
                    BlockPos originPos = blockPos;
                    Town originTown = townInterface.getTown();
                    if (originTown != null) {
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                            "Found origin town: {}", originTown.getName());
                    }
                    
                    // Add all towns except the current one to the packet
                    int addedTowns = 0;
                    for (Map.Entry<UUID, Town> entry : townManager.getAllTowns().entrySet()) {
                        UUID townId = entry.getKey();
                        Town town = entry.getValue();
                        
                        // Skip this town
                        if (originTown != null && townId.equals(originTown.getId())) {
                            continue;
                        }
                        
                        // Calculate distance and direction to the town
                        BlockPos townPos = PositionConverter.toBlockPos(town.getPosition());
                        if (townPos != null) {
                            int distance = (int) Math.sqrt(townPos.distSqr(originPos));
                            
                            // Additional safety check: Skip towns with 0 distance (same location = same town)
                            if (distance <= 1) {
                                continue; // Skip this town - likely the same town
                            }
                            
                            String direction = calculateDirection(
                                townPos.getX() - originPos.getX(),
                                townPos.getZ() - originPos.getZ()
                            );
                            
                            // Add town to packet
                            boolean enabled = platform.isDestinationEnabled(townId);
                            responsePacket.addTown(
                                townId, 
                                town.getName(), 
                                enabled,
                                distance,
                                direction
                            );
                            addedTowns++;
                        }
                    }
                    
                    // Send response packet to open UI on client
                    ModMessages.sendToPlayer(responsePacket, player);
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                        "Sent destinations data for {} towns to player {}", 
                        addedTowns, player.getName().getString());
                }
            }
        });
        context.setPacketHandled(true);
        return true;
    }
    
    /**
     * Calculate cardinal direction based on x and z coordinates
     * @param dx X distance
     * @param dz Z distance
     * @return Cardinal direction (N, NE, E, SE, S, SW, W, NW)
     */
    private String calculateDirection(double dx, double dz) {
        if (dx == 0 && dz == 0) return "";
        
        double angle = Math.toDegrees(Math.atan2(dz, dx));
        // Convert angle to 0-360 range
        if (angle < 0) angle += 360;
        
        // Determine direction based on angle
        if (angle >= 337.5 || angle < 22.5) return "E";
        if (angle >= 22.5 && angle < 67.5) return "SE";
        if (angle >= 67.5 && angle < 112.5) return "S";
        if (angle >= 112.5 && angle < 157.5) return "SW";
        if (angle >= 157.5 && angle < 202.5) return "W";
        if (angle >= 202.5 && angle < 247.5) return "NW";
        if (angle >= 247.5 && angle < 292.5) return "N";
        return "NE"; // 292.5-337.5
    }
} 