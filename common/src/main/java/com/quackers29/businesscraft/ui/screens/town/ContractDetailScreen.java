package com.quackers29.businesscraft.ui.screens.town;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.contract.Contract;
import com.quackers29.businesscraft.contract.CourierContract;
import com.quackers29.businesscraft.contract.SellContract;
import com.quackers29.businesscraft.contract.viewmodel.ContractDetailViewModel;
import com.quackers29.businesscraft.contract.viewmodel.ContractSummaryViewModel;
import com.quackers29.businesscraft.network.packets.ui.BidContractPacket;
import com.quackers29.businesscraft.network.packets.ui.RequestContractDetailPacket;
import com.quackers29.businesscraft.ui.managers.TownDataCacheManager;
import com.quackers29.businesscraft.ui.util.InventoryRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ContractDetailScreen extends Screen {
    // Legacy contract object (deprecated)
    private final Contract contract;
    
    // Phase 5: View-model based approach
    private final ContractSummaryViewModel summaryViewModel;
    private ContractDetailViewModel detailViewModel;
    private boolean detailRequested = false;
    private long lastCacheCheck = 0;
    private static final long CACHE_CHECK_INTERVAL = 100; // ms

    private final Screen parentScreen;
    private final int tabIndex;

    private static final int WINDOW_WIDTH = 280;
    private static final int WINDOW_HEIGHT = 300;

    private EditBox bidInput;
    private boolean showBidInput = false;

    private int scrollOffset = 0;
    private static final int MAX_VISIBLE_BIDS = 2;

    /**
     * Legacy constructor using Contract object.
     * @deprecated Use ContractSummaryViewModel constructor instead.
     */
    @Deprecated
    public ContractDetailScreen(Contract contract, Screen parentScreen, int tabIndex) {
        super(Component.literal("Contract Details"));
        this.contract = contract;
        this.summaryViewModel = null;
        this.detailViewModel = null;
        this.parentScreen = parentScreen;
        this.tabIndex = tabIndex;
    }

    /**
     * Phase 5: Constructor using ContractSummaryViewModel.
     * Full details will be requested from server on init.
     */
    public ContractDetailScreen(ContractSummaryViewModel viewModel, Screen parentScreen, int tabIndex) {
        super(Component.literal("Contract Details"));
        this.contract = null;
        this.summaryViewModel = viewModel;
        this.detailViewModel = null;
        this.parentScreen = parentScreen;
        this.tabIndex = tabIndex;
    }

    @Override
    protected void init() {
        int x = (width - WINDOW_WIDTH) / 2;
        int y = (height - WINDOW_HEIGHT) / 2;

        // Phase 5: Request detail from server if using view-model path
        if (summaryViewModel != null && !detailRequested) {
            TownDataCacheManager.clearContractDetailCache(); // Clear old cache
            PlatformAccess.getNetworkMessages().sendToServer(
                    new RequestContractDetailPacket(summaryViewModel.getContractId()));
            detailRequested = true;
        }

        // Close button
        this.addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(x + WINDOW_WIDTH - 50, y + WINDOW_HEIGHT - 25, 40, 20)
                .build());

        // For view-model path, buttons are added after detail is received
        if (summaryViewModel != null) {
            addViewModelButtons(x, y);
            return;
        }

        // Legacy path: use Contract object directly
        // Bid button conditional on tab
        int quantity = 0;
        if (contract instanceof CourierContract cc) {
            quantity = (int) cc.getQuantity();
        } else if (contract instanceof SellContract sc) {
            quantity = (int) sc.getQuantity();
        }

        if (contract != null && quantity > 0 && !contract.isExpired()) {
            // Check for CourierContract acceptance
            if (tabIndex == 1 && contract instanceof CourierContract cc && !cc.isAccepted()) {
                // Active tab: Accept Courier button
                boolean canAccept = false;
                if (cc.getSourceTownPos() != null) {
                    net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft
                            .getInstance().player;
                    if (player != null) {
                        double distSqr = player.blockPosition().distSqr(cc.getSourceTownPos());
                        double maxDist = cc.getSourceTownRadius(); // no buffer
                        canAccept = distSqr <= maxDist * maxDist;
                    }
                }

                Button acceptBtn = Button.builder(Component.literal("Accept Courier"), b -> {
                    PlatformAccess.getNetworkMessages().sendToServer(new BidContractPacket(contract.getId(), 0f));
                    onClose();
                })
                        .bounds(x + 10, y + WINDOW_HEIGHT - 25, 90, 20)
                        .build();

                if (!canAccept) {
                    acceptBtn.active = false;
                    acceptBtn.setMessage(Component.literal("Too Far"));
                }
                this.addRenderableWidget(acceptBtn);
            }
            // Check for SellContract courier acceptance
            else if (tabIndex == 1 && contract instanceof SellContract sc && sc.isAuctionClosed()
                    && !sc.isCourierAssigned()) {
                // Active tab: Accept Courier Job for SellContract
                boolean canAccept = false;

                // Check if we are at the seller's town
                if (parentScreen instanceof ContractBoardScreen cbs) {
                    net.minecraft.core.BlockPos pos = cbs.getMenu().getTownBlockPos();
                    if (pos != null && net.minecraft.client.Minecraft.getInstance().level != null) {
                        net.minecraft.world.level.block.entity.BlockEntity be = net.minecraft.client.Minecraft
                                .getInstance().level.getBlockEntity(pos);
                        if (be instanceof com.quackers29.businesscraft.block.entity.TownInterfaceEntity tie) {
                            UUID currentTownId = tie.getTownId();
                            if (currentTownId != null && currentTownId.equals(sc.getIssuerTownId())) {
                                canAccept = true;
                            }
                        }
                    }
                }

                Button acceptBtn = Button.builder(Component.literal("Accept Job"), b -> {
                    PlatformAccess.getNetworkMessages().sendToServer(new BidContractPacket(contract.getId(), 0f));
                    onClose();
                })
                        .bounds(x + 10, y + WINDOW_HEIGHT - 25, 90, 20)
                        .build();

                if (!canAccept) {
                    acceptBtn.active = false;
                    acceptBtn.setMessage(Component.literal("Wrong Town"));
                }

                this.addRenderableWidget(acceptBtn);
            }
        }
    }

    /**
     * Phase 5: Add buttons based on view-model data.
     * Accept button only available if player is at the issuer (selling) town.
     */
    private void addViewModelButtons(int x, int y) {
        // Players can't bid - only towns can bid on auctions
        // Accept button is only on Active tab for courier jobs
        
        boolean canAcceptCourier = summaryViewModel.canAcceptCourier();
        UUID contractId = summaryViewModel.getContractId();
        UUID issuerTownId = summaryViewModel.getIssuerTownId();

        // Override with detail data if available
        if (detailViewModel != null) {
            canAcceptCourier = detailViewModel.canAcceptCourier();
        }

        // Active tab: Show Accept button for courier jobs
        if (tabIndex == 1 && canAcceptCourier) {
            // Check if player is at the issuer (selling) town
            boolean atIssuerTown = false;
            
            if (parentScreen instanceof ContractBoardScreen cbs) {
                net.minecraft.core.BlockPos pos = cbs.getMenu().getTownBlockPos();
                if (pos != null && net.minecraft.client.Minecraft.getInstance().level != null) {
                    net.minecraft.world.level.block.entity.BlockEntity be = 
                            net.minecraft.client.Minecraft.getInstance().level.getBlockEntity(pos);
                    if (be instanceof com.quackers29.businesscraft.block.entity.TownInterfaceEntity tie) {
                        UUID currentTownId = tie.getTownId();
                        if (currentTownId != null && currentTownId.equals(issuerTownId)) {
                            atIssuerTown = true;
                        }
                    }
                }
            }

            Button acceptBtn = Button.builder(Component.literal("Accept Job"), b -> {
                PlatformAccess.getNetworkMessages().sendToServer(new BidContractPacket(contractId, 0f));
                onClose();
            })
                    .bounds(x + 10, y + WINDOW_HEIGHT - 25, 90, 20)
                    .build();

            if (!atIssuerTown) {
                acceptBtn.active = false;
                acceptBtn.setMessage(Component.literal("Wrong Town"));
            }

            this.addRenderableWidget(acceptBtn);
        }
    }

    /**
     * Phase 5: Check for detail view-model updates from server.
     */
    private void checkDetailCache() {
        if (summaryViewModel == null) return;
        
        long now = System.currentTimeMillis();
        if (now - lastCacheCheck < CACHE_CHECK_INTERVAL) return;
        lastCacheCheck = now;

        ContractDetailViewModel cached = TownDataCacheManager.getCachedContractDetail();
        if (cached != null && cached.getContractId().equals(summaryViewModel.getContractId())) {
            if (detailViewModel == null || !detailViewModel.equals(cached)) {
                detailViewModel = cached;
                // Reinitialize buttons if needed
                rebuildButtons();
            }
        }
    }

    /**
     * Rebuild buttons after receiving detail view-model.
     */
    private void rebuildButtons() {
        // Clear and re-add widgets
        this.clearWidgets();
        this.init();
    }

    private void sendBid() {
        try {
            float amount = Float.parseFloat(bidInput.getValue());
            UUID id = (contract != null) ? contract.getId() : 
                       (summaryViewModel != null) ? summaryViewModel.getContractId() : null;
            if (id != null) {
                PlatformAccess.getNetworkMessages().sendToServer(new BidContractPacket(id, amount));
                onClose(); // Close after bidding
            }
        } catch (NumberFormatException ignored) {
            // Ignore invalid input
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        // Phase 5: Check for detail update
        checkDetailCache();

        renderBackground(g);

        int x = (width - WINDOW_WIDTH) / 2;
        int y = (height - WINDOW_HEIGHT) / 2;

        // Background
        g.fill(x, y, x + WINDOW_WIDTH, y + WINDOW_HEIGHT, InventoryRenderer.BACKGROUND_COLOR);
        InventoryRenderer.drawBorder(g, x, y, WINDOW_WIDTH, WINDOW_HEIGHT, InventoryRenderer.BORDER_COLOR, 2);

        // Title
        g.drawCenteredString(font, "Contract Details", width / 2, y + 10, 0xFFFCB821);

        // Phase 5: Use view-model path if available
        if (summaryViewModel != null) {
            renderViewModelDetails(g, x, y);
            super.render(g, mx, my, pt);
            return;
        }

        // Legacy path: use Contract object directly
        if (contract == null) {
            super.render(g, mx, my, pt);
            return;
        }

        // Details
        int textY = y + 30;
        int labelColor = 0xFFAAAAAA;
        int valueColor = 0xFFFFFFFF;
        int headerColor = 0xFFFCB821;

        // Type
        g.drawString(font, "Type:", x + 10, textY, labelColor);
        g.drawString(font, contract instanceof SellContract ? "Sell" : "Courier", x + 80, textY, valueColor);
        textY += 15;

        // Resource & Quantity
        String resourceName = "";
        int quantity = 0;
        if (contract instanceof SellContract sc) {
            resourceName = sc.getResourceId();
            quantity = (int) sc.getQuantity();
        } else if (contract instanceof CourierContract cc) {
            resourceName = cc.getResourceId();
            quantity = (int) cc.getQuantity();
        }

        g.drawString(font, "Resource:", x + 10, textY, labelColor);
        g.drawString(font, resourceName + " x" + quantity, x + 80, textY, valueColor);
        textY += 15;

        // Seller/Issuer
        g.drawString(font, "From:", x + 10, textY, labelColor);
        String sellerName = getTownName(contract.getIssuerTownId(), contract.getIssuerTownName());
        g.drawString(font, truncate(sellerName, 20), x + 80, textY, valueColor);
        textY += 15;

        // Destination & Reward (Courier Only)
        if (contract instanceof CourierContract cc) {
            g.drawString(font, "To:", x + 10, textY, labelColor);
            String destName = getTownName(cc.getDestinationTownId(), cc.getDestinationTownName());
            g.drawString(font, truncate(destName, 20), x + 80, textY, valueColor);
            textY += 15;

            g.drawString(font, "Reward:", x + 10, textY, labelColor);
            g.drawString(font, String.format("%.1f ◎", cc.getReward()), x + 80, textY, 0xFF55FF55);
            textY += 15;

            if (cc.isAccepted()) {
                g.drawString(font, "Courier:", x + 10, textY, labelColor);
                // We don't have courier name cached, so use ID for now or "Player"
                g.drawString(font, "Assigned", x + 80, textY, 0xFF55FF55);
                textY += 15;

                // Delivery Progress
                g.drawString(font, "Delivered:", x + 10, textY, labelColor);
                String progress = cc.getDeliveredAmount() + " / " + cc.getQuantity();
                int progressColor = cc.isDelivered() ? 0xFF55FF55 : 0xFFFFAA00;
                g.drawString(font, progress, x + 80, textY, progressColor);
                textY += 15;
            }
        }

        // Created timestamp
        g.drawString(font, "Created:", x + 10, textY, labelColor);
        g.drawString(font, formatTime(contract.getCreationTime()), x + 80, textY, valueColor);
        textY += 15;

        // Expiry timestamp
        g.drawString(font, "Expires:", x + 10, textY, labelColor);
        g.drawString(font, formatTime(contract.getExpiryTime()), x + 80, textY, valueColor);
        textY += 15;

        // Time Remaining
        g.drawString(font, "Time Left:", x + 10, textY, labelColor);
        long currentTime = System.currentTimeMillis();
        long timeLeft = contract.getExpiryTime() - currentTime;
        String time = timeLeft > 0 ? formatDuration(timeLeft) : "Expired";
        int timeColor = timeLeft > 0 ? valueColor : 0xFFFF5555;
        g.drawString(font, time, x + 80, textY, timeColor);
        textY += 18;

        // Bids Section
        g.drawString(font, "Bids:", x + 10, textY, headerColor);
        textY += 12;

        Map<UUID, Float> bids = contract.getBids();
        if (bids.isEmpty()) {
            g.drawString(font, "No bids yet", x + 20, textY, 0xFF888888);
            textY += 12;
        } else {
            // Sort bids by amount (highest first)
            List<Map.Entry<UUID, Float>> sortedBids = bids.entrySet().stream()
                    .sorted(Map.Entry.<UUID, Float>comparingByValue().reversed())
                    .collect(Collectors.toList());

            UUID highestBidder = contract.getHighestBidder();

            int count = 0;
            for (Map.Entry<UUID, Float> entry : sortedBids) {
                if (count >= MAX_VISIBLE_BIDS && count < sortedBids.size()) {
                    g.drawString(font, "... +" + (sortedBids.size() - MAX_VISIBLE_BIDS) + " more",
                            x + 20, textY, 0xFF888888);
                    textY += 12;
                    break;
                }

                String bidderName = contract.getBidderName(entry.getKey());
                String amount = String.format("%.1f", entry.getValue());
                boolean isHighest = entry.getKey().equals(highestBidder);

                String bidText = truncate(bidderName, 15) + ": " + amount + " ◎";
                if (isHighest) {
                    bidText += " ⭐";
                    g.drawString(font, bidText, x + 20, textY, 0xFF55FF55);
                } else {
                    g.drawString(font, bidText, x + 20, textY, valueColor);
                }

                textY += 12;
                count++;
            }
        }

        textY += 6;

        // Winner Section (for completed SellContracts)
        if (contract instanceof SellContract sc && sc.getWinningTownId() != null) {
            // Draw separator line
            g.hLine(x + 10, x + WINDOW_WIDTH - 10, textY, 0xFF666666);
            textY += 8;

            g.drawString(font, "Winner:", x + 10, textY, headerColor);
            textY += 12;

            String winnerName = getTownName(sc.getWinningTownId(), sc.getWinningTownName());
            g.drawString(font, "Buyer:", x + 20, textY, labelColor);
            g.drawString(font, truncate(winnerName, 18), x + 90, textY, 0xFF55FF55);
            textY += 12;

            g.drawString(font, "Price:", x + 20, textY, labelColor);
            g.drawString(font, String.format("%.1f ◎", sc.getAcceptedBid()), x + 90, textY, 0xFF55FF55);
            textY += 15;

            // Courier Info for SellContract
            g.drawString(font, "Courier Info:", x + 10, textY, headerColor);
            textY += 12;

            g.drawString(font, "Reward:", x + 20, textY, labelColor);
            g.drawString(font, String.format("%.1f ◎", sc.getCourierReward()), x + 90, textY, 0xFF55FF55);
            textY += 12;

            g.drawString(font, "Status:", x + 20, textY, labelColor);
            if (sc.isCourierAssigned()) {
                String courierName = sc.isSnailMail() ? "Snail Mail" : "Assigned";
                g.drawString(font, courierName, x + 90, textY, 0xFF55FF55);
                textY += 12;

                g.drawString(font, "Progress:", x + 20, textY, labelColor);
                String progress = sc.getDeliveredAmount() + " / " + sc.getQuantity();
                int progressColor = sc.isDeliveryComplete() ? 0xFF55FF55 : 0xFFFFAA00;
                g.drawString(font, progress, x + 90, textY, progressColor);
            } else {
                g.drawString(font, "Waiting for Courier", x + 90, textY, 0xFFFFAA00);
            }
            textY += 12;
            textY += 12;

            g.drawString(font, "Status:", x + 20, textY, labelColor);
            String status = sc.isDelivered() ? "Delivered ✓" : "Pending";
            int statusColor = sc.isDelivered() ? 0xFF55FF55 : 0xFFFFAA00;
            g.drawString(font, status, x + 90, textY, statusColor);
        }

        super.render(g, mx, my, pt);
    }

    /**
     * Phase 5: Render contract details using view-model data.
     * Visual layout matches legacy path exactly - uses server-calculated display strings.
     */
    private void renderViewModelDetails(GuiGraphics g, int x, int y) {
        int textY = y + 30;
        int labelColor = 0xFFAAAAAA;
        int valueColor = 0xFFFFFFFF;
        int headerColor = 0xFFFCB821;

        // Use detail view-model if available, otherwise fall back to summary
        boolean hasDetail = (detailViewModel != null);
        
        // Type
        String contractType = hasDetail ? detailViewModel.getContractType() : "sell";
        g.drawString(font, "Type:", x + 10, textY, labelColor);
        g.drawString(font, contractType.substring(0, 1).toUpperCase() + contractType.substring(1), x + 80, textY, valueColor);
        textY += 15;

        // Resource & Quantity
        String resourceId = hasDetail ? detailViewModel.getResourceId() : summaryViewModel.getResourceId();
        long quantity = hasDetail ? detailViewModel.getQuantity() : summaryViewModel.getQuantity();
        g.drawString(font, "Resource:", x + 10, textY, labelColor);
        g.drawString(font, resourceId + " x" + quantity, x + 80, textY, valueColor);
        textY += 15;

        // Seller/Issuer
        String issuerName = hasDetail ? detailViewModel.getIssuerTownName() : summaryViewModel.getIssuerTownName();
        g.drawString(font, "From:", x + 10, textY, labelColor);
        g.drawString(font, truncate(issuerName, 20), x + 80, textY, valueColor);
        textY += 15;

        // Destination & Reward (for courier contracts - from detail only)
        if (hasDetail && detailViewModel.getDestinationTownName() != null) {
            g.drawString(font, "To:", x + 10, textY, labelColor);
            g.drawString(font, truncate(detailViewModel.getDestinationTownName(), 20), x + 80, textY, valueColor);
            textY += 15;

            if (detailViewModel.getCourierRewardDisplay() != null) {
                g.drawString(font, "Reward:", x + 10, textY, labelColor);
                g.drawString(font, detailViewModel.getCourierRewardDisplay(), x + 80, textY, 0xFF55FF55);
                textY += 15;
            }
        }

        // Created timestamp
        if (hasDetail) {
            g.drawString(font, "Created:", x + 10, textY, labelColor);
            g.drawString(font, detailViewModel.getCreatedDateDisplay(), x + 80, textY, valueColor);
            textY += 15;

            // Expiry timestamp
            g.drawString(font, "Expires:", x + 10, textY, labelColor);
            g.drawString(font, detailViewModel.getExpiresDateDisplay(), x + 80, textY, valueColor);
            textY += 15;
        }

        // Time Remaining
        String timeDisplay = hasDetail ? detailViewModel.getTimeRemainingDisplay() : summaryViewModel.getTimeRemainingDisplay();
        g.drawString(font, "Time Left:", x + 10, textY, labelColor);
        int timeColor = timeDisplay.equals("Expired") ? 0xFFFF5555 : valueColor;
        g.drawString(font, timeDisplay, x + 80, textY, timeColor);
        textY += 18;

        // Bids Section
        g.drawString(font, "Bids:", x + 10, textY, headerColor);
        textY += 12;

        if (hasDetail) {
            List<ContractDetailViewModel.BidDisplayInfo> bids = detailViewModel.getBids();
            if (bids.isEmpty()) {
                g.drawString(font, "No bids yet", x + 20, textY, 0xFF888888);
                textY += 12;
            } else {
                int count = 0;
                for (ContractDetailViewModel.BidDisplayInfo bid : bids) {
                    if (count >= MAX_VISIBLE_BIDS && count < bids.size()) {
                        g.drawString(font, "... +" + (bids.size() - MAX_VISIBLE_BIDS) + " more",
                                x + 20, textY, 0xFF888888);
                        textY += 12;
                        break;
                    }

                    String bidText = truncate(bid.bidderName(), 15) + ": " + bid.amountDisplay();
                    if (bid.isHighest()) {
                        bidText += " ⭐";
                        g.drawString(font, bidText, x + 20, textY, 0xFF55FF55);
                    } else {
                        g.drawString(font, bidText, x + 20, textY, valueColor);
                    }

                    textY += 12;
                    count++;
                }
            }
        } else {
            // Summary only - show highest bid info
            String highestBidDisplay = summaryViewModel.getHighestBidDisplay();
            if (highestBidDisplay.equals("No bids")) {
                g.drawString(font, "No bids yet", x + 20, textY, 0xFF888888);
            } else {
                g.drawString(font, "Highest: " + highestBidDisplay + " ⭐", x + 20, textY, 0xFF55FF55);
            }
            textY += 12;
        }

        textY += 6;

        // Winner Section (for completed contracts)
        if (hasDetail && detailViewModel.isAuctionClosed() && detailViewModel.getWinningBidderName() != null) {
            // Draw separator line
            g.hLine(x + 10, x + WINDOW_WIDTH - 10, textY, 0xFF666666);
            textY += 8;

            g.drawString(font, "Winner:", x + 10, textY, headerColor);
            textY += 12;

            g.drawString(font, "Buyer:", x + 20, textY, labelColor);
            g.drawString(font, truncate(detailViewModel.getWinningBidderName(), 18), x + 90, textY, 0xFF55FF55);
            textY += 12;

            g.drawString(font, "Price:", x + 20, textY, labelColor);
            g.drawString(font, detailViewModel.getAcceptedBidDisplay(), x + 90, textY, 0xFF55FF55);
            textY += 15;

            // Courier Info
            g.drawString(font, "Courier Info:", x + 10, textY, headerColor);
            textY += 12;

            if (detailViewModel.getCourierRewardDisplay() != null) {
                g.drawString(font, "Reward:", x + 20, textY, labelColor);
                g.drawString(font, detailViewModel.getCourierRewardDisplay(), x + 90, textY, 0xFF55FF55);
                textY += 12;
            }

            g.drawString(font, "Status:", x + 20, textY, labelColor);
            if (detailViewModel.getCourierName() != null) {
                g.drawString(font, detailViewModel.getCourierName(), x + 90, textY, 0xFF55FF55);
                textY += 12;

                if (detailViewModel.getDeliveryProgressDisplay() != null) {
                    g.drawString(font, "Progress:", x + 20, textY, labelColor);
                    String progress = detailViewModel.getDeliveryProgressDisplay();
                    int progressColor = progress.contains("/") && progress.split("/")[0].trim().equals(progress.split("/")[1].trim()) 
                            ? 0xFF55FF55 : 0xFFFFAA00;
                    g.drawString(font, progress, x + 90, textY, progressColor);
                }
            } else {
                g.drawString(font, "Waiting for Courier", x + 90, textY, 0xFFFFAA00);
            }
            textY += 12;

            // Final delivery status
            textY += 12;
            g.drawString(font, "Status:", x + 20, textY, labelColor);
            String statusDisplay = detailViewModel.getStatusDisplay();
            String status = statusDisplay.contains("Delivered") ? "Delivered ✓" : "Pending";
            int statusColor = statusDisplay.contains("Delivered") ? 0xFF55FF55 : 0xFFFFAA00;
            g.drawString(font, status, x + 90, textY, statusColor);
        }
    }

    /**
     * Get town name from UUID, with fallback to provided name or shortened UUID
     * Note: This is a client-side screen, so we use cached names from contracts
     */
    private String getTownName(UUID townId, String fallbackName) {
        if (townId == null) {
            return fallbackName != null ? fallbackName : "Unknown";
        }

        // Use fallback name if provided (from contract's cached name)
        if (fallbackName != null && !fallbackName.isEmpty() && !fallbackName.equals("Unknown Town")) {
            return fallbackName;
        }

        // Fallback to shortened UUID for bidders (no cached names available)
        return "Town-" + townId.toString().substring(0, 8);
    }

    /**
     * Format timestamp to readable date/time
     */
    private String formatTime(long timestamp) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("MM/dd HH:mm");
        return timeFormat.format(new Date(timestamp));
    }

    /**
     * Format duration in a readable way
     */
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        }
    }

    private String truncate(String text, int maxChars) {
        if (text == null || text.length() <= maxChars)
            return text;
        return text.substring(0, maxChars - 2) + "..";
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Handle scrolling for long bid lists if needed in future
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}
