package com.quackers29.businesscraft.entity;

import com.quackers29.businesscraft.economy.JourneyProgress;
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
import java.util.List;
import java.util.Locale;

/**
 * Tourist deals and the words on the tourist screen.
 * <p>
 * The deals are the same item exchanges the villager trade screen used
 * (one paper for a travel ticket, then bread for iron, a compass, and a clock).
 * The old first row was not a deal: it was a barrier icon whose only job was
 * to carry origin and destination in a tooltip. That row is not offered any more.
 * Journey facts are drawn on the screen instead. A showcase-only helper still
 * builds the barrier row so the old screen can be photographed.
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

    private static final List<TradeDef> DEFS = List.of(
            new TradeDef("ticket", Items.PAPER, 1, Items.PAPER, 1, 1, 0.0f,
                    "Travel Ticket", "A souvenir paper. Not a ride."),
            new TradeDef("iron_8", Items.BREAD, 8, Items.IRON_INGOT, 2, 1, 0.05f,
                    "2 iron for 8 bread", "Offered from the first step of the journey."),
            new TradeDef("iron_6", Items.BREAD, 6, Items.IRON_INGOT, 2, 2, 0.10f,
                    "2 iron for 6 bread", "A better rate once they reach Traveler."),
            new TradeDef("iron_4", Items.BREAD, 4, Items.IRON_INGOT, 2, 3, 0.15f,
                    "2 iron for 4 bread", "Offered from Voyager onward."),
            new TradeDef("compass", Items.BREAD, 4, Items.COMPASS, 1, 4, 0.05f,
                    "Compass for 4 bread", "Offered from Veteran onward."),
            new TradeDef("clock", Items.BREAD, 2, Items.CLOCK, 1, 5, 0.05f,
                    "Clock for 2 bread", "Offered once they are Renowned."));

    private TouristTradeCatalog() {
    }

    public static List<TradeDef> all() {
        return DEFS;
    }

    public static MerchantOffers offersFor(int level, TradeContext context) {
        MerchantOffers offers = new MerchantOffers();
        for (TradeDef def : DEFS) {
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

    public static MerchantOffers offersForVanillaScreenshot(int level, TradeContext context) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(legacyInfoOffer(context));
        offers.addAll(offersFor(level, context));
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
        lines.add(Component.literal("It is not a ride. Tourists never walk to a town.").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.literal("Carry them by minecart or train.").withStyle(ChatFormatting.DARK_GRAY));
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

    public static int usesMask(MerchantOffers offers) {
        int mask = 0;
        if (offers == null) {
            return mask;
        }
        List<TradeDef> defs = all();
        for (int i = 0; i < defs.size(); i++) {
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
        String item = costName(def);
        int cost = def == null ? 0 : def.costCount();
        return "Needs " + cost + " " + item + " · you have " + Math.max(0, have);
    }

    public static String tradedLabel() {
        return "Already traded this visit";
    }

    public static String costName(TradeDef def) {
        if (def == null || def.costItem() == null) {
            return "items";
        }
        if (def.costItem() == Items.BREAD) {
            return "bread";
        }
        if (def.costItem() == Items.PAPER) {
            return "paper";
        }
        return def.costItem().getDescription().getString().toLowerCase(Locale.ROOT);
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

    public static String formatDuration(int seconds) {
        if (seconds >= 32_767) {
            return "9h+";
        }
        int safe = Math.max(0, seconds);
        int minutes = safe / 60;
        int hours = minutes / 60;
        minutes %= 60;
        int secs = safe % 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + secs + "s";
        }
        return secs + "s";
    }

    public static String transportNote() {
        return "They never walk. Carry them by minecart or train.";
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
