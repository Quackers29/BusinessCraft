package com.yourdomain.businesscraft.network;

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
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.platform.Platform;
import com.yourdomain.businesscraft.town.Town;
import com.yourdomain.businesscraft.town.TownManager;

/**
 * Packet sent from client to server to open the destinations UI for a platform
 */
public class OpenDestinationsUIPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final BlockPos pos;
    private final UUID platformId;
    
    public OpenDestinationsUIPacket(BlockPos pos, UUID platformId) {
        this.pos = pos;
        this.platformId = platformId;
    }
    
    public OpenDestinationsUIPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.platformId = buf.readUUID();
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUUID(platformId);
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            ServerLevel serverLevel = player.serverLevel();
            BlockEntity be = serverLevel.getBlockEntity(pos);
            
            if (be instanceof TownBlockEntity townBlock) {
                LOGGER.debug("Player {} is opening destinations UI for platform {} at {}", 
                    player.getName().getString(), platformId, pos);
                
                // Find the platform with this ID
                Platform platform = null;
                for (Platform p : townBlock.getPlatforms()) {
                    if (p.getId().equals(platformId)) {
                        platform = p;
                        break;
                    }
                }
                
                if (platform != null) {
                    // Create refresh packet with town data
                    RefreshDestinationsPacket packet = new RefreshDestinationsPacket(pos, platformId);
                    
                    // Get destination data for this platform
                    Map<UUID, Boolean> destinations = platform.getDestinations();
                    
                    // Get all towns
                    Map<UUID, String> allTowns = townBlock.getAllTownsForDestination(serverLevel);
                    
                    // Get the origin town
                    Town originTown = townBlock.getTown();
                    
                    // Use the block's position if town or town position is null
                    BlockPos originPos = (originTown != null && originTown.getPosition() != null) 
                        ? originTown.getPosition() 
                        : pos; // Use the block entity's position as fallback
                    
                    // Add towns to the packet with distance information
                    for (Map.Entry<UUID, String> entry : allTowns.entrySet()) {
                        UUID townId = entry.getKey();
                        String name = entry.getValue();
                        boolean enabled = destinations.getOrDefault(townId, false);
                        
                        // Calculate distance between towns
                        int distance = 100; // Default fallback distance
                        String direction = ""; // Direction (N, NE, E, etc.)
                        
                        Town destTown = TownManager.get(serverLevel).getTown(townId);
                        
                        if (destTown != null) {
                            BlockPos destPos = destTown.getPosition();
                            if (destPos != null) {
                                // Calculate Euclidean distance in blocks
                                double dx = destPos.getX() - originPos.getX();
                                double dy = destPos.getY() - originPos.getY();
                                double dz = destPos.getZ() - originPos.getZ();
                                distance = (int)Math.sqrt(dx*dx + dy*dy + dz*dz);
                                
                                // Calculate direction (ignore Y-axis)
                                direction = calculateDirection(dx, dz);
                            }
                        }
                        
                        packet.addTown(townId, name, enabled, distance, direction);
                    }
                    
                    // Send the packet to the client
                    ModMessages.sendToPlayer(packet, player);
                }
            }
        });
        
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