package com.quackers29.businesscraft.client.showcase;

import com.quackers29.businesscraft.ui.managers.BottomButtonManager;
import com.quackers29.businesscraft.ui.screens.platform.PlatformManagementScreenV2;
import com.quackers29.businesscraft.ui.screens.town.ContractBoardScreen;
import com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen;
import com.quackers29.businesscraft.ui.tabs.SettingsTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Structured view of the open GUI for the all-views manifest.
 * Scaled width and height are the GUI-space size ({@link Screen#width}), not framebuffer pixels.
 */
public final class ShowcaseUiSnapshot {
    private ShowcaseUiSnapshot() {
    }

    public static Map<String, Object> capture(Minecraft minecraft) {
        Screen screen = minecraft.screen;
        Map<String, Object> ui = new LinkedHashMap<>();
        if (screen == null) {
            ui.put("screen", "");
            ui.put("tab", "");
            ui.put("buttonIds", List.of());
            ui.put("scaledWidth", minecraft.getWindow().getGuiScaledWidth());
            ui.put("scaledHeight", minecraft.getWindow().getGuiScaledHeight());
            return ui;
        }
        ui.put("screen", screen.getClass().getSimpleName());
        ui.put("tab", activeTab(screen));
        ui.put("buttonIds", buttonIds(screen));
        ui.put("scaledWidth", screen.width);
        ui.put("scaledHeight", screen.height);
        return ui;
    }

    private static String activeTab(Screen screen) {
        if (screen instanceof TownInterfaceScreen town && town.getTabPanel() != null) {
            String tab = town.getTabPanel().getActiveTabId();
            return tab == null ? "" : tab;
        }
        return "";
    }

    private static List<String> buttonIds(Screen screen) {
        List<String> ids = new ArrayList<>();
        if (screen instanceof TownInterfaceScreen town) {
            BottomButtonManager manager = town.getButtonManager();
            if (manager != null && manager.getBottomButtonsGrid() != null) {
                ids.addAll(manager.getBottomButtonsGrid().buttonIds());
            }
            if (town.getTabController() != null && town.getTabPanel() != null) {
                Object tab = town.getTabController().getTab(town.getTabPanel().getActiveTabId());
                if (tab instanceof SettingsTab settings) {
                    ids.addAll(settings.showcaseButtonIds());
                }
            }
        } else if (screen instanceof ContractBoardScreen contracts) {
            ids.addAll(contracts.showcaseButtonIds());
        } else if (screen instanceof PlatformManagementScreenV2 platforms) {
            ids.addAll(platforms.showcaseButtonIds());
        }
        return List.copyOf(ids);
    }
}
