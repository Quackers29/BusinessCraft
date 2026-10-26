package com.quackers29.businesscraft.platform;

import com.quackers29.businesscraft.config.ConfigLoader;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformSpawnTest {
    private static final BlockPos START = new BlockPos(0, 64, 0);
    private static final BlockPos END = new BlockPos(20, 64, 0);
    private static final BlockPos DIAGONAL_END = new BlockPos(12, 64, 9);

    @Test
    void spreadZero_standsOnTheLine() {
        for (double progress = 0; progress <= 1; progress += 0.25) {
            double[] point = PlatformSpawn.point(START, END, progress, 6, 0, 10);
            assertEquals(0, PlatformSpawn.distanceToSegment(START, END, point[0], point[1]), 1.0e-6);
            assertEquals(START.getX() + 20 * progress, point[0], 1.0e-6);
            assertEquals(0, point[1], 1.0e-6);
        }
    }

    @Test
    void spread_neverLeavesTheAllowedBand() {
        double[] east = PlatformSpawn.point(START, END, 0.4, 3, 4, 10);
        assertEquals(3, PlatformSpawn.distanceToSegment(START, END, east[0], east[1]), 1.0e-6);
        double[] west = PlatformSpawn.point(START, END, 0.4, -9, 4, 10);
        assertEquals(4, PlatformSpawn.distanceToSegment(START, END, west[0], west[1]), 1.0e-6);
        double[] ends = PlatformSpawn.point(START, END, 1.4, 2, 4, 10);
        assertEquals(END.getX(), ends[0], 1.0e-6);
        assertEquals(2, Math.abs(ends[1]), 1.0e-6);
    }

    @Test
    void spread_clampsToSearchRadius() {
        assertEquals(1, PlatformSpawn.clampRadius(0));
        assertEquals(100, PlatformSpawn.clampRadius(500));
        assertEquals(10, PlatformSpawn.clampSpread(50, 10));
        assertEquals(0, PlatformSpawn.clampSpread(-4, 10));
        assertEquals(100, PlatformSpawn.clampSpread(150, 400));

        double[] clamped = PlatformSpawn.point(START, END, 0.5, 40, 40, 10);
        assertEquals(10, PlatformSpawn.distanceToSegment(START, END, clamped[0], clamped[1]), 1.0e-6);
    }

    @Test
    void clampRadius_readsTheConfiguredMax() {
        int saved = ConfigLoader.maxPlatformSearchRadius;
        try {
            ConfigLoader.maxPlatformSearchRadius = 40;
            assertEquals(40, PlatformSpawn.maxRadius());
            assertEquals(40, PlatformSpawn.clampRadius(500));
            assertEquals(1, PlatformSpawn.clampRadius(0));
            assertEquals(40, PlatformSpawn.clampSpread(90, 80));
            assertEquals(12, PlatformSpawn.clampSpread(12, 40));

            ConfigLoader.maxPlatformSearchRadius = 0;
            assertEquals(PlatformSpawn.DEFAULT_MAX_RADIUS, PlatformSpawn.maxRadius());
            assertEquals(100, PlatformSpawn.clampRadius(500));
        } finally {
            ConfigLoader.maxPlatformSearchRadius = saved;
        }
    }

    @Test
    void samples_stayInsideTheSegmentAndTheSpread() {
        Random random = new Random(7);
        int spread = 5;
        int radius = 12;
        int offLine = 0;
        int left = 0;
        int right = 0;
        for (int i = 0; i < 300; i++) {
            double progress = random.nextDouble();
            double lateral = (random.nextDouble() * 2 - 1) * spread;
            double[] point = PlatformSpawn.point(DIAGONAL_END.equals(START) ? START : START, DIAGONAL_END,
                    progress, lateral, spread, radius);
            double distance = PlatformSpawn.distanceToSegment(START, DIAGONAL_END, point[0], point[1]);
            assertTrue(distance <= spread + 1.0e-6, "distance " + distance);
            if (distance > spread * 0.5) {
                offLine++;
            }
            double cross = cross(START, DIAGONAL_END, point[0], point[1]);
            if (cross > 0.2) {
                left++;
            } else if (cross < -0.2) {
                right++;
            }
        }
        assertTrue(offLine > 20, "expected a spread of distances, got " + offLine);
        assertTrue(left > 20 && right > 20, "expected both sides, left=" + left + " right=" + right);
    }

    @Test
    void zeroLengthPath_ignoresLateral() {
        BlockPos point = new BlockPos(4, 64, 4);
        double[] spawn = PlatformSpawn.point(point, point, 0.3, 8, 8, 10);
        assertEquals(4, spawn[0], 1.0e-6);
        assertEquals(4, spawn[1], 1.0e-6);
    }

    private static double cross(BlockPos start, BlockPos end, double x, double z) {
        double dx = end.getX() - start.getX();
        double dz = end.getZ() - start.getZ();
        return dx * (z - start.getZ()) - dz * (x - start.getX());
    }
}
