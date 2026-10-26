package com.quackers29.businesscraft.platform;

import net.minecraft.core.BlockPos;

/**
 * Pure spawn-area rules for one platform. Search radius is how far visitors and vehicles are
 * noticed around the path. Spread is how far a new tourist may stand off the path line.
 * Spread is never larger than the search radius. A spread of 0 stands on the line.
 */
public final class PlatformSpawn {
    public static final int MIN_RADIUS = 1;
    public static final int MAX_RADIUS = 100;
    public static final int DEFAULT_RADIUS = 10;
    public static final int DEFAULT_SPREAD = 0;

    private PlatformSpawn() {
    }

    public static int clampRadius(int radius) {
        return Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, radius));
    }

    public static int clampSpread(int spread, int radius) {
        int cap = clampRadius(radius);
        if (spread < 0) {
            return 0;
        }
        return Math.min(spread, cap);
    }

    /**
     * A point on the path segment, then shifted sideways by {@code lateral} blocks.
     * Progress is clamped to the segment. Lateral is clamped to {@code +/- spread}, and spread
     * itself is clamped to the search radius. A zero-length path ignores lateral.
     */
    public static double[] point(BlockPos start, BlockPos end, double progress, double lateral, int spread, int radius) {
        int allowed = clampSpread(spread, radius);
        double side = lateral;
        if (side > allowed) {
            side = allowed;
        } else if (side < -allowed) {
            side = -allowed;
        }
        double t = progress;
        if (t < 0) {
            t = 0;
        } else if (t > 1) {
            t = 1;
        }
        double dx = end.getX() - start.getX();
        double dz = end.getZ() - start.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        double x = start.getX() + dx * t;
        double z = start.getZ() + dz * t;
        if (length < 1.0e-6 || side == 0) {
            return new double[] { x, z };
        }
        double nx = -dz / length;
        double nz = dx / length;
        return new double[] { x + nx * side, z + nz * side };
    }

    /**
     * Perpendicular distance from {@code (x, z)} to the path segment. Past either end, the
     * distance is to that end, so a point off the segment is not treated as on the line.
     */
    public static double distanceToSegment(BlockPos start, BlockPos end, double x, double z) {
        double dx = end.getX() - start.getX();
        double dz = end.getZ() - start.getZ();
        double lengthSq = dx * dx + dz * dz;
        double t = 0;
        if (lengthSq > 1.0e-8) {
            t = ((x - start.getX()) * dx + (z - start.getZ()) * dz) / lengthSq;
            if (t < 0) {
                t = 0;
            } else if (t > 1) {
                t = 1;
            }
        }
        double px = start.getX() + dx * t;
        double pz = start.getZ() + dz * t;
        double ox = x - px;
        double oz = z - pz;
        return Math.sqrt(ox * ox + oz * oz);
    }
}
