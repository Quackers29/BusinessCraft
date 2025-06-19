package com.yourdomain.businesscraft.network;

import com.yourdomain.businesscraft.BusinessCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

// Import all packet classes from their new package locations
import com.yourdomain.businesscraft.network.packets.town.ToggleTouristSpawningPacket;
import com.yourdomain.businesscraft.network.packets.town.SetTownNamePacket;
import com.yourdomain.businesscraft.network.packets.platform.SetSearchRadiusPacket;
import com.yourdomain.businesscraft.network.packets.platform.AddPlatformPacket;
import com.yourdomain.businesscraft.network.packets.platform.DeletePlatformPacket;
import com.yourdomain.businesscraft.network.packets.platform.SetPlatformEnabledPacket;
import com.yourdomain.businesscraft.network.packets.platform.SetPlatformPathPacket;
import com.yourdomain.businesscraft.network.packets.platform.ResetPlatformPathPacket;
import com.yourdomain.businesscraft.network.packets.platform.SetPlatformPathCreationModePacket;
import com.yourdomain.businesscraft.network.packets.platform.RefreshPlatformsPacket;
import com.yourdomain.businesscraft.network.packets.platform.SetPlatformDestinationPacket;
import com.yourdomain.businesscraft.network.packets.ui.SetPathCreationModePacket;
import com.yourdomain.businesscraft.network.packets.ui.OpenDestinationsUIPacket;
import com.yourdomain.businesscraft.network.packets.ui.RefreshDestinationsPacket;
import com.yourdomain.businesscraft.network.packets.ui.PlayerExitUIPacket;
import com.yourdomain.businesscraft.network.packets.ui.OpenTownInterfacePacket;
import com.yourdomain.businesscraft.network.packets.storage.TradeResourcePacket;
import com.yourdomain.businesscraft.network.packets.storage.CommunalStoragePacket;
import com.yourdomain.businesscraft.network.packets.storage.CommunalStorageResponsePacket;
import com.yourdomain.businesscraft.network.packets.storage.PaymentBoardResponsePacket;
import com.yourdomain.businesscraft.network.packets.storage.PaymentBoardRequestPacket;
import com.yourdomain.businesscraft.network.packets.storage.PaymentBoardClaimPacket;
import com.yourdomain.businesscraft.network.packets.storage.BufferStoragePacket;
import com.yourdomain.businesscraft.network.packets.storage.BufferStorageResponsePacket;
import com.yourdomain.businesscraft.network.packets.storage.PersonalStoragePacket;
import com.yourdomain.businesscraft.network.packets.storage.PersonalStorageResponsePacket;
import com.yourdomain.businesscraft.network.packets.storage.PersonalStorageRequestPacket;
import com.yourdomain.businesscraft.network.packets.misc.PaymentResultPacket;

/**
 * Handles registration and sending of network packets
 */
public class ModMessages {
    private static SimpleChannel INSTANCE;

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(BusinessCraft.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        // Register all packets using a consistent pattern
        net.messageBuilder(ToggleTouristSpawningPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ToggleTouristSpawningPacket::new)
                .encoder(ToggleTouristSpawningPacket::toBytes)
                .consumerMainThread(ToggleTouristSpawningPacket::handle)
                .add();
                
        net.messageBuilder(SetSearchRadiusPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetSearchRadiusPacket::new)
                .encoder(SetSearchRadiusPacket::toBytes)
                .consumerMainThread(SetSearchRadiusPacket::handle)
                .add();
        
        // Register the path creation packet
        // This packet has static encode/decode methods that delegate to instance methods
        net.messageBuilder(SetPathCreationModePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetPathCreationModePacket::decode)
                .encoder(SetPathCreationModePacket::encode)
                .consumerMainThread(SetPathCreationModePacket::handle)
                .add();
                
        // Register the town name change packet
        net.messageBuilder(SetTownNamePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetTownNamePacket::decode)
                .encoder(SetTownNamePacket::encode)
                .consumerMainThread(SetTownNamePacket::handle)
                .add();
                
        // Register platform-related packets
        net.messageBuilder(AddPlatformPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(AddPlatformPacket::decode)
                .encoder(AddPlatformPacket::encode)
                .consumerMainThread(AddPlatformPacket::handle)
                .add();
                
        net.messageBuilder(DeletePlatformPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(DeletePlatformPacket::decode)
                .encoder(DeletePlatformPacket::encode)
                .consumerMainThread(DeletePlatformPacket::handle)
                .add();
                
        net.messageBuilder(SetPlatformEnabledPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetPlatformEnabledPacket::decode)
                .encoder(SetPlatformEnabledPacket::encode)
                .consumerMainThread(SetPlatformEnabledPacket::handle)
                .add();
                
        net.messageBuilder(SetPlatformPathPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetPlatformPathPacket::decode)
                .encoder(SetPlatformPathPacket::encode)
                .consumerMainThread(SetPlatformPathPacket::handle)
                .add();
                
        net.messageBuilder(ResetPlatformPathPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ResetPlatformPathPacket::decode)
                .encoder(ResetPlatformPathPacket::encode)
                .consumerMainThread(ResetPlatformPathPacket::handle)
                .add();
                
        net.messageBuilder(SetPlatformPathCreationModePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(buf -> new SetPlatformPathCreationModePacket(buf))
                .encoder(SetPlatformPathCreationModePacket::encode)
                .consumerMainThread(SetPlatformPathCreationModePacket::handle)
                .add();
                
        net.messageBuilder(RefreshPlatformsPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(buf -> new RefreshPlatformsPacket(buf))
                .encoder(RefreshPlatformsPacket::encode)
                .consumerMainThread(RefreshPlatformsPacket::handle)
                .add();
                
        // Register platform destination packets
        net.messageBuilder(OpenDestinationsUIPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(OpenDestinationsUIPacket::decode)
                .encoder(OpenDestinationsUIPacket::encode)
                .consumerMainThread(OpenDestinationsUIPacket::handle)
                .add();
                
        net.messageBuilder(SetPlatformDestinationPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetPlatformDestinationPacket::new)
                .encoder(SetPlatformDestinationPacket::encode)
                .consumerMainThread(SetPlatformDestinationPacket::handle)
                .add();
                
        net.messageBuilder(RefreshDestinationsPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(RefreshDestinationsPacket::new)
                .encoder(RefreshDestinationsPacket::encode)
                .consumerMainThread(RefreshDestinationsPacket::handle)
                .add();

        // Register player exit UI packet
        net.messageBuilder(PlayerExitUIPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PlayerExitUIPacket::decode)
                .encoder(PlayerExitUIPacket::encode)
                .consumerMainThread(PlayerExitUIPacket::handle)
                .add();

        // Register resource trading packet
        net.messageBuilder(TradeResourcePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(TradeResourcePacket::decode)
                .encoder(TradeResourcePacket::encode)
                .consumerMainThread(TradeResourcePacket::handle)
                .add();
                
        // Register payment result packet (server to client)
        net.messageBuilder(PaymentResultPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PaymentResultPacket::decode)
                .encoder(PaymentResultPacket::encode)
                .consumerMainThread(PaymentResultPacket::handle)
                .add();
                
        // Register communal storage packets
        net.messageBuilder(CommunalStoragePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(CommunalStoragePacket::decode)
                .encoder(CommunalStoragePacket::encode)
                .consumerMainThread(CommunalStoragePacket::handle)
                .add();
                
        net.messageBuilder(CommunalStorageResponsePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(CommunalStorageResponsePacket::decode)
                .encoder(CommunalStorageResponsePacket::encode)
                .consumerMainThread(CommunalStorageResponsePacket::handle)
                .add();
                
        net.messageBuilder(PaymentBoardResponsePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PaymentBoardResponsePacket::decode)
                .encoder(PaymentBoardResponsePacket::encode)
                .consumerMainThread(PaymentBoardResponsePacket::handle)
                .add();
                
        net.messageBuilder(PaymentBoardRequestPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PaymentBoardRequestPacket::decode)
                .encoder(PaymentBoardRequestPacket::encode)
                .consumerMainThread(PaymentBoardRequestPacket::handle)
                .add();
                
        net.messageBuilder(PaymentBoardClaimPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PaymentBoardClaimPacket::decode)
                .encoder(PaymentBoardClaimPacket::encode)
                .consumerMainThread(PaymentBoardClaimPacket::handle)
                .add();
                
        net.messageBuilder(BufferStoragePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(BufferStoragePacket::decode)
                .encoder(BufferStoragePacket::encode)
                .consumerMainThread(BufferStoragePacket::handle)
                .add();
                
        net.messageBuilder(BufferStorageResponsePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(BufferStorageResponsePacket::decode)
                .encoder(BufferStorageResponsePacket::encode)
                .consumerMainThread(BufferStorageResponsePacket::handle)
                .add();
                
        // Register personal storage packets
        net.messageBuilder(PersonalStoragePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PersonalStoragePacket::decode)
                .encoder(PersonalStoragePacket::encode)
                .consumerMainThread(PersonalStoragePacket::handle)
                .add();
                
        net.messageBuilder(PersonalStorageResponsePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PersonalStorageResponsePacket::decode)
                .encoder(PersonalStorageResponsePacket::encode)
                .consumerMainThread(PersonalStorageResponsePacket::handle)
                .add();

        net.messageBuilder(PersonalStorageRequestPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PersonalStorageRequestPacket::decode)
                .encoder(PersonalStorageRequestPacket::encode)
                .consumerMainThread(PersonalStorageRequestPacket::handle)
                .add();
                
        // Register the open town interface packet for proper menu synchronization
        net.messageBuilder(OpenTownInterfacePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(OpenTownInterfacePacket::new)
                .encoder(OpenTownInterfacePacket::toBytes)
                .consumerMainThread(OpenTownInterfacePacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
    
    /**
     * Sends a packet to all clients tracking a specific chunk
     */
    public static <MSG> void sendToAllTrackingChunk(MSG message, Level level, BlockPos pos) {
        INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> 
            level.getChunkAt(pos)), message);
    }
} 