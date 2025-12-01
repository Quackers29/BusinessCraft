package com.quackers29.businesscraft.ui.screens.town;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.contract.Contract;
import com.quackers29.businesscraft.contract.SellContract;
import com.quackers29.businesscraft.contract.CourierContract;
import com.quackers29.businesscraft.menu.ContractBoardMenu;
import com.quackers29.businesscraft.network.packets.ui.BidContractPacket;
import com.quackers29.businesscraft.ui.builders.UIGridBuilder;
import com.quackers29.businesscraft.ui.util.InventoryRenderer;
import com.quackers29.businesscraft.ui.util.ScreenNavigationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.function.Consumer;

public class ContractBoardScreen extends AbstractContainerScreen<ContractBoardMenu> {

    // Layout constants - copied from PaymentBoardScreen
    private static final int SECTION_PADDING = 8;
    private static final int INNER_PADDING = 6;
    private static final int ELEMENT_SPACING = 20;
    private static final int HEADER_HEIGHT = 35;
    private static final int BACK_BUTTON_X = SECTION_PADDING;
    private static final int BACK_BUTTON_Y = SECTION_PADDING;
    private static final int BACK_BUTTON_WIDTH = 28;
    private static final int BACK_BUTTON_HEIGHT = 20;
    private static final int CONTRACT_GRID_X = SECTION_PADDING + 12;
    private static final int CONTRACT_GRID_Y = HEADER_HEIGHT + ELEMENT_SPACING;
    private static final int CONTRACT_GRID_WIDTH = 300;
    private static final int CONTRACT_GRID_HEIGHT = 140;
    private static final int TAB_BUTTON_Y = 22; // Below header
    private static final int TAB_WIDTH = 80;
    private static final int TAB_HEIGHT = 20;

    // Tab state: 0=Available, 1=Active, 2=History
    private int selectedTab = 0;

    private UIGridBuilder contractGrid;
    private List<Contract> currentContracts = new ArrayList<>();
    private EditBox bidInput;
    private UUID biddingContractId = null;
    private boolean showBidInput = false;

    // Colors from PaymentBoardScreen
    private static final int HEADER_COLOR = 0xFFFCB821;
    private static final int SECTION_BG_COLOR = 0x90000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int SUCCESS_COLOR = 0xB0228B22;

    public ContractBoardScreen(ContractBoardMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 340;
        this.imageHeight = 240;
        this.titleLabelX = BACK_BUTTON_X + BACK_BUTTON_WIDTH + 6;
        this.titleLabelY = SECTION_PADDING + 4;
        this.inventoryLabelY = 300; // Hide inventory label
    }

    @Override
    protected void init() {
        super.init();
        createContractGrid(); // Init grid early
        initBidInput();
    }

    private void initBidInput() {
        bidInput = new EditBox(this.font, 0, 0, 100, 20, Component.literal("Bid"));
        bidInput.setMaxLength(10);
        bidInput.setValue("10.0");
    }

    public void updateContracts(List<Contract> contracts) {
        menu.setContracts(contracts);
        currentContracts = new ArrayList<>(contracts);
        if (contractGrid != null) {
            updateContractGrid();
        }
    }

    private void updateContractData() {
        List<Contract> contracts = menu.getContracts();
        if (contracts == null)
            contracts = new ArrayList<>();
        if (!contracts.equals(currentContracts)) {
            currentContracts = new ArrayList<>(contracts);
            updateContractGrid();
        }
    }

    private void createContractGrid() {
        contractGrid = UIGridBuilder
                .create(CONTRACT_GRID_X, CONTRACT_GRID_Y + 5, CONTRACT_GRID_WIDTH - 8, CONTRACT_GRID_HEIGHT - 6, 5)
                .withRowHeight(18)
                .withSpacing(8, 2)
                .drawBackground(false)
                .drawBorder(false)
                .withVerticalScrollAuto(true);
        updateContractGrid();
    }

    private void updateContractGrid() {
        if (contractGrid == null) {
            createContractGrid();
            return;
        }
        contractGrid.clearElements();
        List<Contract> filtered = filterContractsByTab();
        contractGrid.updateTotalRows(filtered.size());
        populateGrid(filtered);
    }

    private List<Contract> filterContractsByTab() {
        return currentContracts.stream().filter(c -> {
            boolean expired = c.isExpired();
            boolean completed = c.isCompleted();
            switch (selectedTab) {
                case 0:
                    return !expired && !completed; // Available
                case 1:
                    return !expired && completed; // Active
                case 2:
                    return expired || completed; // History
            }
            return false;
        }).collect(Collectors.toList());
    }

    private void populateGrid(List<Contract> contracts) {
        for (int i = 0; i < contracts.size(); i++) {
            Contract c = contracts.get(i);
            // Col 0: Icon with tooltip
            ItemStack icon = getResourceIcon(c);
            contractGrid.addItemStackWithTooltip(i, 0, icon, getContractTooltip(c), null);

            // Col 1: Details with tooltip
            String details = getContractDetails(c);
            contractGrid.addLabelWithTooltip(i, 1, truncate(details, 12), getContractTooltip(c), TEXT_COLOR);

            // Col 2: Status
            String status = getContractStatus(c);
            contractGrid.addLabel(i, 2, truncate(status, 12), TEXT_COLOR);

            // Col 3: Time
            long currentTick = this.minecraft.level.getGameTime();
            long ticksLeft = c.getExpiryTime() - currentTick;
            String time = ticksLeft > 0 ? (ticksLeft / 20) + "s" : "Expired";
            contractGrid.addLabel(i, 3, time, TEXT_COLOR);

            // Col 4: Action button
            if (c instanceof CourierContract && selectedTab == 0) {
                contractGrid.addButtonWithTooltip(i, 4, "Bid", "Place bid on courier",
                        (Consumer<Void>) v -> startBid(c.getId()), SUCCESS_COLOR);
            } else {
                contractGrid.addLabel(i, 4, "View", 0xFF666666);
            }
        }
        if (contracts.isEmpty()) {
            contractGrid.addLabel(0, 1, "No contracts", 0xFF808080);
        }
    }

    private String getContractDetails(Contract c) {
        if (c instanceof SellContract sc)
            return "Sell " + sc.getQuantity() + " " + sc.getResourceId();
        if (c instanceof CourierContract cc)
            return "Courier " + cc.getQuantity() + " " + cc.getResourceId();
        return "Contract";
    }

    private String getContractStatus(Contract c) {
        if (c instanceof SellContract sc)
            return "Town bids: " + String.format("%.2f", sc.getHighestBid());
        if (c instanceof CourierContract cc)
            return "Reward: " + String.format("%.2f", cc.getReward());
        return "N/A";
    }

    private ItemStack getResourceIcon(Contract c) {
        String res = "";
        if (c instanceof SellContract sc)
            res = sc.getResourceId();
        else if (c instanceof CourierContract cc)
            res = cc.getResourceId();
        if ("wood".equals(res))
            return new ItemStack(Items.OAK_LOG);
        if ("iron".equals(res))
            return new ItemStack(Items.IRON_INGOT);
        return new ItemStack(Items.EMERALD); // Fallback
    }

    private String getContractTooltip(Contract c) {
        return "ID: " + c.getId() + " Expires: " + c.getExpiryTime();
    }

    private String truncate(String text, int maxChars) {
        if (text == null || text.length() <= maxChars)
            return text;
        return text.substring(0, maxChars - 3) + "...";
    }

    private String getTabTitle() {
        switch (selectedTab) {
            case 0:
                return "Available";
            case 1:
                return "Active";
            case 2:
                return "History";
        }
        return "Contracts";
    }

    private boolean isMouseOverBackButton(int mx, int my) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        return InventoryRenderer.isMouseOverElement(mx, my, x, y, BACK_BUTTON_X, BACK_BUTTON_Y, BACK_BUTTON_WIDTH,
                BACK_BUTTON_HEIGHT);
    }

    private boolean isMouseOverTab(int mx, int my, int tabIndex) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int tabX = x + 50 + tabIndex * (TAB_WIDTH + 4);
        return mx >= tabX && mx < tabX + TAB_WIDTH && my >= y + TAB_BUTTON_Y && my < y + TAB_BUTTON_Y + TAB_HEIGHT;
    }

    private boolean isMouseOverAnyTab(int mx, int my) {
        for (int i = 0; i < 3; i++) {
            if (isMouseOverTab(mx, my, i))
                return true;
        }
        return false;
    }

    private boolean isMouseOverBidArea(int mx, int my) {
        int x = (width - 120) / 2;
        int y = (height - 60) / 2;
        return mx >= x && mx < x + 120 && my >= y && my < y + 40;
    }

    private void playDownSound() {
        playClickSound();
    }

    private void playClickSound() {
        minecraft.getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void returnToTownInterface() {
        this.onClose();
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    private void startBid(UUID id) {
        biddingContractId = id;
        showBidInput = true;
        bidInput.setValue("10.0");
    }

    private void renderBidInput(GuiGraphics g, int mx, int my) {
        int x = (width - 120) / 2;
        int y = (height - 60) / 2;
        g.fill(x - 2, y - 2, x + 122, y + 62, 0xC0000000);
        InventoryRenderer.drawBorder(g, x - 2, y - 2, 124, 64, 0xFFFFFFFF, 1);
        g.drawString(font, "Bid amount:", x, y, 0xFFFFFF);
        bidInput.render(g, mx, my, 0);
        g.drawString(font, "Emeralds", x + bidInput.getWidth() + 4, y + 12, 0xFFFFFF);
    }

    private void renderCustomTooltips(GuiGraphics g, int mx, int my) {
        // Handled by UIGridBuilder
    }

    private void sendBid() {
        try {
            float amount = Float.parseFloat(bidInput.getValue());
            PlatformAccess.getNetworkMessages().sendToServer(new BidContractPacket(biddingContractId, amount));
            showBidInput = false;
        } catch (NumberFormatException ignored) {
            // Ignore invalid
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        updateContractData();
        renderBackground(g);
        super.render(g, mx, my, pt);
        int sx = (width - imageWidth) / 2, sy = (height - imageHeight) / 2;
        if (contractGrid != null) {
            g.pose().pushPose();
            g.pose().translate(sx, sy, 0);
            contractGrid.render(g, mx - sx, my - sy);
            g.pose().popPose();
        }
        renderCustomTooltips(g, mx, my);
        if (showBidInput)
            renderBidInput(g, mx, my);
        renderTooltip(g, mx, my);
        if (isMouseOverBackButton(mx, my)) {
            g.renderTooltip(font, Component.literal("Return to Town Interface"), mx, my);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = (width - imageWidth) / 2, y = (height - imageHeight) / 2;
        g.fill(x, y, x + imageWidth, y + imageHeight, InventoryRenderer.BACKGROUND_COLOR);
        InventoryRenderer.drawBorder(g, x, y, imageWidth, imageHeight, InventoryRenderer.BORDER_COLOR, 2);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, InventoryRenderer.BACKGROUND_COLOR);
        renderHeaderSection(g, x, y, mx, my);
        renderContractSection(g, x, y);
        renderTabButtons(g, x, y, mx, my);
    }

    private void renderHeaderSection(GuiGraphics g, int x, int y, int mx, int my) {
        g.fill(x + 4, y + 4, x + imageWidth - 4, y + HEADER_HEIGHT, SECTION_BG_COLOR);
        boolean hover = isMouseOverBackButton(mx, my);
        InventoryRenderer.drawButton(g, x + BACK_BUTTON_X, y + BACK_BUTTON_Y, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT,
                "◀", font, hover);
        g.drawString(font, "Contracts Board", x + titleLabelX, y + titleLabelY, HEADER_COLOR);
    }

    private void renderContractSection(GuiGraphics g, int x, int y) {
        int padding = INNER_PADDING;
        int sx = x + CONTRACT_GRID_X - padding;
        int sy = y + CONTRACT_GRID_Y - padding;
        int w = CONTRACT_GRID_WIDTH + padding * 2;
        int h = CONTRACT_GRID_HEIGHT + padding * 2;
        g.fill(sx, sy, sx + w, sy + h, SECTION_BG_COLOR);
        InventoryRenderer.drawBorder(g, sx, sy, w, h, InventoryRenderer.INVENTORY_BORDER_COLOR, 2);
        g.drawString(font, getTabTitle(), x + CONTRACT_GRID_X, y + CONTRACT_GRID_Y - 12, HEADER_COLOR);
    }

    private void renderTabButtons(GuiGraphics g, int x, int y, int mx, int my) {
        String[] tabNames = { "Available", "Active", "History" };
        for (int i = 0; i < 3; i++) {
            boolean hover = isMouseOverTab(mx, my, i);
            boolean sel = i == selectedTab;
            int tabX = x + 50 + i * (TAB_WIDTH + 4); // Start after back button
            InventoryRenderer.drawButton(g, tabX, y + TAB_BUTTON_Y, TAB_WIDTH, TAB_HEIGHT, tabNames[i], font,
                    hover || sel);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            if (isMouseOverBackButton((int) mx, (int) my)) {
                playDownSound();
                returnToTownInterface();
                return true;
            }
            if (showBidInput && isMouseOverBidArea((int) mx, (int) my)) {
                bidInput.mouseClicked(mx, my, btn);
                return true;
            }
            if (isMouseOverAnyTab((int) mx, (int) my)) {
                int tabX = (width - imageWidth) / 2 + 50;
                int tx = (int) mx - ((width - imageWidth) / 2);
                if (tx >= 50 && tx < 50 + 3 * TAB_WIDTH + 2 * 4) {
                    selectedTab = ((tx - 50) / (TAB_WIDTH + 4));
                    updateContractGrid(); // Force update on tab switch
                    playDownSound();
                    return true;
                }
            }
        }
        if (contractGrid != null) {
            int sx = (width - imageWidth) / 2;
            int sy = (height - imageHeight) / 2;
            if (contractGrid.mouseClicked((int) mx - sx, (int) my - sy, btn))
                return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (contractGrid != null) {
            int sx = (width - imageWidth) / 2;
            int sy = (height - imageHeight) / 2;
            return contractGrid.mouseScrolled(mx - sx, my - sy, delta);
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (showBidInput && bidInput.keyPressed(key, scan, mod)) {
            if (key == 257 || key == 335)
                sendBid();
            return true;
        }
        return super.keyPressed(key, scan, mod);
    }
}
