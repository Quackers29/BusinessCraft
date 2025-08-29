package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.platform.NetworkHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import com.quackers29.businesscraft.network.ModMessages;
import com.quackers29.businesscraft.network.packets.misc.PaymentResultPacket;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Forge implementation of the NetworkHelper interface using SimpleChannel.
 */
public class ForgeNetworkHelper implements NetworkHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeNetworkHelper.class);
    
    private final SimpleChannel channel;
    private final Map<Class<?>, Integer> packetIds = new HashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(0);
    
    public ForgeNetworkHelper(String modId, String channelVersion) {
        this.channel = NetworkRegistry.newSimpleChannel(
            new net.minecraft.resources.ResourceLocation(modId, "main"),
            () -> channelVersion,
            channelVersion::equals,
            channelVersion::equals
        );
    }
    
    @Override
    public <T> void registerClientToServerPacket(String packetId,
                                                Class<T> messageClass, 
                                                PacketEncoder<T> encoder,
                                                PacketDecoder<T> decoder, 
                                                PacketHandler<T> handler) {
        int id = nextId.getAndIncrement();
        packetIds.put(messageClass, id);
        
        channel.registerMessage(id, messageClass,
            encoder::encode,
            decoder::decode,
            (packet, ctx) -> {
                ctx.get().enqueueWork(() -> {
                    ServerPlayer player = ctx.get().getSender();
                    if (player != null) {
                        handler.handle(packet, player);
                    }
                });
                ctx.get().setPacketHandled(true);
            },
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }
    
    @Override
    public <T> void registerServerToClientPacket(String packetId,
                                                Class<T> messageClass,
                                                PacketEncoder<T> encoder,
                                                PacketDecoder<T> decoder,
                                                ClientPacketHandler<T> handler) {
        int id = nextId.getAndIncrement();
        packetIds.put(messageClass, id);
        
        channel.registerMessage(id, messageClass,
            encoder::encode,
            decoder::decode,
            (packet, ctx) -> {
                ctx.get().enqueueWork(() -> {
                    handler.handle(packet);
                });
                ctx.get().setPacketHandled(true);
            },
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }
    
    @Override
    public <T> void sendToServer(T packet) {
        channel.sendToServer(packet);
    }
    
    @Override
    public <T> void sendToClient(T packet, Object player) {
        ServerPlayer serverPlayer = (ServerPlayer) player;
        channel.send(PacketDistributor.PLAYER.with(() -> serverPlayer), packet);
    }
    
    @Override
    public <T> void sendToAllClients(T packet) {
        channel.send(PacketDistributor.ALL.noArg(), packet);
    }
    
    // ==== BUFFER ABSTRACTION METHODS FOR ENHANCED MULTILOADER PACKETS ====
    
    public Object createBuffer() {
        return new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
    }
    
    public void writeBlockPos(Object buffer, int x, int y, int z) {
        if (buffer instanceof FriendlyByteBuf buf) {
            buf.writeBlockPos(new BlockPos(x, y, z));
        }
    }
    
    public int[] readBlockPos(Object buffer) {
        if (buffer instanceof FriendlyByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            return new int[]{pos.getX(), pos.getY(), pos.getZ()};
        }
        return new int[]{0, 0, 0};
    }
    
    public void writeString(Object buffer, String value) {
        if (buffer instanceof FriendlyByteBuf buf) {
            buf.writeUtf(value);
        }
    }
    
    public String readString(Object buffer) {
        if (buffer instanceof FriendlyByteBuf buf) {
            return buf.readUtf();
        }
        return "";
    }
    
    public void writeBoolean(Object buffer, boolean value) {
        if (buffer instanceof FriendlyByteBuf buf) {
            buf.writeBoolean(value);
        }
    }
    
    public boolean readBoolean(Object buffer) {
        if (buffer instanceof FriendlyByteBuf buf) {
            return buf.readBoolean();
        }
        return false;
    }
    
    public void writeInt(Object buffer, int value) {
        if (buffer instanceof FriendlyByteBuf buf) {
            buf.writeInt(value);
        }
    }
    
    public int readInt(Object buffer) {
        if (buffer instanceof FriendlyByteBuf buf) {
            return buf.readInt();
        }
        return 0;
    }
    
    public void writeUUID(Object buffer, String uuid) {
        if (buffer instanceof FriendlyByteBuf buf) {
            buf.writeUUID(UUID.fromString(uuid));
        }
    }
    
    public String readUUID(Object buffer) {
        if (buffer instanceof FriendlyByteBuf buf) {
            return buf.readUUID().toString();
        }
        return UUID.randomUUID().toString();
    }
    
    public void writeItemStack(Object buffer, Object itemStack) {
        if (buffer instanceof FriendlyByteBuf buf && itemStack instanceof ItemStack stack) {
            buf.writeItem(stack);
        }
    }
    
    public Object readItemStack(Object buffer) {
        if (buffer instanceof FriendlyByteBuf buf) {
            return buf.readItem();
        }
        return ItemStack.EMPTY;
    }
    
    // ==== SPECIALIZED PACKET SENDING METHODS ====
    
    public void sendRefreshDestinationsPacket(Object player, int x, int y, int z, String platformId,
                                            Map<String, Boolean> townDestinations,
                                            Map<String, String> townNames,
                                            Map<String, Integer> distances,
                                            Map<String, String> directions) {
        LOGGER.debug("FORGE NETWORK HELPER: Implementing sendRefreshDestinationsPacket for platform {} at [{}, {}, {}]", 
            platformId, x, y, z);
        
        if (!(player instanceof ServerPlayer serverPlayer)) {
            LOGGER.warn("Player is not a ServerPlayer: {}", player);
            return;
        }
        
        try {
            // Get the block entity at the specified position
            BlockPos pos = new BlockPos(x, y, z);
            BlockEntity blockEntity = serverPlayer.level().getBlockEntity(pos);
            
            if (!(blockEntity instanceof com.quackers29.businesscraft.block.entity.TownInterfaceEntity townInterface)) {
                LOGGER.error("Block entity is not a TownInterfaceEntity at position: [{}, {}, {}]", x, y, z);
                return;
            }

            // Get the platform by ID
            java.util.UUID platformUUID = java.util.UUID.fromString(platformId);
            Object platformObj = townInterface.getPlatform(platformUUID);
            com.quackers29.businesscraft.platform.Platform platform = platformObj instanceof com.quackers29.businesscraft.platform.Platform ? (com.quackers29.businesscraft.platform.Platform) platformObj : null;
            if (platform == null) {
                LOGGER.warn("Platform not found with ID: {}", platformId);
                return;
            }

            // Gather town data using unified architecture (like main branch)
            java.util.Map<java.util.UUID, String> townNamesMap = new java.util.HashMap<>();
            java.util.Map<java.util.UUID, Boolean> enabledStateMap = new java.util.HashMap<>();
            java.util.Map<java.util.UUID, Integer> townDistancesMap = new java.util.HashMap<>();
            java.util.Map<java.util.UUID, String> townDirectionsMap = new java.util.HashMap<>();

            // Get all towns and populate data (main branch approach)
            try {
                com.quackers29.businesscraft.town.TownManager townManager = 
                    com.quackers29.businesscraft.town.TownManager.get(serverPlayer.serverLevel());
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Got TownManager: {}", townManager);
                
                java.util.Collection<com.quackers29.businesscraft.town.Town> allTowns = townManager.getAllTowns();
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "TownManager.getAllTowns() returned: {} towns", allTowns != null ? allTowns.size() : "null");
                
                BlockPos currentPos = townInterface.getBlockPos();
                java.util.UUID currentTownId = townInterface.getTownId();
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Current position: {}, Current town ID: {}", currentPos, currentTownId);
                
                if (allTowns != null) {
                    int townIndex = 0;
                    for (com.quackers29.businesscraft.town.Town town : allTowns) {
                        java.util.UUID townId = town.getId();
                        String townName = town.getName();
                        
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Processing town #{}: ID={}, Name={}", townIndex++, townId, townName);
                        
                        // Skip the current town
                        if (townId.equals(currentTownId)) {
                            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Skipping current town: {}", townName);
                            continue;
                        }
                        
                        townNamesMap.put(townId, townName);
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Added town '{}' to destinations", townName);
                        
                        // Calculate distance and direction
                        com.quackers29.businesscraft.api.ITownDataProvider.Position townPos = town.getPosition();
                        if (townPos != null) {
                            BlockPos townBlockPos = new BlockPos(townPos.getX(), townPos.getY(), townPos.getZ());
                            double distance = currentPos.distManhattan(townBlockPos);
                            townDistancesMap.put(townId, (int) distance);
                            
                            // Calculate direction
                            int dx = townPos.getX() - currentPos.getX();
                            int dz = townPos.getZ() - currentPos.getZ();
                            String direction = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? "East" : "West") : (dz > 0 ? "South" : "North");
                            townDirectionsMap.put(townId, direction);
                            
                            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Town '{}' at {} - distance: {}m, direction: {}", 
                                townName, townBlockPos, (int)distance, direction);
                        } else {
                            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Town '{}' has null position!", townName);
                        }
                        
                        // Get enabled state from platform
                        boolean enabled = platform.isDestinationEnabled(townId);
                        enabledStateMap.put(townId, enabled);
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Town '{}' destination enabled: {}", townName, enabled);
                    }
                } else {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "TownManager.getAllTowns() returned null!");
                }
            } catch (Exception e) {
                LOGGER.error("Failed to get town data for destinations: {}", e.getMessage(), e);
            }

            // Create and send RefreshDestinationsPacket directly (unified architecture)
            String platformName = "Platform #" + (townInterface.getPlatforms().indexOf(platform) + 1);
            com.quackers29.businesscraft.network.packets.ui.RefreshDestinationsPacket responsePacket = 
                new com.quackers29.businesscraft.network.packets.ui.RefreshDestinationsPacket(x, y, z, platformId, platformName);
            
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Creating packet with platform name: '{}'", platformName);
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "About to add {} towns to packet", townNamesMap.size());
            
            // Add all town data to the packet
            for (java.util.Map.Entry<java.util.UUID, String> entry : townNamesMap.entrySet()) {
                java.util.UUID townId = entry.getKey();
                String townName = entry.getValue();
                boolean enabled = enabledStateMap.getOrDefault(townId, false);
                int distance = townDistancesMap.getOrDefault(townId, 0);
                String direction = townDirectionsMap.getOrDefault(townId, "");
                
                responsePacket.addTown(townId, townName, enabled, distance, direction);
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Added to packet - Town: '{}', Enabled: {}, Distance: {}m, Direction: {}", 
                    townName, enabled, distance, direction);
            }

            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Packet created with {} towns total", townNamesMap.size());

            // Send to client using Forge networking
            ModMessages.sendToPlayer(responsePacket, serverPlayer);
            
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Successfully sent RefreshDestinationsPacket for platform '{}' with {} towns at [{}, {}, {}]", 
                platformId, townNamesMap.size(), x, y, z);
                
        } catch (Exception e) {
            LOGGER.error("Failed to send RefreshDestinationsPacket for platform '{}' at [{}, {}, {}]: {}", 
                platformId, x, y, z, e.getMessage());
        }
    }
    
    public void sendRefreshPlatformsPacketToChunk(Object player, int x, int y, int z) {
        // TODO: Implement specialized refresh platforms packet sending to chunk
        DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "sendRefreshPlatformsPacketToChunk not yet implemented for Forge");
    }
    
    public void sendPaymentResultPacket(Object player, Object paymentItemStack) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            LOGGER.warn("FORGE NETWORK HELPER: Player is not a ServerPlayer: {}", 
                player != null ? player.getClass().getSimpleName() : "null");
            return;
        }
        
        if (paymentItemStack == null) {
            LOGGER.warn("FORGE NETWORK HELPER: PaymentItemStack is null");
            return;
        }
        
        // Create and send the payment result packet
        PaymentResultPacket packet = new PaymentResultPacket(paymentItemStack);
        ModMessages.sendToPlayer(packet, serverPlayer);
        
        LOGGER.debug("Sent PaymentResultPacket to player {}: {}", 
            serverPlayer.getName().getString(), paymentItemStack);
    }
    
    public void sendPaymentBoardResponsePacket(Object player, List<Object> unclaimedRewards) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            LOGGER.warn("FORGE NETWORK HELPER: Player is not a ServerPlayer: {}", 
                player != null ? player.getClass().getSimpleName() : "null");
            return;
        }
        
        if (unclaimedRewards == null) {
            LOGGER.warn("FORGE NETWORK HELPER: Unclaimed rewards list is null");
            return;
        }
        
        // Refresh origin town names with current names before sending to client
        LOGGER.info("DEBUG: About to refresh {} reward names before sending to client", unclaimedRewards.size());
        List<Object> refreshedRewards = refreshOriginTownNames(unclaimedRewards, serverPlayer.serverLevel());
        
        // Create and send the payment board response packet with refreshed data
        com.quackers29.businesscraft.network.packets.storage.PaymentBoardResponsePacket packet = 
            new com.quackers29.businesscraft.network.packets.storage.PaymentBoardResponsePacket(refreshedRewards);
        
        ModMessages.sendToPlayer(packet, serverPlayer);
        
        LOGGER.debug("Sent PaymentBoardResponsePacket to player {}: {} rewards", 
            serverPlayer.getName().getString(), refreshedRewards.size());
    }
    
    /**
     * Refreshes origin town names in reward metadata with current town names
     * UNIFIED ARCHITECTURE: Server-side name resolution like map view system
     */
    private List<Object> refreshOriginTownNames(List<Object> rewards, net.minecraft.server.level.ServerLevel serverLevel) {
        List<Object> refreshedRewards = new ArrayList<>();
        int refreshedCount = 0;
        
        LOGGER.info("REWARD REFRESH DEBUG: Processing {} rewards for name refresh", rewards.size());
        
        for (Object reward : rewards) {
            if (reward instanceof com.quackers29.businesscraft.town.data.RewardEntry rewardEntry) {
                LOGGER.info("REWARD REFRESH DEBUG: Processing RewardEntry {}", rewardEntry.getId());
                
                // Create a copy of the reward with refreshed origin town name
                com.quackers29.businesscraft.town.data.RewardEntry refreshedReward = createRewardCopyWithRefreshedOriginName(rewardEntry, serverLevel);
                refreshedRewards.add(refreshedReward);
                
                // Check if name was actually refreshed
                String oldName = rewardEntry.getMetadata().get("originTown");
                String newName = refreshedReward.getMetadata().get("originTown");
                if (!java.util.Objects.equals(oldName, newName)) {
                    refreshedCount++;
                }
            } else {
                // Non-RewardEntry objects pass through unchanged
                refreshedRewards.add(reward);
                LOGGER.info("REWARD REFRESH DEBUG: Non-RewardEntry object passed through: {}", reward.getClass().getSimpleName());
            }
        }
        
        LOGGER.info("REWARD REFRESH DEBUG: Refreshed {} out of {} reward names", refreshedCount, rewards.size());
        return refreshedRewards;
    }
    
    /**
     * Creates a copy of a RewardEntry with refreshed origin town name
     */
    private com.quackers29.businesscraft.town.data.RewardEntry createRewardCopyWithRefreshedOriginName(
            com.quackers29.businesscraft.town.data.RewardEntry original, 
            net.minecraft.server.level.ServerLevel serverLevel) {
        
        // Get the origin town UUID from metadata
        String originTownId = original.getMetadata().get("originTownId");
        String currentOriginName = original.getMetadata().get("originTown");
        
        LOGGER.info("REFRESH DETAIL: Reward {} has originTownId='{}', current originTown='{}'", 
            original.getId(), originTownId, currentOriginName);
        
        if (originTownId != null && !originTownId.isEmpty()) {
            try {
                java.util.UUID townUUID = java.util.UUID.fromString(originTownId);
                LOGGER.info("REFRESH DETAIL: Looking up town with UUID {}", townUUID);
                
                // Get current town name from server
                com.quackers29.businesscraft.town.Town town = com.quackers29.businesscraft.town.TownManager.get(serverLevel).getTown(townUUID);
                if (town != null) {
                    String currentTownName = town.getName();
                    LOGGER.info("REFRESH DETAIL: Found town {} with current name '{}'", townUUID, currentTownName);
                    
                    // Create a copy with updated origin town name
                    com.quackers29.businesscraft.town.data.RewardEntry copy = com.quackers29.businesscraft.town.data.RewardEntry.fromNetworkWithMetadata(
                        original.getId(),
                        original.getTimestamp(),
                        original.getExpirationTime(),
                        original.getSource(),
                        original.getRewards(),
                        original.getStatus(),
                        original.getEligibility(),
                        new java.util.HashMap<>(original.getMetadata())
                    );
                    
                    // Update the origin town name to current name
                    copy.addMetadata("originTown", currentTownName);
                    
                    LOGGER.info("REFRESH DETAIL: Updated reward {} origin town: '{}' -> '{}'", 
                        original.getId(), currentOriginName, currentTownName);
                    
                    return copy;
                } else {
                    LOGGER.warn("REFRESH DETAIL: Town with UUID {} not found in TownManager!", townUUID);
                }
            } catch (IllegalArgumentException e) {
                LOGGER.warn("REFRESH DETAIL: Invalid UUID in reward metadata: '{}'", originTownId);
            }
        } else {
            LOGGER.info("REFRESH DETAIL: Reward {} has no originTownId metadata", original.getId());
        }
        
        // Return original if we can't refresh
        LOGGER.info("REFRESH DETAIL: Returning original reward {} unchanged", original.getId());
        return original;
    }
    
    public void sendBufferSlotStorageResponsePacket(Object player, Object bufferSlots) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            LOGGER.warn("FORGE NETWORK HELPER: Player is not a ServerPlayer: {}", 
                player != null ? player.getClass().getSimpleName() : "null");
            return;
        }

        if (bufferSlots == null) {
            LOGGER.warn("FORGE NETWORK HELPER: Buffer slots is null");
            return;
        }

        if (!(bufferSlots instanceof com.quackers29.businesscraft.town.data.SlotBasedStorage slotStorage)) {
            LOGGER.warn("FORGE NETWORK HELPER: Buffer slots is not SlotBasedStorage: {}", 
                bufferSlots.getClass().getSimpleName());
            return;
        }

        // Convert SlotBasedStorage to Map<Integer, Object> for packet transmission
        Map<Integer, Object> slotMap = new HashMap<>();
        for (int i = 0; i < slotStorage.getSlotCount(); i++) {
            net.minecraft.world.item.ItemStack stack = slotStorage.getSlot(i);
            if (!stack.isEmpty()) {
                slotMap.put(i, stack);
            }
        }

        // Get block position from player's currently interacting town interface
        // For now we'll use the player's position - this should be improved to get the actual block position
        net.minecraft.core.BlockPos playerPos = serverPlayer.blockPosition();

        // Create and send the buffer slot storage response packet
        com.quackers29.businesscraft.network.packets.storage.BufferSlotStorageResponsePacket packet = 
            new com.quackers29.businesscraft.network.packets.storage.BufferSlotStorageResponsePacket(
                playerPos.getX(), playerPos.getY(), playerPos.getZ(), slotMap);

        ModMessages.sendToPlayer(packet, serverPlayer);

        LOGGER.debug("Sent BufferSlotStorageResponsePacket to player {}: {} slots with items", 
            serverPlayer.getName().getString(), slotMap.size());
    }
    
    /**
     * Gets the underlying SimpleChannel for Forge-specific operations.
     * This is Forge-specific functionality.
     */
    public SimpleChannel getChannel() {
        return channel;
    }
    
    /**
     * Gets the packet ID for a registered packet class.
     * This is Forge-specific functionality.
     */
    public Integer getPacketId(Class<?> packetClass) {
        return packetIds.get(packetClass);
    }
    
    @Override
    public void writeRewardEntry(Object buffer, Object rewardEntry) {
        if (!(buffer instanceof FriendlyByteBuf buf)) {
            throw new IllegalArgumentException("Buffer must be a FriendlyByteBuf for Forge");
        }
        
        if (!(rewardEntry instanceof com.quackers29.businesscraft.town.data.RewardEntry entry)) {
            throw new IllegalArgumentException("RewardEntry must be a BusinessCraft RewardEntry for Forge");
        }
        
        try {
            // Serialize all RewardEntry data preserving UUID and metadata
            buf.writeUUID(entry.getId());
            buf.writeLong(entry.getTimestamp());
            buf.writeLong(entry.getExpirationTime());
            buf.writeEnum(entry.getSource());
            buf.writeEnum(entry.getStatus());
            buf.writeUtf(entry.getEligibility());
            
            // Serialize items
            buf.writeInt(entry.getRewards().size());
            for (net.minecraft.world.item.ItemStack stack : entry.getRewards()) {
                buf.writeItem(stack);
            }
            
            // Serialize metadata
            buf.writeInt(entry.getMetadata().size());
            for (java.util.Map.Entry<String, String> metaEntry : entry.getMetadata().entrySet()) {
                buf.writeUtf(metaEntry.getKey());
                buf.writeUtf(metaEntry.getValue());
            }
            
            LOGGER.debug("Serialized RewardEntry: ID={}, source={}, items={}", 
                entry.getId(), entry.getSource(), entry.getRewards().size());
                
        } catch (Exception e) {
            LOGGER.error("Failed to write RewardEntry to buffer", e);
            throw new RuntimeException("Failed to serialize RewardEntry", e);
        }
    }
    
    @Override
    public Object readRewardEntry(Object buffer) {
        if (!(buffer instanceof FriendlyByteBuf buf)) {
            throw new IllegalArgumentException("Buffer must be a FriendlyByteBuf for Forge");
        }
        
        try {
            // Deserialize all RewardEntry data preserving UUID and metadata
            java.util.UUID id = buf.readUUID();
            long timestamp = buf.readLong();
            long expirationTime = buf.readLong();
            com.quackers29.businesscraft.town.data.RewardSource source = buf.readEnum(com.quackers29.businesscraft.town.data.RewardSource.class);
            com.quackers29.businesscraft.town.data.ClaimStatus status = buf.readEnum(com.quackers29.businesscraft.town.data.ClaimStatus.class);
            String eligibility = buf.readUtf();
            
            // Deserialize items
            int itemCount = buf.readInt();
            java.util.List<net.minecraft.world.item.ItemStack> items = new java.util.ArrayList<>();
            for (int i = 0; i < itemCount; i++) {
                items.add(buf.readItem());
            }
            
            // Deserialize metadata
            int metaCount = buf.readInt();
            java.util.Map<String, String> metadata = new java.util.HashMap<>();
            for (int i = 0; i < metaCount; i++) {
                String key = buf.readUtf();
                String value = buf.readUtf();
                metadata.put(key, value);
            }
            
            // Create a RewardEntry using the private constructor to preserve original data
            com.quackers29.businesscraft.town.data.RewardEntry entry = createRewardEntryWithOriginalData(
                id, timestamp, expirationTime, source, items, status, eligibility, metadata);
            
            LOGGER.debug("Deserialized RewardEntry: preservedID={}, source={}, items={}, timestamp={}, expiration={}", 
                entry.getId(), source, items.size(), timestamp, expirationTime);
            
            return entry;
            
        } catch (Exception e) {
            LOGGER.error("Failed to read RewardEntry from buffer", e);
            throw new RuntimeException("Failed to deserialize RewardEntry", e);
        }
    }
    
    /**
     * Create a RewardEntry using the private constructor to preserve original timestamp and UUID
     */
    private com.quackers29.businesscraft.town.data.RewardEntry createRewardEntryWithOriginalData(
            java.util.UUID id, long timestamp, long expirationTime, 
            com.quackers29.businesscraft.town.data.RewardSource source,
            java.util.List<net.minecraft.world.item.ItemStack> items,
            com.quackers29.businesscraft.town.data.ClaimStatus status,
            String eligibility, java.util.Map<String, String> metadata) {
        
        try {
            // Get the private constructor
            java.lang.reflect.Constructor<com.quackers29.businesscraft.town.data.RewardEntry> constructor =
                com.quackers29.businesscraft.town.data.RewardEntry.class.getDeclaredConstructor(
                    java.util.UUID.class, long.class, long.class,
                    com.quackers29.businesscraft.town.data.RewardSource.class,
                    java.util.List.class,
                    com.quackers29.businesscraft.town.data.ClaimStatus.class,
                    String.class, java.util.Map.class
                );
            
            // Make it accessible
            constructor.setAccessible(true);
            
            // Create the instance with original data
            com.quackers29.businesscraft.town.data.RewardEntry entry = constructor.newInstance(
                id, timestamp, expirationTime, source, items, status, eligibility, metadata);
            
            LOGGER.debug("Successfully created RewardEntry with preserved data using reflection");
            return entry;
            
        } catch (Exception e) {
            LOGGER.error("Failed to create RewardEntry with preserved data, falling back to public constructor", e);
            
            // Fallback to public constructor if reflection fails
            com.quackers29.businesscraft.town.data.RewardEntry entry = 
                new com.quackers29.businesscraft.town.data.RewardEntry(source, items, eligibility);
            
            // Store original data in metadata as backup
            entry.addMetadata("originalUUID", id.toString());
            entry.addMetadata("originalTimestamp", String.valueOf(timestamp));
            entry.addMetadata("originalExpirationTime", String.valueOf(expirationTime));
            
            return entry;
        }
    }
    
    @Override
    public void sendVisitorHistoryResponsePacket(Object player, com.quackers29.businesscraft.network.packets.ui.VisitorHistoryResponsePacket packet) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            LOGGER.warn("FORGE NETWORK HELPER: Player is not a ServerPlayer: {}", 
                player != null ? player.getClass().getSimpleName() : "null");
            return;
        }
        
        if (packet == null) {
            LOGGER.warn("FORGE NETWORK HELPER: Visitor history response packet is null");
            return;
        }
        
        // Send the packet to the client
        ModMessages.sendToPlayer(packet, serverPlayer);
        
        LOGGER.debug("Sent VisitorHistoryResponsePacket to player {}: {} entries", 
            serverPlayer.getName().getString(), packet.getEntries().size());
    }
}