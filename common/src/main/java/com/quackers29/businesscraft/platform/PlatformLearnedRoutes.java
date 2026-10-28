package com.quackers29.businesscraft.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * Rolling destination scores learned from successful arrivals at an AUTO platform.
 * Decay uses world game time (ticks), not wall clock.
 */
public final class PlatformLearnedRoutes {

    /** Weight given to each new trip-duration sample when updating the rolling average. */
    public static final double TRIP_DURATION_EMA_ALPHA = 0.25;

    public static final class Entry {
        public double score;
        public long lastUpdateTick;
        /** Rolling average in-game trip time (ticks), updated on each paid arrival. */
        public double avgTripDurationTicks;

        Entry(double score, long lastUpdateTick, double avgTripDurationTicks) {
            this.score = score;
            this.lastUpdateTick = lastUpdateTick;
            this.avgTripDurationTicks = avgTripDurationTicks;
        }

        Entry copy() {
            return new Entry(score, lastUpdateTick, avgTripDurationTicks);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Entry that)) {
                return false;
            }
            return Double.compare(that.score, score) == 0
                    && lastUpdateTick == that.lastUpdateTick
                    && Double.compare(that.avgTripDurationTicks, avgTripDurationTicks) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(score, lastUpdateTick, avgTripDurationTicks);
        }
    }

    /**
     * Half-life for decaying this route's score: at least the config base, or trip duration × multiplier.
     */
    public static double entryHalfLifeTicks(double baseHalfLifeTicks, double avgTripDurationTicks,
            double fadeTripMultiplier) {
        if (baseHalfLifeTicks <= 0) {
            baseHalfLifeTicks = 1;
        }
        if (avgTripDurationTicks <= 0 || fadeTripMultiplier <= 0) {
            return baseHalfLifeTicks;
        }
        return Math.max(baseHalfLifeTicks, avgTripDurationTicks * fadeTripMultiplier);
    }

    private final Map<UUID, Entry> entries = new HashMap<>();
    private long lastDecayTick = -1L;

    public Map<UUID, Entry> snapshotEntries() {
        Map<UUID, Entry> copy = new HashMap<>();
        for (Map.Entry<UUID, Entry> e : entries.entrySet()) {
            copy.put(e.getKey(), e.getValue().copy());
        }
        return copy;
    }

    public long getLastDecayTick() {
        return lastDecayTick;
    }

    public void clear() {
        entries.clear();
        lastDecayTick = -1L;
    }

    public void removeDestination(UUID townId) {
        if (townId != null) {
            entries.remove(townId);
        }
    }

    public void applyDecay(long currentTick, double baseHalfLifeTicks, double fadeTripMultiplier,
            double pruneFloor) {
        if (baseHalfLifeTicks <= 0) {
            lastDecayTick = currentTick;
            return;
        }
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, Entry> e : entries.entrySet()) {
            Entry entry = e.getValue();
            long entryDelta = currentTick - entry.lastUpdateTick;
            if (entryDelta > 0) {
                double halfLife = entryHalfLifeTicks(baseHalfLifeTicks, entry.avgTripDurationTicks, fadeTripMultiplier);
                entry.score *= Math.pow(0.5, (double) entryDelta / halfLife);
            }
            entry.lastUpdateTick = currentTick;
            if (entry.score < pruneFloor) {
                toRemove.add(e.getKey());
            }
        }
        for (UUID id : toRemove) {
            entries.remove(id);
        }
        lastDecayTick = currentTick;
    }

    public void recordSuccess(UUID destinationTownId, long currentTick, double weight, long tripDurationTicks,
            int maxEntries, double pruneFloor) {
        if (destinationTownId == null || weight <= 0) {
            return;
        }
        Entry entry = entries.computeIfAbsent(destinationTownId, id -> new Entry(0, currentTick, 0));
        if (tripDurationTicks > 0) {
            if (entry.avgTripDurationTicks <= 0) {
                entry.avgTripDurationTicks = tripDurationTicks;
            } else {
                entry.avgTripDurationTicks = (1.0 - TRIP_DURATION_EMA_ALPHA) * entry.avgTripDurationTicks
                        + TRIP_DURATION_EMA_ALPHA * tripDurationTicks;
            }
        }
        entry.score += weight;
        entry.lastUpdateTick = currentTick;
        trimAndPrune(maxEntries, pruneFloor);
    }

    public void recordFailure(UUID destinationTownId, long currentTick, double penalty,
            int maxEntries, double pruneFloor) {
        if (destinationTownId == null || penalty <= 0) {
            return;
        }
        Entry entry = entries.get(destinationTownId);
        if (entry == null) {
            return;
        }
        entry.score = Math.max(0, entry.score - penalty);
        entry.lastUpdateTick = currentTick;
        if (entry.score < pruneFloor) {
            entries.remove(destinationTownId);
        } else {
            trimAndPrune(maxEntries, pruneFloor);
        }
    }

    private void trimAndPrune(int maxEntries, double pruneFloor) {
        entries.entrySet().removeIf(e -> e.getValue().score < pruneFloor);
        if (maxEntries <= 0 || entries.size() <= maxEntries) {
            return;
        }
        List<Map.Entry<UUID, Entry>> sorted = new ArrayList<>(entries.entrySet());
        sorted.sort(Comparator.comparingDouble((Map.Entry<UUID, Entry> e) -> e.getValue().score).reversed());
        for (int i = maxEntries; i < sorted.size(); i++) {
            entries.remove(sorted.get(i).getKey());
        }
    }

    public double totalScoreFor(java.util.Set<UUID> validDestinations, UUID originTownId) {
        double total = 0;
        for (Map.Entry<UUID, Entry> e : entries.entrySet()) {
            UUID dest = e.getKey();
            if (dest.equals(originTownId)) {
                continue;
            }
            if (validDestinations != null && !validDestinations.contains(dest)) {
                continue;
            }
            if (e.getValue().score > 0) {
                total += e.getValue().score;
            }
        }
        return total;
    }

    public List<Map.Entry<UUID, Double>> topRoutes(int limit, java.util.Set<UUID> validDestinations,
            UUID originTownId) {
        List<Map.Entry<UUID, Double>> list = new ArrayList<>();
        for (Map.Entry<UUID, Entry> e : entries.entrySet()) {
            UUID dest = e.getKey();
            if (dest.equals(originTownId)) {
                continue;
            }
            if (validDestinations != null && !validDestinations.contains(dest)) {
                continue;
            }
            double score = e.getValue().score;
            if (score > 0) {
                list.add(Map.entry(dest, score));
            }
        }
        list.sort(Comparator.comparingDouble(Map.Entry<UUID, Double>::getValue).reversed());
        if (limit > 0 && list.size() > limit) {
            return new ArrayList<>(list.subList(0, limit));
        }
        return list;
    }

    public double effectiveLearnedShare(double maxShare, double minScoreForLearning,
            java.util.Set<UUID> validDestinations, UUID originTownId) {
        if (maxShare <= 0) {
            return 0;
        }
        double total = totalScoreFor(validDestinations, originTownId);
        if (total < minScoreForLearning) {
            return 0;
        }
        double confidence = Math.min(1.0, total / minScoreForLearning);
        return Math.min(maxShare, maxShare * confidence);
    }

    public UUID pickWeightedDestination(Random random, java.util.Set<UUID> validDestinations, UUID originTownId) {
        double total = 0;
        List<UUID> ids = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        for (Map.Entry<UUID, Entry> e : entries.entrySet()) {
            UUID dest = e.getKey();
            if (dest.equals(originTownId)) {
                continue;
            }
            if (validDestinations != null && !validDestinations.contains(dest)) {
                continue;
            }
            double w = e.getValue().score;
            if (w <= 0) {
                continue;
            }
            ids.add(dest);
            weights.add(w);
            total += w;
        }
        if (total <= 0 || ids.isEmpty()) {
            return null;
        }
        double roll = random.nextDouble() * total;
        double cumulative = 0;
        for (int i = 0; i < ids.size(); i++) {
            cumulative += weights.get(i);
            if (roll <= cumulative) {
                return ids.get(i);
            }
        }
        return ids.get(ids.size() - 1);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("LastDecayTick", lastDecayTick);
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Entry> e : entries.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("Town", e.getKey());
            entryTag.putDouble("Score", e.getValue().score);
            entryTag.putLong("LastTick", e.getValue().lastUpdateTick);
            entryTag.putDouble("AvgTripTicks", e.getValue().avgTripDurationTicks);
            list.add(entryTag);
        }
        tag.put("Routes", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        entries.clear();
        if (tag == null || tag.isEmpty()) {
            lastDecayTick = -1L;
            return;
        }
        lastDecayTick = tag.contains("LastDecayTick") ? tag.getLong("LastDecayTick") : -1L;
        ListTag list = tag.getList("Routes", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            UUID town = entryTag.getUUID("Town");
            double score = entryTag.getDouble("Score");
            long tick = entryTag.contains("LastTick") ? entryTag.getLong("LastTick") : lastDecayTick;
            double avgTrip = entryTag.contains("AvgTripTicks") ? entryTag.getDouble("AvgTripTicks") : 0;
            entries.put(town, new Entry(score, tick, avgTrip));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PlatformLearnedRoutes that)) {
            return false;
        }
        return lastDecayTick == that.lastDecayTick && entries.equals(that.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entries, lastDecayTick);
    }
}
