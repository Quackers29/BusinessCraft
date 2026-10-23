package com.quackers29.businesscraft.client.showcase.testbed;

import com.quackers29.businesscraft.client.showcase.Showcase;
import com.quackers29.businesscraft.client.showcase.ShowcaseButtonIds;
import com.quackers29.businesscraft.client.showcase.ShowcaseNavigator;
import com.quackers29.businesscraft.contract.ContractBoard;
import com.quackers29.businesscraft.contract.SellContract;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.ui.screens.town.ContractBoardScreen;
import com.quackers29.businesscraft.ui.screens.town.ContractDetailScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Creates a sell contract from town A, bids from town B via {@link ContractBoard#addBid} (same server logic as
 * {@code BidContractPacket}), walks the contract UI, then time-skips auction and snail-mail delivery.
 */
public final class TradeContractCheck implements ShowcaseTestbedCheck {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/Showcase");
    private static final String RESOURCE = "emerald";
    private static final long AUCTION_MS = 60_000L;
    private static final float BID_AMOUNT = 12f;

    private enum Phase {
        CREATE_ON_SERVER,
        OPEN_LIST,
        PLACE_BID,
        OPEN_DETAIL,
        ADVANCE_DELIVERY,
        DONE
    }

    private Phase phase = Phase.CREATE_ON_SERVER;
    private int ticks;
    private boolean pass;
    private UUID contractId;
    private long buyerEmeraldsBefore;
    private long buyerEmeraldsAfter;
    private volatile boolean auctionClosed;
    private volatile boolean contractCompleted;
    private String error;

    @Override
    public String name() {
        return "trade-contract";
    }

    @Override
    public void begin(Minecraft minecraft, ShowcaseTestbedContext context) {
        phase = Phase.CREATE_ON_SERVER;
        ticks = 0;
        pass = false;
        contractId = null;
        error = null;
        auctionClosed = false;
        contractCompleted = false;
        Showcase.setActiveTownBlockPos(context.townBPos());
    }

    @Override
    public boolean tick(Minecraft minecraft, ShowcaseTestbedContext context) {
        ticks++;
        BlockPos active = context.townBPos();
        return switch (phase) {
            case CREATE_ON_SERVER -> {
                if (ticks == 1 && minecraft.getSingleplayerServer() != null) {
                    minecraft.getSingleplayerServer().execute(() -> createContract(minecraft, context));
                } else if (ticks >= 5) {
                    if (contractId == null) {
                        fail("contract not created on server");
                        yield true;
                    }
                    phase = Phase.OPEN_LIST;
                    ticks = 0;
                }
                yield false;
            }
            case OPEN_LIST -> {
                if (ticks == 1) {
                    ShowcaseNavigator.runStep(minecraft, active, "open-town");
                } else if (ticks == 8) {
                    ShowcaseNavigator.runStep(minecraft, active, "set-tab:resources");
                } else if (ticks == 12) {
                    ShowcaseNavigator.runStep(minecraft, active, "press-button:id:" + ShowcaseButtonIds.CONTRACTS);
                } else if (ticks >= 60) {
                    boolean listed = minecraft.screen instanceof ContractBoardScreen board
                            && board.showcaseHasAuctionContracts();
                    grab(minecraft, "trade_contract_list");
                    if (!listed) {
                        fail("contract not listed on auction tab");
                        yield true;
                    }
                    phase = Phase.PLACE_BID;
                    ticks = 0;
                }
                yield false;
            }
            case PLACE_BID -> {
                if (ticks == 1 && minecraft.getSingleplayerServer() != null) {
                    minecraft.getSingleplayerServer().execute(() -> placeBid(minecraft, context));
                } else if (ticks >= 10) {
                    if (contractId == null || !auctionClosed) {
                        fail("bid did not close auction");
                        yield true;
                    }
                    phase = Phase.OPEN_DETAIL;
                    ticks = 0;
                }
                yield false;
            }
            case OPEN_DETAIL -> {
                if (ticks == 1) {
                    ShowcaseNavigator.runStep(minecraft, active, "press-button:id:" + ShowcaseButtonIds.CONTRACT_VIEW);
                } else if (ticks >= 40) {
                    boolean detail = minecraft.screen instanceof ContractDetailScreen;
                    grab(minecraft, "trade_contract_detail");
                    if (!detail) {
                        fail("contract detail screen did not open");
                        yield true;
                    }
                    phase = Phase.ADVANCE_DELIVERY;
                    ticks = 0;
                }
                yield false;
            }
            case ADVANCE_DELIVERY -> {
                if (ticks == 1 && minecraft.getSingleplayerServer() != null) {
                    minecraft.getSingleplayerServer().execute(() -> fastForwardDelivery(minecraft, context));
                } else if (ticks >= 15) {
                    pass = auctionClosed && contractCompleted;
                    if (!pass && error == null) {
                        error = "auction did not close or contract did not complete";
                    }
                    grab(minecraft, "trade_contract_complete");
                    phase = Phase.DONE;
                    yield true;
                }
                yield false;
            }
            case DONE -> true;
        };
    }

    private void createContract(Minecraft minecraft, ShowcaseTestbedContext context) {
        ServerLevel level = minecraft.getSingleplayerServer().overworld();
        TownManager manager = TownManager.get(level);
        Town seller = manager.getTown(context.townAId());
        if (seller == null) {
            error = "town A missing";
            return;
        }
        seller.addResource(Items.EMERALD, 10);
        ContractBoard board = ContractBoard.get(level);
        SellContract contract = new SellContract(
                seller.getId(),
                seller.getName(),
                AUCTION_MS,
                RESOURCE,
                1,
                1.0f);
        board.addContract(contract);
        seller.addResource(Items.EMERALD, -1);
        seller.addEscrowResource(Items.EMERALD, 1);
        contractId = contract.getId();
        context.put("tradeContractId", contractId.toString());
        LOGGER.info("Testbed created sell contract {} from town {}", contractId, seller.getName());
    }

    private void placeBid(Minecraft minecraft, ShowcaseTestbedContext context) {
        ServerLevel level = minecraft.getSingleplayerServer().overworld();
        Town buyer = TownManager.get(level).getTown(context.townBId());
        if (buyer == null || contractId == null) {
            error = "town B or contract missing for bid";
            return;
        }
        buyer.addResource(Items.EMERALD, 500);
        buyerEmeraldsBefore = buyer.getResourceCount(Items.EMERALD);
        ContractBoard board = ContractBoard.get(level);
        board.addBid(contractId, buyer.getId(), BID_AMOUNT, level);
        SellContract sc = (SellContract) board.getContract(contractId);
        if (sc != null) {
            sc.expireNow();
            board.tick(level);
            auctionClosed = sc.isAuctionClosed() && buyer.getId().equals(sc.getWinningTownId());
            buyerEmeraldsAfter = buyer.getResourceCount(Items.EMERALD);
        }
        LOGGER.info("Testbed bid placed; auctionClosed={} buyerEmeralds {} -> {}",
                auctionClosed, buyerEmeraldsBefore, buyerEmeraldsAfter);
    }

    private void fastForwardDelivery(Minecraft minecraft, ShowcaseTestbedContext context) {
        ServerLevel level = minecraft.getSingleplayerServer().overworld();
        ContractBoard board = ContractBoard.get(level);
        SellContract sc = (SellContract) board.getContract(contractId);
        if (sc == null) {
            error = "contract missing after bid";
            return;
        }
        Town buyer = TownManager.get(level).getTown(context.townBId());
        if (buyer != null) {
            buyerEmeraldsBefore = buyerEmeraldsBefore > 0 ? buyerEmeraldsBefore : buyer.getResourceCount(Items.EMERALD);
        }
        sc.expireNow();
        board.tick(level);
        sc.expireNow();
        board.tick(level);
        sc.expireNow();
        board.tick(level);
        contractCompleted = sc.isCompleted();
        if (buyer != null) {
            buyerEmeraldsAfter = buyer.getResourceCount(Items.EMERALD);
        }
        LOGGER.info("Testbed delivery fast-forward completed={} delivered={}", contractCompleted, sc.isDelivered());
    }

    private void fail(String message) {
        error = message;
        pass = false;
        phase = Phase.DONE;
    }

    private static void grab(Minecraft minecraft, String step) {
        String file = "showcase_testbed_" + step + ".png";
        Screenshot.grab(minecraft.gameDirectory, file, minecraft.getMainRenderTarget(), message ->
                LOGGER.info("Testbed shot {}: {}", file, message.getString()));
    }

    @Override
    public boolean passed() {
        return pass;
    }

    @Override
    public Map<String, Object> details() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("contractId", contractId != null ? contractId.toString() : null);
        d.put("auctionClosed", auctionClosed);
        d.put("contractCompleted", contractCompleted);
        d.put("buyerEmeraldsBefore", buyerEmeraldsBefore);
        d.put("buyerEmeraldsAfter", buyerEmeraldsAfter);
        if (error != null) {
            d.put("error", error);
        }
        return d;
    }

    @Override
    public String failureMessage() {
        return error;
    }
}
