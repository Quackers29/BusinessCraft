package com.quackers29.businesscraft.network;

import com.quackers29.businesscraft.BusinessCraft;
import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.platform.forge.ForgeNetworkHelper;
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
// TODO: Migrate remaining UI packets to common module
// import com.quackers29.businesscraft.network.packets.ui.SetPathCreationModePacket;
import com.quackers29.businesscraft.network.packets.ui.OpenDestinationsUIPacket; // ✅ MIGRATED
import com.quackers29.businesscraft.network.packets.ui.RefreshDestinationsPacket; // ✅ MIGRATED
import com.quackers29.businesscraft.network.packets.ui.PlayerExitUIPacket; // ✅ MIGRATED
// import com.quackers29.businesscraft.network.packets.ui.PlatformVisualizationPacket;
import com.quackers29.businesscraft.network.packets.ui.BoundarySyncRequestPacket; // ✅ MIGRATED
// import com.quackers29.businesscraft.network.packets.ui.BoundarySyncResponsePacket;
import com.quackers29.businesscraft.network.packets.ui.OpenTownInterfacePacket;
import com.quackers29.businesscraft.network.packets.ui.OpenPaymentBoardPacket;
// TODO: Migrate remaining UI response packets to common module
import com.quackers29.businesscraft.network.packets.ui.RequestTownMapDataPacket; // ✅ MIGRATED
import com.quackers29.businesscraft.network.packets.ui.TownMapDataResponsePacket; // ✅ MIGRATED
// import com.quackers29.businesscraft.network.packets.ui.RequestTownPlatformDataPacket;
// import com.quackers29.businesscraft.network.packets.ui.TownPlatformDataResponsePacket;
import com.quackers29.businesscraft.network.packets.storage.TradeResourcePacket;
// TODO: Migrate remaining storage packets to common module
import com.quackers29.businesscraft.network.packets.storage.CommunalStoragePacket; // ✅ MIGRATED
import com.quackers29.businesscraft.network.packets.storage.CommunalStorageResponsePacket; // ✅ MIGRATED
// import com.quackers29.businesscraft.network.packets.storage.PaymentBoardResponsePacket;
import com.quackers29.businesscraft.network.packets.storage.PaymentBoardRequestPacket;
import com.quackers29.businesscraft.network.packets.storage.PaymentBoardClaimPacket;
// TODO: Migrate remaining buffer and personal storage packets to common module  
// import com.quackers29.businesscraft.network.packets.storage.BufferStoragePacket;
// import com.quackers29.businesscraft.network.packets.storage.BufferStorageResponsePacket;
// import com.quackers29.businesscraft.network.packets.storage.BufferSlotStorageResponsePacket;
// import com.quackers29.businesscraft.network.packets.storage.PersonalStoragePacket;
// import com.quackers29.businesscraft.network.packets.storage.PersonalStorageResponsePacket;
// import com.quackers29.businesscraft.network.packets.storage.PersonalStorageRequestPacket;
import com.quackers29.businesscraft.network.packets.misc.PaymentResultPacket;

/**
 * Platform-agnostic network packet manager.
 * 
 * MIGRATION STATUS: ✅ PARTIALLY MIGRATED
 * - Now uses ForgeNetworkHelper internally via PlatformServices
 * - Maintains full backward compatibility with existing packet registrations
 * - 37 packets still registered using Forge SimpleChannel (preservation of existing functionality)
 * - Send methods remain unchanged for existing code compatibility
 * - Foundation ready for future platform abstraction enhancements
 * 
 * ARCHITECTURE: ModMessages → ForgeNetworkHelper → SimpleChannel
 */
public class ModMessages {
    private static SimpleChannel INSTANCE;
    private static ForgeNetworkHelper networkHelper;

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        // Get the ForgeNetworkHelper instance from platform services
        networkHelper = (ForgeNetworkHelper) PlatformServices.getNetworkHelper();
        
        // Get the underlying SimpleChannel for backward compatibility
        SimpleChannel net = networkHelper.getChannel();
        INSTANCE = net;

        // Register all packets using a consistent pattern
        net.messageBuilder(ToggleTouristSpawningPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ToggleTouristSpawningPacket::new)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();
                
        net.messageBuilder(SetSearchRadiusPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetSearchRadiusPacket::new)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();
        
        // TODO: Migrate SetPathCreationModePacket to common module
        // Register the path creation packet
        // This packet has static encode/decode methods that delegate to instance methods
        // net.messageBuilder(SetPathCreationModePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
        //         .decoder(SetPathCreationModePacket::decode)
        //         .encoder(SetPathCreationModePacket::encode)
        //         .consumerMainThread(SetPathCreationModePacket::handle)
        //         .add();
                
        // Register the town name change packet
        net.messageBuilder(SetTownNamePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetTownNamePacket::new)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();
                
        // Register platform-related packets
        net.messageBuilder(AddPlatformPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(AddPlatformPacket::new)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();
                
        net.messageBuilder(DeletePlatformPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(DeletePlatformPacket::new)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();
                
        net.messageBuilder(SetPlatformEnabledPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetPlatformEnabledPacket::decode)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();
                
        net.messageBuilder(SetPlatformPathPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetPlatformPathPacket::decode)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();
                
        net.messageBuilder(ResetPlatformPathPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ResetPlatformPathPacket::decode)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();
                
        net.messageBuilder(SetPlatformPathCreationModePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetPlatformPathCreationModePacket::decode)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();
                
        net.messageBuilder(RefreshPlatformsPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(RefreshPlatformsPacket::new)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();
                
        // Register platform destination packets
        net.messageBuilder(OpenDestinationsUIPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(OpenDestinationsUIPacket::decode)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();
                
        net.messageBuilder(SetPlatformDestinationPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetPlatformDestinationPacket::decode)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();
                
        net.messageBuilder(RefreshDestinationsPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(RefreshDestinationsPacket::decode)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();

        // Register player exit UI packet
        net.messageBuilder(PlayerExitUIPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PlayerExitUIPacket::decode)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();

        // Register platform visualization packet (server to client)
        // TODO: Migrate PlatformVisualizationPacket to common module
        // net.messageBuilder(PlatformVisualizationPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
        //         .decoder(PlatformVisualizationPacket::decode)
        //         .encoder(PlatformVisualizationPacket::encode)
        //         .consumerMainThread(PlatformVisualizationPacket::handle)
        //         .add();

        // Register resource trading packet
        net.messageBuilder(TradeResourcePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(TradeResourcePacket::decode)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();
                
        // Register payment result packet (server to client)
        net.messageBuilder(PaymentResultPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PaymentResultPacket::decode)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();
                
        // Register communal storage packets
        net.messageBuilder(CommunalStoragePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(CommunalStoragePacket::decode)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();
                
        net.messageBuilder(CommunalStorageResponsePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(CommunalStorageResponsePacket::decode)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();
                
        // TODO: Migrate PaymentBoardResponsePacket to common module
        // net.messageBuilder(PaymentBoardResponsePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
        //         .decoder(PaymentBoardResponsePacket::decode)
        //         .encoder(PaymentBoardResponsePacket::encode)
        //         .consumerMainThread(PaymentBoardResponsePacket::handle)
        //         .add();
                
        net.messageBuilder(PaymentBoardRequestPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PaymentBoardRequestPacket::decode)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();
                
        net.messageBuilder(PaymentBoardClaimPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PaymentBoardClaimPacket::decode)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();
                
        // TODO: Migrate BufferStoragePacket to common module
        // net.messageBuilder(BufferStoragePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
        //         .decoder(BufferStoragePacket::decode)
        //         .encoder(BufferStoragePacket::encode)
        //         .consumerMainThread(BufferStoragePacket::handle)
        //         .add();
                
        // TODO: Migrate BufferStorageResponsePacket to common module
        // net.messageBuilder(BufferStorageResponsePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
        //         .decoder(BufferStorageResponsePacket::decode)
        //         .encoder(BufferStorageResponsePacket::encode)
        //         .consumerMainThread(BufferStorageResponsePacket::handle)
        //         .add();
                
        // Register new slot-based buffer storage response packet
        // TODO: Migrate BufferSlotStorageResponsePacket to common module
        // net.messageBuilder(BufferSlotStorageResponsePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
        //         .decoder(BufferSlotStorageResponsePacket::decode)
        //         .encoder(BufferSlotStorageResponsePacket::encode)
        //         .consumerMainThread(BufferSlotStorageResponsePacket::handle)
        //         .add();
                
        // Register personal storage packets
        // TODO: Migrate PersonalStoragePacket to common module
        // net.messageBuilder(PersonalStoragePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
        //         .decoder(PersonalStoragePacket::decode)
        //         .encoder(PersonalStoragePacket::encode)
        //         .consumerMainThread(PersonalStoragePacket::handle)
        //         .add();
                
        // TODO: Migrate PersonalStorageResponsePacket to common module
        // net.messageBuilder(PersonalStorageResponsePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
        //         .decoder(PersonalStorageResponsePacket::decode)
        //         .encoder(PersonalStorageResponsePacket::encode)
        //         .consumerMainThread(PersonalStorageResponsePacket::handle)
        //         .add();

        // TODO: Migrate PersonalStorageRequestPacket to common module
        // net.messageBuilder(PersonalStorageRequestPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
        //         .decoder(PersonalStorageRequestPacket::decode)
        //         .encoder(PersonalStorageRequestPacket::encode)
        //         .consumerMainThread(PersonalStorageRequestPacket::handle)
        //         .add();
                
        // Register the open town interface packet for proper menu synchronization
        net.messageBuilder(OpenTownInterfacePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(OpenTownInterfacePacket::decode)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();
                
        // Register the open payment board packet for proper menu synchronization
        net.messageBuilder(OpenPaymentBoardPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(OpenPaymentBoardPacket::decode)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();
                
        // Register town map data packets
        net.messageBuilder(RequestTownMapDataPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(RequestTownMapDataPacket::decode)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();
                
        net.messageBuilder(TownMapDataResponsePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(TownMapDataResponsePacket::decode)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();
                
        // Register town platform data packets
        // TODO: Migrate RequestTownPlatformDataPacket to common module
        // net.messageBuilder(RequestTownPlatformDataPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
        //         .decoder(RequestTownPlatformDataPacket::decode)
        //         .encoder((msg, buf) -> msg.encode(buf))
        //         .consumerMainThread(RequestTownPlatformDataPacket::handle)
        //         .add();
                
        // TODO: Migrate TownPlatformDataResponsePacket to common module
        // net.messageBuilder(TownPlatformDataResponsePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
        //         .decoder(TownPlatformDataResponsePacket::decode)
        //         .encoder((msg, buf) -> msg.encode(buf))
        //         .consumerMainThread(TownPlatformDataResponsePacket::handle)
        //         .add();
                
        // Register boundary sync packets for real-time boundary updates
        net.messageBuilder(BoundarySyncRequestPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(BoundarySyncRequestPacket::decode)
                .encoder((msg, buf) -> msg.encode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handle(ctx.get().getSender());
                    });
                    ctx.get().setPacketHandled(true);
                })
                .add();
                
        // TODO: Migrate BoundarySyncResponsePacket to common module
        // net.messageBuilder(BoundarySyncResponsePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
        //         .decoder(BoundarySyncResponsePacket::decode)
        //         .encoder(BoundarySyncResponsePacket::encode)
        //         .consumerMainThread(BoundarySyncResponsePacket::handle)
        //         .add();
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