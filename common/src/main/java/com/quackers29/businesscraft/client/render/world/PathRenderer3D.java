package com.quackers29.businesscraft.client.render.world;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;

/**
 * Specialized renderer for 3D path visualization.
 *
 * Uses {@link LineRenderer3D} for actual line rendering while providing
 * path interpolation between two points.
 */
public class PathRenderer3D {

    public enum InterpolationType {
        LINEAR,
        STEPPED
    }

    public static class PathConfig {
        private InterpolationType interpolation = InterpolationType.STEPPED;
        private LineRenderer3D.LineConfig lineConfig = new LineRenderer3D.LineConfig();

        public PathConfig interpolation(InterpolationType interpolation) {
            this.interpolation = interpolation;
            return this;
        }

        public PathConfig lineConfig(LineRenderer3D.LineConfig lineConfig) {
            this.lineConfig = lineConfig;
            return this;
        }

        public InterpolationType getInterpolation() { return interpolation; }
        public LineRenderer3D.LineConfig getLineConfig() { return lineConfig; }
    }

    public static void renderPath(PoseStack poseStack, BlockPos startPos, BlockPos endPos, 
                                LineRenderer3D.Color color) {
        renderPath(poseStack, startPos, endPos, color, new PathConfig());
    }

    public static void renderPath(PoseStack poseStack, BlockPos startPos, BlockPos endPos, 
                                LineRenderer3D.Color color, PathConfig config) {

        switch (config.getInterpolation()) {
            case LINEAR:
                renderLinearPath(poseStack, startPos, endPos, color, config);
                break;
            case STEPPED:
                renderSteppedPath(poseStack, startPos, endPos, color, config);
                break;
        }
    }

    private static void renderLinearPath(PoseStack poseStack, BlockPos startPos, BlockPos endPos, 
                                       LineRenderer3D.Color color, PathConfig config) {
        LineRenderer3D.renderLine(poseStack, startPos, endPos, color, config.getLineConfig());
    }

    private static void renderSteppedPath(PoseStack poseStack, BlockPos startPos, BlockPos endPos, 
                                        LineRenderer3D.Color color, PathConfig config) {
        
        // Calculate path points using the same algorithm as the original platform system
        int x0 = startPos.getX();
        int y0 = startPos.getY();
        int z0 = startPos.getZ();
        int x1 = endPos.getX();
        int y1 = endPos.getY();
        int z1 = endPos.getZ();
        
        // Calculate differences
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int dz = Math.abs(z1 - z0);
        
        // Find the maximum difference to determine number of steps
        int maxSteps = Math.max(Math.max(dx, dy), dz);

        if (maxSteps == 0) {
            // Single point, just render a small line
            LineRenderer3D.renderLine(poseStack, startPos, startPos, color, config.getLineConfig());
            return;
        }
        
        // Generate connected thick line segments along the path
        for (int i = 0; i < maxSteps; i++) {
            double t1 = (double) i / maxSteps;
            double t2 = (double) (i + 1) / maxSteps;
            
            int x1_pos = x0 + (int) Math.round(t1 * (x1 - x0));
            int y1_pos = y0 + (int) Math.round(t1 * (y1 - y0));
            int z1_pos = z0 + (int) Math.round(t1 * (z1 - z0));
            
            int x2_pos = x0 + (int) Math.round(t2 * (x1 - x0));
            int y2_pos = y0 + (int) Math.round(t2 * (y1 - y0));
            int z2_pos = z0 + (int) Math.round(t2 * (z1 - z0));
            
            // Use thick line rendering for each segment
            BlockPos segmentStart = new BlockPos(x1_pos, y1_pos, z1_pos);
            BlockPos segmentEnd = new BlockPos(x2_pos, y2_pos, z2_pos);
            LineRenderer3D.renderLine(poseStack, segmentStart, segmentEnd, color, config.getLineConfig());
        }
    }
}
