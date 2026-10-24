package com.quackers29.businesscraft.ui.screens.platform;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.network.packets.ui.PlatformInterfaceActionPacket;
import com.quackers29.businesscraft.platform.PlatformInterfaceSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * One platform's info, opened from a Town Interface block.
 * Plain widgets on purpose: the town tabs are a separate screen.
 */
public class PlatformInterfaceScreen extends Screen {
    private static final int PANEL_WIDTH = 280;
    private static final int MAX_DESTINATIONS = 5;
    private static final int TEXT = 0xFFE7E1D6;
    private static final int MUTED = 0xFFB7B1A6;
    private static final int TITLE = 0xFFF2D48A;
    private static final int ENABLED = 0xFF8FCB7A;
    private static final int DISABLED = 0xFFE07A7A;

    private PlatformInterfaceSnapshot snapshot;
    private int panelLeft;
    private int panelTop;
    private int panelHeight;
    private int destinationY;
    private int actionY;

    public PlatformInterfaceScreen(PlatformInterfaceSnapshot snapshot) {
        super(Component.translatable("block.businesscraft.platform_interface"));
        this.snapshot = snapshot;
    }

    public static void open(PlatformInterfaceSnapshot snapshot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!snapshot.active()) {
            if (minecraft.screen instanceof PlatformInterfaceScreen) {
                minecraft.setScreen(null);
            }
            return;
        }
        if (minecraft.screen instanceof PlatformInterfaceScreen existing
                && existing.snapshot.kioskPos().equals(snapshot.kioskPos())) {
            existing.snapshot = snapshot;
            existing.rebuildButtons();
            return;
        }
        minecraft.setScreen(new PlatformInterfaceScreen(snapshot));
    }

    @Override
    protected void init() {
        super.init();
        rebuildButtons();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void rebuildButtons() {
        clearWidgets();
        int shown = Math.min(MAX_DESTINATIONS, snapshot.destinations().size());
        boolean note = snapshot.destinations().isEmpty() || noneSelected(snapshot.destinations());
        int noteLines = note ? 12 : 0;
        int farLine = snapshot.townInReach() ? 0 : 22;
        panelHeight = 108 + noteLines + shown * 22 + farLine + 56;
        panelLeft = (this.width - PANEL_WIDTH) / 2;
        panelTop = Math.max(8, (this.height - panelHeight) / 2);
        destinationY = panelTop + 96 + noteLines;
        actionY = destinationY + shown * 22 + farLine + 6;

        int buttonX = panelLeft + 12;
        int buttonW = PANEL_WIDTH - 24;
        List<PlatformInterfaceSnapshot.Destination> destinations = snapshot.destinations();
        for (int i = 0; i < shown; i++) {
            PlatformInterfaceSnapshot.Destination destination = destinations.get(i);
            String label = (destination.enabled() ? "Stop " : "Allow ") + destination.name()
                    + "  (" + destination.distance() + "m)";
            addRenderableWidget(Button.builder(Component.literal(truncate(label, 38)), button ->
                    PlatformAccess.getNetworkMessages().sendToServer(new PlatformInterfaceActionPacket(
                            snapshot.kioskPos(),
                            PlatformInterfaceActionPacket.Action.TOGGLE_DESTINATION,
                            destination.townId())))
                    .bounds(buttonX, destinationY + i * 22, buttonW, 20)
                    .build());
        }

        Component enabledLabel = Component.translatable(snapshot.enabled()
                ? "gui.businesscraft.platform_interface.disable"
                : "gui.businesscraft.platform_interface.enable");
        addRenderableWidget(Button.builder(enabledLabel, button ->
                PlatformAccess.getNetworkMessages().sendToServer(new PlatformInterfaceActionPacket(
                        snapshot.kioskPos(),
                        PlatformInterfaceActionPacket.Action.TOGGLE_ENABLED,
                        null)))
                .bounds(buttonX, actionY, 120, 20)
                .build());

        if (snapshot.townInReach()) {
            addRenderableWidget(Button.builder(
                    Component.translatable("gui.businesscraft.platform_interface.open_town"), button ->
                            PlatformAccess.getNetworkMessages().sendToServer(new PlatformInterfaceActionPacket(
                                    snapshot.kioskPos(),
                                    PlatformInterfaceActionPacket.Action.OPEN_TOWN,
                                    null)))
                    .bounds(buttonX + 128, actionY, 128, 20)
                    .build());
        }

        addRenderableWidget(Button.builder(Component.translatable("businesscraft.back_button"), button -> onClose())
                .bounds(buttonX, actionY + 24, buttonW, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + panelHeight, 0xF0181612);
        graphics.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + 2, 0xFFC4A46A);

        int x = panelLeft + 12;
        int y = panelTop + 10;
        String heading = snapshot.platformName().isBlank() ? title.getString() : snapshot.platformName();
        graphics.drawString(font, Component.literal(heading), x, y, TITLE, false);
        y += 14;
        graphics.drawString(font, Component.literal(snapshot.townName()), x, y, MUTED, false);
        y += 14;
        int statusColor = snapshot.enabled() ? ENABLED : DISABLED;
        graphics.drawString(font, Component.literal(snapshot.enabled() ? "Enabled" : "Disabled"), x, y, statusColor, false);
        graphics.drawString(font, Component.literal(snapshot.pathSet() ? "Path set" : "Path not set"), x + 90, y, TEXT, false);
        y += 12;
        graphics.drawString(font, Component.literal(snapshot.pathText()), x, y, MUTED, false);
        y += 14;
        graphics.drawString(font, Component.literal("Waiting: " + snapshot.waitingText()), x, y, TEXT, false);
        y += 16;
        graphics.drawString(font, Component.translatable("gui.businesscraft.platform_interface.destinations"), x, y,
                TITLE, false);
        y += 12;
        if (snapshot.destinations().isEmpty()) {
            graphics.drawString(font,
                    Component.translatable("gui.businesscraft.platform_interface.no_other_towns"), x, y, MUTED, false);
        } else if (noneSelected(snapshot.destinations())) {
            graphics.drawString(font,
                    Component.translatable("gui.businesscraft.platform_interface.any_destination"), x, y, MUTED, false);
        }
        if (!snapshot.townInReach()) {
            graphics.drawString(font,
                    Component.translatable("gui.businesscraft.platform_interface.town_far"),
                    x, actionY - 14, MUTED, false);
        }
        if (snapshot.destinations().size() > MAX_DESTINATIONS) {
            graphics.drawString(font, Component.literal("+" + (snapshot.destinations().size() - MAX_DESTINATIONS) + " more"),
                    x, destinationY + MAX_DESTINATIONS * 22, MUTED, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static boolean noneSelected(List<PlatformInterfaceSnapshot.Destination> destinations) {
        for (PlatformInterfaceSnapshot.Destination destination : destinations) {
            if (destination.enabled()) {
                return false;
            }
        }
        return true;
    }

    private static String truncate(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max - 1) + "…";
    }
}
