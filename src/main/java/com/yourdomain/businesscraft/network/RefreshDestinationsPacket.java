package com.yourdomain.businesscraft.network;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
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
import com.yourdomain.businesscraft.screen.DestinationsScreen;

/**
 * Packet sent from server to client to refresh the destinations UI
 */
public class RefreshDestinationsPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final BlockPos pos;
    private final UUID platformId;
    private final Map<UUID, String> townNames = new HashMap<>();
    private final Map<UUID, Boolean> enabledState = new HashMap<>();
    private final Map<UUID, Integer> townDistances = new HashMap<>();
    private final Map<UUID, String> townDirections = new HashMap<>(); // Store directions
    
    public RefreshDestinationsPacket(BlockPos pos, UUID platformId) {
        this.pos = pos;
        this.platformId = platformId;
    }
    
    public RefreshDestinationsPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.platformId = buf.readUUID();
        
        // Read town names and enabled states
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            UUID id = buf.readUUID();
            String name = buf.readUtf();
            boolean enabled = buf.readBoolean();
            int distance = buf.readInt(); // Read distance
            String direction = buf.readUtf(); // Read direction
            townNames.put(id, name);
            enabledState.put(id, enabled);
            townDistances.put(id, distance);
            townDirections.put(id, direction);
        }
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUUID(platformId);
        
        // Write town names and enabled states
        buf.writeInt(townNames.size());
        for (Map.Entry<UUID, String> entry : townNames.entrySet()) {
            UUID townId = entry.getKey();
            buf.writeUUID(townId);
            buf.writeUtf(entry.getValue());
            buf.writeBoolean(enabledState.getOrDefault(townId, false));
            buf.writeInt(townDistances.getOrDefault(townId, 0));
            buf.writeUtf(townDirections.getOrDefault(townId, ""));
        }
    }
    
    /**
     * Add town data to this packet
     * @param townId The town ID
     * @param name The town name
     * @param enabled Whether this town is enabled as a destination
     * @param distance Distance to this town in meters
     * @param direction Cardinal direction to this town (N, NE, E, SE, S, SW, W, NW)
     */
    public void addTown(UUID townId, String name, boolean enabled, int distance, String direction) {
        townNames.put(townId, name);
        enabledState.put(townId, enabled);
        townDistances.put(townId, distance);
        townDirections.put(townId, direction);
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            // This runs on the client side
            Minecraft minecraft = Minecraft.getInstance();
            Level level = minecraft.level;
            
            if (level != null) {
                BlockEntity be = level.getBlockEntity(pos);
                
                if (be instanceof TownBlockEntity townBlock) {
                    // Find the platform with this ID
                    Platform platform = null;
                    for (Platform p : townBlock.getPlatforms()) {
                        if (p.getId().equals(platformId)) {
                            platform = p;
                            break;
                        }
                    }
                    
                    if (platform != null) {
                        // Open destinations screen
                        minecraft.setScreen(new DestinationsScreen(
                            pos, 
                            platformId, 
                            platform.getName(),
                            townNames,
                            enabledState,
                            townDistances,
                            townDirections
                        ));
                    }
                }
            }
        });
        
        return true;
    }
} 