package com.quackers29.businesscraft.ui.screens;

import com.quackers29.businesscraft.economy.JourneyProgress;
import com.quackers29.businesscraft.entity.TouristTimer;
import com.quackers29.businesscraft.entity.TouristTradeCatalog;
import com.quackers29.businesscraft.entity.TouristTradeCatalog.RowState;
import com.quackers29.businesscraft.entity.TouristTradeCatalog.TradeContext;
import com.quackers29.businesscraft.entity.TouristTradeCatalog.TradeDef;
import com.quackers29.businesscraft.menu.TouristMenu;
import com.quackers29.businesscraft.ui.util.UiTextWrap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Tourist conversation. The header is the personal name, rank, and route.
 * Trades sit in a list that scrolls when the window is too short to show them all.
 */
public class TouristScreen extends AbstractContainerScreen<TouristMenu> {
    private static final int PANEL = 0xF014161C;
    private static final int BORDER = 0xFFD7B56D;
    private static final int ROW = 0xFF181C24;
    private static final int ROW_ALT = 0xFF1E2430;
    private static final int ROW_HOVER = 0xFF2A3548;
    private static final int BAR_BG = 0xFF2A3140;
    private static final int BAR_FILL = 0xFF5BD07A;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int RANK = 0xFF7DFF7D;
    private static final int MUTED = 0xFFC2C6CE;
    private static final int GOLD = 0xFFFFD56A;
    private static final int AQUA = 0xFF8FDBFF;
    private static final int YELLOW = 0xFFFFE38A;
    private static final int NEED = 0xFFFFA3A3;
    private static final int LOCKED = 0xFFAEB4BE;
    private static final int TRACK = 0xFF2A3140;
    private static final int THUMB = 0xFFD7B56D;

    private TouristScreenLayout layout = TouristScreenLayout.choose(420, 800, 6);
    private final List<Button> tradeButtons = new ArrayList<>();
    private final TouristTimer.Clock clock = new TouristTimer.Clock();
    private int scroll;
    private int forcedTooltipRow = -1;
    private boolean dragThumb;
    private boolean dragList;
    private boolean pressOnList;
    private double dragY;
    private int dragScroll;

    /**
     * Showcase leaves the cursor in the middle of the window, which lands on the
     * first trade. While this is set, only an explicit hover request draws a tooltip.
     */
    private static boolean showcaseSuppressHover;

    public TouristScreen(TouristMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 420;
        this.imageHeight = 360;
    }

    public static void showcaseSuppressHover(boolean suppress) {
        showcaseSuppressHover = suppress;
    }

    /** Showcase pins a trade tooltip without needing a real mouse. */
    public void showcaseHoverTrade(int index) {
        this.forcedTooltipRow = index;
    }

    /** Showcase moves the trade list by whole rows. A short list does not move. */
    public void showcaseScrollRows(int rows) {
        setScroll(rows * this.layout.rowHeight);
    }

    @Override
    protected void init() {
        List<TradeDef> defs = defs();
        this.layout = TouristScreenLayout.choose(this.width, this.height, defs.size());
        this.imageWidth = layout.width;
        this.imageHeight = layout.height;
        this.scroll = 0;
        super.init();
        this.tradeButtons.clear();
        for (int i = 0; i < defs.size(); i++) {
            int index = i;
            this.tradeButtons.add(Button.builder(Component.literal("Trade"), pressed -> pressTrade(index))
                    .bounds(0, 0, 68, 16)
                    .build());
        }
        positionButtons();
    }

    @Override
    public void removed() {
        showcaseSuppressHover = false;
        super.removed();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        positionButtons();
    }

    private List<TradeDef> defs() {
        return TouristTradeCatalog.forLook(this.menu.lookId());
    }

    private void pressTrade(int index) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, index);
        }
    }

    private void positionButtons() {
        List<TradeDef> defs = defs();
        int listTop = this.topPos + layout.rowsTop;
        int listBottom = listTop + layout.listHeight;
        int buttonX = this.leftPos + this.imageWidth - (layout.maxScroll > 0 ? 22 : 12) - 68;
        for (int i = 0; i < tradeButtons.size() && i < defs.size(); i++) {
            RowState state = stateFor(defs.get(i), i);
            Button button = tradeButtons.get(i);
            int by = listTop + i * layout.rowHeight + (layout.rowHeight - 16) / 2 - scroll;
            boolean onScreen = by < listBottom && by + 16 > listTop;
            button.setX(buttonX);
            button.setY(by);
            button.visible = onScreen && (state == RowState.READY || state == RowState.NEED);
            button.active = state == RowState.READY;
        }
    }

    private void setScroll(int next) {
        this.scroll = Math.max(0, Math.min(layout.maxScroll, next));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        positionButtons();
        drawChrome(graphics, this.leftPos, this.topPos);
        int listLeft = this.leftPos + 8;
        int listTop = this.topPos + layout.rowsTop;
        int listRight = this.leftPos + this.imageWidth - (layout.maxScroll > 0 ? 12 : 8);
        int listBottom = listTop + layout.listHeight;
        graphics.enableScissor(listLeft, listTop, listRight, listBottom);
        List<TradeDef> defs = defs();
        int hovered = tooltipRow(mouseX, mouseY);
        for (int i = 0; i < defs.size(); i++) {
            drawRow(graphics, this.leftPos, this.topPos, i, defs.get(i), i == hovered);
        }
        for (Button button : tradeButtons) {
            if (button.visible) {
                button.render(graphics, mouseX, mouseY, partialTick);
            }
        }
        graphics.disableScissor();
        if (layout.maxScroll > 0) {
            drawScrollbar(graphics, this.leftPos, this.topPos);
        }
        int tip = tooltipRow(mouseX, mouseY);
        if (tip >= 0 && rowVisible(tip)) {
            drawSideTooltip(graphics, tooltipFor(tip));
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Drawn from render so the trade rows can be scissored with their buttons.
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // The name and rank are drawn with the panel, not as the villager "Novice" label.
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (layout.maxScroll <= 0) {
            return false;
        }
        if (mouseX < this.leftPos || mouseX > this.leftPos + this.imageWidth
                || mouseY < this.topPos || mouseY > this.topPos + this.imageHeight) {
            return false;
        }
        setScroll(this.scroll - (int) Math.round(delta * layout.rowHeight));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && overThumb(mouseX, mouseY)) {
            dragThumb = true;
            dragY = mouseY;
            dragScroll = scroll;
            return true;
        }
        if (button == 0 && overTrack(mouseX, mouseY)) {
            jumpScroll(mouseY);
            return true;
        }
        int listTop = this.topPos + layout.rowsTop;
        int listBottom = listTop + layout.listHeight;
        if (mouseY >= listTop && mouseY < listBottom) {
            positionButtons();
            for (Button trade : tradeButtons) {
                if (trade.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        if (button == 0 && overList(mouseX, mouseY)) {
            pressOnList = true;
            dragY = mouseY;
            dragScroll = scroll;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragThumb) {
            int trackH = Math.max(1, layout.listHeight);
            int thumbH = thumbHeight();
            double range = Math.max(1.0, trackH - thumbH);
            setScroll(dragScroll + (int) Math.round((mouseY - this.dragY) * (layout.maxScroll / range)));
            return true;
        }
        if (pressOnList && !dragList && Math.abs(mouseY - this.dragY) > 3.0) {
            dragList = true;
        }
        if (dragList) {
            setScroll(dragScroll - (int) Math.round(mouseY - this.dragY));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragThumb = false;
        dragList = false;
        pressOnList = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void drawChrome(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + imageWidth + 1, y + imageHeight + 1, BORDER);
        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL);

        int level = this.menu.level();
        String rank = JourneyProgress.tierName(level);
        String identity = TouristTradeCatalog.identityLine(this.menu.typeName(), this.menu.lookLabel());
        String name = this.menu.personalName();
        if (name == null || name.isBlank()) {
            name = "Tourist";
        }
        int rankWidth = this.font.width(rank);
        int nameMax = imageWidth - 24 - rankWidth - 8;
        if (this.font.width(name) > nameMax) {
            name = this.font.plainSubstrByWidth(name, Math.max(8, nameMax - this.font.width("…"))) + "…";
        }
        graphics.drawString(this.font, Component.literal(name), x + 12, y + layout.titleY, WHITE, false);
        graphics.drawString(this.font, Component.literal(rank), x + imageWidth - 12 - rankWidth, y + layout.titleY, RANK, false);
        if (!identity.isEmpty()) {
            graphics.drawString(this.font, Component.literal(identity), x + 12, y + layout.identityY, AQUA, false);
        }

        drawLabeled(graphics, x, y + layout.fromY, "From", this.menu.origin(), WHITE);
        String dest = TouristTradeCatalog.destinationLabel(this.menu.destination(), this.menu.anyTown());
        drawLabeled(graphics, x, y + layout.toY, "To", dest, GOLD);

        String detail = TouristTradeCatalog.destinationDetail(this.menu.destination(), this.menu.anyTown());
        drawWrapped(graphics, detail, x + 12, y + layout.detailY, imageWidth - 24, MUTED, layout.detailLines);

        String progress = TouristTradeCatalog.progressLine(this.menu.distanceMeters(), level, this.menu.thresholds());
        graphics.drawString(this.font, Component.literal(progress), x + 12, y + layout.progressY, YELLOW, false);
        drawBar(graphics, x, y, level);

        long now = this.minecraft != null && this.minecraft.level != null ? this.minecraft.level.getGameTime() : 0L;
        String waiting = this.clock.text(now, this.menu.openedJourneySeconds(), this.menu.openedRemainingSeconds(),
                this.menu.remainingSeconds(), this.menu.stationary());
        graphics.drawString(this.font, Component.literal(waiting), x + 12, y + layout.waitingY, MUTED, false);

        graphics.fill(x + 12, y + layout.tradesY - 4, x + imageWidth - 12, y + layout.tradesY - 3, 0xFF3A404C);
        graphics.drawString(this.font, Component.literal("Trades"), x + 12, y + layout.tradesY, GOLD, false);
    }

    private void drawScrollbar(GuiGraphics graphics, int x, int y) {
        int trackX = x + imageWidth - 10;
        int trackY = y + layout.rowsTop;
        int trackH = layout.listHeight;
        graphics.fill(trackX, trackY, trackX + 5, trackY + trackH, TRACK);
        int thumbH = thumbHeight();
        int travel = Math.max(1, trackH - thumbH);
        int thumbY = trackY + (int) Math.round(travel * (scroll / (double) layout.maxScroll));
        graphics.fill(trackX, thumbY, trackX + 5, thumbY + thumbH, THUMB);
    }

    private int thumbHeight() {
        if (layout.contentHeight <= 0) {
            return layout.listHeight;
        }
        int thumb = (int) Math.round(layout.listHeight * (layout.listHeight / (double) layout.contentHeight));
        return Math.max(12, Math.min(layout.listHeight, thumb));
    }

    private boolean overTrack(double mouseX, double mouseY) {
        if (layout.maxScroll <= 0) {
            return false;
        }
        int trackX = this.leftPos + imageWidth - 10;
        int trackY = this.topPos + layout.rowsTop;
        return mouseX >= trackX && mouseX <= trackX + 5
                && mouseY >= trackY && mouseY <= trackY + layout.listHeight;
    }

    private boolean overThumb(double mouseX, double mouseY) {
        if (!overTrack(mouseX, mouseY)) {
            return false;
        }
        int trackY = this.topPos + layout.rowsTop;
        int thumbH = thumbHeight();
        int travel = Math.max(1, layout.listHeight - thumbH);
        int thumbY = trackY + (int) Math.round(travel * (scroll / (double) Math.max(1, layout.maxScroll)));
        return mouseY >= thumbY && mouseY <= thumbY + thumbH;
    }

    private void jumpScroll(double mouseY) {
        int trackY = this.topPos + layout.rowsTop;
        double fraction = (mouseY - trackY - thumbHeight() / 2.0) / Math.max(1, layout.listHeight - thumbHeight());
        setScroll((int) Math.round(fraction * layout.maxScroll));
    }

    private boolean overList(double mouseX, double mouseY) {
        int listTop = this.topPos + layout.rowsTop;
        return mouseX >= this.leftPos + 8 && mouseX <= this.leftPos + imageWidth - 8
                && mouseY >= listTop && mouseY < listTop + layout.listHeight;
    }

    private void drawLabeled(GuiGraphics graphics, int x, int y, String label, String value, int valueColor) {
        graphics.drawString(this.font, Component.literal(label), x + 12, y, MUTED, false);
        String shown = value == null || value.isBlank() ? "Unknown" : value;
        int max = imageWidth - 78;
        if (this.font.width(shown) > max) {
            shown = this.font.plainSubstrByWidth(shown, Math.max(8, max - this.font.width("…"))) + "…";
        }
        graphics.drawString(this.font, Component.literal(shown), x + 58, y, valueColor, false);
    }

    private void drawWrapped(GuiGraphics graphics, String text, int x, int y, int width, int color, int maxLines) {
        List<String> lines = UiTextWrap.wrap(text, width, this.font::width);
        int limit = Math.min(maxLines, lines.size());
        for (int i = 0; i < limit; i++) {
            String line = lines.get(i);
            if (i == limit - 1 && i < lines.size() - 1) {
                line = this.font.plainSubstrByWidth(line, Math.max(8, width - this.font.width("…"))) + "…";
            }
            graphics.drawString(this.font, Component.literal(line), x, y + i * 11, color, false);
        }
    }

    private void drawBar(GuiGraphics graphics, int x, int y, int level) {
        int barX = x + 12;
        int barY = y + layout.barY;
        int barW = imageWidth - 24;
        graphics.fill(barX, barY, barX + barW, barY + 5, BAR_BG);
        double fraction = JourneyProgress.rankFraction(this.menu.distanceMeters(), level, this.menu.thresholds());
        int fill = (int) Math.round(barW * fraction);
        if (fill > 0) {
            graphics.fill(barX, barY, barX + Math.min(barW, fill), barY + 5, BAR_FILL);
        }
    }

    private void drawRow(GuiGraphics graphics, int x, int y, int index, TradeDef def, boolean hovered) {
        int rowY = y + layout.rowsTop + index * layout.rowHeight - scroll;
        int bg = hovered ? ROW_HOVER : (index % 2 == 0 ? ROW : ROW_ALT);
        int right = x + imageWidth - (layout.maxScroll > 0 ? 14 : 8);
        graphics.fill(x + 8, rowY + 1, right, rowY + layout.rowHeight - 1, bg);

        ItemStack cost = new ItemStack(def.costItem(), def.costCount());
        ItemStack result = "ticket".equals(def.id())
                ? TouristTradeCatalog.ticketStack(context())
                : new ItemStack(def.resultItem(), def.resultCount());
        int iconY = rowY + (layout.rowHeight - 16) / 2;
        graphics.renderItem(cost, x + 12, iconY);
        graphics.renderItemDecorations(this.font, cost, x + 12, iconY);
        graphics.drawString(this.font, Component.literal("→"), x + 34, iconY + 4, MUTED, false);
        graphics.renderItem(result, x + 48, iconY);
        graphics.renderItemDecorations(this.font, result, x + 48, iconY);

        RowState state = stateFor(def, index);
        int textX = x + 70;
        boolean buttonRoom = index < tradeButtons.size() && tradeButtons.get(index).visible;
        int textW = imageWidth - 70 - (buttonRoom ? 96 : 24) - (layout.maxScroll > 0 ? 8 : 0);
        String title = def.title();
        if (this.font.width(title) > textW) {
            title = this.font.plainSubstrByWidth(title, Math.max(8, textW - this.font.width("…"))) + "…";
        }
        graphics.drawString(this.font, Component.literal(title), textX, rowY + (layout.roomy ? 4 : 3), WHITE, false);
        if (layout.roomy) {
            String second = secondLine(def, state);
            int color = switch (state) {
                case NEED -> NEED;
                case LOCKED, TRADED -> LOCKED;
                case READY -> MUTED;
            };
            if (this.font.width(second) > textW) {
                second = this.font.plainSubstrByWidth(second, Math.max(8, textW - this.font.width("…"))) + "…";
            }
            graphics.drawString(this.font, Component.literal(second), textX, rowY + 16, color, false);
        }
    }

    private String secondLine(TradeDef def, RowState state) {
        return switch (state) {
            case LOCKED -> TouristTradeCatalog.lockLabel(def, this.menu.thresholds());
            case NEED -> TouristTradeCatalog.needLabel(def, count(def));
            case TRADED -> TouristTradeCatalog.tradedLabel();
            case READY -> def.detail();
        };
    }

    private RowState stateFor(TradeDef def, int index) {
        boolean used = TouristTradeCatalog.isUsed(this.menu.usesMask(), index);
        return TouristTradeCatalog.rowState(this.menu.level(), count(def), used, def);
    }

    private int count(TradeDef def) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return 0;
        }
        Inventory inventory = this.minecraft.player.getInventory();
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            stacks.add(inventory.getItem(i));
        }
        return TouristTradeCatalog.countOf(stacks, def.costItem());
    }

    private boolean rowVisible(int index) {
        int rowY = index * layout.rowHeight - scroll;
        return rowY < layout.listHeight && rowY + layout.rowHeight > 0;
    }

    private int tooltipRow(int mouseX, int mouseY) {
        if (forcedTooltipRow >= 0 && forcedTooltipRow < defs().size() && rowVisible(forcedTooltipRow)) {
            return forcedTooltipRow;
        }
        if (showcaseSuppressHover) {
            return -1;
        }
        return rowAt(mouseX, mouseY);
    }

    private void drawSideTooltip(GuiGraphics graphics, List<Component> raw) {
        int gap = 8;
        int side = Math.max(this.leftPos, this.width - (this.leftPos + this.imageWidth)) - gap;
        int wrap = Math.min(220, Math.max(96, side - 8));
        List<Component> lines = fitLines(raw, wrap);
        if (lines.isEmpty()) {
            return;
        }
        int textW = 0;
        for (Component line : lines) {
            textW = Math.max(textW, this.font.width(line));
        }
        int boxW = textW + 8;
        int boxH = lines.size() * 10 + 8;
        int rightX = this.leftPos + this.imageWidth + gap;
        int leftX = this.leftPos - gap - boxW;
        int x = rightX + boxW <= this.width - 2 ? rightX : (leftX >= 2 ? leftX : 4);
        int y = this.topPos + this.layout.rowsTop;
        if (x + boxW > this.width - 2) {
            x = Math.max(2, this.width - 2 - boxW);
        }
        if (y + boxH > this.height - 2) {
            y = Math.max(2, this.height - 2 - boxH);
        }
        int textX = x + 4;
        int textY = y + 4;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0, 0.0, 400.0);
        graphics.fill(textX - 4, textY - 4, textX + textW + 4, textY - 3, 0xF0100010);
        graphics.fill(textX - 4, textY + lines.size() * 10 + 3, textX + textW + 4, textY + lines.size() * 10 + 4, 0xF0100010);
        graphics.fill(textX - 4, textY - 3, textX + textW + 4, textY + lines.size() * 10 + 3, 0xF0100010);
        graphics.fill(textX - 5, textY - 3, textX - 4, textY + lines.size() * 10 + 3, 0xF0100010);
        graphics.fill(textX + textW + 4, textY - 3, textX + textW + 5, textY + lines.size() * 10 + 3, 0xF0100010);
        int borderTop = 0x505000FF;
        int borderBottom = 0x5028007F;
        int innerBottom = textY + lines.size() * 10 + 3;
        graphics.fillGradient(textX - 4, textY - 2, textX - 3, innerBottom - 1, borderTop, borderBottom);
        graphics.fillGradient(textX + textW + 3, textY - 2, textX + textW + 4, innerBottom - 1, borderTop, borderBottom);
        graphics.fillGradient(textX - 4, textY - 3, textX + textW + 4, textY - 2, borderTop, borderTop);
        graphics.fillGradient(textX - 4, innerBottom - 1, textX + textW + 4, innerBottom, borderBottom, borderBottom);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(this.font, lines.get(i), textX, textY + i * 10, 0xFFFFFFFF, false);
        }
        graphics.pose().popPose();
    }

    private List<Component> fitLines(List<Component> raw, int maxWidth) {
        List<Component> lines = new ArrayList<>();
        for (Component line : raw) {
            for (String part : UiTextWrap.wrap(line.getString(), maxWidth, this.font::width)) {
                if (!part.isEmpty()) {
                    lines.add(Component.literal(part).withStyle(line.getStyle()));
                }
            }
        }
        return lines;
    }

    private int rowAt(double mouseX, double mouseY) {
        if (!overList(mouseX, mouseY)) {
            return -1;
        }
        int local = (int) mouseY - (this.topPos + layout.rowsTop) + scroll;
        if (local < 0) {
            return -1;
        }
        int index = local / layout.rowHeight;
        if (index < 0 || index >= defs().size()) {
            return -1;
        }
        return index;
    }

    private List<Component> tooltipFor(int index) {
        TradeDef def = defs().get(index);
        RowState state = stateFor(def, index);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(def.title()).withStyle(ChatFormatting.GOLD));
        if ("ticket".equals(def.id())) {
            lines.addAll(TouristTradeCatalog.ticketLore(context()));
        } else {
            lines.add(Component.literal(def.detail()).withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal(def.costCount() + " " + TouristTradeCatalog.itemName(def.costItem())
                    + "  →  " + def.resultCount() + " " + TouristTradeCatalog.itemName(def.resultItem()))
                    .withStyle(ChatFormatting.WHITE));
        }
        Component status = switch (state) {
            case LOCKED -> Component.literal(TouristTradeCatalog.lockLabel(def, this.menu.thresholds()))
                    .withStyle(ChatFormatting.RED);
            case NEED -> Component.literal(TouristTradeCatalog.needLabel(def, count(def)))
                    .withStyle(ChatFormatting.RED);
            case TRADED -> Component.literal(TouristTradeCatalog.tradedLabel()).withStyle(ChatFormatting.DARK_GRAY);
            case READY -> Component.literal("Click Trade. The cost comes out of your inventory.")
                    .withStyle(ChatFormatting.GREEN);
        };
        lines.add(status);
        return lines;
    }

    private TradeContext context() {
        int level = this.menu.level();
        return new TradeContext(
                this.menu.origin(),
                this.menu.destination(),
                this.menu.anyTown(),
                JourneyProgress.tierName(level),
                this.menu.distanceMeters(),
                TouristTradeCatalog.formatDuration(this.clock.shownJourney()),
                TouristTradeCatalog.formatDuration(this.clock.shownRemaining()),
                this.menu.stationary());
    }
}
