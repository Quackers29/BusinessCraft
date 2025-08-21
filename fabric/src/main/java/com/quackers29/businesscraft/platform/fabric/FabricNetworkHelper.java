package com.quackers29.businesscraft.platform.fabric;

import com.quackers29.businesscraft.platform.NetworkHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fabric implementation of NetworkHelper matching the common interface.
 * Implements cross-platform networking using Fabric Networking API.
 */
public class FabricNetworkHelper implements NetworkHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricNetworkHelper.class);
    
    private final Map<Class<?>, Identifier> packetIds = new HashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(0);
    private final String modId;
    
    public FabricNetworkHelper(String modId) {
        this.modId = modId;
    }
    
    @Override
    public <T> void registerClientToServerPacket(String packetId,
                                                Class<T> packetClass, 
                                                PacketEncoder<T> encoder,
                                                PacketDecoder<T> decoder, 
                                                PacketHandler<T> handler) {
        Identifier id = new Identifier(modId, packetId);
        packetIds.put(packetClass, id);
        
        ServerPlayNetworking.registerGlobalReceiver(id, (server, player, handler1, buf, responseSender) -> {
            T packet = decoder.decode(buf);
            server.execute(() -> handler.handle(packet, player));
        });
    }
    
    @Override
    public <T> void registerServerToClientPacket(String packetId,
                                                Class<T> packetClass,
                                                PacketEncoder<T> encoder,
                                                PacketDecoder<T> decoder,
                                                ClientPacketHandler<T> handler) {
        Identifier id = new Identifier(modId, packetId);
        packetIds.put(packetClass, id);
        
        ClientPlayNetworking.registerGlobalReceiver(id, (client, handler1, buf, responseSender) -> {
            T packet = decoder.decode(buf);
            client.execute(() -> handler.handle(packet));
        });
    }
    
    @Override
    public <T> void sendToServer(T packet) {
        com.quackers29.businesscraft.network.FabricModMessages.sendToServer(packet);
    }
    
    @Override
    public <T> void sendToClient(T packet, Object player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            LOGGER.warn("Player is not a ServerPlayerEntity: {}", player);
            return;
        }
        
        com.quackers29.businesscraft.network.FabricModMessages.sendToPlayer(packet, serverPlayer);
    }
    
    @Override
    public <T> void sendToAllClients(T packet) {
        com.quackers29.businesscraft.network.FabricModMessages.sendToAllClients(packet);
    }
    
    // ==== BUFFER ABSTRACTION METHODS ====
    
    @Override
    public Object createBuffer() {
        return PacketByteBufs.create();
    }
    
    @Override
    public void writeBlockPos(Object buffer, int x, int y, int z) {
        if (buffer instanceof PacketByteBuf buf) {
            buf.writeBlockPos(new BlockPos(x, y, z));
        }
    }
    
    @Override
    public int[] readBlockPos(Object buffer) {
        if (buffer instanceof PacketByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            return new int[]{pos.getX(), pos.getY(), pos.getZ()};
        }
        return new int[]{0, 0, 0};
    }
    
    @Override
    public void writeString(Object buffer, String value) {
        if (buffer instanceof PacketByteBuf buf) {
            buf.writeString(value);
        }
    }
    
    @Override
    public String readString(Object buffer) {
        if (buffer instanceof PacketByteBuf buf) {
            return buf.readString();
        }
        return "";
    }
    
    @Override
    public void writeBoolean(Object buffer, boolean value) {
        if (buffer instanceof PacketByteBuf buf) {
            buf.writeBoolean(value);
        }
    }
    
    @Override
    public boolean readBoolean(Object buffer) {
        if (buffer instanceof PacketByteBuf buf) {
            return buf.readBoolean();
        }
        return false;
    }
    
    @Override
    public void writeInt(Object buffer, int value) {
        if (buffer instanceof PacketByteBuf buf) {
            buf.writeInt(value);
        }
    }
    
    @Override
    public int readInt(Object buffer) {
        if (buffer instanceof PacketByteBuf buf) {
            return buf.readInt();
        }
        return 0;
    }
    
    @Override
    public void writeUUID(Object buffer, String uuid) {
        if (buffer instanceof PacketByteBuf buf) {
            buf.writeUuid(UUID.fromString(uuid));
        }
    }
    
    @Override
    public String readUUID(Object buffer) {
        if (buffer instanceof PacketByteBuf buf) {
            return buf.readUuid().toString();
        }
        return UUID.randomUUID().toString();
    }
    
    @Override
    public void writeItemStack(Object buffer, Object itemStack) {
        if (buffer instanceof PacketByteBuf buf && itemStack instanceof ItemStack stack) {
            buf.writeItemStack(stack);
        }
    }
    
    @Override
    public Object readItemStack(Object buffer) {
        if (buffer instanceof PacketByteBuf buf) {
            return buf.readItemStack();
        }
        return ItemStack.EMPTY;
    }
    
    // ==== SPECIALIZED PACKET SENDING METHODS ====
    
    @Override
    public void sendRefreshDestinationsPacket(Object player, int x, int y, int z, String platformId,
                                            Map<String, Boolean> townDestinations,
                                            Map<String, String> townNames,
                                            Map<String, Integer> distances,
                                            Map<String, String> directions) {
        LOGGER.debug("FABRIC NETWORK HELPER: sendRefreshDestinationsPacket not yet implemented");
        // TODO: Implement Fabric-specific destination refresh packet
    }
    
    @Override
    public void sendRefreshPlatformsPacketToChunk(Object player, int x, int y, int z) {
        LOGGER.debug("FABRIC NETWORK HELPER: sendRefreshPlatformsPacketToChunk not yet implemented");
        // TODO: Implement Fabric-specific platform refresh packet
    }
    
    @Override
    public void sendPaymentResultPacket(Object player, Object paymentItemStack) {
        LOGGER.debug("FABRIC NETWORK HELPER: sendPaymentResultPacket not yet implemented");
        // TODO: Implement Fabric-specific payment result packet
    }
    
    @Override
    public void sendPaymentBoardResponsePacket(Object player, List<Object> unclaimedRewards) {
        LOGGER.debug("FABRIC NETWORK HELPER: sendPaymentBoardResponsePacket not yet implemented");
        // TODO: Implement Fabric-specific payment board response packet
    }
    
    @Override
    public void sendBufferSlotStorageResponsePacket(Object player, Object bufferSlots) {
        LOGGER.debug("FABRIC NETWORK HELPER: sendBufferSlotStorageResponsePacket not yet implemented");
        // TODO: Implement Fabric-specific buffer storage response packet
    }
    
    @Override
    public void writeRewardEntry(Object buffer, Object rewardEntry) {
        LOGGER.debug("FABRIC NETWORK HELPER: writeRewardEntry not yet implemented");
        // TODO: Implement Fabric-specific reward entry serialization
    }
    
    @Override
    public Object readRewardEntry(Object buffer) {
        LOGGER.debug("FABRIC NETWORK HELPER: readRewardEntry not yet implemented");
        // TODO: Implement Fabric-specific reward entry deserialization
        return null;
    }
    
    @Override
    public void sendVisitorHistoryResponsePacket(Object player, com.quackers29.businesscraft.network.packets.ui.VisitorHistoryResponsePacket packet) {
        LOGGER.debug("FABRIC NETWORK HELPER: sendVisitorHistoryResponsePacket not yet implemented");
        // TODO: Implement Fabric-specific visitor history response packet
    }
}