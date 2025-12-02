package com.quackers29.businesscraft.network;

import com.quackers29.businesscraft.network.packets.town.ToggleTouristSpawningPacket;
import com.quackers29.businesscraft.network.packets.town.SetTownNamePacket;
import com.quackers29.businesscraft.network.packets.platform.*;
import com.quackers29.businesscraft.network.packets.ui.*;
import com.quackers29.businesscraft.network.packets.storage.*;
import com.quackers29.businesscraft.network.packets.misc.PaymentResultPacket;
import com.quackers29.businesscraft.network.packets.debug.RequestTownDataPacket;
import com.quackers29.businesscraft.network.packets.debug.TownDataResponsePacket;
import com.quackers29.businesscraft.network.packets.ResourceSyncPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Central registry for all network packets.
 * This allows defining packets once in common code and registering them
 * in platform-specific network handlers.
 */
public class PacketRegistry {
        public static final String MOD_ID = "businesscraft";
        private static final List<PacketDefinition<?>> PACKETS = new ArrayList<>();

        public enum NetworkDirection {
                PLAY_TO_SERVER,
                PLAY_TO_CLIENT
        }

        public record PacketDefinition<MSG>(
                        Class<MSG> packetClass,
                        String name,
                        NetworkDirection direction,
                        Function<FriendlyByteBuf, MSG> decoder,
                        BiConsumer<MSG, FriendlyByteBuf> encoder,
                        BiConsumer<MSG, Object> handler) {
        }

        static {
                registerPackets();
        }

        public static List<PacketDefinition<?>> getPackets() {
                return PACKETS;
        }

        private static <MSG> void register(
                        Class<MSG> packetClass,
                        String name,
                        NetworkDirection direction,
                        Function<FriendlyByteBuf, MSG> decoder,
                        BiConsumer<MSG, FriendlyByteBuf> encoder,
                        BiConsumer<MSG, Object> handler) {
                PACKETS.add(new PacketDefinition<>(packetClass, name, direction, decoder, encoder, handler));
        }

        private static void registerPackets() {
                // Server-bound packets (PLAY_TO_SERVER)
                register(ToggleTouristSpawningPacket.class, "toggle_tourist_spawning_packet",
                                NetworkDirection.PLAY_TO_SERVER,
                                ToggleTouristSpawningPacket::new, ToggleTouristSpawningPacket::toBytes,
                                ToggleTouristSpawningPacket::handle);

                register(SetTownNamePacket.class, "set_town_name_packet", NetworkDirection.PLAY_TO_SERVER,
                                SetTownNamePacket::decode, SetTownNamePacket::encode, SetTownNamePacket::handle);

                register(SetSearchRadiusPacket.class, "set_search_radius_packet", NetworkDirection.PLAY_TO_SERVER,
                                SetSearchRadiusPacket::new, SetSearchRadiusPacket::toBytes,
                                SetSearchRadiusPacket::handle);

                register(SetPathCreationModePacket.class, "set_path_creation_mode_packet",
                                NetworkDirection.PLAY_TO_SERVER,
                                SetPathCreationModePacket::decode, SetPathCreationModePacket::encode,
                                SetPathCreationModePacket::handle);

                register(AddPlatformPacket.class, "add_platform_packet", NetworkDirection.PLAY_TO_SERVER,
                                AddPlatformPacket::decode, AddPlatformPacket::encode, AddPlatformPacket::handle);

                register(DeletePlatformPacket.class, "delete_platform_packet", NetworkDirection.PLAY_TO_SERVER,
                                DeletePlatformPacket::decode, DeletePlatformPacket::encode,
                                DeletePlatformPacket::handle);

                register(SetPlatformEnabledPacket.class, "set_platform_enabled_packet", NetworkDirection.PLAY_TO_SERVER,
                                SetPlatformEnabledPacket::decode, SetPlatformEnabledPacket::encode,
                                SetPlatformEnabledPacket::handle);

                register(SetPlatformPathPacket.class, "set_platform_path_packet", NetworkDirection.PLAY_TO_SERVER,
                                SetPlatformPathPacket::decode, SetPlatformPathPacket::encode,
                                SetPlatformPathPacket::handle);

                register(ResetPlatformPathPacket.class, "reset_platform_path_packet", NetworkDirection.PLAY_TO_SERVER,
                                ResetPlatformPathPacket::decode, ResetPlatformPathPacket::encode,
                                ResetPlatformPathPacket::handle);

                register(SetPlatformPathCreationModePacket.class, "set_platform_path_creation_mode_packet",
                                NetworkDirection.PLAY_TO_SERVER,
                                SetPlatformPathCreationModePacket::decode, SetPlatformPathCreationModePacket::encode,
                                SetPlatformPathCreationModePacket::handle);

                register(SetPlatformDestinationPacket.class, "set_platform_destination_packet",
                                NetworkDirection.PLAY_TO_SERVER,
                                SetPlatformDestinationPacket::decode, SetPlatformDestinationPacket::encode,
                                SetPlatformDestinationPacket::handle);

                register(OpenDestinationsUIPacket.class, "open_destinations_uipacket", NetworkDirection.PLAY_TO_SERVER,
                                OpenDestinationsUIPacket::decode, OpenDestinationsUIPacket::encode,
                                OpenDestinationsUIPacket::handle);

                register(PlayerExitUIPacket.class, "player_exit_uipacket", NetworkDirection.PLAY_TO_SERVER,
                                PlayerExitUIPacket::decode, PlayerExitUIPacket::encode, PlayerExitUIPacket::handle);

                register(BoundarySyncRequestPacket.class, "boundary_sync_request_packet",
                                NetworkDirection.PLAY_TO_SERVER,
                                BoundarySyncRequestPacket::decode, BoundarySyncRequestPacket::encode,
                                BoundarySyncRequestPacket::handle);

                register(RequestTownPlatformDataPacket.class, "request_town_platform_data_packet",
                                NetworkDirection.PLAY_TO_SERVER,
                                RequestTownPlatformDataPacket::decode, RequestTownPlatformDataPacket::encode,
                                RequestTownPlatformDataPacket::handle);

                register(TradeResourcePacket.class, "trade_resource_packet", NetworkDirection.PLAY_TO_SERVER,
                                TradeResourcePacket::decode, TradeResourcePacket::encode, TradeResourcePacket::handle);

                register(CommunalStoragePacket.class, "communal_storage_packet", NetworkDirection.PLAY_TO_SERVER,
                                CommunalStoragePacket::decode, CommunalStoragePacket::encode,
                                CommunalStoragePacket::handle);

                register(PaymentBoardRequestPacket.class, "payment_board_request_packet",
                                NetworkDirection.PLAY_TO_SERVER,
                                PaymentBoardRequestPacket::decode, PaymentBoardRequestPacket::encode,
                                PaymentBoardRequestPacket::handle);

                register(PaymentBoardClaimPacket.class, "payment_board_claim_packet", NetworkDirection.PLAY_TO_SERVER,
                                PaymentBoardClaimPacket::decode, PaymentBoardClaimPacket::encode,
                                PaymentBoardClaimPacket::handle);

                register(BufferStoragePacket.class, "buffer_storage_packet", NetworkDirection.PLAY_TO_SERVER,
                                BufferStoragePacket::decode, BufferStoragePacket::encode, BufferStoragePacket::handle);

                register(PersonalStoragePacket.class, "personal_storage_packet", NetworkDirection.PLAY_TO_SERVER,
                                PersonalStoragePacket::decode, PersonalStoragePacket::encode,
                                PersonalStoragePacket::handle);

                register(PersonalStorageRequestPacket.class, "personal_storage_request_packet",
                                NetworkDirection.PLAY_TO_SERVER,
                                PersonalStorageRequestPacket::decode, PersonalStorageRequestPacket::encode,
                                PersonalStorageRequestPacket::handle);

                register(RequestTownDataPacket.class, "request_town_data_packet", NetworkDirection.PLAY_TO_SERVER,
                                RequestTownDataPacket::decode, RequestTownDataPacket::encode,
                                RequestTownDataPacket::handle);

                register(OpenPaymentBoardPacket.class, "open_payment_board_packet", NetworkDirection.PLAY_TO_SERVER,
                                OpenPaymentBoardPacket::decode, OpenPaymentBoardPacket::encode,
                                OpenPaymentBoardPacket::handle);

                register(OpenContractBoardPacket.class, "open_contract_board_packet", NetworkDirection.PLAY_TO_SERVER,
                                OpenContractBoardPacket::decode, OpenContractBoardPacket::encode,
                                OpenContractBoardPacket::handle);

                register(OpenTownInterfacePacket.class, "open_town_interface_packet", NetworkDirection.PLAY_TO_SERVER,
                                OpenTownInterfacePacket::decode, OpenTownInterfacePacket::encode,
                                OpenTownInterfacePacket::handle);

                register(BidContractPacket.class, "bid_contract_packet", NetworkDirection.PLAY_TO_SERVER,
                                BidContractPacket::decode, BidContractPacket::encode,
                                BidContractPacket::handle);

                register(AcceptContractPacket.class, "accept_contract_packet", NetworkDirection.PLAY_TO_SERVER,
                                AcceptContractPacket::decode, AcceptContractPacket::encode,
                                AcceptContractPacket::handle);

                // Client-bound packets (PLAY_TO_CLIENT)
                register(RefreshPlatformsPacket.class, "refresh_platforms_packet", NetworkDirection.PLAY_TO_CLIENT,
                                RefreshPlatformsPacket::decode, RefreshPlatformsPacket::encode,
                                RefreshPlatformsPacket::handle);

                register(RefreshDestinationsPacket.class, "refresh_destinations_packet",
                                NetworkDirection.PLAY_TO_CLIENT,
                                RefreshDestinationsPacket::decode, RefreshDestinationsPacket::encode,
                                RefreshDestinationsPacket::handle);

                register(PlatformVisualizationPacket.class, "platform_visualization_packet",
                                NetworkDirection.PLAY_TO_CLIENT,
                                PlatformVisualizationPacket::decode, PlatformVisualizationPacket::encode,
                                PlatformVisualizationPacket::handle);

                register(BoundarySyncResponsePacket.class, "boundary_sync_response_packet",
                                NetworkDirection.PLAY_TO_CLIENT,
                                BoundarySyncResponsePacket::decode, BoundarySyncResponsePacket::encode,
                                BoundarySyncResponsePacket::handle);

                register(TownMapDataResponsePacket.class, "town_map_data_response_packet",
                                NetworkDirection.PLAY_TO_CLIENT,
                                TownMapDataResponsePacket::decode, TownMapDataResponsePacket::encode,
                                TownMapDataResponsePacket::handle);

                register(TownPlatformDataResponsePacket.class, "town_platform_data_response_packet",
                                NetworkDirection.PLAY_TO_CLIENT,
                                TownPlatformDataResponsePacket::decode, TownPlatformDataResponsePacket::encode,
                                TownPlatformDataResponsePacket::handle);

                register(CommunalStorageResponsePacket.class, "communal_storage_response_packet",
                                NetworkDirection.PLAY_TO_CLIENT,
                                CommunalStorageResponsePacket::decode, CommunalStorageResponsePacket::encode,
                                CommunalStorageResponsePacket::handle);

                register(PaymentBoardResponsePacket.class, "payment_board_response_packet",
                                NetworkDirection.PLAY_TO_CLIENT,
                                PaymentBoardResponsePacket::decode, PaymentBoardResponsePacket::encode,
                                PaymentBoardResponsePacket::handle);

                register(BufferStorageResponsePacket.class, "buffer_storage_response_packet",
                                NetworkDirection.PLAY_TO_CLIENT,
                                BufferStorageResponsePacket::decode, BufferStorageResponsePacket::encode,
                                BufferStorageResponsePacket::handle);

                register(BufferSlotStorageResponsePacket.class, "buffer_slot_storage_response_packet",
                                NetworkDirection.PLAY_TO_CLIENT,
                                BufferSlotStorageResponsePacket::decode, BufferSlotStorageResponsePacket::encode,
                                BufferSlotStorageResponsePacket::handle);

                register(PersonalStorageResponsePacket.class, "personal_storage_response_packet",
                                NetworkDirection.PLAY_TO_CLIENT,
                                PersonalStorageResponsePacket::decode, PersonalStorageResponsePacket::encode,
                                PersonalStorageResponsePacket::handle);

                register(PaymentResultPacket.class, "payment_result_packet", NetworkDirection.PLAY_TO_CLIENT,
                                PaymentResultPacket::decode, PaymentResultPacket::encode, PaymentResultPacket::handle);

                register(TownDataResponsePacket.class, "town_data_response_packet", NetworkDirection.PLAY_TO_CLIENT,
                                TownDataResponsePacket::decode, TownDataResponsePacket::encode,
                                TownDataResponsePacket::handle);

                register(ResourceSyncPacket.class, "resource_sync_packet", NetworkDirection.PLAY_TO_CLIENT,
                                ResourceSyncPacket::decode, ResourceSyncPacket::encode, ResourceSyncPacket::handle);

                register(ContractSyncPacket.class, "contract_sync_packet", NetworkDirection.PLAY_TO_CLIENT,
                                ContractSyncPacket::decode, ContractSyncPacket::encode, ContractSyncPacket::handle);
        }
}
