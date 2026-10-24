package com.quackers29.businesscraft.platform;

import com.quackers29.businesscraft.api.ITouristHelper;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.PlatformInterfaceBlock;
import com.quackers29.businesscraft.block.entity.PlatformInterfaceEntity;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.menu.TownInterfaceMenu;
import com.quackers29.businesscraft.network.LoadedTownAccess;
import com.quackers29.businesscraft.network.packets.ui.PlatformInterfaceSyncPacket;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side link checks, the platform screen snapshot, and kiosk actions.
 * Actions are authorised by standing at the kiosk. The client never sends the
 * town centre position.
 */
public final class PlatformInterfaceService {
    private static Component inactiveMessage() {
        return Component.translatable("gui.businesscraft.platform_interface.inactive").withStyle(ChatFormatting.RED);
    }

    private PlatformInterfaceService() {
    }

    public record LiveLink(Town town, TownInterfaceEntity centre, Platform platform, boolean known) {
        public static LiveLink unknown() {
            return new LiveLink(null, null, null, false);
        }

        public static LiveLink gone() {
            return new LiveLink(null, null, null, true);
        }

        public static LiveLink live(Town town, TownInterfaceEntity centre, Platform platform) {
            return new LiveLink(town, centre, platform, true);
        }

        public boolean live() {
            return known && platform != null;
        }
    }

    /**
     * @param loadChunk when false, a centre chunk that is not already loaded
     *                  leaves the link as unknown instead of loading it
     */
    public static LiveLink resolve(ServerLevel level, PlatformInterfaceEntity kiosk, boolean loadChunk) {
        if (kiosk == null || kiosk.getTownId() == null || kiosk.getPlatformId() == null) {
            return LiveLink.gone();
        }
        Town town = TownManager.get(level).getTown(kiosk.getTownId());
        if (town == null || town.getPosition() == null) {
            return LiveLink.gone();
        }
        if (!loadChunk && !level.hasChunkAt(town.getPosition())) {
            return LiveLink.unknown();
        }
        BlockEntity blockEntity = level.getBlockEntity(town.getPosition());
        if (!(blockEntity instanceof TownInterfaceEntity centre)) {
            return LiveLink.gone();
        }
        Platform platform = centre.getPlatform(kiosk.getPlatformId());
        if (platform == null) {
            return LiveLink.gone();
        }
        return LiveLink.live(town, centre, platform);
    }

    public static void refreshActiveFlag(ServerLevel level, BlockPos pos, boolean loadChunk) {
        if (level == null || pos == null) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof PlatformInterfaceEntity kiosk)) {
            return;
        }
        LiveLink link = resolve(level, kiosk, loadChunk);
        if (!link.known()) {
            return;
        }
        if (link.live()) {
            kiosk.rememberNames(link.town().getName(), link.platform().getName());
        }
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof PlatformInterfaceBlock
                && state.getValue(PlatformInterfaceBlock.ACTIVE) != link.live()) {
            level.setBlock(pos, state.setValue(PlatformInterfaceBlock.ACTIVE, link.live()), 3);
        }
    }

    public static void open(ServerPlayer player, BlockPos kioskPos) {
        if (!withinReach(player, kioskPos)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (!(level.getBlockEntity(kioskPos) instanceof PlatformInterfaceEntity kiosk)) {
            return;
        }
        LiveLink link = resolve(level, kiosk, true);
        refreshActiveFlag(level, kioskPos, true);
        if (!link.live()) {
            player.displayClientMessage(inactiveMessage(), false);
            return;
        }
        PlatformAccess.getNetworkMessages().sendToPlayer(new PlatformInterfaceSyncPacket(snapshot(player, kioskPos, link)),
                player);
    }

    public static void toggleEnabled(ServerPlayer player, BlockPos kioskPos) {
        LiveLink link = linkedInReach(player, kioskPos);
        if (link == null) {
            return;
        }
        link.platform().setEnabled(!link.platform().isEnabled());
        link.centre().setChanged();
        sendSnapshot(player, kioskPos, link);
    }

    public static void toggleDestination(ServerPlayer player, BlockPos kioskPos, UUID destinationId) {
        LiveLink link = linkedInReach(player, kioskPos);
        if (link == null || destinationId == null || destinationId.equals(link.town().getId())) {
            return;
        }
        boolean enabled = link.platform().isDestinationEnabled(destinationId);
        link.platform().setDestinationEnabled(destinationId, !enabled);
        link.centre().setChanged();
        sendSnapshot(player, kioskPos, link);
    }

    public static void openTown(ServerPlayer player, BlockPos kioskPos) {
        LiveLink link = linkedInReach(player, kioskPos);
        if (link == null) {
            return;
        }
        BlockPos centre = link.town().getPosition();
        if (!withinReach(player, centre)) {
            player.displayClientMessage(Component.translatable("gui.businesscraft.platform_interface.town_far")
                    .withStyle(ChatFormatting.YELLOW), false);
            sendSnapshot(player, kioskPos, link);
            return;
        }
        PlatformAccess.getNetwork().openScreen(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.businesscraft.town_interface");
            }

            @Override
            public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player menuPlayer) {
                return new TownInterfaceMenu(windowId, inventory, centre);
            }
        }, centre);
    }

    private static void sendSnapshot(ServerPlayer player, BlockPos kioskPos, LiveLink link) {
        PlatformAccess.getNetworkMessages().sendToPlayer(
                new PlatformInterfaceSyncPacket(snapshot(player, kioskPos, link)), player);
    }

    private static LiveLink linkedInReach(ServerPlayer player, BlockPos kioskPos) {
        if (!withinReach(player, kioskPos)) {
            return null;
        }
        ServerLevel level = player.serverLevel();
        if (!(level.getBlockEntity(kioskPos) instanceof PlatformInterfaceEntity kiosk)) {
            return null;
        }
        LiveLink link = resolve(level, kiosk, true);
        refreshActiveFlag(level, kioskPos, true);
        if (!link.live()) {
            player.displayClientMessage(inactiveMessage(), false);
            PlatformAccess.getNetworkMessages().sendToPlayer(
                    new PlatformInterfaceSyncPacket(PlatformInterfaceSnapshot.inactive(kioskPos,
                            inactiveMessage().getString())), player);
            return null;
        }
        return link;
    }

    public static boolean withinReach(Player player, BlockPos pos) {
        if (player == null || pos == null) {
            return false;
        }
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)
                <= LoadedTownAccess.REACH_DISTANCE_SQR;
    }

    public static PlatformInterfaceSnapshot snapshot(ServerPlayer player, BlockPos kioskPos, LiveLink link) {
        Town town = link.town();
        Platform platform = link.platform();
        boolean pathSet = platform.isComplete();
        String pathText = pathSet
                ? formatPos(platform.getStartPos()) + " → " + formatPos(platform.getEndPos())
                : Component.translatable("gui.businesscraft.platform_interface.path_missing").getString();
        BlockPos centre = town.getPosition();
        boolean townInReach = withinReach(player, centre);
        List<PlatformInterfaceSnapshot.Destination> destinations = destinations(link);
        return new PlatformInterfaceSnapshot(
                kioskPos,
                true,
                "",
                town.getName(),
                platform.getName(),
                platform.isEnabled(),
                pathSet,
                pathText,
                waitingText(player.serverLevel(), town, link.centre(), platform),
                townInReach,
                destinations);
    }

    private static List<PlatformInterfaceSnapshot.Destination> destinations(LiveLink link) {
        List<PlatformInterfaceSnapshot.Destination> rows = new ArrayList<>();
        BlockPos from = link.platform().isComplete() ? link.platform().getStartPos() : link.town().getPosition();
        ServerLevel level = (ServerLevel) link.centre().getLevel();
        if (level == null || from == null) {
            return rows;
        }
        for (Town other : TownManager.get(level).getAllTowns().values()) {
            if (other.getId().equals(link.town().getId()) || other.getPosition() == null) {
                continue;
            }
            int distance = (int) Math.sqrt(from.distSqr(other.getPosition()));
            rows.add(new PlatformInterfaceSnapshot.Destination(
                    other.getId(),
                    other.getName(),
                    link.platform().isDestinationEnabled(other.getId()),
                    distance));
        }
        rows.sort(Comparator.comparingInt(PlatformInterfaceSnapshot.Destination::distance)
                .thenComparing(PlatformInterfaceSnapshot.Destination::name));
        return rows;
    }

    /**
     * Waiting tourists whose origin is this town and who are inside this
     * platform's path search box. Same box as the town overview summary,
     * limited to one platform.
     */
    public static String waitingText(ServerLevel level, Town town, TownInterfaceEntity centre, Platform platform) {
        if (town == null || centre == null || platform == null || !platform.isEnabled() || !platform.isComplete()) {
            return "None waiting";
        }
        int searchRadius = centre.getSearchRadius();
        BlockPos startPos = platform.getStartPos();
        BlockPos endPos = platform.getEndPos();
        AABB bounds = new AABB(
                Math.min(startPos.getX(), endPos.getX()) - searchRadius,
                Math.min(startPos.getY(), endPos.getY()) - 2,
                Math.min(startPos.getZ(), endPos.getZ()) - searchRadius,
                Math.max(startPos.getX(), endPos.getX()) + searchRadius,
                Math.max(startPos.getY(), endPos.getY()) + 4,
                Math.max(startPos.getZ(), endPos.getZ()) + searchRadius);

        Map<String, Integer> byDestination = new HashMap<>();
        ITouristHelper helper = PlatformAccess.getTouristHelper();
        for (Villager villager : level.getEntitiesOfClass(Villager.class, bounds)) {
            if (!helper.isTourist(villager) || !(villager instanceof TouristEntity tourist)) {
                continue;
            }
            if (!town.getId().equals(tourist.getOriginTownId())) {
                continue;
            }
            String dest = tourist.getDestinationTownName();
            if (dest == null || dest.isBlank()) {
                dest = "Unknown";
            }
            byDestination.merge(dest, 1, Integer::sum);
        }
        int total = byDestination.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            return "None waiting";
        }
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(byDestination.entrySet());
        sorted.sort(Comparator.comparingInt((Map.Entry<String, Integer> entry) -> entry.getValue()).reversed());
        StringBuilder text = new StringBuilder();
        text.append(total).append(" waiting");
        int parts = 0;
        for (Map.Entry<String, Integer> entry : sorted) {
            if (parts >= 3) {
                text.append("…");
                break;
            }
            text.append(parts == 0 ? ": " : ", ");
            text.append(entry.getValue()).append("→").append(entry.getKey());
            parts++;
        }
        return text.toString();
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }
}
