package com.quackers29.businesscraft.menu;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.economy.JourneyProgress;
import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.entity.TouristTradeCatalog;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Facts the tourist screen needs that are not item stacks.
 * Written once when the screen opens. Distance, rank, and trade uses keep
 * moving through the menu's data slots after that.
 */
public final class TouristScreenData {
    public static final int VERSION = 2;

    private final String origin;
    private final String destination;
    private final boolean anyTown;
    private final String typeName;
    private final String lookLabel;
    private final String personalName;
    private final String lookId;
    private final int[] thresholds;
    private final int level;
    private final int distanceMeters;
    private final int usesMask;
    private final int journeySeconds;
    private final int remainingSeconds;
    private final boolean stationary;

    public TouristScreenData(String origin, String destination, boolean anyTown, String typeName, String lookLabel,
            String personalName, String lookId, int[] thresholds, int level, int distanceMeters, int usesMask,
            int journeySeconds, int remainingSeconds, boolean stationary) {
        this.origin = origin == null ? "Unknown" : origin;
        this.destination = destination == null ? "Unknown" : destination;
        this.anyTown = anyTown;
        this.typeName = typeName == null ? "" : typeName;
        this.lookLabel = lookLabel == null ? "" : lookLabel;
        this.personalName = personalName == null ? "" : personalName;
        this.lookId = lookId == null ? "" : lookId;
        this.thresholds = thresholds == null || thresholds.length == 0
                ? JourneyProgress.DEFAULT_TOURIST_LEVELS.clone()
                : thresholds.clone();
        this.level = Math.max(1, level);
        this.distanceMeters = Math.max(0, distanceMeters);
        this.usesMask = usesMask;
        this.journeySeconds = Math.max(0, journeySeconds);
        this.remainingSeconds = Math.max(0, remainingSeconds);
        this.stationary = stationary;
    }

    public static TouristScreenData capture(TouristEntity tourist) {
        tourist.ensurePersonalName();
        int level = tourist.getVillagerData().getLevel();
        double distance = tourist.getTotalDistanceTraveled();
        int[] thresholds = ConfigLoader.touristLevelDistances;
        int meters = (int) Math.round(Math.max(0.0, distance));
        String lookId = tourist.getLookId();
        return new TouristScreenData(
                tourist.getOriginTownName(),
                tourist.getDestinationTownName(),
                tourist.isAnyTownDestination(),
                tourist.getTouristType().displayName(),
                TouristTradeCatalog.lookLabel(lookId),
                tourist.getPersonalName(),
                lookId,
                thresholds,
                level,
                meters,
                TouristTradeCatalog.usesMask(tourist.getOffers(), TouristTradeCatalog.forLook(lookId)),
                Math.max(0, tourist.getJourneyTicks() / 20),
                Math.max(0, tourist.getExpiryTicks() / 20),
                tourist.isStationary());
    }

    public static TouristScreenData fallback() {
        return new TouristScreenData("Unknown", TouristEntity.ANY_TOWN_NAME, true, "", "", "", "",
                JourneyProgress.DEFAULT_TOURIST_LEVELS, 1, 0, 0, 0, 0, true);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeByte(VERSION);
        buf.writeUtf(clamp(origin, 64));
        buf.writeUtf(clamp(destination, 64));
        buf.writeBoolean(anyTown);
        buf.writeUtf(clamp(typeName, 32));
        buf.writeUtf(clamp(lookLabel, 64));
        buf.writeUtf(clamp(personalName, 64));
        buf.writeUtf(clamp(lookId, 64));
        buf.writeVarInt(thresholds.length);
        for (int threshold : thresholds) {
            buf.writeVarInt(threshold);
        }
        buf.writeVarInt(level);
        buf.writeVarInt(distanceMeters);
        buf.writeVarInt(usesMask);
        buf.writeVarInt(Math.max(0, journeySeconds));
        buf.writeVarInt(Math.max(0, remainingSeconds));
        buf.writeBoolean(stationary);
    }

    public static TouristScreenData read(FriendlyByteBuf buf) {
        if (buf == null || buf.readableBytes() < 1) {
            return fallback();
        }
        int version = buf.readByte();
        if (version != VERSION) {
            return fallback();
        }
        String origin = buf.readUtf(64);
        String destination = buf.readUtf(64);
        boolean anyTown = buf.readBoolean();
        String typeName = buf.readUtf(32);
        String lookLabel = buf.readUtf(64);
        String personalName = buf.readUtf(64);
        String lookId = buf.readUtf(64);
        int count = buf.readVarInt();
        int[] thresholds = new int[Math.max(0, Math.min(count, 16))];
        for (int i = 0; i < thresholds.length; i++) {
            thresholds[i] = buf.readVarInt();
        }
        if (count > thresholds.length) {
            for (int i = thresholds.length; i < count; i++) {
                buf.readVarInt();
            }
        }
        return new TouristScreenData(
                origin,
                destination,
                anyTown,
                typeName,
                lookLabel,
                personalName,
                lookId,
                thresholds,
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean());
    }

    /** Low 16 bits of a distance, safe to ship through a menu data slot. */
    public static int distanceLo(int meters) {
        return meters & 0xFFFF;
    }

    /** High 16 bits of a distance. */
    public static int distanceHi(int meters) {
        return (meters >>> 16) & 0xFFFF;
    }

    /** Rebuilds meters after a signed-short menu sync. */
    public static int combineDistance(int lo, int hi) {
        return (lo & 0xFFFF) | ((hi & 0xFFFF) << 16);
    }

    public String origin() {
        return origin;
    }

    public String destination() {
        return destination;
    }

    public boolean anyTown() {
        return anyTown;
    }

    public String typeName() {
        return typeName;
    }

    public String lookLabel() {
        return lookLabel;
    }

    public String personalName() {
        return personalName;
    }

    public String lookId() {
        return lookId;
    }

    public int[] thresholds() {
        return thresholds.clone();
    }

    public int level() {
        return level;
    }

    public int distanceMeters() {
        return distanceMeters;
    }

    public int usesMask() {
        return usesMask;
    }

    public int journeySeconds() {
        return journeySeconds;
    }

    public int remainingSeconds() {
        return remainingSeconds;
    }

    public boolean stationary() {
        return stationary;
    }

    public static int clampSeconds(int ticks) {
        if (ticks <= 0) {
            return 0;
        }
        int seconds = ticks / 20;
        return Math.min(seconds, 32_767);
    }

    private static String clamp(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max);
    }
}
