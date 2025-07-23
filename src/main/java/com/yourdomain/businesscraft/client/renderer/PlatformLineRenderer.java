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
     * Renders a solid line between two positions in world space
     * @param poseStack The pose stack for transformations
     * @param startPos Starting position
     * @param endPos Ending position
     * @param red Red component (0.0 - 1.0)
     * @param green Green component (0.0 - 1.0)
     * @param blue Blue component (0.0 - 1.0)
     * @param alpha Alpha component (0.0 - 1.0)
     * @param lineWidth Width of the line
     */
    public static void renderLine(PoseStack poseStack, BlockPos startPos, BlockPos endPos,
                                float red, float green, float blue, float alpha, float lineWidth) {
        
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        
        // Convert block positions to world coordinates, offset by camera position
        double startX = startPos.getX() + 0.5 - cameraPos.x;
        double startY = startPos.getY() + 1.0 - cameraPos.y;
        double startZ = startPos.getZ() + 0.5 - cameraPos.z;
        double endX = endPos.getX() + 0.5 - cameraPos.x;
        double endY = endPos.getY() + 1.0 - cameraPos.y;
        double endZ = endPos.getZ() + 0.5 - cameraPos.z;
        
        // Setup rendering state
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.lineWidth(lineWidth);
        
        // Create buffer and tessellator
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        
        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();
        
        // Render the line
        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, (float) startX, (float) startY, (float) startZ).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, (float) endX, (float) endY, (float) endZ).color(red, green, blue, alpha).endVertex();
        tesselator.end();
        
        poseStack.popPose();
        
        // Restore rendering state
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.lineWidth(1.0f);
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
        
        // Use a fixed Y for the boundary visualization
        double boundaryY = Math.min(startPos.getY(), endPos.getY()) + 1.0;
        
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        
        // Setup rendering state
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.lineWidth(lineWidth);
        
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        
        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();
        
        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        
        // Bottom edge (minX to maxX at minZ)
        for (int x = minX; x < maxX; x++) {
            float startX = (float) (x + 0.5 - cameraPos.x);
            float startZ = (float) (minZ + 0.5 - cameraPos.z);
            float endX = (float) (x + 1.5 - cameraPos.x);
            float endZ = (float) (minZ + 0.5 - cameraPos.z);
            float y = (float) (boundaryY - cameraPos.y);
            
            buffer.vertex(matrix, startX, y, startZ).color(red, green, blue, alpha).endVertex();
            buffer.vertex(matrix, endX, y, endZ).color(red, green, blue, alpha).endVertex();
        }
        
        // Top edge (minX to maxX at maxZ)
        for (int x = minX; x < maxX; x++) {
            float startX = (float) (x + 0.5 - cameraPos.x);
            float startZ = (float) (maxZ + 0.5 - cameraPos.z);
            float endX = (float) (x + 1.5 - cameraPos.x);
            float endZ = (float) (maxZ + 0.5 - cameraPos.z);
            float y = (float) (boundaryY - cameraPos.y);
            
            buffer.vertex(matrix, startX, y, startZ).color(red, green, blue, alpha).endVertex();
            buffer.vertex(matrix, endX, y, endZ).color(red, green, blue, alpha).endVertex();
        }
        
        // Left edge (minZ to maxZ at minX)
        for (int z = minZ; z < maxZ; z++) {
            float startX = (float) (minX + 0.5 - cameraPos.x);
            float startZ = (float) (z + 0.5 - cameraPos.z);
            float endX = (float) (minX + 0.5 - cameraPos.x);
            float endZ = (float) (z + 1.5 - cameraPos.z);
            float y = (float) (boundaryY - cameraPos.y);
            
            buffer.vertex(matrix, startX, y, startZ).color(red, green, blue, alpha).endVertex();
            buffer.vertex(matrix, endX, y, endZ).color(red, green, blue, alpha).endVertex();
        }
        
        // Right edge (minZ to maxZ at maxX)
        for (int z = minZ; z < maxZ; z++) {
            float startX = (float) (maxX + 0.5 - cameraPos.x);
            float startZ = (float) (z + 0.5 - cameraPos.z);
            float endX = (float) (maxX + 0.5 - cameraPos.x);
            float endZ = (float) (z + 1.5 - cameraPos.z);
            float y = (float) (boundaryY - cameraPos.y);
            
            buffer.vertex(matrix, startX, y, startZ).color(red, green, blue, alpha).endVertex();
            buffer.vertex(matrix, endX, y, endZ).color(red, green, blue, alpha).endVertex();
        }
        
        tesselator.end();
        poseStack.popPose();
        
        // Restore rendering state
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.lineWidth(1.0f);
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
        
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        
        // Setup rendering state
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.lineWidth(lineWidth);
        
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        
        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();
        
        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        
        // Generate connected line segments along the path
        for (int i = 0; i < maxSteps; i++) {
            double t1 = (double) i / maxSteps;
            double t2 = (double) (i + 1) / maxSteps;
            
            int x1_pos = x0 + (int) Math.round(t1 * (x1 - x0));
            int y1_pos = y0 + (int) Math.round(t1 * (y1 - y0));
            int z1_pos = z0 + (int) Math.round(t1 * (z1 - z0));
            
            int x2_pos = x0 + (int) Math.round(t2 * (x1 - x0));
            int y2_pos = y0 + (int) Math.round(t2 * (y1 - y0));
            int z2_pos = z0 + (int) Math.round(t2 * (z1 - z0));
            
            // Convert to world coordinates relative to camera
            float startX = (float) (x1_pos + 0.5 - cameraPos.x);
            float startY = (float) (y1_pos + 1.0 - cameraPos.y);
            float startZ = (float) (z1_pos + 0.5 - cameraPos.z);
            float endX = (float) (x2_pos + 0.5 - cameraPos.x);
            float endY = (float) (y2_pos + 1.0 - cameraPos.y);
            float endZ = (float) (z2_pos + 0.5 - cameraPos.z);
            
            buffer.vertex(matrix, startX, startY, startZ).color(red, green, blue, alpha).endVertex();
            buffer.vertex(matrix, endX, endY, endZ).color(red, green, blue, alpha).endVertex();
        }
        
        tesselator.end();
        poseStack.popPose();
        
        // Restore rendering state
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.lineWidth(1.0f);
    }
}