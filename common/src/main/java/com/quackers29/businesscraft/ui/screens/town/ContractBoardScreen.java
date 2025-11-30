package com.quackers29.businesscraft.ui.screens.town;

import com.quackers29.businesscraft.menu.ContractBoardMenu;
import com.quackers29.businesscraft.ui.util.InventoryRenderer;
import com.quackers29.businesscraft.ui.util.ScreenNavigationHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import com.quackers29.businesscraft.contract.Contract;
import com.quackers29.businesscraft.contract.SellContract;
import com.quackers29.businesscraft.contract.CourierContract;
import java.util.List;
import java.util.stream.Collectors;

public class ContractBoardScreen extends AbstractContainerScreen<ContractBoardMenu> {
        // Tab constants
        private static final int TAB_TRADING = 0;
        private static final int TAB_COURIER = 1;
        private int selectedTab = TAB_TRADING;

        // Tab button coordinates
        private static final int TAB_WIDTH = 60;
        private static final int TAB_HEIGHT = 16;
        private static final int TAB_Y_OFFSET = -18;

        // Back button coordinates
        private static final int BACK_BUTTON_X = 8;
        private static final int BACK_BUTTON_Y = 6;
        private static final int BACK_BUTTON_WIDTH = 20;
        private static final int BACK_BUTTON_HEIGHT = 20;

        public ContractBoardScreen(ContractBoardMenu menu, Inventory inventory, Component title) {
                super(menu, inventory, title);

                // Set the size of the screen
                this.imageWidth = 176;
                this.imageHeight = 166;

                // Position the title and inventory text
                this.titleLabelX = 8 + BACK_BUTTON_WIDTH + 4;
                this.titleLabelY = 8;
                this.inventoryLabelX = 8;
                this.inventoryLabelY = this.imageHeight - 94;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                this.renderBackground(guiGraphics);
                super.render(guiGraphics, mouseX, mouseY, partialTick);
                this.renderTooltip(guiGraphics, mouseX, mouseY);

                // Render tooltip for back button if mouse is over it
                if (isMouseOverBackButton(mouseX, mouseY)) {
                        guiGraphics.renderTooltip(this.font, Component.literal("Return to Town Interface"), mouseX,
                                        mouseY);
                }
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
                // Calculate position for centered interface
                int x = (this.width - this.imageWidth) / 2;
                int y = (this.height - this.imageHeight) / 2;

                // Draw the background panel
                guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, InventoryRenderer.BACKGROUND_COLOR);
                guiGraphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + this.imageHeight - 1,
                                InventoryRenderer.BORDER_COLOR);
                guiGraphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + this.imageHeight - 2,
                                InventoryRenderer.BACKGROUND_COLOR);

                // Draw tabs
                drawTabs(guiGraphics, x, y, mouseX, mouseY);

                // Draw contract list based on selected tab
                drawContractList(guiGraphics, x, y, mouseX, mouseY);

                // Draw screen title
                InventoryRenderer.drawLabel(guiGraphics, this.font, "Contract Board",
                                x + this.titleLabelX, y + this.titleLabelY);

                // Draw inventory label
                InventoryRenderer.drawLabel(guiGraphics, this.font, "Inventory",
                                x + this.inventoryLabelX, y + this.inventoryLabelY);

                // Draw player inventory using the utility class
                InventoryRenderer.drawInventoryWithHotbar(guiGraphics,
                                x + 8, y + 84,
                                9, 3, 1, 4);

                // Draw the back button
                boolean isBackButtonHovered = isMouseOverBackButton(mouseX, mouseY);
                InventoryRenderer.drawButton(guiGraphics,
                                x + BACK_BUTTON_X, y + BACK_BUTTON_Y,
                                BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT,
                                "B", this.font, isBackButtonHovered);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
                // Check if the back button was clicked
                if (button == 0 && isMouseOverBackButton((int) mouseX, (int) mouseY)) {
                        playClickSound();
                        returnToMainUI();
                        return true;
                }

                // Check tab clicks
                int x = (this.width - this.imageWidth) / 2;
                int y = (this.height - this.imageHeight) / 2;

                if (mouseY >= y + TAB_Y_OFFSET && mouseY < y + TAB_Y_OFFSET + TAB_HEIGHT) {
                        if (mouseX >= x && mouseX < x + TAB_WIDTH) {
                                selectedTab = TAB_TRADING;
                                playClickSound();
                                return true;
                        } else if (mouseX >= x + TAB_WIDTH + 4 && mouseX < x + TAB_WIDTH * 2 + 4) {
                                selectedTab = TAB_COURIER;
                                playClickSound();
                                return true;
                        }
                }

                // Check contract accept buttons
                if (selectedTab == TAB_COURIER) {
                        List<Contract> allContracts = this.menu.getContracts();
                        List<Contract> filteredContracts = allContracts.stream()
                                        .filter(c -> c instanceof CourierContract)
                                        .collect(Collectors.toList());

                        int listX = x + 8;
                        int listY = y + 20;
                        int itemHeight = 35;

                        for (int i = 0; i < Math.min(filteredContracts.size(), 2); i++) {
                                int itemY = listY + (i * itemHeight);
                                int btnX = listX + 110;
                                int btnY = itemY + 2;
                                int btnW = 35;
                                int btnH = 12;

                                if (mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                                        Contract c = filteredContracts.get(i);
                                        playClickSound();
                                        // Send packet to accept contract
                                        com.quackers29.businesscraft.api.PlatformAccess.getNetworkMessages()
                                                        .sendToServer(
                                                                        new com.quackers29.businesscraft.network.packets.ui.AcceptContractPacket(
                                                                                        c.getId()));
                                        return true;
                                }
                        }
                }

                return super.mouseClicked(mouseX, mouseY, button);
        }

        private void playClickSound() {
                net.minecraft.client.resources.sounds.SimpleSoundInstance sound = net.minecraft.client.resources.sounds.SimpleSoundInstance
                                .forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F);
                this.minecraft.getSoundManager().play(sound);
        }

        private void drawTabs(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
                // Trading Tab
                boolean isTradingHovered = mouseX >= x && mouseX < x + TAB_WIDTH &&
                                mouseY >= y + TAB_Y_OFFSET && mouseY < y + TAB_Y_OFFSET + TAB_HEIGHT;
                InventoryRenderer.drawButton(guiGraphics, x, y + TAB_Y_OFFSET, TAB_WIDTH, TAB_HEIGHT,
                                "Trading", this.font, isTradingHovered || selectedTab == TAB_TRADING);

                // Courier Tab
                boolean isCourierHovered = mouseX >= x + TAB_WIDTH + 4 && mouseX < x + TAB_WIDTH * 2 + 4 &&
                                mouseY >= y + TAB_Y_OFFSET && mouseY < y + TAB_Y_OFFSET + TAB_HEIGHT;
                InventoryRenderer.drawButton(guiGraphics, x + TAB_WIDTH + 4, y + TAB_Y_OFFSET, TAB_WIDTH, TAB_HEIGHT,
                                "Courier", this.font, isCourierHovered || selectedTab == TAB_COURIER);
        }

        private void drawContractList(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
                List<Contract> allContracts = this.menu.getContracts();
                List<Contract> filteredContracts = allContracts.stream()
                                .filter(c -> (selectedTab == TAB_TRADING && c instanceof SellContract) ||
                                                (selectedTab == TAB_COURIER && c instanceof CourierContract))
                                .collect(Collectors.toList());

                int listX = x + 8;
                int listY = y + 20;
                int itemHeight = 35; // Increased height for more details

                // Draw header
                if (selectedTab == TAB_TRADING) {
                        guiGraphics.drawString(this.font, "Trading Contracts (View Only)", listX, listY - 10, 0x404040,
                                        false);
                } else {
                        guiGraphics.drawString(this.font, "Courier Contracts", listX, listY - 10, 0x404040, false);
                }

                // Draw items
                for (int i = 0; i < Math.min(filteredContracts.size(), 2); i++) { // Show fewer items due to increased
                                                                                  // height
                        Contract c = filteredContracts.get(i);
                        int itemY = listY + (i * itemHeight);

                        // Draw background for item
                        guiGraphics.fill(listX, itemY, listX + 150, itemY + itemHeight - 2, 0xFFC6C6C6);

                        if (c instanceof SellContract sellContract) {
                                drawSellContract(guiGraphics, sellContract, listX + 2, itemY + 2);
                        } else if (c instanceof CourierContract courierContract) {
                                drawCourierContract(guiGraphics, courierContract, listX + 2, itemY + 2, mouseX, mouseY);
                        }
                }

                if (filteredContracts.isEmpty()) {
                        guiGraphics.drawString(this.font, "No active contracts", listX, listY + 10, 0x808080, false);
                }
        }

        private void drawSellContract(GuiGraphics guiGraphics, SellContract c, int x, int y) {
                guiGraphics.drawString(this.font, "Selling: " + c.getAmount() + " " + c.getResourceType(), x, y,
                                0x000000, false);
                guiGraphics.drawString(this.font, "Bid: " + c.getCurrentBid(), x, y + 10, 0x000000, false);
                guiGraphics.drawString(this.font,
                                "Expires: " + (c.getExpiryTime() - System.currentTimeMillis()) / 1000 + "s", x, y + 20,
                                0x000000, false);
        }

        private void drawCourierContract(GuiGraphics guiGraphics, CourierContract c, int x, int y, int mouseX,
                        int mouseY) {
                guiGraphics.drawString(this.font, "Deliver: " + c.getAmount() + " " + c.getResourceType(), x, y,
                                0x000000, false);
                guiGraphics.drawString(this.font, "Reward: " + c.getReward(), x, y + 10, 0x000000, false);

                // Draw Accept button
                int btnX = x + 110;
                int btnY = y;
                int btnW = 35;
                int btnH = 12;
                boolean isHovered = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;

                InventoryRenderer.drawButton(guiGraphics, btnX, btnY, btnW, btnH, "Accept", this.font, isHovered);
        }

        private boolean isMouseOverBackButton(int mouseX, int mouseY) {
                int x = (this.width - this.imageWidth) / 2;
                int y = (this.height - this.imageHeight) / 2;

                return InventoryRenderer.isMouseOverElement(mouseX, mouseY, x, y,
                                BACK_BUTTON_X, BACK_BUTTON_Y, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT);
        }

        private void returnToMainUI() {
                this.onClose();
                ScreenNavigationHelper.returnToTownInterface(this.minecraft, this.minecraft.player,
                                this.menu.getTownBlockPos());
        }
}
