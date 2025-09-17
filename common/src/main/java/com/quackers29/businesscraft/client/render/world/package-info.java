/**
 * Modular 3D World Visualization Rendering System for BusinessCraft
 * 
 * This package provides a comprehensive, reusable framework for rendering 3D visualizations
 * in the Minecraft world, including lines, paths, boundaries, and other overlay graphics.
 * 
 * <h2>Core Components</h2>
 * 
 * <h3>LineRenderer3D</h3>
 * The foundation of the system, providing thick 3D rectangular prism line rendering:
 * <pre>{@code
 * // Simple line rendering
 * LineRenderer3D.renderLine(poseStack, startPos, endPos, LineRenderer3D.Color.GREEN);
 * 
 * // Advanced line with custom configuration
 * LineRenderer3D.LineConfig config = new LineRenderer3D.LineConfig()
 *     .thickness(0.1f)
 *     .yOffset(1.5f)
 *     .style(LineRenderer3D.LineStyle.SOLID);
 * LineRenderer3D.renderLine(poseStack, startPos, endPos, LineRenderer3D.Color.BLUE, config);
 * }</pre>
 * 
 * <h3>PathRenderer3D</h3>
 * Specialized renderer for multi-point paths with various interpolation methods:
 * <pre>{@code
 * // Simple path between two points
 * PathRenderer3D.renderPath(poseStack, startPos, endPos, LineRenderer3D.Color.GREEN);
 * 
 * // Multi-point path with custom configuration
 * List<BlockPos> waypoints = Arrays.asList(pos1, pos2, pos3, pos4);
 * PathRenderer3D.PathConfig pathConfig = new PathRenderer3D.PathConfig()
 *     .interpolation(PathRenderer3D.InterpolationType.STEPPED)
 *     .directionalIndicators(true, 5.0f);
 * PathRenderer3D.renderMultiPointPath(poseStack, waypoints, LineRenderer3D.Color.YELLOW, pathConfig);
 * }</pre>
 * 
 * <h3>BoundaryRenderer3D</h3>
 * Renderer for various boundary shapes around areas:
 * <pre>{@code
 * // Rectangular boundary
 * BoundaryRenderer3D.renderRectangularBoundary(poseStack, startPos, endPos, radius, LineRenderer3D.Color.ORANGE);
 * 
 * // Custom polygon boundary
 * List<Vec3> boundaryPoints = createCustomBoundary();
 * BoundaryRenderer3D.renderPolygonBoundary(poseStack, boundaryPoints, LineRenderer3D.Color.RED, config);
 * }</pre>
 * 
 * <h3>WorldVisualizationRenderer</h3>
 * Abstract base class for creating custom visualization renderers:
 * <pre>{@code
 * public class MyCustomRenderer extends WorldVisualizationRenderer {
 *     {@literal @}Override
 *     protected List<VisualizationData> getVisualizations(Level level, BlockPos playerPos) {
 *         // Return list of visualizations to render
 *         return myVisualizationList;
 *     }
 *     
 *     {@literal @}Override
 *     protected void renderVisualization(RenderLevelStageEvent event, VisualizationData visualization) {
 *         // Render individual visualization using the core renderers
 *         LineRenderer3D.renderLine(event.getPoseStack(), start, end, color);
 *     }
 * }
 * }</pre>
 * 
 * <h3>VisualizationManager</h3>
 * Central manager for tracking multiple visualization types with timing:
 * <pre>{@code
 * // Register a new visualization type
 * VisualizationManager.VisualizationTypeConfig config = 
 *     new VisualizationManager.VisualizationTypeConfig("my_visualization")
 *         .defaultDuration(1200) // 60 seconds
 *         .maxActive(50)
 *         .allowMultiple(true);
 * VisualizationManager.getInstance().registerVisualizationType(config);
 * 
 * // Show a visualization
 * VisualizationManager.getInstance().showVisualization("my_visualization", position, data);
 * 
 * // Check if visualization is active
 * boolean isActive = VisualizationManager.getInstance().shouldShowVisualization("my_visualization", position);
 * }</pre>
 * 
 * <h2>Integration with Forge Rendering</h2>
 * 
 * To integrate a custom renderer with the Forge rendering system:
 * <pre>{@code
 * // In your ClientRenderEvents class
 * private static final MyCustomRenderer myRenderer = new MyCustomRenderer();
 * 
 * static {
 *     VisualizationManager.getInstance().registerRenderer("my_type", myRenderer);
 * }
 * 
 * {@literal @}SubscribeEvent
 * public static void onRenderLevelStage(RenderLevelStageEvent event) {
 *     myRenderer.render(event);
 * }
 * }</pre>
 * 
 * <h2>Performance Considerations</h2>
 * 
 * <ul>
 * <li><strong>Distance Culling</strong>: Renderers automatically cull visualizations beyond configurable distances</li>
 * <li><strong>Chunk Loading</strong>: Only renders in loaded chunks to prevent crashes</li>
 * <li><strong>Batching</strong>: Multiple visualizations are batched for optimal performance</li>
 * <li><strong>Cleanup</strong>: Automatic cleanup of expired visualizations prevents memory leaks</li>
 * </ul>
 * 
 * <h2>Common Use Cases</h2>
 * 
 * <ul>
 * <li><strong>Platform Visualization</strong>: Shows platform paths and boundaries (implemented)</li>
 * <li><strong>Route Systems</strong>: Visualize transportation or logistics routes</li>
 * <li><strong>Debug Overlays</strong>: Development and debugging visualizations</li>
 * <li><strong>Territory Marking</strong>: Claim boundaries and area indicators</li>
 * <li><strong>Quest Systems</strong>: Path guidance and objective markers</li>
 * <li><strong>Building Guides</strong>: Construction assistance and planning</li>
 * </ul>
 * 
 * <h2>Extensibility</h2>
 * 
 * The system is designed for easy extension:
 * 
 * <ul>
 * <li>Add new line styles (dashed, animated) by extending LineRenderer3D</li>
 * <li>Create new boundary shapes by extending BoundaryRenderer3D</li>
 * <li>Implement new interpolation methods in PathRenderer3D</li>
 * <li>Build complex visualization systems using WorldVisualizationRenderer</li>
 * </ul>
 * 
 * @author BusinessCraft Development Team
 * @version 1.0
 * @since 1.20.1
 */
package com.quackers29.businesscraft.client.render.world;
