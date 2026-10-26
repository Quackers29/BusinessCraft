package com.quackers29.businesscraft.ui.screens.platform;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.PlatformInterfaceEntity;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.client.PlatformPathKeyHandler;
import com.quackers29.businesscraft.menu.PlatformInterfaceMenu;
import com.quackers29.businesscraft.network.packets.platform.ResetPlatformPathPacket;
import com.quackers29.businesscraft.network.packets.platform.SetPlatformPathCreationModePacket;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.ui.util.InventoryRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Path-only screen for one Platform Interface. Enable and destination allow-list stay on the Town Centre.
 */
public class PlatformInterfaceScreen extends AbstractContainerScreen<PlatformInterfaceMenu> {
    private static final int HEADER = 0xFFFCB821;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int MUTED = 0xFFB0B0B0;

    private boolean buttonsForActive;

    public PlatformInterfaceScreen(PlatformInterfaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 240;
        this.imageHeight = 150;
    }

    @Override
    protected void init() {
        super.init();
        requestPlatformData();
        addButtons();
    }

    private void requestPlatformData() {
        PlatformInterfaceEntity entity = entity();
        if (entity == null || entity.getTownId() == null || PlatformAccess.getNetworkMessages() == null) {
            return;
        }
        PlatformAccess.getNetworkMessages().sendToServer(
                new com.quackers29.businesscraft.network.packets.ui.RequestTownPlatformDataPacket(entity.getTownId()));
    }

    private void addButtons() {
        PlatformInterfaceEntity entity = entity();
        if (entity == null || !entity.isActive() || entity.getTownCentre() == null || entity.getPlatformId() == null) {
            return;
        }
        int x = this.leftPos + 16;
        int y = this.topPos + this.imageHeight - 32;
        addRenderableWidget(Button.builder(Component.translatable("businesscraft.platform_interface.set_path"), pressed -> startPath())
                .bounds(x, y, 100, 20)
                .build());
        Platform platform = currentPlatform();
        if (platform != null && platform.isComplete()) {
            addRenderableWidget(Button.builder(Component.translatable("businesscraft.reset"), pressed -> resetPath())
                    .bounds(x + 108, y, 100, 20)
                    .build());
        }
    }

    private void rebuild() {
        clearWidgets();
        addButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PlatformInterfaceEntity current = entity();
        boolean active = current != null && current.isActive();
        if (active != buttonsForActive) {
            buttonsForActive = active;
            if (active) {
                requestPlatformData();
            }
            rebuild();
        }
        this.renderBackground(graphics);
        int x = this.leftPos;
        int y = this.topPos;
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, InventoryRenderer.BACKGROUND_COLOR);
        InventoryRenderer.drawBorder(graphics, x, y, this.imageWidth, this.imageHeight, InventoryRenderer.BORDER_COLOR, 2);

        graphics.drawString(this.font, this.title, x + 12, y + 10, HEADER, false);

        PlatformInterfaceEntity entity = entity();
        int line = y + 32;
        if (entity == null) {
            graphics.drawString(this.font, Component.translatable("businesscraft.platform_interface.not_linked"),
                    x + 12, line, TEXT, false);
        } else if (!entity.isActive()) {
            String reason = entity.getInactiveReason() == null
                    ? PlatformInterfaceEntity.REASON_NOT_LINKED
                    : entity.getInactiveReason();
            graphics.drawString(this.font, Component.translatable("businesscraft.platform_interface.inactive",
                    Component.translatable(reason)), x + 12, line, 0xFFFF8888, false);
            graphics.drawString(this.font, Component.translatable("businesscraft.platform_interface.inactive_hint"),
                    x + 12, line + 14, MUTED, false);
        } else {
            graphics.drawString(this.font, Component.translatable("businesscraft.platform_interface.path_only"),
                    x + 12, line, TEXT, false);
            Platform platform = currentPlatform();
            String path = pathLine(platform);
            graphics.drawString(this.font, path, x + 12, line + 16, MUTED, false);
            String town = townName(entity);
            if (town != null) {
                graphics.drawString(this.font, town, x + 12, line + 30, MUTED, false);
            }
        }

        for (var child : this.children()) {
            if (child instanceof Renderable renderable) {
                renderable.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    private static String pathLine(Platform platform) {
        if (platform == null) {
            return "Path: …";
        }
        if (!platform.isComplete()) {
            return "Path: not set";
        }
        BlockPos start = platform.getStartPos();
        BlockPos end = platform.getEndPos();
        return "Path: " + start.getX() + ", " + start.getY() + ", " + start.getZ()
                + " → " + end.getX() + ", " + end.getY() + ", " + end.getZ();
    }

    private String townName(PlatformInterfaceEntity entity) {
        if (this.minecraft == null || entity.getTownCentre() == null) {
            return null;
        }
        BlockEntity be = this.minecraft.level.getBlockEntity(entity.getTownCentre());
        if (be instanceof TownInterfaceEntity town && town.getTownName() != null && !town.getTownName().isEmpty()) {
            return "Town: " + town.getTownName();
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
                || this.minecraft == null) {
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

    private void startPath() {
        PlatformInterfaceEntity entity = entity();
        Platform platform = currentPlatform();
        if (entity == null || !entity.isActive() || platform == null || PlatformAccess.getNetworkMessages() == null) {
            return;
        }
        PlatformAccess.getNetworkMessages().sendToServer(new SetPlatformPathCreationModePacket(
                entity.getTownCentre(), platform.getId(), true));
        PlatformPathKeyHandler.setActivePlatform(entity.getTownCentre(), platform.getId());
        if (this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                    Component.translatable("businesscraft.platform_path_instructions"), false);
        }
        this.minecraft.setScreen(null);
    }

    private void resetPath() {
        PlatformInterfaceEntity entity = entity();
        Platform platform = currentPlatform();
        if (entity == null || platform == null || PlatformAccess.getNetworkMessages() == null) {
            return;
        }
        PlatformAccess.getNetworkMessages().sendToServer(new ResetPlatformPathPacket(
                entity.getTownCentre(), platform.getId()));
        platform.setStartPos(null);
        platform.setEndPos(null);
        if (this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                    Component.translatable("businesscraft.platform_path_reset"), false);
        }
        rebuild();
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
}
