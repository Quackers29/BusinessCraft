package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.platform.NetworkHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import com.quackers29.businesscraft.network.ModMessages;
import com.quackers29.businesscraft.network.packets.misc.PaymentResultPacket;

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
        // TODO: Implement specialized refresh destinations packet sending
        LOGGER.warn("sendRefreshDestinationsPacket not yet implemented for Forge");
    }
    
    public void sendRefreshPlatformsPacketToChunk(Object player, int x, int y, int z) {
        // TODO: Implement specialized refresh platforms packet sending to chunk
        LOGGER.warn("sendRefreshPlatformsPacketToChunk not yet implemented for Forge");
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
        
        // Create and send the payment board response packet
        com.quackers29.businesscraft.network.packets.storage.PaymentBoardResponsePacket packet = 
            new com.quackers29.businesscraft.network.packets.storage.PaymentBoardResponsePacket(unclaimedRewards);
        
        ModMessages.sendToPlayer(packet, serverPlayer);
        
        LOGGER.debug("Sent PaymentBoardResponsePacket to player {}: {} rewards", 
            serverPlayer.getName().getString(), unclaimedRewards.size());
    }
    
    public void sendBufferSlotStorageResponsePacket(Object player, Object bufferSlots) {
        // TODO: Implement specialized buffer slot storage response packet sending
        LOGGER.warn("sendBufferSlotStorageResponsePacket not yet implemented for Forge");
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
}