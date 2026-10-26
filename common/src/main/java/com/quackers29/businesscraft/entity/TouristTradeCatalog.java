package com.quackers29.businesscraft.entity;

import com.quackers29.businesscraft.economy.JourneyProgress;
import com.quackers29.businesscraft.entity.tourist.TouristLook;
import com.quackers29.businesscraft.entity.tourist.TouristLooks;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tourist deals and the words on the tourist screen.
 * <p>
 * Deals are records in this class (not a config file yet) so a later loader can
 * replace the lists without changing the screen. A look with an entry in
 * {@link #LOOKS} uses that list. Every other look uses the list for its type.
 * The travel ticket is on every list. Rank gates use {@code minLevel}; the screen
 * still says "Offered once they are X" and "Locked · X at Y".
 * <p>
 * Output value is never above input value on the scale in the trade tests, so a
 * player cannot gain goods by cycling one tourist's deals into another's.
 * The distance fare is not part of this catalog.
 */
public final class TouristTradeCatalog {
    public enum RowState {
        READY, NEED, LOCKED, TRADED
    }

    public record TradeDef(
            String id,
            Item costItem,
            int costCount,
            Item resultItem,
            int resultCount,
            int minLevel,
            float priceMultiplier,
            String title,
            String detail) {
    }

    /**
     * Snapshot the ticket lore is built from. Rank and distance are display-only;
     * they do not change the items exchanged.
     */
    public record TradeContext(
            String origin,
            String destination,
            boolean anyTown,
            String rankName,
            double distanceMeters,
            String journeyTime,
            String timeLeft,
            boolean stationary) {
    }

    private static final TradeDef TICKET = def("ticket", Items.PAPER, 1, Items.PAPER, 1, 1, "Travel Ticket");

    private static final Map<TouristType, List<TradeDef>> BY_TYPE = Map.of(
            TouristType.SIGHTSEER, List.of(
                    TICKET,
                    def("sight_map", Items.BREAD, 6, Items.MAP, 1, 1, "Map for 6 bread"),
                    def("sight_flowers", Items.BREAD, 4, Items.CORNFLOWER, 4, 1, "4 cornflowers for 4 bread"),
                    def("sight_spyglass", Items.BREAD, 8, Items.SPYGLASS, 1, 2, "Spyglass for 8 bread"),
                    def("sight_candle", Items.BREAD, 4, Items.CANDLE, 1, 2, "Candle for 4 bread"),
                    def("sight_painting", Items.BREAD, 8, Items.PAINTING, 1, 3, "Painting for 8 bread"),
                    def("sight_compass", Items.BREAD, 10, Items.COMPASS, 1, 4, "Compass for 10 bread"),
                    def("sight_clock", Items.BREAD, 14, Items.CLOCK, 1, 5, "Clock for 14 bread")),
            TouristType.PILGRIM, List.of(
                    TICKET,
                    def("pilgrim_candles", Items.BREAD, 6, Items.CANDLE, 2, 1, "2 candles for 6 bread"),
                    def("pilgrim_flowers", Items.WHEAT, 6, Items.OXEYE_DAISY, 4, 1, "4 daisies for 6 wheat"),
                    def("pilgrim_book", Items.BREAD, 8, Items.BOOK, 1, 2, "Book for 8 bread"),
                    def("pilgrim_journal", Items.BREAD, 6, Items.WRITABLE_BOOK, 1, 3, "Journal for 6 bread"),
                    def("pilgrim_lantern", Items.BREAD, 10, Items.LANTERN, 1, 4, "Lantern for 10 bread"),
                    def("pilgrim_name", Items.BREAD, 12, Items.NAME_TAG, 1, 5, "Name tag for 12 bread")),
            TouristType.SHOPPER, List.of(
                    TICKET,
                    def("shop_copper", Items.BREAD, 8, Items.COPPER_INGOT, 4, 1, "4 copper for 8 bread"),
                    def("shop_iron", Items.BREAD, 8, Items.IRON_INGOT, 1, 1, "Iron for 8 bread"),
                    def("shop_iron_better", Items.BREAD, 6, Items.IRON_INGOT, 1, 2, "Iron for 6 bread"),
                    def("shop_leather", Items.BREAD, 6, Items.LEATHER, 2, 3, "2 leather for 6 bread"),
                    def("shop_nuggets", Items.BREAD, 8, Items.GOLD_NUGGET, 4, 4, "4 gold nuggets for 8 bread"),
                    def("shop_lantern", Items.BREAD, 8, Items.LANTERN, 1, 4, "Lantern for 8 bread"),
                    def("shop_clock", Items.BREAD, 16, Items.CLOCK, 1, 5, "Clock for 16 bread")),
            TouristType.SCHOLAR, List.of(
                    TICKET,
                    def("scholar_ink", Items.BREAD, 4, Items.INK_SAC, 4, 1, "4 ink for 4 bread"),
                    def("scholar_book", Items.PAPER, 6, Items.BOOK, 1, 1, "Book for 6 paper"),
                    def("scholar_map", Items.BREAD, 8, Items.MAP, 1, 2, "Map for 8 bread"),
                    def("scholar_journal", Items.BREAD, 8, Items.WRITABLE_BOOK, 1, 3, "Journal for 8 bread"),
                    def("scholar_lantern", Items.BREAD, 8, Items.LANTERN, 1, 4, "Lantern for 8 bread"),
                    def("scholar_compass", Items.BREAD, 10, Items.COMPASS, 1, 4, "Compass for 10 bread"),
                    def("scholar_clock", Items.BREAD, 12, Items.CLOCK, 1, 5, "Clock for 12 bread")),
            TouristType.ADVENTURER, List.of(
                    TICKET,
                    def("trail_torches", Items.BREAD, 4, Items.TORCH, 8, 1, "8 torches for 4 bread"),
                    def("trail_leather", Items.BREAD, 4, Items.LEATHER, 2, 1, "2 leather for 4 bread"),
                    def("trail_map", Items.BREAD, 6, Items.MAP, 1, 2, "Map for 6 bread"),
                    def("trail_spyglass", Items.BREAD, 8, Items.SPYGLASS, 1, 2, "Spyglass for 8 bread"),
                    def("trail_compass", Items.BREAD, 8, Items.COMPASS, 1, 3, "Compass for 8 bread"),
                    def("trail_iron", Items.COAL, 8, Items.IRON_INGOT, 2, 4, "2 iron for 8 coal"),
                    def("trail_clock", Items.BREAD, 14, Items.CLOCK, 1, 5, "Clock for 14 bread")));

    private static final Map<String, List<TradeDef>> LOOKS = lookLists();

    private static final List<TradeDef> EVERY = everyDef();

    private TouristTradeCatalog() {
    }

    public static List<TradeDef> all() {
        return EVERY;
    }

    /** Trades this look shows, including ones still locked by rank. */
    public static List<TradeDef> forLook(String lookId) {
        if (lookId != null) {
            List<TradeDef> override = LOOKS.get(lookId);
            if (override != null) {
                return override;
            }
            for (TouristLook look : TouristLooks.all()) {
                if (look.id().equals(lookId) && look.type() != null) {
                    List<TradeDef> typed = BY_TYPE.get(look.type());
                    if (typed != null) {
                        return typed;
                    }
                }
            }
        }
        return BY_TYPE.get(TouristType.SIGHTSEER);
    }

    public static TradeDef findDef(String lookId, String id) {
        for (TradeDef def : forLook(lookId)) {
            if (def.id().equals(id)) {
                return def;
            }
        }
        return null;
    }

    public static MerchantOffers offersFor(int level, TradeContext context, String lookId) {
        MerchantOffers offers = new MerchantOffers();
        for (TradeDef def : forLook(lookId)) {
            if (level >= def.minLevel()) {
                offers.add(toOffer(def, context));
            }
        }
        return offers;
    }

    public static MerchantOffer toOffer(TradeDef def, TradeContext context) {
        ItemStack cost = new ItemStack(def.costItem(), def.costCount());
        ItemStack result = "ticket".equals(def.id())
                ? ticketStack(context)
                : new ItemStack(def.resultItem(), def.resultCount());
        return new MerchantOffer(cost, result, 1, 0, def.priceMultiplier());
    }

    /**
     * The old untradeable first row (barrier → paper). Showcase uses it to
     * photograph the vanilla merchant screen. Players never see it.
     */
    public static MerchantOffer legacyInfoOffer(TradeContext context) {
        ItemStack info = new ItemStack(Items.PAPER);
        info.setHoverName(Component.literal("Tourist Information").withStyle(ChatFormatting.GOLD));
        String origin = blankTo(context.origin(), "Unknown");
        String destination = blankTo(context.destination(), "Unknown");
        setLore(info, List.of(
                Component.literal("Origin: " + origin),
                Component.literal("Destination: " + destination),
                Component.literal("Journey Time: " + blankTo(context.journeyTime(), "0s")),
                Component.literal("Time Remaining: " + blankTo(context.timeLeft(), "0s")),
                Component.literal("Distance Traveled: " + String.format(Locale.ROOT, "%.1f", context.distanceMeters()) + "m"),
                Component.literal("Status: " + (context.stationary() ? "Stationary" : "Moving"))));
        return new MerchantOffer(new ItemStack(Items.BARRIER), info, 1, 0, 0.0f);
    }

    public static MerchantOffers offersForVanillaScreenshot(int level, TradeContext context, String lookId) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(legacyInfoOffer(context));
        offers.addAll(offersFor(level, context, lookId));
        return offers;
    }

    public static ItemStack ticketStack(TradeContext context) {
        ItemStack ticket = new ItemStack(Items.PAPER);
        ticket.setHoverName(Component.literal("Travel Ticket").withStyle(ChatFormatting.GOLD));
        setLore(ticket, ticketLore(context));
        return ticket;
    }

    public static List<Component> ticketLore(TradeContext context) {
        String origin = blankTo(context.origin(), "Unknown");
        String destination = destinationLabel(context.destination(), context.anyTown());
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("From: " + origin).withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("To: " + destination).withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("A paper souvenir from this tourist.").withStyle(ChatFormatting.WHITE));
        if (context.anyTown()) {
            lines.add(Component.literal("Any Town means no destination was chosen.").withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.literal("They pay the first town they reach.").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            lines.add(Component.literal("They pay " + destination + " when they arrive.").withStyle(ChatFormatting.DARK_GRAY));
        }
        String rank = blankTo(context.rankName(), JourneyProgress.tierName(1));
        lines.add(Component.literal(rank + " · " + TouristDisplayFormat.formatDistance(Math.max(0.0, context.distanceMeters()))
                + " travelled").withStyle(ChatFormatting.YELLOW));
        return lines;
    }

    public static void copyUses(MerchantOffers from, MerchantOffers to) {
        if (from == null || to == null) {
            return;
        }
        for (MerchantOffer next : to) {
            for (MerchantOffer old : from) {
                if (sameDeal(old, next)) {
                    for (int i = 0; i < old.getUses(); i++) {
                        next.increaseUses();
                    }
                    break;
                }
            }
        }
    }

    public static boolean sameDeal(MerchantOffer left, MerchantOffer right) {
        return sameStack(left.getBaseCostA(), right.getBaseCostA())
                && sameStack(left.getCostB(), right.getCostB())
                && sameStack(left.getResult(), right.getResult());
    }

    public static int usesMask(MerchantOffers offers, List<TradeDef> defs) {
        int mask = 0;
        if (offers == null || defs == null) {
            return mask;
        }
        int limit = Math.min(defs.size(), 31);
        for (int i = 0; i < limit; i++) {
            MerchantOffer offer = find(offers, defs.get(i));
            if (offer != null && offer.isOutOfStock()) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    public static MerchantOffer find(MerchantOffers offers, TradeDef def) {
        if (offers == null || def == null) {
            return null;
        }
        for (MerchantOffer offer : offers) {
            if (matches(def, offer)) {
                return offer;
            }
        }
        return null;
    }

    public static boolean matches(TradeDef def, MerchantOffer offer) {
        if (offer == null || def == null) {
            return false;
        }
        if (!offer.getCostB().isEmpty()) {
            return false;
        }
        return offer.getBaseCostA().is(def.costItem())
                && offer.getBaseCostA().getCount() == def.costCount()
                && offer.getResult().is(def.resultItem())
                && offer.getResult().getCount() == def.resultCount();
    }

    public static boolean isUsed(int usesMask, int index) {
        return index >= 0 && index < 31 && (usesMask & (1 << index)) != 0;
    }

    public static RowState rowState(int level, int have, boolean used, TradeDef def) {
        if (def == null || level < def.minLevel()) {
            return RowState.LOCKED;
        }
        if (used) {
            return RowState.TRADED;
        }
        if (have < def.costCount()) {
            return RowState.NEED;
        }
        return RowState.READY;
    }

    public static String lockLabel(TradeDef def, int[] thresholds) {
        int level = def == null ? 2 : def.minLevel();
        String rank = JourneyProgress.tierName(level);
        int at = JourneyProgress.distanceToReach(level, thresholds);
        return "Locked · " + rank + " at " + TouristDisplayFormat.formatDistance(at);
    }

    public static String needLabel(TradeDef def, int have) {
        String item = itemName(def == null ? null : def.costItem());
        int cost = def == null ? 0 : def.costCount();
        return "Needs " + cost + " " + item + " · you have " + Math.max(0, have);
    }

    public static String tradedLabel() {
        return "Already traded this visit";
    }

    public static String offeredLine(int minLevel) {
        if (minLevel <= 1) {
            return "Offered from the first step of the journey.";
        }
        return "Offered once they are " + JourneyProgress.tierName(minLevel) + ".";
    }

    public static String costName(TradeDef def) {
        if (def == null) {
            return "items";
        }
        return itemName(def.costItem());
    }

    public static String itemName(Item item) {
        if (item == null) {
            return "items";
        }
        if (item == Items.BREAD) {
            return "bread";
        }
        if (item == Items.PAPER) {
            return "paper";
        }
        if (item == Items.IRON_INGOT) {
            return "iron";
        }
        if (item == Items.COMPASS) {
            return "compass";
        }
        if (item == Items.CLOCK) {
            return "clock";
        }
        if (item == Items.WHEAT) {
            return "wheat";
        }
        if (item == Items.SAND) {
            return "sand";
        }
        if (item == Items.COAL) {
            return "coal";
        }
        if (item == Items.EMERALD) {
            return "emerald";
        }
        if (item == Items.PUMPKIN_PIE) {
            return "pumpkin pie";
        }
        return item.getDescription().getString().toLowerCase(Locale.ROOT);
    }

    public static String destinationLabel(String destination, boolean anyTown) {
        if (anyTown) {
            return TouristEntity.ANY_TOWN_NAME;
        }
        return blankTo(destination, "Unknown");
    }

    public static String destinationDetail(String destination, boolean anyTown) {
        if (anyTown) {
            return "No town was chosen. They pay the first town they reach.";
        }
        String name = blankTo(destination, "that town");
        return "They pay " + name + " when they arrive.";
    }

    public static String progressLine(double distance, int level, int[] thresholds) {
        double safe = Math.max(0.0, distance);
        String travelled = TouristDisplayFormat.formatDistance(safe) + " travelled";
        Integer next = JourneyProgress.distanceToNext(level, thresholds);
        if (next == null) {
            return travelled + " · " + JourneyProgress.tierName(level);
        }
        int remaining = (int) Math.max(0, Math.ceil(next - safe));
        return travelled + " · " + TouristDisplayFormat.formatDistance(remaining)
                + " to " + JourneyProgress.tierName(level + 1);
    }

    public static String waitingLine(int journeySeconds, int remainingSeconds, boolean stationary) {
        String out = "Out for " + formatDuration(journeySeconds);
        String left = formatDuration(remainingSeconds);
        if (stationary) {
            return out + " · " + left + " left while they wait";
        }
        return out + " · timer paused (" + left + " left)";
    }

    public static String identityLine(String typeName, String lookLabel) {
        String type = typeName == null ? "" : typeName.trim();
        String look = lookLabel == null ? "" : lookLabel.trim();
        if (look.isEmpty()) {
            return type;
        }
        if (type.isEmpty() || look.toLowerCase(Locale.ROOT).endsWith(type.toLowerCase(Locale.ROOT))) {
            return look;
        }
        return type + " · " + look;
    }

    public static String lookLabel(String lookId) {
        if (lookId == null || lookId.isBlank()) {
            return "";
        }
        String[] parts = lookId.trim().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    /** Minutes only. Under a minute is {@code <1m}. */
    public static String formatDuration(int seconds) {
        if (seconds < 0) {
            return "<1m";
        }
        if (seconds >= 100 * 3600) {
            return "99h+";
        }
        if (seconds < 60) {
            return "<1m";
        }
        int minutes = seconds / 60;
        int hours = minutes / 60;
        minutes %= 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    public static int countOf(List<ItemStack> stacks, Item item) {
        if (stacks == null || item == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : stacks) {
            if (stack != null && stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static TradeDef def(String id, Item cost, int costCount, Item result, int resultCount, int minLevel, String title) {
        return new TradeDef(id, cost, costCount, result, resultCount, minLevel, 0.05f, title, offeredLine(minLevel));
    }

    private static Map<String, List<TradeDef>> lookLists() {
        Map<String, List<TradeDef>> lists = new LinkedHashMap<>();
        lists.put("desert_guide", List.of(
                TICKET,
                def("desert_cactus", Items.SAND, 8, Items.CACTUS, 4, 1, "4 cactus for 8 sand"),
                def("desert_sand", Items.BREAD, 6, Items.SAND, 8, 1, "8 sand for 6 bread"),
                def("desert_bush", Items.BREAD, 4, Items.DEAD_BUSH, 1, 2, "Dead bush for 4 bread"),
                def("desert_sandstone", Items.BREAD, 8, Items.SANDSTONE, 4, 2, "4 sandstone for 8 bread"),
                def("desert_compass", Items.BREAD, 10, Items.COMPASS, 1, 3, "Compass for 10 bread"),
                def("desert_redsand", Items.BREAD, 6, Items.RED_SAND, 8, 4, "8 red sand for 6 bread"),
                def("desert_clock", Items.BREAD, 12, Items.CLOCK, 1, 5, "Clock for 12 bread")));
        lists.put("safari_sightseer", List.of(
                TICKET,
                def("safari_spyglass", Items.BREAD, 6, Items.SPYGLASS, 1, 1, "Spyglass for 6 bread"),
                def("safari_map", Items.BREAD, 6, Items.MAP, 1, 1, "Map for 6 bread"),
                def("safari_cocoa", Items.BREAD, 4, Items.COCOA_BEANS, 4, 2, "4 cocoa for 4 bread"),
                def("safari_leather", Items.BREAD, 6, Items.LEATHER, 2, 2, "2 leather for 6 bread"),
                def("safari_bamboo", Items.BREAD, 4, Items.BAMBOO, 8, 3, "8 bamboo for 4 bread"),
                def("safari_compass", Items.BREAD, 10, Items.COMPASS, 1, 4, "Compass for 10 bread"),
                def("safari_clock", Items.BREAD, 16, Items.CLOCK, 1, 5, "Clock for 16 bread")));
        lists.put("coast_visitor", List.of(
                TICKET,
                def("coast_kelp", Items.BREAD, 4, Items.KELP, 8, 1, "8 kelp for 4 bread"),
                def("coast_dried", Items.BREAD, 4, Items.DRIED_KELP, 6, 1, "6 dried kelp for 4 bread"),
                def("coast_spyglass", Items.BREAD, 8, Items.SPYGLASS, 1, 2, "Spyglass for 8 bread"),
                def("coast_compass", Items.BREAD, 10, Items.COMPASS, 1, 3, "Compass for 10 bread"),
                def("coast_shards", Items.BREAD, 6, Items.PRISMARINE_SHARD, 4, 3, "4 prismarine shards for 6 bread"),
                def("coast_crystals", Items.BREAD, 8, Items.PRISMARINE_CRYSTALS, 2, 4, "2 prismarine crystals for 8 bread"),
                def("coast_shell", Items.BREAD, 12, Items.NAUTILUS_SHELL, 1, 4, "Nautilus shell for 12 bread"),
                def("coast_clock", Items.BREAD, 14, Items.CLOCK, 1, 5, "Clock for 14 bread")));
        lists.put("festival_guest", List.of(
                TICKET,
                def("fest_cookies", Items.BREAD, 4, Items.COOKIE, 4, 1, "4 cookies for 4 bread"),
                def("fest_flowers", Items.BREAD, 4, Items.CORNFLOWER, 4, 1, "4 cornflowers for 4 bread"),
                def("fest_candles", Items.BREAD, 6, Items.CANDLE, 2, 2, "2 candles for 6 bread"),
                def("fest_glow", Items.BREAD, 6, Items.GLOW_BERRIES, 4, 3, "4 glow berries for 6 bread"),
                def("fest_painting", Items.BREAD, 6, Items.PAINTING, 1, 3, "Painting for 6 bread"),
                def("fest_lantern", Items.BREAD, 8, Items.LANTERN, 1, 4, "Lantern for 8 bread"),
                def("fest_clock", Items.BREAD, 14, Items.CLOCK, 1, 5, "Clock for 14 bread")));
        lists.put("night_luxury", List.of(
                TICKET,
                def("night_spyglass", Items.PUMPKIN_PIE, 4, Items.SPYGLASS, 1, 1, "Spyglass for 4 pumpkin pie"),
                def("night_lantern", Items.PUMPKIN_PIE, 2, Items.LANTERN, 1, 2, "Lantern for 2 pumpkin pie"),
                def("night_nuggets", Items.PUMPKIN_PIE, 4, Items.GOLD_NUGGET, 4, 3, "4 gold nuggets for 4 pumpkin pie"),
                def("night_compass", Items.PUMPKIN_PIE, 6, Items.COMPASS, 1, 4, "Compass for 6 pumpkin pie"),
                def("night_name", Items.EMERALD, 1, Items.NAME_TAG, 1, 4, "Name tag for 1 emerald"),
                def("night_clock", Items.PUMPKIN_PIE, 6, Items.CLOCK, 1, 5, "Clock for 6 pumpkin pie")));
        lists.put("bazaar_merchant", List.of(
                TICKET,
                def("bazaar_lantern", Items.BREAD, 6, Items.LANTERN, 1, 1, "Lantern for 6 bread"),
                def("bazaar_candles", Items.BREAD, 4, Items.CANDLE, 2, 1, "2 candles for 4 bread"),
                def("bazaar_copper", Items.BREAD, 8, Items.COPPER_INGOT, 4, 2, "4 copper for 8 bread"),
                def("bazaar_nuggets", Items.BREAD, 6, Items.GOLD_NUGGET, 3, 3, "3 gold nuggets for 6 bread"),
                def("bazaar_glow", Items.BREAD, 6, Items.GLOW_BERRIES, 3, 3, "3 glow berries for 6 bread"),
                def("bazaar_compass", Items.BREAD, 10, Items.COMPASS, 1, 4, "Compass for 10 bread"),
                def("bazaar_clock", Items.BREAD, 14, Items.CLOCK, 1, 5, "Clock for 14 bread")));
        lists.put("harbour_skipper", List.of(
                TICKET,
                def("harbour_kelp", Items.BREAD, 4, Items.KELP, 8, 1, "8 kelp for 4 bread"),
                def("harbour_dried", Items.BREAD, 4, Items.DRIED_KELP, 6, 1, "6 dried kelp for 4 bread"),
                def("harbour_iron", Items.BREAD, 8, Items.IRON_INGOT, 1, 2, "Iron for 8 bread"),
                def("harbour_shards", Items.BREAD, 6, Items.PRISMARINE_SHARD, 4, 3, "4 prismarine shards for 6 bread"),
                def("harbour_compass", Items.BREAD, 10, Items.COMPASS, 1, 3, "Compass for 10 bread"),
                def("harbour_lantern", Items.BREAD, 8, Items.LANTERN, 1, 4, "Lantern for 8 bread"),
                def("harbour_clock", Items.BREAD, 14, Items.CLOCK, 1, 5, "Clock for 14 bread")));
        lists.put("canyon_scout", List.of(
                TICKET,
                def("canyon_torches", Items.BREAD, 4, Items.TORCH, 8, 1, "8 torches for 4 bread"),
                def("canyon_leather", Items.BREAD, 4, Items.LEATHER, 2, 1, "2 leather for 4 bread"),
                def("canyon_spyglass", Items.BREAD, 8, Items.SPYGLASS, 1, 2, "Spyglass for 8 bread"),
                def("canyon_clay", Items.BREAD, 6, Items.TERRACOTTA, 4, 2, "4 terracotta for 6 bread"),
                def("canyon_compass", Items.BREAD, 8, Items.COMPASS, 1, 3, "Compass for 8 bread"),
                def("canyon_iron", Items.COAL, 8, Items.IRON_INGOT, 2, 4, "2 iron for 8 coal"),
                def("canyon_clock", Items.BREAD, 14, Items.CLOCK, 1, 5, "Clock for 14 bread")));
        lists.put("alpine_walker", List.of(
                TICKET,
                def("alpine_torches", Items.BREAD, 4, Items.TORCH, 8, 1, "8 torches for 4 bread"),
                def("alpine_leather", Items.BREAD, 4, Items.LEATHER, 2, 1, "2 leather for 4 bread"),
                def("alpine_camp", Items.BREAD, 6, Items.CAMPFIRE, 1, 2, "Campfire for 6 bread"),
                def("alpine_snow", Items.BREAD, 4, Items.SNOWBALL, 8, 2, "8 snowballs for 4 bread"),
                def("alpine_lantern", Items.COAL, 8, Items.LANTERN, 1, 3, "Lantern for 8 coal"),
                def("alpine_compass", Items.BREAD, 8, Items.COMPASS, 1, 4, "Compass for 8 bread"),
                def("alpine_clock", Items.BREAD, 14, Items.CLOCK, 1, 5, "Clock for 14 bread")));
        lists.put("station_conductor", List.of(
                TICKET,
                def("conductor_map", Items.BREAD, 4, Items.MAP, 1, 1, "Map for 4 bread"),
                def("conductor_paper", Items.BREAD, 2, Items.PAPER, 4, 1, "4 paper for 2 bread"),
                def("conductor_ink", Items.BREAD, 4, Items.INK_SAC, 3, 2, "3 ink for 4 bread"),
                def("conductor_lantern", Items.BREAD, 6, Items.LANTERN, 1, 2, "Lantern for 6 bread"),
                def("conductor_compass", Items.BREAD, 8, Items.COMPASS, 1, 3, "Compass for 8 bread"),
                def("conductor_clock", Items.BREAD, 14, Items.CLOCK, 1, 5, "Clock for 14 bread")));
        return Map.copyOf(lists);
    }

    private static List<TradeDef> everyDef() {
        Map<String, TradeDef> byId = new LinkedHashMap<>();
        for (List<TradeDef> list : BY_TYPE.values()) {
            for (TradeDef def : list) {
                byId.putIfAbsent(def.id(), def);
            }
        }
        for (List<TradeDef> list : LOOKS.values()) {
            for (TradeDef def : list) {
                byId.putIfAbsent(def.id(), def);
            }
        }
        return List.copyOf(byId.values());
    }

    private static boolean sameStack(ItemStack left, ItemStack right) {
        if (left.isEmpty() && right.isEmpty()) {
            return true;
        }
        if (left.isEmpty() || right.isEmpty()) {
            return false;
        }
        return ItemStack.isSameItem(left, right) && left.getCount() == right.getCount();
    }

    private static void setLore(ItemStack stack, List<Component> lines) {
        ListTag lore = new ListTag();
        for (Component line : lines) {
            lore.add(StringTag.valueOf(Component.Serializer.toJson(line)));
        }
        stack.getOrCreateTagElement("display").put("Lore", lore);
    }

    private static String blankTo(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
