package com.quackers29.businesscraft.ui.screens.platform;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.PlatformInterfaceEntity;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.client.PlatformPathKeyHandler;
import com.quackers29.businesscraft.menu.PlatformInterfaceMenu;
import com.quackers29.businesscraft.network.packets.platform.ResetPlatformPathPacket;
import com.quackers29.businesscraft.network.packets.platform.SetPlatformAreaPacket;
import com.quackers29.businesscraft.network.packets.platform.SetPlatformDestinationPacket;
import com.quackers29.businesscraft.network.packets.platform.SetPlatformEnabledPacket;
import com.quackers29.businesscraft.network.packets.platform.SetPlatformPathCreationModePacket;
import com.quackers29.businesscraft.network.packets.ui.OpenDestinationsUIPacket;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.ui.util.UiTextWrap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One platform's screen. Path, enable, search radius, spread, and the destination allow-list
 * are edited here. The Town Centre only shows the result.
 */
public class PlatformInterfaceScreen extends AbstractContainerScreen<PlatformInterfaceMenu> {
    private static final int PANEL = 0xF014161C;
    private static final int BORDER = 0xFFD7B56D;
    private static final int SECTION = 0xFF1A1F2A;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int MUTED = 0xFFC2C6CE;
    private static final int GOLD = 0xFFFFD56A;
    private static final int AQUA = 0xFF8FDBFF;
    private static final int GREEN = 0xFF7DFF7D;
    private static final int RED = 0xFFFFA3A3;
    private static final int ON_BG = 0xFF8FDB9A;
    private static final int ON_FG = 0xFF102816;
    private static final int OFF_BG = 0xFFF0A3A3;
    private static final int OFF_FG = 0xFF3A1216;
    private static final int LINE = 0xFF3A404C;

    private final List<Destination> destinations = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();
    private int scroll;
    private int contentHeight;
    private int bodyTop;
    private int bodyHeight;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public PlatformInterfaceScreen(PlatformInterfaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 440;
        this.imageHeight = 360;
    }

    public void receiveDestinations(String platformName, Map<UUID, String> names, Map<UUID, Boolean> enabled,
            Map<UUID, Integer> distances, Map<UUID, String> directions) {
        destinations.clear();
        if (names != null) {
            for (Map.Entry<UUID, String> entry : names.entrySet()) {
                UUID id = entry.getKey();
                destinations.add(new Destination(
                        id,
                        entry.getValue() == null ? "Town" : entry.getValue(),
                        enabled != null && Boolean.TRUE.equals(enabled.get(id)),
                        distances == null ? 0 : distances.getOrDefault(id, 0),
                        directions == null ? "" : directions.getOrDefault(id, "")));
            }
            destinations.sort(Comparator.comparingInt(Destination::distance).thenComparing(Destination::name));
        }
    }

    @Override
    protected void init() {
        fitPanel();
        super.init();
        requestData();
    }

    private void fitPanel() {
        int margin = 8;
        this.panelW = Math.min(460, Math.max(280, this.width - margin * 2));
        this.panelH = Math.min(430, Math.max(148, this.height - margin * 2));
        if (this.panelW > this.width - 4) {
            this.panelW = Math.max(120, this.width - 4);
        }
        if (this.panelH > this.height - 4) {
            this.panelH = Math.max(120, this.height - 4);
        }
        this.imageWidth = panelW;
        this.imageHeight = panelH;
    }

    private void requestData() {
        PlatformInterfaceEntity entity = entity();
        if (entity == null || entity.getTownId() == null || PlatformAccess.getNetworkMessages() == null) {
            return;
        }
        PlatformAccess.getNetworkMessages().sendToServer(
                new com.quackers29.businesscraft.network.packets.ui.RequestTownPlatformDataPacket(entity.getTownId()));
        if (entity.getTownCentre() != null && entity.getPlatformId() != null) {
            PlatformAccess.getNetworkMessages().sendToServer(
                    new OpenDestinationsUIPacket(entity.getTownCentre(), entity.getPlatformId()));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        fitPanel();
        this.leftPos = (this.width - panelW) / 2;
        this.topPos = (this.height - panelH) / 2;
        this.panelX = this.leftPos;
        this.panelY = this.topPos;
        this.renderBackground(graphics);
        hits.clear();
        graphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, BORDER);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL);

        PlatformInterfaceEntity entity = entity();
        Platform platform = currentPlatform();
        drawHeader(graphics, entity, platform);

        int headerBottom = panelY + headerHeight();
        this.bodyTop = headerBottom;
        this.bodyHeight = panelH - headerHeight() - (compact() ? 4 : 6);
        if (entity == null || !entity.isActive() || platform == null) {
            String reason = entity == null || entity.getInactiveReason() == null
                    ? "it is not linked to a town"
                    : Component.translatable(entity.getInactiveReason()).getString();
            graphics.drawString(this.font, "This platform is inactive.", panelX + 16, headerBottom + 12, RED, false);
            drawWrapped(graphics, reason, panelX + 16, headerBottom + 28, panelW - 32, MUTED, 3);
            graphics.drawString(this.font, "Break the block to pick it up.", panelX + 16, headerBottom + 56, MUTED, false);
            return;
        }

        graphics.enableScissor(panelX + 4, bodyTop, panelX + panelW - 4, bodyTop + bodyHeight);
        graphics.pose().pushPose();
        graphics.pose().translate(0, -scroll, 0);
        int y = drawBody(graphics, mouseX, mouseY + scroll, platform);
        graphics.pose().popPose();
        graphics.disableScissor();
        this.contentHeight = y - bodyTop;
        int maxScroll = Math.max(0, contentHeight - bodyHeight);
        if (scroll > maxScroll) {
            scroll = maxScroll;
        }
        if (maxScroll > 0) {
            int trackX = panelX + panelW - 6;
            int thumbH = Math.max(12, bodyHeight * bodyHeight / (contentHeight + bodyHeight));
            int thumbY = bodyTop + (bodyHeight - thumbH) * scroll / maxScroll;
            graphics.fill(trackX, bodyTop, trackX + 3, bodyTop + bodyHeight, 0xFF2A3140);
            graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, BORDER);
        }
    }

    private void drawHeader(GuiGraphics graphics, PlatformInterfaceEntity entity, Platform platform) {
        boolean tight = compact();
        int nameY = panelY + (tight ? 7 : 10);
        String name = platform != null && platform.getName() != null ? platform.getName() : "Platform";
        graphics.drawString(this.font, name, panelX + 14, nameY, WHITE, false);
        String town = townName(entity);
        if (town != null) {
            int width = this.font.width(town);
            graphics.drawString(this.font, town, panelX + panelW - 14 - width, nameY, AQUA, false);
        }
        int lineY = panelY + (tight ? 18 : 24);
        graphics.fill(panelX + 12, lineY, panelX + panelW - 12, lineY + 1, LINE);

        boolean enabled = platform != null && platform.isEnabled();
        boolean path = platform != null && platform.isComplete();
        int allowed = countAllowed();
        String dest = allowed == 0 ? "Any town" : allowed + (allowed == 1 ? " town" : " towns");
        int chipY = panelY + (tight ? 22 : 32);
        String state = enabled ? "Enabled" : "Disabled";
        if (panelW < 360) {
            chip(graphics, panelX + 14, chipY, state, enabled ? ON_FG : OFF_FG, enabled ? ON_BG : OFF_BG);
            String rest = (path ? "Path set" : "No path") + "  ·  " + dest;
            int stateW = this.font.width(state) + 14;
            graphics.drawString(this.font, fit(rest, panelW - 28 - stateW), panelX + 14 + stateW, chipY + 4, MUTED, false);
        } else {
            chip(graphics, panelX + 14, chipY, enabled ? "Enabled" : "Disabled",
                    enabled ? ON_FG : OFF_FG, enabled ? ON_BG : OFF_BG);
            chip(graphics, panelX + 110, chipY, path ? "Path set" : "No path", path ? GREEN : GOLD, SECTION);
            chip(graphics, panelX + 200, chipY, dest, GOLD, SECTION);
        }
    }

    private int countAllowed() {
        int n = 0;
        for (Destination destination : destinations) {
            if (destination.enabled) {
                n++;
            }
        }
        if (n == 0 && destinations.isEmpty()) {
            Platform platform = currentPlatform();
            if (platform != null) {
                return platform.getEnabledDestinations().size();
            }
        }
        return n;
    }

    private void chip(GuiGraphics graphics, int x, int y, String text, int color, int background) {
        int w = this.font.width(text) + 10;
        if (x + w > panelX + panelW - 8) {
            return;
        }
        graphics.fill(x, y, x + w, y + 16, background);
        graphics.drawString(this.font, text, x + 5, y + 4, color, false);
    }

    private boolean compact() {
        return panelH < 260;
    }

    private int headerHeight() {
        return compact() ? 40 : 52;
    }

    private int drawBody(GuiGraphics graphics, int mouseX, int mouseY, Platform platform) {
        boolean tight = compact();
        int x = panelX + 14;
        int y = bodyTop + (tight ? 4 : 8);
        int inner = panelW - 28;
        int buttonH = panelH < 250 ? 16 : 20;
        int gap = tight ? 4 : 14;
        y = sectionTitle(graphics, x, y, "Platform");
        boolean on = platform.isEnabled();
        control(graphics, "toggle", on ? "Disable platform" : "Enable platform",
                x, y, Math.min(180, inner), buttonH, mouseX, mouseY, true,
                on ? ON_BG : OFF_BG, on ? ON_FG : OFF_FG);
        y += buttonH + gap;
        y = sectionTitle(graphics, x, y, "Path");
        graphics.drawString(this.font, fit(pathLine(platform), inner), x, y, MUTED, false);
        y += tight ? 12 : 14;
        int buttonW = Math.min(120, (inner - 8) / 2);
        control(graphics, "set-path", "Set Path", x, y, buttonW, buttonH, mouseX, mouseY, true, 0, 0);
        control(graphics, "reset-path", "Reset", x + buttonW + 8, y, buttonW, buttonH, mouseX, mouseY,
                platform.isComplete(), 0, 0);
        y += buttonH + gap;

        y = sectionTitle(graphics, x, y, "Spawn");
        if (!tight) {
            y += drawWrapped(graphics,
                    "Search radius is how far this platform notices visitors. Spread is how far tourists may stand off the path. Spread cannot exceed the radius.",
                    x, y, inner, MUTED, 3) + 6;
        }
        y = stepper(graphics, mouseX, mouseY, x, y, inner, buttonH, "radius",
                "Search radius", platform.getSearchRadius(), "blocks");
        y += tight ? 4 : 8;
        y = stepper(graphics, mouseX, mouseY, x, y, inner, buttonH, "spread",
                "Spread", platform.getSpread(), platform.getSpread() == 0 ? "on the line" : "blocks off the line");
        y += tight ? 4 : 16;

        y = sectionTitle(graphics, x, y, "Destinations");
        if (!tight) {
            String hint = destinations.stream().anyMatch(d -> d.enabled)
                    ? "Allowed towns only. Turn them all off to accept any town."
                    : "No towns selected. Tourists may travel to any town.";
            y += drawWrapped(graphics, hint, x, y, inner, MUTED, 3) + 6;
        }
        if (destinations.isEmpty()) {
            graphics.drawString(this.font, "Looking for other towns…", x, y, MUTED, false);
            y += 16;
        }
        for (Destination destination : destinations) {
            int rowH = 22;
            graphics.fill(x, y, x + inner, y + rowH, SECTION);
            String label = destination.name;
            String meta = destination.direction.isEmpty()
                    ? destination.distance + "m"
                    : destination.direction + "  " + destination.distance + "m";
            int toggleW = 64;
            int nameMax = inner - toggleW - this.font.width(meta) - 16;
            if (this.font.width(label) > nameMax && nameMax > 12) {
                label = this.font.plainSubstrByWidth(label, nameMax - this.font.width("…")) + "…";
            }
            graphics.drawString(this.font, label, x + 6, y + 7, WHITE, false);
            graphics.drawString(this.font, meta, x + inner - toggleW - 8 - this.font.width(meta), y + 7, MUTED, false);
            control(graphics, "dest:" + destination.id, destination.enabled ? "On" : "Off",
                    x + inner - toggleW, y + 2, toggleW, rowH - 4, mouseX, mouseY, true,
                    destination.enabled ? ON_BG : OFF_BG, destination.enabled ? ON_FG : OFF_FG);
            y += rowH + 4;
        }
        return y + 8;
    }

    private int sectionTitle(GuiGraphics graphics, int x, int y, String title) {
        graphics.drawString(this.font, title, x, y, GOLD, false);
        int line = compact() ? 9 : 11;
        graphics.fill(x, y + line, panelX + panelW - 14, y + line + 1, LINE);
        return y + (compact() ? 13 : 18);
    }

    private int stepper(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int inner, int buttonH,
            String id, String label, int value, String suffix) {
        graphics.drawString(this.font, label, x, y + 6, WHITE, false);
        String valueText = value + "  " + suffix;
        int valueW = this.font.width(valueText);
        int minusX = x + inner - buttonH * 2 - 8 - valueW - 8;
        if (minusX < x + this.font.width(label) + 8) {
            y += 14;
            minusX = x + inner - buttonH * 2 - 8 - valueW - 8;
        }
        graphics.drawString(this.font, valueText, x + inner - buttonH * 2 - 16 - valueW, y + 6, AQUA, false);
        control(graphics, id + "-minus", "−", x + inner - buttonH * 2 - 8, y, buttonH, buttonH, mouseX, mouseY, true,
                0, 0);
        control(graphics, id + "-plus", "+", x + inner - buttonH, y, buttonH, buttonH, mouseX, mouseY, true,
                0, 0);
        return y + buttonH;
    }

    private void control(GuiGraphics graphics, String id, String label, int x, int y, int w, int h,
            int mouseX, int mouseY, boolean enabled, int fill, int labelColor) {
        boolean hover = enabled && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        boolean tinted = fill != 0;
        int bg = !enabled ? 0xFF242830 : tinted ? (hover ? brighten(fill) : fill) : hover ? 0xFF3C4A62 : 0xFF2C3342;
        graphics.fill(x, y, x + w, y + h, bg);
        graphics.fill(x, y, x + w, y + 1, enabled ? (tinted ? darken(fill) : BORDER) : 0xFF3A404C);
        int color = !enabled ? 0xFF8A909A : tinted ? labelColor : WHITE;
        int textW = this.font.width(label);
        graphics.drawString(this.font, label, x + (w - textW) / 2, y + (h - 8) / 2, color, false);
        if (enabled) {
            hits.add(new Hit(id, x, y - scroll, w, h));
        }
    }

    private static int brighten(int color) {
        int r = Math.min(255, ((color >> 16) & 0xFF) + 24);
        int g = Math.min(255, ((color >> 8) & 0xFF) + 24);
        int b = Math.min(255, (color & 0xFF) + 24);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int darken(int color) {
        int r = Math.max(0, ((color >> 16) & 0xFF) - 40);
        int g = Math.max(0, ((color >> 8) & 0xFF) - 40);
        int b = Math.max(0, (color & 0xFF) - 40);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private int drawWrapped(GuiGraphics graphics, String text, int x, int y, int width, int color, int maxLines) {
        List<String> lines = UiTextWrap.wrap(text, width, this.font::width);
        int limit = Math.min(maxLines, lines.size());
        for (int i = 0; i < limit; i++) {
            graphics.drawString(this.font, lines.get(i), x, y + i * 10, color, false);
        }
        return limit * 10;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        for (Hit hit : hits) {
            int screenY = hit.y;
            if (mouseX >= hit.x && mouseX < hit.x + hit.w && mouseY >= screenY && mouseY < screenY + hit.h) {
                if (mouseY < bodyTop || mouseY >= bodyTop + bodyHeight) {
                    continue;
                }
                activate(hit.id);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxScroll = Math.max(0, contentHeight - bodyHeight);
        if (maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        int step = panelH < 220 ? 18 : 24;
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(delta) * step));
        return true;
    }

    private void activate(String id) {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
        Platform platform = currentPlatform();
        PlatformInterfaceEntity entity = entity();
        if (platform == null || entity == null || entity.getTownCentre() == null || entity.getPlatformId() == null) {
            return;
        }
        int step = hasShiftDown() ? 5 : 1;
        switch (id) {
            case "toggle" -> toggleEnabled(entity, platform);
            case "set-path" -> startPath(entity, platform);
            case "reset-path" -> resetPath(entity, platform);
            case "radius-minus" -> changeArea(entity, platform, platform.getSearchRadius() - step, platform.getSpread());
            case "radius-plus" -> changeArea(entity, platform, platform.getSearchRadius() + step, platform.getSpread());
            case "spread-minus" -> changeArea(entity, platform, platform.getSearchRadius(), platform.getSpread() - step);
            case "spread-plus" -> changeArea(entity, platform, platform.getSearchRadius(), platform.getSpread() + step);
            default -> {
                if (id.startsWith("dest:")) {
                    toggleDestination(entity, platform, UUID.fromString(id.substring("dest:".length())));
                }
            }
        }
    }

    private void toggleEnabled(PlatformInterfaceEntity entity, Platform platform) {
        boolean next = !platform.isEnabled();
        platform.setEnabled(next);
        if (PlatformAccess.getNetworkMessages() == null) {
            return;
        }
        PlatformAccess.getNetworkMessages().sendToServer(new SetPlatformEnabledPacket(
                entity.getTownCentre(), entity.getPlatformId(), next));
    }

    private String fit(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        return this.font.plainSubstrByWidth(text, Math.max(8, maxWidth - this.font.width("…"))) + "…";
    }

    private void changeArea(PlatformInterfaceEntity entity, Platform platform, int radius, int spread) {
        platform.setSearchRadius(radius);
        platform.setSpread(spread);
        if (PlatformAccess.getNetworkMessages() == null) {
            return;
        }
        PlatformAccess.getNetworkMessages().sendToServer(new SetPlatformAreaPacket(
                entity.getTownCentre(), entity.getPlatformId(), platform.getSearchRadius(), platform.getSpread()));
    }

    private void toggleDestination(PlatformInterfaceEntity entity, Platform platform, UUID townId) {
        boolean next = !platform.isDestinationEnabled(townId);
        platform.setDestinationEnabled(townId, next);
        for (Destination destination : destinations) {
            if (destination.id.equals(townId)) {
                destination.enabled = next;
            }
        }
        if (PlatformAccess.getNetworkMessages() == null) {
            return;
        }
        PlatformAccess.getNetworkMessages().sendToServer(new SetPlatformDestinationPacket(
                entity.getTownCentre(), entity.getPlatformId(), townId, next));
    }

    private void startPath(PlatformInterfaceEntity entity, Platform platform) {
        if (PlatformAccess.getNetworkMessages() == null) {
            return;
        }
        PlatformAccess.getNetworkMessages().sendToServer(new SetPlatformPathCreationModePacket(
                entity.getTownCentre(), platform.getId(), true));
        PlatformPathKeyHandler.setActivePlatform(entity.getTownCentre(), platform.getId());
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                    Component.translatable("businesscraft.platform_path_instructions"), false);
        }
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    private void resetPath(PlatformInterfaceEntity entity, Platform platform) {
        if (PlatformAccess.getNetworkMessages() == null) {
            return;
        }
        PlatformAccess.getNetworkMessages().sendToServer(new ResetPlatformPathPacket(
                entity.getTownCentre(), platform.getId()));
        platform.setStartPos(null);
        platform.setEndPos(null);
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                    Component.translatable("businesscraft.platform_path_reset"), false);
        }
    }

    private static String pathLine(Platform platform) {
        if (platform == null || !platform.isComplete()) {
            return "Click two blocks in the world to mark the track.";
        }
        BlockPos start = platform.getStartPos();
        BlockPos end = platform.getEndPos();
        return start.getX() + ", " + start.getY() + ", " + start.getZ()
                + "   →   " + end.getX() + ", " + end.getY() + ", " + end.getZ();
    }

    private String townName(PlatformInterfaceEntity entity) {
        if (entity == null || entity.getTownCentre() == null || this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        BlockEntity be = this.minecraft.level.getBlockEntity(entity.getTownCentre());
        if (be instanceof TownInterfaceEntity town && town.getTownName() != null && !town.getTownName().isEmpty()) {
            return town.getTownName();
        }
        return null;
    }

    private PlatformInterfaceEntity entity() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        BlockEntity be = this.minecraft.level.getBlockEntity(this.menu.getBlockPos());
        return be instanceof PlatformInterfaceEntity entity ? entity : null;
    }

    private Platform currentPlatform() {
        PlatformInterfaceEntity entity = entity();
        if (entity == null || entity.getPlatformId() == null || entity.getTownCentre() == null
                || this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        BlockEntity be = this.minecraft.level.getBlockEntity(entity.getTownCentre());
        if (!(be instanceof TownInterfaceEntity town)) {
            return null;
        }
        for (Platform platform : town.getPlatforms()) {
            if (entity.getPlatformId().equals(platform.getId())) {
                return platform;
            }
        }
        return null;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class Destination {
        final UUID id;
        final String name;
        boolean enabled;
        final int distance;
        final String direction;

        Destination(UUID id, String name, boolean enabled, int distance, String direction) {
            this.id = id;
            this.name = name;
            this.enabled = enabled;
            this.distance = distance;
            this.direction = direction;
        }

        String name() {
            return name;
        }

        int distance() {
            return distance;
        }
    }

    private record Hit(String id, int x, int y, int w, int h) {
    }
}
