package com.quackers29.businesscraft.client.network;

import com.quackers29.businesscraft.api.ClientHelper;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.menu.StorageMenu;
import com.quackers29.businesscraft.menu.TradeMenu;
import com.quackers29.businesscraft.network.ClientPacketDispatcher;
import com.quackers29.businesscraft.network.packets.BaseViewModelSyncPacket;
import com.quackers29.businesscraft.network.packets.misc.PaymentResultPacket;
import com.quackers29.businesscraft.network.packets.storage.BufferSlotStorageResponsePacket;
import com.quackers29.businesscraft.network.packets.storage.BufferStorageResponsePacket;
import com.quackers29.businesscraft.network.packets.storage.CommunalStorageResponsePacket;
import com.quackers29.businesscraft.network.packets.storage.PaymentBoardResponsePacket;
import com.quackers29.businesscraft.network.packets.storage.PersonalStorageResponsePacket;
import com.quackers29.businesscraft.network.packets.ui.LeaderboardDataResponsePacket;
import com.quackers29.businesscraft.network.packets.ui.VisitHistoryResponsePacket;
import com.quackers29.businesscraft.ui.managers.VisitorHistoryManager;
import com.quackers29.businesscraft.network.packets.ui.TownMapDataResponsePacket;
import com.quackers29.businesscraft.network.packets.ui.TownPlatformDataResponsePacket;
import com.quackers29.businesscraft.network.packets.ui.ClientTownMapCache;
import com.quackers29.businesscraft.town.viewmodel.IViewModel;
import com.quackers29.businesscraft.ui.modal.specialized.BCModalInventoryScreen;
import com.quackers29.businesscraft.ui.modal.specialized.TownMapModal;
import com.quackers29.businesscraft.ui.screens.town.PaymentBoardScreen;
import com.quackers29.businesscraft.ui.screens.town.StorageScreen;
import com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen;
import com.quackers29.businesscraft.ui.screens.town.TownLeaderboardScreen;
import com.quackers29.businesscraft.ui.screens.town.TradeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Client-only packet reactions. Installed from the loader client entrypoints
 * so dedicated servers never load this class.
 */
public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void register() {
        ClientPacketDispatcher.setHandler(ClientPacketHandler::handle);
    }

    public static void handle(Object packet) {
        if (packet instanceof LeaderboardDataResponsePacket leaderboard) {
            openLeaderboard(leaderboard);
        } else if (packet instanceof VisitHistoryResponsePacket visitHistory) {
            VisitorHistoryManager.openFromServerResponse(visitHistory);
        } else if (packet instanceof BaseViewModelSyncPacket<?> viewModel) {
            applyViewModel(viewModel);
        } else if (packet instanceof PaymentResultPacket payment) {
            applyPayment(payment);
        } else if (packet instanceof BufferSlotStorageResponsePacket slots) {
            applyBufferSlots(slots);
        } else if (packet instanceof BufferStorageResponsePacket buffer) {
            applyBuffer(buffer);
        } else if (packet instanceof CommunalStorageResponsePacket communal) {
            applyCommunal(communal);
        } else if (packet instanceof PaymentBoardResponsePacket board) {
            applyPaymentBoard(board);
        } else if (packet instanceof PersonalStorageResponsePacket personal) {
            applyPersonal(personal);
        } else if (packet instanceof TownMapDataResponsePacket map) {
            applyTownMap(map);
        } else if (packet instanceof TownPlatformDataResponsePacket platforms) {
            applyPlatforms(platforms);
        }
    }

    private static void openLeaderboard(LeaderboardDataResponsePacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen currentScreen = minecraft.screen;
        if (minecraft.player != null) {
            TownLeaderboardScreen rankingScreen = new TownLeaderboardScreen(
                    Component.literal("Town Ranking"),
                    currentScreen,
                    minecraft.player.blockPosition(),
                    packet.getCurrentTownName());
            rankingScreen.setTownData(packet.getTowns());
            minecraft.setScreen(rankingScreen);
        }
    }

    private static void applyViewModel(BaseViewModelSyncPacket<?> packet) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            return;
        }
        BlockEntity be = level.getBlockEntity(packet.getPos());
        if (be instanceof TownInterfaceEntity entity) {
            IViewModel viewModel = packet.getViewModel();
            @SuppressWarnings("unchecked")
            Class<IViewModel> vmClass = (Class<IViewModel>) viewModel.getClass();
            entity.getVmCache().update(vmClass, viewModel);
            if (minecraft.screen instanceof TownInterfaceScreen screen) {
                screen.getMenu().refreshDataSlots();
            }
        }
    }

    private static void applyPayment(PaymentResultPacket packet) {
        ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper == null) {
            return;
        }
        clientHelper.executeOnClientThread(() -> {
            Object currentScreen = clientHelper.getCurrentScreen();
            if (currentScreen instanceof TradeScreen tradeScreen) {
                tradeScreen.setOutputItem(packet.getPaymentItem());
            } else if (currentScreen instanceof BCModalInventoryScreen<?> modalScreen
                    && modalScreen.getMenu() instanceof TradeMenu tradeMenu) {
                tradeMenu.setOutputItem(packet.getPaymentItem().copy());
            }
        });
    }

    private static void applyBufferSlots(BufferSlotStorageResponsePacket packet) {
        ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper == null) {
            return;
        }
        clientHelper.executeOnClientThread(() -> {
            Object currentScreen = clientHelper.getCurrentScreen();
            if (currentScreen instanceof PaymentBoardScreen paymentScreen) {
                paymentScreen.getMenu().updateBufferStorageSlots(packet.getSlotStorage());
            }
        });
    }

    private static void applyBuffer(BufferStorageResponsePacket packet) {
        ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper == null) {
            return;
        }
        clientHelper.executeOnClientThread(() -> {
            Object currentScreen = clientHelper.getCurrentScreen();
            if (currentScreen instanceof PaymentBoardScreen paymentScreen) {
                paymentScreen.updateBufferStorageItems(packet.getBufferItems());
            }
        });
    }

    private static void applyCommunal(CommunalStorageResponsePacket packet) {
        ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper == null) {
            return;
        }
        clientHelper.executeOnClientThread(() -> {
            Object currentScreen = clientHelper.getCurrentScreen();
            if (currentScreen instanceof StorageScreen storageScreen) {
                storageScreen.updateStorageItems(packet.getStorageItems());
            } else if (currentScreen instanceof BCModalInventoryScreen<?> modalScreen
                    && modalScreen.getMenu() instanceof StorageMenu storageMenu) {
                storageMenu.updateStorageItems(packet.getStorageItems());
            }
        });
    }

    private static void applyPaymentBoard(PaymentBoardResponsePacket packet) {
        ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper == null) {
            return;
        }
        clientHelper.executeOnClientThread(() -> {
            Object currentScreen = clientHelper.getCurrentScreen();
            if (currentScreen instanceof PaymentBoardScreen paymentScreen) {
                paymentScreen.updateRewardData(packet.getRewards());
                paymentScreen.getMenu().updateCachedRewards(packet.getRewards());
            }
        });
    }

    private static void applyPersonal(PersonalStorageResponsePacket packet) {
        ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper == null) {
            return;
        }
        clientHelper.executeOnClientThread(() -> {
            Object currentScreen = clientHelper.getCurrentScreen();
            if (currentScreen instanceof StorageScreen storageScreen) {
                storageScreen.updatePersonalStorageItems(packet.getStorageItems());
            } else if (currentScreen instanceof BCModalInventoryScreen<?> modalScreen
                    && modalScreen.getMenu() instanceof StorageMenu storageMenu) {
                storageMenu.updatePersonalStorageItems(packet.getStorageItems());
            }
        });
    }

    private static void applyTownMap(TownMapDataResponsePacket packet) {
        ClientTownMapCache.getInstance().updateTownData(packet.getTownData());
        ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper != null && clientHelper.getCurrentScreen() instanceof TownMapModal mapModal) {
            mapModal.refreshFromCache();
        }
    }

    private static void applyPlatforms(TownPlatformDataResponsePacket packet) {
        ClientTownMapCache.getInstance()
                .updateTownPlatformData(packet.getTownId(), packet.getPlatforms());
        TownPlatformDataResponsePacket.TownInfo townInfo = packet.getTownInfo();
        if (townInfo != null) {
            ClientTownMapCache.getInstance()
                    .updateTownInfo(packet.getTownId(), townInfo.name, townInfo.population, townInfo.touristCount);
        }
        ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper != null) {
            Object levelObj = clientHelper.getClientLevel();
            if (levelObj instanceof Level level) {
                BlockPos foundPos = findTownBlockPos(level, packet.getTownId());
                if (foundPos != null) {
                    BlockEntity be = level.getBlockEntity(foundPos);
                    if (be instanceof TownInterfaceEntity townEntity) {
                        townEntity.updateClientPlatformsFromPacket(platformTag(packet));
                    }
                }
            }
            if (clientHelper.getCurrentScreen() instanceof TownMapModal mapModal) {
                mapModal.refreshPlatformData(packet.getTownId(), packet.getPlatforms());
                if (townInfo != null) {
                    mapModal.refreshTownData(packet.getTownId(), townInfo);
                }
            }
        }
    }

    private static CompoundTag platformTag(TownPlatformDataResponsePacket packet) {
        CompoundTag platformTag = new CompoundTag();
        ListTag platformsTag = new ListTag();
        for (TownPlatformDataResponsePacket.PlatformInfo platform : packet.getPlatforms().values()) {
            com.quackers29.businesscraft.platform.Platform restored = new com.quackers29.businesscraft.platform.Platform(
                    platform.id);
            restored.setName(platform.name);
            restored.setEnabled(platform.enabled);
            restored.setStartPos(platform.startPos);
            restored.setEndPos(platform.endPos);
            restored.setSearchRadius(platform.searchRadius);
            restored.setSpread(platform.spread);
            restored.setInterfacePos(platform.interfacePos);
            for (java.util.UUID destId : platform.enabledDestinations) {
                restored.enableDestination(destId);
            }
            platformsTag.add(restored.toNBT());
        }
        platformTag.put("platforms", platformsTag);
        return platformTag;
    }

    private static BlockPos findTownBlockPos(Level level, java.util.UUID townId) {
        ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper == null) {
            return null;
        }
        Object playerObj = clientHelper.getClientPlayer();
        if (!(playerObj instanceof Player player)) {
            return null;
        }
        BlockPos playerPos = player.blockPosition();
        int searchRadius = 128;
        for (int x = -searchRadius; x <= searchRadius; x += 16) {
            for (int z = -searchRadius; z <= searchRadius; z += 16) {
                BlockPos chunkPos = playerPos.offset(x, 0, z);
                ChunkAccess chunk = level.getChunk(chunkPos);
                if (chunk == null) {
                    continue;
                }
                for (BlockPos pos : chunk.getBlockEntitiesPos()) {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof TownInterfaceEntity townEntity && townId.equals(townEntity.getTownId())) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }
}
