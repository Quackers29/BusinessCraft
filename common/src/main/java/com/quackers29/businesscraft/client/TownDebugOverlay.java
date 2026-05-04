package com.quackers29.businesscraft.client;

import com.quackers29.businesscraft.api.EventCallbacks;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.api.RenderHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.*;

public class TownDebugOverlay {
    private static boolean visible = false;
    private static final int BACKGROUND_COLOR = 0x80000000;
    private static final int HEADER_COLOR = 0xFFFFAA00;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int WARNING_COLOR = 0xFFFF5555;
    private static final int REFRESH_INTERVAL_TICKS = 20;
    private static final int SCROLL_AMOUNT = 15;

    private static List<TownDebugData> townData = new ArrayList<>();
    private static long lastRefreshTime = 0;
    private static long lastUpdateTick = 0;
    private static int scrollOffset = 0;

    public static void toggleVisibility() {
        visible = !visible;
        if (visible) {
            refreshData();
            scrollOffset = 0;
        }
    }

    public static void refreshData() {
        TownDebugNetwork.requestTownData();
        lastRefreshTime = System.currentTimeMillis();
    }

    public static void setTownData(List<TownDebugData> data) {
        townData = data;
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper != null) {
            Object levelObj = clientHelper.getClientLevel();
            if (levelObj instanceof net.minecraft.world.level.Level level) {
                lastUpdateTick = level.getGameTime();
            } else {
                lastUpdateTick = 0;
            }
        } else {
            lastUpdateTick = 0;
        }
    }

    public static boolean isVisible() {
        return visible;
    }

    public static void initialize() {
        PlatformAccess.getEvents().registerMouseScrollCallback(TownDebugOverlay::onMouseScroll);
        PlatformAccess.getEvents().registerClientTickCallback(TownDebugOverlay::onClientTick);

        RenderHelper renderHelper = PlatformAccess.getRender();
        if (renderHelper != null) {
            renderHelper.registerOverlay("town_debug", (guiGraphics, partialTick, screenWidth, screenHeight) -> {
                if (guiGraphics instanceof net.minecraft.client.gui.GuiGraphics gfx) {
                    renderOverlay(gfx, partialTick, screenWidth, screenHeight);
                }
            });
        }
    }

    private static void renderOverlay(GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper == null)
            return;

        Object minecraftObj = clientHelper.getMinecraft();
        if (!(minecraftObj instanceof net.minecraft.client.Minecraft minecraft))
            return;

        if (!visible || minecraft.options.hideGui) {
            return;
        }

        Object fontObj = clientHelper.getFont();
        if (!(fontObj instanceof Font font))
            return;

        int lineHeight = font.lineHeight + 1;
        int y = 5 - scrollOffset; // Apply scroll offset to starting position
        int leftMargin = 5;

        guiGraphics.fill(2, 2, screenWidth - 2, 2 + lineHeight + 3, BACKGROUND_COLOR);

        guiGraphics.drawString(font,
                Component.literal("BusinessCraft Town Debug Overlay")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                leftMargin, 5, HEADER_COLOR, false);
        y += lineHeight + 8;

        if (townData.isEmpty()) {
            if (y + scrollOffset >= 0 && y <= screenHeight) { // Only render if in view
                guiGraphics.fill(2, y, screenWidth - 2, y + lineHeight + 3, BACKGROUND_COLOR);
                guiGraphics.drawString(font,
                        Component.literal("No town data available. Server out of range or not running?")
                                .withStyle(ChatFormatting.RED),
                        leftMargin, y, WARNING_COLOR, false);
            }
            return;
        }

        for (TownDebugData town : townData) {
            if (y + lineHeight < 0) {
                y += lineHeight + 2;
                y += (lineHeight * 5);
                y += 5;
                continue;
            }

            if (y > screenHeight) {
                break;
            }

            guiGraphics.fill(2, y, screenWidth - 2, y + lineHeight + 3, BACKGROUND_COLOR);
            guiGraphics.drawString(font,
                    Component.literal("Town: " + town.name + " (" + town.id + ")").withStyle(ChatFormatting.YELLOW),
                    leftMargin, y, HEADER_COLOR, false);
            y += lineHeight + 2;

            String[] dataLines = {
                    "Position: " + town.position,
                    "Population: " + town.population,
                    "Tourist Spawning: " + (town.touristSpawningEnabled ? "Enabled" : "Disabled") +
                            " | Can Spawn: " + (town.canSpawnTourists ? "Yes" : "No"),
                    "Path: " + (town.pathStart != null ? town.pathStart : "None") + " → " +
                            (town.pathEnd != null ? town.pathEnd : "None"),
                    "Search Radius: " + town.searchRadius + " | Total Visitors: " + town.totalVisitors
            };

            for (String line : dataLines) {
                if (y >= 0 && y <= screenHeight) {
                    guiGraphics.fill(2, y, screenWidth - 2, y + lineHeight, BACKGROUND_COLOR);
                    guiGraphics.drawString(font, line, leftMargin + 10, y, TEXT_COLOR, false);
                }
                y += lineHeight;
            }

            y += 5;
        }

        if (scrollOffset > 0) {
            guiGraphics.fill(screenWidth - 20, 2 + lineHeight + 3, screenWidth - 5, 2 + lineHeight + 10, 0xAAFFFFFF);
        }
    }

    private static boolean onMouseScroll(double scrollDelta) {
        if (visible) {
            scrollOffset -= (int) (scrollDelta * SCROLL_AMOUNT);
            if (scrollOffset < 0) {
                scrollOffset = 0;
            }
            return true;
        }
        return false;
    }

    private static void onClientTick() {
        if (visible) {
            com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
            if (clientHelper != null) {
                Object levelObj = clientHelper.getClientLevel();
                Object playerObj = clientHelper.getClientPlayer();
                if (levelObj instanceof net.minecraft.world.level.Level level && playerObj != null) {
                    long currentTick = level.getGameTime();
                    if (currentTick - lastUpdateTick > REFRESH_INTERVAL_TICKS) {
                        refreshData();
                    }
                }
            }
        }
    }

    public static class TownDebugData {
        public String id;
        public String name;
        public String position;
        public int population;

        public boolean touristSpawningEnabled;
        public boolean canSpawnTourists;
        public String pathStart;
        public String pathEnd;
        public int searchRadius;
        public int totalVisitors;

        public TownDebugData(String id, String name, String position, int population,
                boolean touristSpawningEnabled, boolean canSpawnTourists,
                String pathStart, String pathEnd, int searchRadius, int totalVisitors) {
            this.id = id;
            this.name = name;
            this.position = position;
            this.population = population;

            this.touristSpawningEnabled = touristSpawningEnabled;
            this.canSpawnTourists = canSpawnTourists;
            this.pathStart = pathStart;
            this.pathEnd = pathEnd;
            this.searchRadius = searchRadius;
            this.totalVisitors = totalVisitors;
        }
    }
}
