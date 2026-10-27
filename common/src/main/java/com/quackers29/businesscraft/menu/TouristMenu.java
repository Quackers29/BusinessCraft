package com.quackers29.businesscraft.menu;

import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.entity.TouristTimer;
import com.quackers29.businesscraft.entity.TouristTradeCatalog;
import com.quackers29.businesscraft.network.PlayerItemExchange;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.quackers29.businesscraft.entity.TouristSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Tourist conversation. There are no input slots: choosing a row takes the
 * cost from the player's inventory and gives the same result the old merchant
 * offer did. Distance, rank, and which deals were used travel in data slots.
 * The journey clock is snapshotted when the screen opens. Those slots are not
 * rewritten every second: the client advances the label from the snapshot, and
 * the remaining time is sent again only when the tourist starts or stops moving.
 */
public class TouristMenu extends AbstractContainerMenu {
    public static final int DATA_DISTANCE_LO = 0;
    public static final int DATA_DISTANCE_HI = 1;
    public static final int DATA_LEVEL = 2;
    public static final int DATA_USES = 3;
    public static final int DATA_JOURNEY_SEC = 4;
    public static final int DATA_REMAINING_SEC = 5;
    public static final int DATA_FLAGS = 6;
    public static final int DATA_COUNT = 7;
    private static final int FLAG_ANY_TOWN = 1;
    private static final int FLAG_STATIONARY = 2;
    private static final double MAX_DISTANCE_SQR = 64.0 * 64.0;

    private final ContainerData data;
    private final TouristScreenData opened;
    @Nullable
    private final TouristEntity tourist;
    /** Server only. Stops the journey slots from changing every second. */
    private boolean timersSeeded;
    private boolean timerStationary;

    public TouristMenu(int containerId, Inventory playerInventory, TouristEntity tourist) {
        this(containerId, playerInventory, tourist, TouristScreenData.capture(tourist), new SimpleContainerData(DATA_COUNT));
        pushFromTourist();
    }

    public TouristMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, null, TouristScreenData.read(extraData), new SimpleContainerData(DATA_COUNT));
        seedFromOpened();
    }

    private TouristMenu(int containerId, Inventory playerInventory, @Nullable TouristEntity tourist,
            TouristScreenData opened, ContainerData data) {
        super(com.quackers29.businesscraft.init.CommonModMenuTypes.TOURIST_MENU.get(), containerId);
        this.tourist = tourist;
        this.opened = opened == null ? TouristScreenData.fallback() : opened;
        this.data = data;
        addDataSlots(data);
    }

    @Override
    public void broadcastChanges() {
        if (tourist != null && !tourist.level().isClientSide) {
            pushFromTourist();
        }
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide || tourist == null || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        List<TouristTradeCatalog.TradeDef> defs = defs();
        if (id < 0 || id >= defs.size()) {
            return false;
        }
        TouristTradeCatalog.TradeDef def = defs.get(id);
        int level = tourist.getVillagerData().getLevel();
        if (level < def.minLevel()) {
            return false;
        }
        MerchantOffer offer = TouristTradeCatalog.find(tourist.getOffers(), def);
        if (offer == null) {
            tourist.refreshOffersWithLiveData();
            offer = TouristTradeCatalog.find(tourist.getOffers(), def);
        }
        if (offer == null || offer.isOutOfStock()) {
            return false;
        }
        if (!PlayerItemExchange.tryTake(serverPlayer, def.costItem(), def.costCount())) {
            return false;
        }
        ItemStack result = "ticket".equals(def.id())
                ? TouristTradeCatalog.ticketStack(contextFromTourist())
                : offer.getResult().copy();
        if (!serverPlayer.getInventory().add(result) && !result.isEmpty()) {
            serverPlayer.drop(result, false);
        }
        tourist.notifyTrade(offer);
        serverPlayer.level().playSound(null, tourist.blockPosition(), TouristSounds.GREET,
                SoundSource.NEUTRAL, 0.45f, TouristSounds.pitch(tourist, 1.1f));
        pushFromTourist();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (tourist == null) {
            return true;
        }
        return tourist.isAlive() && !tourist.isRemoved() && player.distanceToSqr(tourist) <= MAX_DISTANCE_SQR;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (tourist != null && tourist.getTradingPlayer() == player) {
            tourist.stopTrading();
        }
    }

    public String origin() {
        return opened.origin();
    }

    public String destination() {
        return opened.destination();
    }

    public boolean anyTown() {
        if (tourist != null && !tourist.level().isClientSide) {
            return tourist.isAnyTownDestination();
        }
        return (data.get(DATA_FLAGS) & FLAG_ANY_TOWN) != 0 || opened.anyTown();
    }

    public String typeName() {
        return opened.typeName();
    }

    public String lookLabel() {
        return opened.lookLabel();
    }

    public int[] thresholds() {
        return opened.thresholds();
    }

    public int distanceMeters() {
        return TouristScreenData.combineDistance(data.get(DATA_DISTANCE_LO), data.get(DATA_DISTANCE_HI));
    }

    public int level() {
        int level = data.get(DATA_LEVEL);
        return level <= 0 ? opened.level() : level;
    }

    public int usesMask() {
        return data.get(DATA_USES);
    }

    public int journeySeconds() {
        int seconds = data.get(DATA_JOURNEY_SEC);
        return seconds < 0 ? opened.journeySeconds() : seconds;
    }

    public int remainingSeconds() {
        int seconds = data.get(DATA_REMAINING_SEC);
        return seconds < 0 ? opened.remainingSeconds() : seconds;
    }

    public boolean stationary() {
        return (data.get(DATA_FLAGS) & FLAG_STATIONARY) != 0;
    }

    /** Name captured when the screen opened. */
    public String personalName() {
        return opened.personalName();
    }

    /** Look whose trade list this screen is showing. */
    public String lookId() {
        if (tourist != null && !tourist.level().isClientSide) {
            String live = tourist.getLookId();
            if (live != null && !live.isBlank()) {
                return live;
            }
        }
        return opened.lookId();
    }

    /** Journey seconds from the open snapshot, not the data slot. */
    public int openedJourneySeconds() {
        return opened.journeySeconds();
    }

    /** Remaining seconds from the open snapshot, not the data slot. */
    public int openedRemainingSeconds() {
        return opened.remainingSeconds();
    }

    private TouristTradeCatalog.TradeContext contextFromTourist() {
        int level = Math.max(1, tourist.getVillagerData().getLevel());
        return new TouristTradeCatalog.TradeContext(
                tourist.getOriginTownName(),
                tourist.getDestinationTownName(),
                tourist.isAnyTownDestination(),
                com.quackers29.businesscraft.economy.JourneyProgress.tierName(level),
                tourist.getTotalDistanceTraveled(),
                TouristTradeCatalog.formatDuration(Math.max(0, tourist.getJourneyTicks() / 20)),
                TouristTradeCatalog.formatDuration(Math.max(0, tourist.getExpiryTicks() / 20)),
                tourist.isStationary());
    }

    private void seedFromOpened() {
        data.set(DATA_DISTANCE_LO, TouristScreenData.distanceLo(opened.distanceMeters()));
        data.set(DATA_DISTANCE_HI, TouristScreenData.distanceHi(opened.distanceMeters()));
        data.set(DATA_LEVEL, opened.level());
        data.set(DATA_USES, opened.usesMask());
        data.set(DATA_JOURNEY_SEC, Math.min(opened.journeySeconds(), 32_767));
        data.set(DATA_REMAINING_SEC, Math.min(opened.remainingSeconds(), 32_767));
        int flags = 0;
        if (opened.anyTown()) {
            flags |= FLAG_ANY_TOWN;
        }
        if (opened.stationary()) {
            flags |= FLAG_STATIONARY;
        }
        data.set(DATA_FLAGS, flags);
    }

    private void pushFromTourist() {
        int meters = (int) Math.round(Math.max(0.0, tourist.getTotalDistanceTraveled()));
        data.set(DATA_DISTANCE_LO, TouristScreenData.distanceLo(meters));
        data.set(DATA_DISTANCE_HI, TouristScreenData.distanceHi(meters));
        data.set(DATA_LEVEL, Math.max(1, tourist.getVillagerData().getLevel()));
        data.set(DATA_USES, TouristTradeCatalog.usesMask(tourist.getOffers(), defs()));
        boolean stationary = tourist.isStationary();
        if (TouristTimer.shouldSync(timersSeeded, timerStationary, stationary)) {
            int remaining = TouristScreenData.clampSeconds(tourist.getExpiryTicks());
            if (!timersSeeded) {
                data.set(DATA_JOURNEY_SEC, TouristScreenData.clampSeconds(tourist.getJourneyTicks()));
                data.set(DATA_REMAINING_SEC, remaining);
                timersSeeded = true;
            } else {
                data.set(DATA_REMAINING_SEC, remaining);
            }
            timerStationary = stationary;
        }
        int flags = 0;
        if (tourist.isAnyTownDestination()) {
            flags |= FLAG_ANY_TOWN;
        }
        if (stationary) {
            flags |= FLAG_STATIONARY;
        }
        data.set(DATA_FLAGS, flags);
    }

    private List<TouristTradeCatalog.TradeDef> defs() {
        String lookId = tourist != null ? tourist.getLookId() : opened.lookId();
        return TouristTradeCatalog.forLook(lookId);
    }
}
