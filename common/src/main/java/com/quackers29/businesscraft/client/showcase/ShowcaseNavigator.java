package com.quackers29.businesscraft.client.showcase;

import com.quackers29.businesscraft.block.TownInterfaceBlock;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.entity.TouristType;
import com.quackers29.businesscraft.entity.tourist.TouristLook;
import com.quackers29.businesscraft.entity.tourist.TouristLooks;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.ui.builders.UIGridBuilder;
import com.quackers29.businesscraft.ui.screens.TouristScreen;
import com.quackers29.businesscraft.ui.managers.BottomButtonManager;
import com.quackers29.businesscraft.ui.screens.BaseTownScreen;
import com.quackers29.businesscraft.ui.screens.platform.PlatformManagementScreenV2;
import com.quackers29.businesscraft.ui.screens.town.ContractBoardScreen;
import com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen;
import com.quackers29.businesscraft.ui.screens.town.TownLeaderboardScreen;
import com.quackers29.businesscraft.ui.tabs.SettingsTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Executes showcase route steps without simulating mouse clicks.
 */
public final class ShowcaseNavigator {
    private ShowcaseNavigator() {
    }

    public static String runStep(Minecraft minecraft, BlockPos townPos, String step) {
        if (step == null || step.isBlank()) {
            return "empty step";
        }
        if (step.startsWith("setup:")) {
            MinecraftServer server = minecraft.getSingleplayerServer();
            return ShowcaseSetup.runStep(server, townPos, step);
        }
        if ("open-town".equals(step)) {
            openTownInterface(minecraft, townPos);
            return null;
        }
        if ("open-platform-interface".equals(step)) {
            ShowcasePlatforms.openScreen(minecraft, townPos);
            return null;
        }
        if (step.startsWith("set-tab:")) {
            String tab = step.substring("set-tab:".length());
            if (!(minecraft.screen instanceof TownInterfaceScreen screen) || screen.getTabPanel() == null) {
                return "town screen not open for set-tab";
            }
            screen.getTabPanel().setActiveTab(tab);
            if (screen.getButtonManager() != null) {
                screen.getButtonManager().updateBottomButtons(tab);
            }
            return null;
        }
        if (step.startsWith("wait-tab:")) {
            return null;
        }
        if (step.startsWith("press-button:id:")) {
            String id = step.substring("press-button:id:".length());
            if (!pressButton(minecraft, id, null)) {
                return "button id not found or not pressed: " + id;
            }
            return null;
        }
        if (step.startsWith("press-button:label:")) {
            String label = step.substring("press-button:label:".length());
            if (!pressButton(minecraft, null, label)) {
                return "button label not found or not pressed: " + label;
            }
            return null;
        }
        if (step.startsWith("open-leaderboard-detail:")) {
            int row = Integer.parseInt(step.substring("open-leaderboard-detail:".length()));
            if (!(minecraft.screen instanceof TownLeaderboardScreen ranking)
                    || !ranking.showcaseOpenDetailRow(row)) {
                return "could not open leaderboard detail row " + row;
            }
            return null;
        }
        if (step.startsWith("wait-for-screen:") || step.startsWith("wait-for-popup:")) {
            return null;
        }
        if (step.startsWith("open-tourist:")) {
            return openTourist(minecraft, townPos, step.substring("open-tourist:".length()));
        }
        if (step.startsWith("hover-tourist-trade:")) {
            int index = Integer.parseInt(step.substring("hover-tourist-trade:".length()));
            if (minecraft.screen instanceof TouristScreen screen) {
                screen.showcaseHoverTrade(index);
                return null;
            }
            return "tourist screen not open for hover";
        }
        if (step.startsWith("scroll-tourist:")) {
            int rows = Integer.parseInt(step.substring("scroll-tourist:".length()));
            if (minecraft.screen instanceof TouristScreen screen) {
                screen.showcaseScrollRows(rows);
                return null;
            }
            return "tourist screen not open for scroll";
        }
        return "unknown step: " + step;
    }

    public static boolean waitConditionMet(Minecraft minecraft, String step, String activeTabHint) {
        if (step.startsWith("wait-for-popup:")) {
            if (minecraft.screen instanceof BaseTownScreen<?> town) {
                return town.getActivePopup() != null;
            }
            return false;
        }
        if (step.startsWith("wait-for-screen:")) {
            String[] parts = step.split(":");
            if (parts.length < 2) {
                return false;
            }
            String className = parts[1];
            Screen screen = minecraft.screen;
            if (screen == null) {
                return false;
            }
            return screen.getClass().getSimpleName().equals(className);
        }
        if (step.startsWith("wait-tab:")) {
            String tab = step.substring("wait-tab:".length());
            if (!(minecraft.screen instanceof TownInterfaceScreen town) || town.getTabPanel() == null) {
                return false;
            }
            return tab.equals(town.getTabPanel().getActiveTabId());
        }
        if (step.startsWith("wait-for-contracts:")) {
            return minecraft.screen instanceof ContractBoardScreen board && board.showcaseHasAuctionContracts();
        }
        if (step.startsWith("wait-for-platforms:")) {
            return minecraft.screen instanceof PlatformManagementScreenV2 platform
                    && platform.showcaseHasPlatforms();
        }
        if (step.startsWith("wait-for-platform-block:")) {
            BlockPos pos = ShowcasePlatforms.lastPlaced();
            if (pos == null) {
                pos = ShowcasePlatforms.interfacePos(Showcase.getActiveTownBlockPos());
            }
            if (pos == null || minecraft.level == null) {
                return false;
            }
            return minecraft.level.getBlockState(pos).getBlock() instanceof com.quackers29.businesscraft.block.PlatformInterfaceBlock;
        }
        return true;
    }

    public static int waitTimeoutTicks(String step) {
        if (step.startsWith("wait-for-screen:") || step.startsWith("wait-tab:")
                || step.startsWith("wait-for-popup:") || step.startsWith("wait-for-contracts:")
                || step.startsWith("wait-for-platforms:") || step.startsWith("wait-for-platform-block:")) {
            String[] parts = step.split(":");
            String timeout = parts.length >= 3 ? parts[2] : parts.length == 2 ? parts[1] : "";
            if (!timeout.isEmpty()) {
                try {
                    return Integer.parseInt(timeout);
                } catch (NumberFormatException ignored) {
                    return 100;
                }
            }
            return 100;
        }
        return 0;
    }

    public static boolean isWaitStep(String step) {
        return step.startsWith("wait-for-screen:") || step.startsWith("wait-tab:")
                || step.startsWith("wait-for-popup:") || step.startsWith("wait-for-contracts:")
                || step.startsWith("wait-for-platforms:") || step.startsWith("wait-for-platform-block:");
    }

    private static boolean pressButton(Minecraft minecraft, String id, String label) {
        Screen screen = minecraft.screen;
        if (screen instanceof PlatformManagementScreenV2 platform && id != null) {
            return platform.showcaseInvokeButton(id);
        }
        if (screen instanceof ContractBoardScreen contracts && id != null) {
            return contracts.showcaseInvokeButton(id);
        }
        if (screen instanceof TownInterfaceScreen townScreen) {
            if (id != null && id.startsWith("town.settings.") && townScreen.getTabController() != null) {
                Object tab = townScreen.getTabController().getTab("settings");
                if (tab instanceof SettingsTab settingsTab && settingsTab.showcaseInvokeButton(id)) {
                    return true;
                }
            }
            BottomButtonManager manager = townScreen.getButtonManager();
            if (manager == null) {
                return false;
            }
            UIGridBuilder grid = manager.getBottomButtonsGrid();
            if (grid == null) {
                return false;
            }
            if (id != null) {
                return grid.invokeButtonById(id);
            }
            return grid.invokeButtonByLabel(label);
        }
        return false;
    }

    /**
     * Opens a tourist who is standing on Brookside's platform. {@code legacy}
     * still uses the vanilla merchant window so the before shot stays reproducible.
     */
    private static String openTourist(Minecraft minecraft, BlockPos townPos, String which) {
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (server == null || minecraft.player == null || townPos == null) {
            return "no server for tourist screen";
        }
        boolean legacy = "legacy".equals(which);
        boolean anyTown = legacy || "wanderer".equals(which) || "ticket".equals(which);
        double distance = "voyager".equals(which) ? 1800.0 : 120.0;
        String destination = anyTown ? TouristEntity.ANY_TOWN_NAME : "Hillcrest";
        UUID playerId = minecraft.player.getUUID();
        minecraft.setScreen(null);
        // After setScreen, so the closing tourist screen cannot clear the flag.
        TouristScreen.showcaseSuppressHover(true);
        server.execute(() -> openTouristOnServer(server, playerId, townPos, legacy, anyTown, destination, distance));
        return null;
    }

    private static void openTouristOnServer(MinecraftServer server, UUID playerId, BlockPos townPos,
            boolean legacy, boolean anyTown, String destination, double distance) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        player.setGameMode(GameType.CREATIVE);
        give(player, Items.PAPER, 4);
        give(player, Items.BREAD, 16);
        give(player, Items.SAND, 16);
        TouristEntity tourist = nearestTourist(level, townPos);
        if (tourist == null) {
            tourist = spawnTourist(level, townPos);
        }
        if (tourist == null) {
            return;
        }
        TouristLook desert = null;
        for (TouristLook look : TouristLooks.all()) {
            if ("desert_guide".equals(look.id())) {
                desert = look;
                break;
            }
        }
        TouristType type = tourist.getTouristType();
        if (desert != null) {
            tourist.applyLook(desert);
            if (desert.type() != null) {
                type = desert.type();
            }
        }
        tourist.applyJourneyForScreen("Brookside", destination, anyTown, type, distance);
        tourist.applyScreenClock(17 * 60 + 31, 60 * 60 + 51 * 60);
        player.teleportTo(level, tourist.getX(), tourist.getY(), tourist.getZ() + 2.0, 180.0f, 0.0f);
        TouristEntity.showcaseVanillaScreen = legacy;
        try {
            tourist.mobInteract(player, InteractionHand.MAIN_HAND);
        } finally {
            TouristEntity.showcaseVanillaScreen = false;
        }
    }

    private static void give(ServerPlayer player, net.minecraft.world.item.Item item, int want) {
        int have = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                have += stack.getCount();
            }
        }
        if (have < want) {
            player.getInventory().add(new ItemStack(item, want - have));
        }
    }

    private static TouristEntity nearestTourist(ServerLevel level, BlockPos townPos) {
        AABB box = new AABB(townPos).inflate(64);
        List<TouristEntity> tourists = level.getEntitiesOfClass(TouristEntity.class, box);
        TouristEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (TouristEntity tourist : tourists) {
            double distance = tourist.distanceToSqr(Vec3.atCenterOf(townPos));
            if (distance < best) {
                best = distance;
                nearest = tourist;
            }
        }
        return nearest;
    }

    private static TouristEntity spawnTourist(ServerLevel level, BlockPos townPos) {
        BlockEntity be = level.getBlockEntity(townPos);
        if (!(be instanceof TownInterfaceEntity townInterface)) {
            return null;
        }
        Town town = townInterface.getTown();
        if (town == null) {
            return null;
        }
        TouristEntity tourist = new TouristEntity(
                (net.minecraft.world.entity.EntityType<TouristEntity>) com.quackers29.businesscraft.api.PlatformAccess
                        .getEntities().getTouristEntityType(),
                level,
                town,
                null,
                UUID.randomUUID(),
                "Hillcrest");
        BlockPos pos = townPos.east(3);
        tourist.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        if (!level.addFreshEntity(tourist)) {
            return null;
        }
        return tourist;
    }

    private static void openTownInterface(Minecraft minecraft, BlockPos pos) {
        if (minecraft.screen instanceof TownInterfaceScreen) {
            return;
        }
        minecraft.setScreen(null);
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (server == null || pos == null) {
            return;
        }
        server.execute(() -> {
            ServerPlayer serverPlayer = server.getPlayerList().getPlayer(minecraft.player.getUUID());
            if (serverPlayer == null) {
                return;
            }
            ServerLevel level = serverPlayer.serverLevel();
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof TownInterfaceBlock block) {
                block.use(state, level, pos, serverPlayer, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            }
        });
    }
}
