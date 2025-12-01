package com.quackers29.businesscraft.ui.screens.town;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.contract.Contract;
import com.quackers29.businesscraft.contract.CourierContract;
import com.quackers29.businesscraft.contract.SellContract;
import com.quackers29.businesscraft.network.packets.ui.BidContractPacket;
import com.quackers29.businesscraft.ui.util.InventoryRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.UUID;

public class ContractDetailScreen extends Screen {
    private final Contract contract;
    private final Screen parentScreen;

    private static final int WINDOW_WIDTH = 220;
    private static final int WINDOW_HEIGHT = 180;

    private EditBox bidInput;
    private boolean showBidInput = false;

    public ContractDetailScreen(Contract contract, Screen parentScreen) {
        super(Component.literal("Contract Details"));
        this.contract = contract;
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        int x = (width - WINDOW_WIDTH) / 2;
        int y = (height - WINDOW_HEIGHT) / 2;

        // Close button
        this.addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(x + WINDOW_WIDTH - 50, y + WINDOW_HEIGHT - 25, 40, 20)
                .build());

        // Bid button (only for CourierContract that isn't expired/completed)
        if (contract instanceof CourierContract && !contract.isExpired() && !contract.isCompleted()) {
            this.addRenderableWidget(Button.builder(Component.literal("Bid"), b -> {
                if (showBidInput) {
                    sendBid();
                } else {
                    showBidInput = true;
                    // Re-init to show input box
                    this.rebuildWidgets();
                }
            })
                    .bounds(x + 10, y + WINDOW_HEIGHT - 25, 40, 20)
                    .build());

            if (showBidInput) {
                bidInput = new EditBox(this.font, x + 60, y + WINDOW_HEIGHT - 25, 60, 20,
                        Component.literal("Bid Amount"));
                bidInput.setValue("10.0");
                this.addRenderableWidget(bidInput);
            }
        }
    }

    private void sendBid() {
        try {
            float amount = Float.parseFloat(bidInput.getValue());
            PlatformAccess.getNetworkMessages().sendToServer(new BidContractPacket(contract.getId(), amount));
            onClose(); // Close after bidding
        } catch (NumberFormatException ignored) {
            // Ignore invalid input
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);

        int x = (width - WINDOW_WIDTH) / 2;
        int y = (height - WINDOW_HEIGHT) / 2;

        // Background
        g.fill(x, y, x + WINDOW_WIDTH, y + WINDOW_HEIGHT, InventoryRenderer.BACKGROUND_COLOR);
        InventoryRenderer.drawBorder(g, x, y, WINDOW_WIDTH, WINDOW_HEIGHT, InventoryRenderer.BORDER_COLOR, 2);

        // Title
        g.drawCenteredString(font, "Contract Details", width / 2, y + 10, 0xFFFCB821);

        // Details
        int textY = y + 30;
        int labelColor = 0xFFAAAAAA;
        int valueColor = 0xFFFFFFFF;

        // Type
        g.drawString(font, "Type:", x + 10, textY, labelColor);
        g.drawString(font, contract instanceof SellContract ? "Sell" : "Courier", x + 80, textY, valueColor);
        textY += 15;

        // Resource & Quantity
        String resourceName = "";
        int quantity = 0;
        if (contract instanceof SellContract sc) {
            resourceName = sc.getResourceId();
            quantity = sc.getQuantity();
        } else if (contract instanceof CourierContract cc) {
            resourceName = cc.getResourceId();
            quantity = cc.getQuantity();
        }

        g.drawString(font, "Resource:", x + 10, textY, labelColor);
        g.drawString(font, resourceName + " x" + quantity, x + 80, textY, valueColor);
        textY += 15;

        // Issuer
        g.drawString(font, "Town ID:", x + 10, textY, labelColor);
        String townId = contract.getIssuerTownId().toString();
        g.drawString(font, townId.substring(0, 8) + "...", x + 80, textY, valueColor);
        textY += 15;

        // Time Remaining
        g.drawString(font, "Time Left:", x + 10, textY, labelColor);
        long currentTime = System.currentTimeMillis();
        long timeLeft = contract.getExpiryTime() - currentTime;
        String time = timeLeft > 0 ? (timeLeft / 1000) + "s" : "Expired";
        g.drawString(font, time, x + 80, textY, valueColor);
        textY += 20;

        // Bids Section
        g.drawString(font, "Bids:", x + 10, textY, 0xFFFCB821);
        textY += 15;

        Map<UUID, Float> bids = contract.getBids();
        if (bids.isEmpty()) {
            g.drawString(font, "No bids yet", x + 20, textY, 0xFF888888);
        } else {
            int count = 0;
            for (Map.Entry<UUID, Float> entry : bids.entrySet()) {
                if (count >= 3) {
                    g.drawString(font, "... and " + (bids.size() - 3) + " more", x + 20, textY, 0xFF888888);
                    break;
                }
                String bidder = entry.getKey().toString().substring(0, 8);
                String amount = String.format("%.2f", entry.getValue());
                g.drawString(font, bidder + ": " + amount, x + 20, textY, valueColor);
                textY += 12;
                count++;
            }
        }

        super.render(g, mx, my, pt);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
