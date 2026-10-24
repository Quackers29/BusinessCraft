package com.quackers29.businesscraft.ui.managers;

import com.quackers29.businesscraft.api.ITownDataProvider.VisitHistoryRecord;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.network.packets.ui.VisitHistoryRequestPacket;
import com.quackers29.businesscraft.network.packets.ui.VisitHistoryResponsePacket;
import com.quackers29.businesscraft.ui.modal.factories.BCModalGridFactory;
import com.quackers29.businesscraft.ui.modal.specialized.BCModalGridScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Manages visitor history modal creation and handling.
 * Extracted from TownInterfaceScreen to improve code organization.
 * Refactored to use BaseModalManager for common functionality.
 */
public class VisitorHistoryManager extends BaseModalManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(VisitorHistoryManager.class);

    private record PendingOpen(
            Screen parentScreen,
            BlockPos blockPos,
            String targetTab,
            Consumer<BCModalGridScreen<VisitHistoryRecord>> onScreenClosed) {
    }

    private static PendingOpen pendingOpen;

    /**
     * Requests fresh visit history from the server, then opens the screen when the response arrives.
     */
    public static void showVisitorHistoryScreen(
            Screen parentScreen,
            BlockPos blockPos,
            String targetTab,
            Consumer<BCModalGridScreen<VisitHistoryRecord>> onScreenClosed) {

        validateParentScreen(parentScreen, "parentScreen");
        prepareParentScreen(parentScreen);

        pendingOpen = new PendingOpen(parentScreen, blockPos, targetTab, onScreenClosed);
        PlatformAccess.getNetworkMessages().sendToServer(new VisitHistoryRequestPacket(blockPos));
    }

    /**
     * Opens the visitor history screen using server-authoritative data.
     */
    public static void openFromServerResponse(VisitHistoryResponsePacket packet) {
        PendingOpen pending = pendingOpen;
        pendingOpen = null;
        if (pending == null) {
            LOGGER.warn("Visit history response with no pending open request");
            return;
        }
        if (packet.getBlockPos() != null && !packet.getBlockPos().equals(pending.blockPos())) {
            LOGGER.warn("Visit history response block pos mismatch: expected {}, got {}",
                    pending.blockPos(), packet.getBlockPos());
        }

        List<VisitHistoryRecord> visitHistory = new ArrayList<>();
        Map<UUID, String> townNames = new HashMap<>();
        for (VisitHistoryResponsePacket.Entry entry : packet.getEntries()) {
            visitHistory.add(new VisitHistoryRecord(
                    entry.timestamp(),
                    entry.originTownId(),
                    entry.count(),
                    entry.originPos()));
            if (entry.originTownId() != null && entry.originTownName() != null) {
                townNames.put(entry.originTownId(), entry.originTownName());
            }
        }

        Function<UUID, String> townNameLookup = townId -> {
            if (townId == null) {
                return "Unknown";
            }
            String name = townNames.get(townId);
            if (name != null && !name.isEmpty()) {
                return name;
            }
            return "Town-" + townId.toString().substring(0, 8);
        };

        openVisitorHistoryScreen(
                pending.parentScreen(),
                visitHistory,
                townNameLookup,
                pending.onScreenClosed());
    }

    private static void openVisitorHistoryScreen(
            Screen parentScreen,
            List<VisitHistoryRecord> visitHistory,
            Function<UUID, String> townNameLookup,
            Consumer<BCModalGridScreen<VisitHistoryRecord>> onScreenClosed) {

        BCModalGridScreen<VisitHistoryRecord> visitorScreen = BCModalGridFactory.createVisitorHistoryScreen(
                Component.literal("Town Visitor History"),
                parentScreen,
                visitHistory,
                createStandardCallback(parentScreen, onScreenClosed),
                townNameLookup);

        visitorScreen.withBackButtonText("Back")
                .withTitleScale(1.2f)
                .withRowHeight(20);

        displayModal(visitorScreen);
    }
}
