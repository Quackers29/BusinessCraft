package com.yourdomain.businesscraft.client.renderer;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Client-side renderer for platform visualization lines.
 * Renders solid lines in 3D world space for platform paths and boundaries.
 */
public class PlatformLineRenderer {
    
    /**
     * Renders a thick solid line between two positions in world space using multiple parallel lines
     * @param poseStack The pose stack for transformations
     * @param startPos Starting position
     * @param endPos Ending position
     * @param red Red component (0.0 - 1.0)
     * @param green Green component (0.0 - 1.0)
     * @param blue Blue component (0.0 - 1.0)
     * @param alpha Alpha component (0.0 - 1.0)
     * @param lineWidth Width of the line (ignored, using fixed thick rendering)
     */
    public static void renderLine(PoseStack poseStack, BlockPos startPos, BlockPos endPos,
                                float red, float green, float blue, float alpha, float lineWidth) {
        
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        
        // Convert block positions to world coordinates, offset by camera position
        // Adding 1.1 to Y to ensure lines appear 0.1 blocks above the actual block surface
        double startX = startPos.getX() + 0.5 - cameraPos.x;
        double startY = startPos.getY() + 1.1 - cameraPos.y; // Fixed positioning: +1.1 for 0.1 above block
        double startZ = startPos.getZ() + 0.5 - cameraPos.z;
        double endX = endPos.getX() + 0.5 - cameraPos.x;
        double endY = endPos.getY() + 1.1 - cameraPos.y; // Fixed positioning: +1.1 for 0.1 above block
        double endZ = endPos.getZ() + 0.5 - cameraPos.z;
        
        // Setup rendering state
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();

        // Create buffer and tessellator
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();

        // Calculate direction and perpendicular for quad thickness
        double thickness = lineWidth / 10.0; // Convert to world units, adjust scale as needed
        Vec3 direction = new Vec3(endX - startX, endY - startY, endZ - startZ).normalize();
        Vec3 perp = new Vec3(-direction.z, 0, direction.x).normalize().scale(thickness / 2.0);

        // Calculate quad vertices
        Vec3 v1 = new Vec3(startX, startY, startZ).add(perp);
        Vec3 v2 = new Vec3(startX, startY, startZ).subtract(perp);
        Vec3 v3 = new Vec3(endX, endY, endZ).subtract(perp);
        Vec3 v4 = new Vec3(endX, endY, endZ).add(perp);

        // Render as quad
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        buffer.vertex(matrix, (float)v1.x, (float)v1.y, (float)v1.z).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, (float)v2.x, (float)v2.y, (float)v2.z).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, (float)v3.x, (float)v3.y, (float)v3.z).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, (float)v4.x, (float)v4.y, (float)v4.z).color(red, green, blue, alpha).endVertex();

        tesselator.end();
        poseStack.popPose();

        // Restore rendering state
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }
    
    /**
     * Renders a solid rectangular boundary around a platform area
     * @param poseStack The pose stack for transformations
     * @param startPos Platform start position
     * @param endPos Platform end position
     * @param radius Boundary radius
     * @param red Red component (0.0 - 1.0)
     * @param green Green component (0.0 - 1.0)
     * @param blue Blue component (0.0 - 1.0)
     * @param alpha Alpha component (0.0 - 1.0)
     * @param lineWidth Width of the boundary lines
     */
    public static void renderBoundary(PoseStack poseStack, BlockPos startPos, BlockPos endPos, int radius,
                                    float red, float green, float blue, float alpha, float lineWidth) {
        
        // Calculate bounding box the same way as the particle system
        int minX = Math.min(startPos.getX(), endPos.getX()) - radius;
        int minZ = Math.min(startPos.getZ(), endPos.getZ()) - radius;
        int maxX = Math.max(startPos.getX(), endPos.getX()) + radius;
        int maxZ = Math.max(startPos.getZ(), endPos.getZ()) + radius;
        
        // Use the same Y level as the platform positions for consistency
        // This ensures boundary lines appear at the same height as path lines
        int platformY = Math.min(startPos.getY(), endPos.getY());
        
        // Use thick line rendering for each boundary segment
        // Bottom edge (minX to maxX at minZ)
        for (int x = minX; x < maxX; x++) {
            BlockPos segmentStart = new BlockPos(x, platformY, minZ);
            BlockPos segmentEnd = new BlockPos(x + 1, platformY, minZ);
            renderLine(poseStack, segmentStart, segmentEnd, red, green, blue, alpha, lineWidth);
        }
        
        // Top edge (minX to maxX at maxZ)
        for (int x = minX; x < maxX; x++) {
            BlockPos segmentStart = new BlockPos(x, platformY, maxZ);
            BlockPos segmentEnd = new BlockPos(x + 1, platformY, maxZ);
            renderLine(poseStack, segmentStart, segmentEnd, red, green, blue, alpha, lineWidth);
        }
        
        // Left edge (minZ to maxZ at minX)
        for (int z = minZ; z < maxZ; z++) {
            BlockPos segmentStart = new BlockPos(minX, platformY, z);
            BlockPos segmentEnd = new BlockPos(minX, platformY, z + 1);
            renderLine(poseStack, segmentStart, segmentEnd, red, green, blue, alpha, lineWidth);
        }
        
        // Right edge (minZ to maxZ at maxX)
        for (int z = minZ; z < maxZ; z++) {
            BlockPos segmentStart = new BlockPos(maxX, platformY, z);
            BlockPos segmentEnd = new BlockPos(maxX, platformY, z + 1);
            renderLine(poseStack, segmentStart, segmentEnd, red, green, blue, alpha, lineWidth);
        }
    }
    
    /**
     * Renders a complete platform path as a series of connected line segments
     * @param poseStack The pose stack for transformations
     * @param startPos Platform start position
     * @param endPos Platform end position
     * @param red Red component (0.0 - 1.0)
     * @param green Green component (0.0 - 1.0)
     * @param blue Blue component (0.0 - 1.0)
     * @param alpha Alpha component (0.0 - 1.0)
     * @param lineWidth Width of the path line
     */
    public static void renderPath(PoseStack poseStack, BlockPos startPos, BlockPos endPos,
                                float red, float green, float blue, float alpha, float lineWidth) {
        
        // Calculate path points using the same algorithm as the particle system
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
            renderLine(poseStack, startPos, startPos, red, green, blue, alpha, lineWidth);
            return;
        }
        
        // Generate connected thick line segments along the path using renderLine
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
            renderLine(poseStack, segmentStart, segmentEnd, red, green, blue, alpha, lineWidth);
        }
    }
}