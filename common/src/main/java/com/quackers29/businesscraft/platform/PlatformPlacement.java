package com.quackers29.businesscraft.platform;

import com.quackers29.businesscraft.town.data.PlatformManager;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * Pure rules for placing a Platform Interface. The block asks this before it keeps the block in the world.
 * Boundary checks use the same 3D distance as town entry. Two interfaces of one town must be at least
 * {@link #MIN_SEPARATION} blocks apart (3D).
 */
public final class PlatformPlacement {
    public static final int MIN_SEPARATION = 5;

    public enum Reject {
        OUTSIDE("businesscraft.platform_interface.outside"),
        AT_CAP("businesscraft.platform_interface.at_cap"),
        TOO_CLOSE("businesscraft.platform_interface.too_close"),
        DUPLICATE("businesscraft.platform_interface.duplicate");

        private final String translationKey;

        Reject(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }
    }

    public record TownCandidate(UUID id, BlockPos centre, int radius, int platformCount,
            List<BlockPos> interfacePositions) {
    }

    public record Decision(UUID townId, BlockPos townCentre, Reject reject) {
        public boolean accepted() {
            return reject == null && townId != null;
        }
    }

    private PlatformPlacement() {
    }

    public static boolean tooClose(BlockPos a, BlockPos b) {
        if (a == null || b == null) {
            return false;
        }
        double dx = (double) a.getX() - b.getX();
        double dy = (double) a.getY() - b.getY();
        double dz = (double) a.getZ() - b.getZ();
        double min = MIN_SEPARATION;
        return dx * dx + dy * dy + dz * dz < min * min;
    }

    public static Decision evaluate(BlockPos pos, List<TownCandidate> towns) {
        if (pos == null) {
            return new Decision(null, null, Reject.OUTSIDE);
        }

        TownCandidate best = null;
        double bestDistance = Double.MAX_VALUE;
        if (towns != null) {
            for (TownCandidate town : towns) {
                if (town == null || town.centre() == null || town.id() == null) {
                    continue;
                }
                double distance = Math.sqrt(pos.distSqr(town.centre()));
                if (distance <= town.radius() && distance < bestDistance) {
                    best = town;
                    bestDistance = distance;
                }
            }
        }
        if (best == null) {
            return new Decision(null, null, Reject.OUTSIDE);
        }
        if (best.platformCount() >= PlatformManager.MAX_PLATFORMS) {
            return new Decision(best.id(), best.centre(), Reject.AT_CAP);
        }
        if (best.interfacePositions() != null) {
            for (BlockPos other : best.interfacePositions()) {
                if (other == null) {
                    continue;
                }
                if (other.equals(pos)) {
                    return new Decision(best.id(), best.centre(), Reject.DUPLICATE);
                }
                if (tooClose(pos, other)) {
                    return new Decision(best.id(), best.centre(), Reject.TOO_CLOSE);
                }
            }
        }
        return new Decision(best.id(), best.centre(), null);
    }
}
