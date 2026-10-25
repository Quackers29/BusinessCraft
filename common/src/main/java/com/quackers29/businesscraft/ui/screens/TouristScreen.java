package com.quackers29.businesscraft.ui.screens;

import com.quackers29.businesscraft.economy.JourneyProgress;
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
 * Tourist conversation. Replaces the villager trade window, which titled every
 * tourist "Novice" and hid the journey inside an untradeable barrier row.
 * Rank names match the distance ranks on the name tag. The deals themselves
 * are unchanged.
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

    private Layout layout = Layout.roomy(420, 6);
    private final List<Button> tradeButtons = new ArrayList<>();
    private int forcedTooltipRow = -1;

    /**
     * Showcase leaves the cursor in the middle of the window, which lands on the
     * first trade. While this is set, only an explicit hover request draws a tooltip.
     */
    private static boolean showcaseSuppressHover;

    public TouristScreen(TouristMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.literal("Tourist"));
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

    @Override
    protected void init() {
        this.layout = Layout.choose(this.width, this.height, TouristTradeCatalog.all().size());
        this.imageWidth = layout.width;
        this.imageHeight = layout.height;
        super.init();
        this.tradeButtons.clear();
        List<TradeDef> defs = TouristTradeCatalog.all();
        for (int i = 0; i < defs.size(); i++) {
            int index = i;
            int bx = this.leftPos + this.imageWidth - 12 - 68;
            int by = this.topPos + layout.rowsTop + i * layout.rowHeight + (layout.rowHeight - 16) / 2;
            Button button = Button.builder(Component.literal("Trade"), pressed -> pressTrade(index))
                    .bounds(bx, by, 68, 16)
                    .build();
            this.tradeButtons.add(this.addRenderableWidget(button));
        }
        refreshButtons();
    }

    @Override
    public void removed() {
        showcaseSuppressHover = false;
        super.removed();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshButtons();
    }

    private void pressTrade(int index) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, index);
        }
    }

    private void refreshButtons() {
        List<TradeDef> defs = TouristTradeCatalog.all();
        for (int i = 0; i < tradeButtons.size() && i < defs.size(); i++) {
            RowState state = stateFor(defs.get(i), i);
            Button button = tradeButtons.get(i);
            button.visible = state == RowState.READY || state == RowState.NEED;
            button.active = state == RowState.READY;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        int tip = tooltipRow(mouseX, mouseY);
        if (tip >= 0) {
            drawSideTooltip(graphics, tooltipFor(tip));
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        drawPanel(graphics, this.leftPos, this.topPos, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // The villager screen appends merchant.level.N ("Novice") here. We draw our own title.
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        graphics.fill(x - 1, y - 1, x + imageWidth + 1, y + imageHeight + 1, BORDER);
        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL);

        int level = this.menu.level();
        String rank = JourneyProgress.tierName(level);
        String identity = TouristTradeCatalog.identityLine(this.menu.typeName(), this.menu.lookLabel());
        graphics.drawString(this.font, Component.literal("Tourist"), x + 12, y + layout.titleY, WHITE, false);
        int rankWidth = this.font.width(rank);
        graphics.drawString(this.font, Component.literal(rank), x + imageWidth - 12 - rankWidth, y + layout.titleY, RANK, false);
        if (!identity.isEmpty()) {
            graphics.drawString(this.font, Component.literal(identity), x + 12, y + layout.identityY, AQUA, false);
        }

        drawLabeled(graphics, x, y + layout.fromY, "From", this.menu.origin(), WHITE);
        String dest = TouristTradeCatalog.destinationLabel(this.menu.destination(), this.menu.anyTown());
        drawLabeled(graphics, x, y + layout.toY, "To", dest, GOLD);

        String detail = TouristTradeCatalog.destinationDetail(this.menu.destination(), this.menu.anyTown());
        drawWrapped(graphics, detail, x + 12, y + layout.detailY, imageWidth - 24, MUTED, layout.roomy ? 2 : 1);

        String progress = TouristTradeCatalog.progressLine(this.menu.distanceMeters(), level, this.menu.thresholds());
        graphics.drawString(this.font, Component.literal(progress), x + 12, y + layout.progressY, YELLOW, false);
        drawBar(graphics, x, y, level);

        String waiting = TouristTradeCatalog.waitingLine(
                this.menu.journeySeconds(), this.menu.remainingSeconds(), this.menu.stationary());
        graphics.drawString(this.font, Component.literal(waiting), x + 12, y + layout.waitingY, MUTED, false);
        graphics.drawString(this.font, Component.literal(TouristTradeCatalog.transportNote()),
                x + 12, y + layout.transportY, MUTED, false);

        graphics.fill(x + 12, y + layout.tradesY - 4, x + imageWidth - 12, y + layout.tradesY - 3, 0xFF3A404C);
        graphics.drawString(this.font, Component.literal("Trades"), x + 12, y + layout.tradesY, GOLD, false);

        List<TradeDef> defs = TouristTradeCatalog.all();
        int hovered = tooltipRow(mouseX, mouseY);
        for (int i = 0; i < defs.size(); i++) {
            drawRow(graphics, x, y, i, defs.get(i), i == hovered);
        }
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
        int rowY = y + layout.rowsTop + index * layout.rowHeight;
        int bg = hovered ? ROW_HOVER : (index % 2 == 0 ? ROW : ROW_ALT);
        graphics.fill(x + 8, rowY + 1, x + imageWidth - 8, rowY + layout.rowHeight - 1, bg);

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
        int textW = imageWidth - 70 - (tradeButtons.size() > index && tradeButtons.get(index).visible ? 88 : 20);
        String title = def.title();
        if (this.font.width(title) > textW) {
            title = this.font.plainSubstrByWidth(title, Math.max(8, textW - this.font.width("…"))) + "…";
        }
        graphics.drawString(this.font, Component.literal(title), textX, rowY + (layout.roomy ? 4 : 3), WHITE, false);
        if (layout.roomy) {
            String second = secondLine(def, state);
            int color = switch (state) {
                case NEED -> NEED;
                case LOCKED -> LOCKED;
                case TRADED -> LOCKED;
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

    private int tooltipRow(int mouseX, int mouseY) {
        if (forcedTooltipRow >= 0 && forcedTooltipRow < TouristTradeCatalog.all().size()) {
            return forcedTooltipRow;
        }
        if (showcaseSuppressHover) {
            return -1;
        }
        return rowAt(mouseX, mouseY);
    }

    /**
     * Draws the trade explanation beside the panel. A cursor tooltip lands on the
     * row itself and hides the words it is explaining, and on a narrow window it
     * runs off the edge.
     */
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
        int x = this.leftPos;
        int y = this.topPos;
        if (mouseX < x + 8 || mouseX > x + imageWidth - 8) {
            return -1;
        }
        int local = (int) mouseY - (y + layout.rowsTop);
        if (local < 0) {
            return -1;
        }
        int index = local / layout.rowHeight;
        if (index < 0 || index >= TouristTradeCatalog.all().size()) {
            return -1;
        }
        return index;
    }

    private List<Component> tooltipFor(int index) {
        TradeDef def = TouristTradeCatalog.all().get(index);
        RowState state = stateFor(def, index);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(def.title()).withStyle(ChatFormatting.GOLD));
        if ("ticket".equals(def.id())) {
            lines.addAll(TouristTradeCatalog.ticketLore(context()));
        } else {
            lines.add(Component.literal(def.detail()).withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal(def.costCount() + " " + TouristTradeCatalog.costName(def)
                    + "  →  " + def.resultCount() + " " + resultName(def)).withStyle(ChatFormatting.WHITE));
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

    private static String resultName(TradeDef def) {
        if (def.resultItem() == net.minecraft.world.item.Items.IRON_INGOT) {
            return "iron";
        }
        if (def.resultItem() == net.minecraft.world.item.Items.COMPASS) {
            return "compass";
        }
        if (def.resultItem() == net.minecraft.world.item.Items.CLOCK) {
            return "clock";
        }
        return "item";
    }

    private TradeContext context() {
        int level = this.menu.level();
        return new TradeContext(
                this.menu.origin(),
                this.menu.destination(),
                this.menu.anyTown(),
                JourneyProgress.tierName(level),
                this.menu.distanceMeters(),
                TouristTradeCatalog.formatDuration(this.menu.journeySeconds()),
                TouristTradeCatalog.formatDuration(this.menu.remainingSeconds()),
                this.menu.stationary());
    }

    private static final class Layout {
        final int width;
        final int height;
        final int rowHeight;
        final int rowsTop;
        final boolean roomy;
        final int titleY;
        final int identityY;
        final int fromY;
        final int toY;
        final int detailY;
        final int progressY;
        final int barY;
        final int waitingY;
        final int transportY;
        final int tradesY;

        private Layout(int width, int height, int rowHeight, int rowsTop, boolean roomy,
                int titleY, int identityY, int fromY, int toY, int detailY, int progressY, int barY,
                int waitingY, int transportY, int tradesY) {
            this.width = width;
            this.height = height;
            this.rowHeight = rowHeight;
            this.rowsTop = rowsTop;
            this.roomy = roomy;
            this.titleY = titleY;
            this.identityY = identityY;
            this.fromY = fromY;
            this.toY = toY;
            this.detailY = detailY;
            this.progressY = progressY;
            this.barY = barY;
            this.waitingY = waitingY;
            this.transportY = transportY;
            this.tradesY = tradesY;
        }

        static Layout roomy(int width, int rows) {
            return choose(width + 40, 800, rows);
        }

        static Layout choose(int screenWidth, int screenHeight, int rows) {
            // 420 leaves a gutter for the trade tooltip and fits the longest row
            // ("Offered from the first step of the journey.") beside the Trade button.
            int width = Math.min(420, Math.max(320, screenWidth - 16));
            int roomyHeight = 174 + rows * 32;
            if (screenHeight >= roomyHeight + 8) {
                return new Layout(width, roomyHeight, 32, 162, true,
                        10, 24, 40, 54, 68, 96, 110, 120, 134, 148);
            }
            int compactHeight = 140 + rows * 22;
            int height = Math.min(compactHeight, Math.max(160, screenHeight - 8));
            return new Layout(width, height, 22, 128, false,
                    6, 18, 32, 44, 56, 72, 84, 92, 104, 116);
        }
    }
}
