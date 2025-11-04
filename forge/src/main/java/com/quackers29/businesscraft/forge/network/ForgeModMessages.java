package com.quackers29.businesscraft.forge.network;

import com.quackers29.businesscraft.forge.platform.ForgeNetworkHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Function;
import java.util.function.Supplier;

// Import all packet classes from common module
import com.quackers29.businesscraft.network.packets.town.ToggleTouristSpawningPacket;
import com.quackers29.businesscraft.network.packets.town.SetTownNamePacket;
import com.quackers29.businesscraft.network.packets.platform.SetSearchRadiusPacket;
import com.quackers29.businesscraft.network.packets.platform.AddPlatformPacket;
import com.quackers29.businesscraft.network.packets.platform.DeletePlatformPacket;
import com.quackers29.businesscraft.network.packets.platform.SetPlatformEnabledPacket;
import com.quackers29.businesscraft.network.packets.platform.SetPlatformPathPacket;
import com.quackers29.businesscraft.network.packets.platform.ResetPlatformPathPacket;
import com.quackers29.businesscraft.network.packets.platform.SetPlatformPathCreationModePacket;
import com.quackers29.businesscraft.network.packets.platform.RefreshPlatformsPacket;
import com.quackers29.businesscraft.network.packets.platform.SetPlatformDestinationPacket;
import com.quackers29.businesscraft.network.packets.ui.SetPathCreationModePacket;
import com.quackers29.businesscraft.network.packets.ui.OpenDestinationsUIPacket;
import com.quackers29.businesscraft.network.packets.ui.RefreshDestinationsPacket;
import com.quackers29.businesscraft.network.packets.ui.PlayerExitUIPacket;
import com.quackers29.businesscraft.network.packets.ui.PlatformVisualizationPacket;
import com.quackers29.businesscraft.network.packets.ui.BoundarySyncRequestPacket;
import com.quackers29.businesscraft.network.packets.ui.BoundarySyncResponsePacket;
import com.quackers29.businesscraft.network.packets.ui.OpenTownInterfacePacket;
import com.quackers29.businesscraft.network.packets.ui.OpenPaymentBoardPacket;
import com.quackers29.businesscraft.network.packets.ui.RequestTownMapDataPacket;
import com.quackers29.businesscraft.network.packets.ui.TownMapDataResponsePacket;
import com.quackers29.businesscraft.network.packets.ui.RequestTownPlatformDataPacket;
import com.quackers29.businesscraft.network.packets.ui.TownPlatformDataResponsePacket;
import com.quackers29.businesscraft.network.packets.storage.TradeResourcePacket;
import com.quackers29.businesscraft.network.packets.storage.CommunalStoragePacket;
import com.quackers29.businesscraft.network.packets.storage.CommunalStorageResponsePacket;
import com.quackers29.businesscraft.network.packets.storage.PaymentBoardResponsePacket;
import com.quackers29.businesscraft.network.packets.storage.PaymentBoardRequestPacket;
import com.quackers29.businesscraft.network.packets.storage.PaymentBoardClaimPacket;
import com.quackers29.businesscraft.network.packets.storage.BufferStoragePacket;
import com.quackers29.businesscraft.network.packets.storage.BufferStorageResponsePacket;
import com.quackers29.businesscraft.network.packets.storage.BufferSlotStorageResponsePacket;
import com.quackers29.businesscraft.network.packets.storage.PersonalStoragePacket;
import com.quackers29.businesscraft.network.packets.storage.PersonalStorageRequestPacket;
import com.quackers29.businesscraft.network.packets.storage.PersonalStorageResponsePacket;
import com.quackers29.businesscraft.network.packets.misc.PaymentResultPacket;
import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;

/**
 * Forge-specific network message registration and handling
 */
public class ForgeModMessages {
    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        ForgeNetworkHelper networkHelper = (ForgeNetworkHelper) com.quackers29.businesscraft.forge.BusinessCraftForge.NETWORK;
        SimpleChannel net = networkHelper.getChannel();

        // Register all packets using a consistent pattern
        // Platform-agnostic: wrap handler to convert Supplier<NetworkEvent.Context> to Object
        net.messageBuilder(ToggleTouristSpawningPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ToggleTouristSpawningPacket::new)
                .encoder(ToggleTouristSpawningPacket::toBytes)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get()); // Pass NetworkEvent.Context as Object
                    // Packet already calls setPacketHandled internally via PlatformAccess
                })
                .add();

        net.messageBuilder(SetSearchRadiusPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetSearchRadiusPacket::new)
                .encoder(SetSearchRadiusPacket::toBytes)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get()); // Pass NetworkEvent.Context as Object
                    // Packet already calls setPacketHandled internally via PlatformAccess
                })
                .add();

        // Register the path creation packet
        net.messageBuilder(SetPathCreationModePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetPathCreationModePacket::decode)
                .encoder(SetPathCreationModePacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        // Register the town name change packet
        net.messageBuilder(SetTownNamePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetTownNamePacket::decode)
                .encoder(SetTownNamePacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get()); // Pass NetworkEvent.Context as Object
                    // Packet already calls setPacketHandled internally via PlatformAccess
                })
                .add();

        // Register platform-related packets
        net.messageBuilder(AddPlatformPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(AddPlatformPacket::decode)
                .encoder(AddPlatformPacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(DeletePlatformPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(DeletePlatformPacket::decode)
                .encoder(DeletePlatformPacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(SetPlatformEnabledPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetPlatformEnabledPacket::decode)
                .encoder(SetPlatformEnabledPacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(SetPlatformPathPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetPlatformPathPacket::decode)
                .encoder(SetPlatformPathPacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(ResetPlatformPathPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ResetPlatformPathPacket::decode)
                .encoder(ResetPlatformPathPacket::encode)
                .consumerMainThread(ResetPlatformPathPacket::handle)
                .add();

        net.messageBuilder(SetPlatformPathCreationModePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetPlatformPathCreationModePacket::decode)
                .encoder(SetPlatformPathCreationModePacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(SetPathCreationModePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetPathCreationModePacket::decode)
                .encoder(SetPathCreationModePacket::encode)
                .consumerMainThread(SetPathCreationModePacket::handle)
                .add();

        net.messageBuilder(RefreshPlatformsPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(RefreshPlatformsPacket::decode)
                .encoder(RefreshPlatformsPacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(SetPlatformDestinationPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetPlatformDestinationPacket::decode)
                .encoder(SetPlatformDestinationPacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        // UI-related packets
        net.messageBuilder(OpenDestinationsUIPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(OpenDestinationsUIPacket::decode)
                .encoder(OpenDestinationsUIPacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(RefreshDestinationsPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(RefreshDestinationsPacket::decode)
                .encoder(RefreshDestinationsPacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(PlayerExitUIPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PlayerExitUIPacket::decode)
                .encoder(PlayerExitUIPacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(PlatformVisualizationPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PlatformVisualizationPacket::decode)
                .encoder(PlatformVisualizationPacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(BoundarySyncRequestPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(BoundarySyncRequestPacket::decode)
                .encoder(BoundarySyncRequestPacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(BoundarySyncResponsePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(BoundarySyncResponsePacket::decode)
                .encoder(BoundarySyncResponsePacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(OpenTownInterfacePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(OpenTownInterfacePacket::decode)
                .encoder(OpenTownInterfacePacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(OpenPaymentBoardPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(OpenPaymentBoardPacket::decode)
                .encoder(OpenPaymentBoardPacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(RequestTownMapDataPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(RequestTownMapDataPacket::decode)
                .encoder(RequestTownMapDataPacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(TownMapDataResponsePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(TownMapDataResponsePacket::decode)
                .encoder(TownMapDataResponsePacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(RequestTownPlatformDataPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(RequestTownPlatformDataPacket::decode)
                .encoder(RequestTownPlatformDataPacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(TownPlatformDataResponsePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(TownPlatformDataResponsePacket::decode)
                .encoder(TownPlatformDataResponsePacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        // Storage-related packets
        net.messageBuilder(TradeResourcePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(TradeResourcePacket::decode)
                .encoder(TradeResourcePacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(CommunalStoragePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(CommunalStoragePacket::decode)
                .encoder(CommunalStoragePacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(CommunalStorageResponsePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(CommunalStorageResponsePacket::decode)
                .encoder(CommunalStorageResponsePacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(PaymentBoardResponsePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PaymentBoardResponsePacket::decode)
                .encoder(PaymentBoardResponsePacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(PaymentBoardRequestPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PaymentBoardRequestPacket::decode)
                .encoder(PaymentBoardRequestPacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(PaymentBoardClaimPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PaymentBoardClaimPacket::decode)
                .encoder(PaymentBoardClaimPacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(BufferStoragePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(BufferStoragePacket::decode)
                .encoder(BufferStoragePacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(BufferStorageResponsePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(BufferStorageResponsePacket::decode)
                .encoder(BufferStorageResponsePacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(BufferSlotStorageResponsePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(BufferSlotStorageResponsePacket::decode)
                .encoder(BufferSlotStorageResponsePacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(PersonalStoragePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PersonalStoragePacket::decode)
                .encoder(PersonalStoragePacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(PersonalStorageRequestPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PersonalStorageRequestPacket::decode)
                .encoder(PersonalStorageRequestPacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        net.messageBuilder(PersonalStorageResponsePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PersonalStorageResponsePacket::decode)
                .encoder(PersonalStorageResponsePacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();

        // Misc packets
        net.messageBuilder(PaymentResultPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PaymentResultPacket::decode)
                .encoder(PaymentResultPacket::encode)
                .consumerMainThread((msg, ctxSupplier) -> {
                    msg.handle(ctxSupplier.get());
                })
                .add();
    }

    public static void sendToServer(Object message) {
        ForgeNetworkHelper networkHelper = (ForgeNetworkHelper) com.quackers29.businesscraft.forge.BusinessCraftForge.NETWORK;
        networkHelper.sendToServer(message);
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        ForgeNetworkHelper networkHelper = (ForgeNetworkHelper) com.quackers29.businesscraft.forge.BusinessCraftForge.NETWORK;
        networkHelper.sendToPlayer(message, player);
    }

    public static void sendToAllPlayers(Object message) {
        ForgeNetworkHelper networkHelper = (ForgeNetworkHelper) com.quackers29.businesscraft.forge.BusinessCraftForge.NETWORK;
        networkHelper.sendToAllPlayers(message);
    }

    public static void sendToAllTrackingChunk(Object message, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        ForgeNetworkHelper networkHelper = (ForgeNetworkHelper) com.quackers29.businesscraft.forge.BusinessCraftForge.NETWORK;
        networkHelper.sendToAllTrackingChunk(message, level, pos);
    }
}
