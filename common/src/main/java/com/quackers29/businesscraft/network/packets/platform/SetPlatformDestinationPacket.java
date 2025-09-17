package com.quackers29.businesscraft.network.packets.platform;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.Map;
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
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.network.packets.ui.RefreshDestinationsPacket;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Packet sent from client to server to set a destination for a platform
 */
public class SetPlatformDestinationPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final BlockPos pos;
    private final UUID platformId;
    private final UUID townId;
    private final boolean enabled;
    
    public SetPlatformDestinationPacket(BlockPos pos, UUID platformId, UUID townId, boolean enabled) {
        this.pos = pos;
        this.platformId = platformId;
        this.townId = townId;
        this.enabled = enabled;
    }
    
    public SetPlatformDestinationPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.platformId = buf.readUUID();
        this.townId = buf.readUUID();
        this.enabled = buf.readBoolean();
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUUID(platformId);
        buf.writeUUID(townId);
        buf.writeBoolean(enabled);
    }

    // Static decode method for Forge network registration
    public static SetPlatformDestinationPacket decode(FriendlyByteBuf buf) {
        return new SetPlatformDestinationPacket(buf);
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            try {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "SetPlatformDestinationPacket: No player sender");
                    return;
                }

                Level level = player.level();
                BlockEntity be = level.getBlockEntity(pos);

                if (be instanceof TownInterfaceEntity townInterface) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "Player {} is setting destination {} to {} for platform {} at {}", 
                    player.getName().getString(), townId, enabled, platformId, pos);
                
                // Find the platform with this ID
                Platform platform = null;
                for (Platform p : townInterface.getPlatforms()) {
                    if (p.getId().equals(platformId)) {
                        platform = p;
                        break;
                    }
                }
                
                if (platform != null) {
                    // Set the destination enabled state
                    platform.setDestinationEnabled(townId, enabled);
                    townInterface.setChanged();
                    
                    // Create a refresh packet with updated data
                    RefreshDestinationsPacket refreshPacket = new RefreshDestinationsPacket(pos, platformId);
                    
                    // Add all towns to the packet
                    if (level instanceof ServerLevel serverLevel) {
                        Map<UUID, Boolean> destinations = platform.getDestinations();
                        
                        // Get the origin town
                        Town originTown = townInterface.getTown();
                        
                        // Use the block's position if town or town position is null
                        final BlockPos originPos = (originTown != null && originTown.getPosition() != null) 
                            ? originTown.getPosition() 
                            : pos; // Use the block entity's position as fallback
                        
                        // Add all towns from the server to the refresh packet
                        townInterface.getAllTownsForDestination(serverLevel).forEach((id, townName) -> {
                            // Skip the current town (platforms shouldn't route to themselves)
                            if (originTown != null && id.equals(originTown.getId())) {
                                return; // Skip this iteration
                            }
                            
                            boolean isEnabled = destinations.getOrDefault(id, false);
                            
                            // Calculate distance between towns
                            int distance = 100; // Default fallback distance
                            String direction = ""; // Direction (N, NE, E, etc.)
                            
                            Town destTown = TownManager.get(serverLevel).getTown(id);
                            
                            if (destTown != null) {
                                BlockPos destPos = destTown.getPosition();
                                if (destPos != null) {
                                    // Calculate Euclidean distance in blocks
                                    double dx = destPos.getX() - originPos.getX();
                                    double dy = destPos.getY() - originPos.getY();
                                    double dz = destPos.getZ() - originPos.getZ();
                                    distance = (int)Math.sqrt(dx*dx + dy*dy + dz*dz);
                                    
                                    // Additional safety check: Skip towns with 0 distance (same location = same town)
                                    if (distance <= 1) {
                                        return; // Skip this iteration - likely the same town
                                    }
                                    
                                    // Calculate direction (ignore Y-axis)
                                    direction = calculateDirection(dx, dz);
                                }
                            }
                            
                            refreshPacket.addTown(id, townName, isEnabled, distance, direction);
                        });
                        
                        // Send the refresh packet back to the client
                        PlatformAccess.getNetworkMessages().sendToPlayer(refreshPacket, player);
                    }
                    
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                        "Successfully set destination {} to {} for platform {} at {}",
                        townId, enabled, platformId, pos);
                } else {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                        "SetPlatformDestinationPacket: Platform {} not found at {}", platformId, pos);
                }
                } else {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                        "SetPlatformDestinationPacket: Block entity at {} is not a TownInterfaceEntity", pos);
                }
            } catch (Exception e) {
                LOGGER.error("Error handling SetPlatformDestinationPacket", e);
                // Don't crash the server, but log the error
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
