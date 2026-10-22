package com.quackers29.businesscraft.client.showcase;

import com.quackers29.businesscraft.block.TownInterfaceBlock;
import com.quackers29.businesscraft.ui.builders.UIGridBuilder;
import com.quackers29.businesscraft.ui.managers.BottomButtonManager;
import com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

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
        if ("open-town".equals(step)) {
            openTownInterface(minecraft, townPos);
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
            if (!pressBottomButton(minecraft, id, null)) {
                return "button id not found or not pressed: " + id;
            }
            return null;
        }
        if (step.startsWith("press-button:label:")) {
            String label = step.substring("press-button:label:".length());
            if (!pressBottomButton(minecraft, null, label)) {
                return "button label not found or not pressed: " + label;
            }
            return null;
        }
        if (step.startsWith("wait-for-screen:")) {
            return null;
        }
        return "unknown step: " + step;
    }

    public static boolean waitConditionMet(Minecraft minecraft, String step, String activeTabHint) {
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
        return true;
    }

    public static int waitTimeoutTicks(String step) {
        if (step.startsWith("wait-for-screen:") || step.startsWith("wait-tab:")) {
            String[] parts = step.split(":");
            if (parts.length >= 3) {
                try {
                    return Integer.parseInt(parts[2]);
                } catch (NumberFormatException ignored) {
                    return 100;
                }
            }
            return 100;
        }
        return 0;
    }

    public static boolean isWaitStep(String step) {
        return step.startsWith("wait-for-screen:") || step.startsWith("wait-tab:");
    }

    private static boolean pressBottomButton(Minecraft minecraft, String id, String label) {
        if (!(minecraft.screen instanceof TownInterfaceScreen townScreen)) {
            return false;
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
