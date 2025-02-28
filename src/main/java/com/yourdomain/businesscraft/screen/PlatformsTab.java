package com.yourdomain.businesscraft.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.platform.Platform;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.network.AddPlatformPacket;
import com.yourdomain.businesscraft.network.DeletePlatformPacket;
import com.yourdomain.businesscraft.network.SetPlatformEnabledPacket;
import com.yourdomain.businesscraft.network.SetPlatformPathPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Tab for managing multiple tourist platforms
 */
public class PlatformsTab extends Tab {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation BUTTON_TEXTURES = new ResourceLocation(BusinessCraft.MOD_ID, "textures/gui/town_block_buttons.png");
    private static final ResourceLocation TOGGLE_TEXTURES = new ResourceLocation(BusinessCraft.MOD_ID, "textures/gui/toggle_buttons.png");
    
    private static final int PLATFORM_ENTRY_HEIGHT = 25;
    private static final int MAX_VISIBLE_PLATFORMS = 5;
    
    private final TownBlockScreen screen;
    private final List<PlatformEntry> platformEntries = new ArrayList<>();
    
    private Button addPlatformButton;
    private Button returnButton;
    private int scrollOffset = 0;
    private Button scrollUpButton;
    private Button scrollDownButton;
    
    public PlatformsTab(TownBlockScreen screen) {
        super(Component.translatable("businesscraft.platforms_tab"));
        this.screen = screen;
    }
    
    @Override
    public void init(int leftPos, int topPos, int width, int height) {
        super.init(leftPos, topPos, width, height);
        
        platformEntries.clear();
        
        // Return button - at the top right
        returnButton = Button.builder(Component.literal("<-"), (button) -> screen.showMainTab())
            .bounds(leftPos + width - 20, topPos + 5, 15, 15)
            .build();
        
        // Add platform button - centered if no platforms
        addPlatformButton = Button.builder(Component.literal("+"), this::onAddPlatform)
            .bounds(leftPos + (width / 2) - 10, topPos + 70, 20, 20)
            .tooltip(Tooltip.create(Component.translatable("businesscraft.add_platform")))
            .build();
        
        // Scroll buttons (only shown if needed)
        scrollUpButton = Button.builder(Component.literal("▲"), b -> scroll(-1))
            .bounds(leftPos + width - 20, topPos + 40, 15, 15)
            .build();
            
        scrollDownButton = Button.builder(Component.literal("▼"), b -> scroll(1))
            .bounds(leftPos + width - 20, topPos + 130, 15, 15)
            .build();
        
        refreshPlatforms();
    }
    
    private void scroll(int direction) {
        int newOffset = scrollOffset + direction;
        int maxOffset = Math.max(0, platformEntries.size() - MAX_VISIBLE_PLATFORMS);
        
        if (newOffset >= 0 && newOffset <= maxOffset) {
            scrollOffset = newOffset;
            updatePlatformPositions();
        }
    }
    
    private void updatePlatformPositions() {
        int startIndex = scrollOffset;
        int endIndex = Math.min(scrollOffset + MAX_VISIBLE_PLATFORMS, platformEntries.size());
        
        for (int i = 0; i < platformEntries.size(); i++) {
            PlatformEntry entry = platformEntries.get(i);
            if (i >= startIndex && i < endIndex) {
                int relativeIndex = i - startIndex;
                entry.setVisible(true);
                entry.updatePosition(contentLeft + 5, contentTop + 40 + (relativeIndex * PLATFORM_ENTRY_HEIGHT));
            } else {
                entry.setVisible(false);
            }
        }
    }
    
    public void refreshPlatforms() {
        platformEntries.clear();
        List<Platform> platforms = screen.getPlatforms();
        
        LOGGER.debug("Refreshing platforms tab with {} platforms", platforms.size());
        
        for (Platform platform : platforms) {
            LOGGER.debug("Adding platform '{}' (enabled: {}, id: {})", 
                platform.getName(), platform.isEnabled(), platform.getId());
            platformEntries.add(new PlatformEntry(platform));
        }
        
        scrollOffset = 0;
        updatePlatformPositions();
    }
    
    private void onAddPlatform(Button button) {
        LOGGER.debug("Adding new platform");
        ModMessages.sendToServer(new AddPlatformPacket(screen.getBlockPos()));
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        
        PoseStack poseStack = graphics.pose();
        
        // Title
        graphics.drawCenteredString(
            Minecraft.getInstance().font,
            Component.translatable("businesscraft.manage_platforms"),
            contentLeft + contentWidth / 2,
            contentTop + 10,
            0xFFFFFF
        );
        
        // Platform entries or "No platforms" message
        if (platformEntries.isEmpty()) {
            graphics.drawCenteredString(
                Minecraft.getInstance().font,
                Component.translatable("businesscraft.no_platforms"),
                contentLeft + contentWidth / 2,
                contentTop + 50,
                0xCCCCCC
            );
            
            addPlatformButton.render(graphics, mouseX, mouseY, partialTick);
        } else {
            LOGGER.debug("Rendering {} platform entries", platformEntries.size());
            
            // Draw list of platforms
            for (PlatformEntry entry : platformEntries) {
                if (entry.isVisible()) {
                    entry.render(graphics, mouseX, mouseY, partialTick);
                }
            }
            
            // Only show scroll buttons if needed
            if (platformEntries.size() > MAX_VISIBLE_PLATFORMS) {
                scrollUpButton.render(graphics, mouseX, mouseY, partialTick);
                scrollDownButton.render(graphics, mouseX, mouseY, partialTick);
            }
            
            // Draw add button at the bottom if there's room for more platforms
            if (platformEntries.size() < 5) {
                addPlatformButton.visible = true;
                addPlatformButton.setX(contentLeft + contentWidth - 25);
                addPlatformButton.setY(contentTop + contentHeight - 25);
                addPlatformButton.render(graphics, mouseX, mouseY, partialTick);
            } else {
                addPlatformButton.visible = false;
            }
        }
        
        returnButton.render(graphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (returnButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        if (addPlatformButton.visible && addPlatformButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        if (platformEntries.size() > MAX_VISIBLE_PLATFORMS) {
            if (scrollUpButton.mouseClicked(mouseX, mouseY, button) || 
                scrollDownButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        
        for (PlatformEntry entry : platformEntries) {
            if (entry.isVisible() && entry.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    /**
     * Represents a single platform entry in the list
     */
    private class PlatformEntry {
        private final Platform platform;
        private Button toggleButton;
        private Button setPathButton;
        private Button deleteButton;
        private boolean visible = true;
        
        public PlatformEntry(Platform platform) {
            this.platform = platform;
            
            int baseX = contentLeft + 5;
            int baseY = contentTop + 40;
            
            // Toggle button (on/off)
            toggleButton = new ImageButton(
                baseX + 140, baseY, 20, 20, 
                0, platform.isEnabled() ? 0 : 20, 20, 
                TOGGLE_TEXTURES, 64, 64, 
                button -> onToggle()
            );
            toggleButton.setTooltip(Tooltip.create(
                Component.translatable(platform.isEnabled() ? 
                    "businesscraft.enabled" : "businesscraft.disabled")
            ));
            
            // Set path button
            setPathButton = Button.builder(Component.translatable("businesscraft.set"), this::onSetPath)
                .bounds(baseX + 170, baseY, 30, 20)
                .tooltip(Tooltip.create(Component.translatable("businesscraft.set_platform_path")))
                .build();
            
            // Delete button
            deleteButton = Button.builder(Component.translatable("businesscraft.del"), this::onDelete)
                .bounds(baseX + 210, baseY, 30, 20)
                .tooltip(Tooltip.create(Component.translatable("businesscraft.delete_platform")))
                .build();
        }
        
        public void updatePosition(int x, int y) {
            toggleButton.setX(x + 140);
            toggleButton.setY(y);
            
            setPathButton.setX(x + 170);
            setPathButton.setY(y);
            
            deleteButton.setX(x + 210);
            deleteButton.setY(y);
        }
        
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!visible) return;
            
            // Draw platform name
            graphics.drawString(
                Minecraft.getInstance().font,
                platform.getName(),
                toggleButton.getX() - 130, 
                toggleButton.getY() + 5, 
                0xFFFFFF
            );
            
            // Draw buttons
            toggleButton.render(graphics, mouseX, mouseY, partialTick);
            setPathButton.render(graphics, mouseX, mouseY, partialTick);
            deleteButton.render(graphics, mouseX, mouseY, partialTick);
            
            // Optional: Show path status
            String pathStatus = platform.isComplete() ? 
                "✓" : "✗";
            int color = platform.isComplete() ? 0x55FF55 : 0xFF5555;
            
            graphics.drawString(
                Minecraft.getInstance().font,
                pathStatus,
                setPathButton.getX() - 10,
                setPathButton.getY() + 5,
                color
            );
        }
        
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!visible) return false;
            
            return toggleButton.mouseClicked(mouseX, mouseY, button) ||
                   setPathButton.mouseClicked(mouseX, mouseY, button) ||
                   deleteButton.mouseClicked(mouseX, mouseY, button);
        }
        
        private void onToggle() {
            LOGGER.debug("Toggling platform {} ({})", platform.getName(), platform.getId());
            
            // Update UI immediately for responsiveness
            platform.setEnabled(!platform.isEnabled());
            toggleButton.setTooltip(Tooltip.create(
                Component.translatable(platform.isEnabled() ? 
                    "businesscraft.enabled" : "businesscraft.disabled")
            ));
            
            // Update button texture (using a different approach since setTexture isn't available)
            toggleButton = new ImageButton(
                toggleButton.getX(), toggleButton.getY(), 20, 20,
                0, platform.isEnabled() ? 0 : 20, 20,
                TOGGLE_TEXTURES, 64, 64,
                button -> onToggle()
            );
            toggleButton.setTooltip(Tooltip.create(
                Component.translatable(platform.isEnabled() ? 
                    "businesscraft.enabled" : "businesscraft.disabled")
            ));
            
            // Send to server
            ModMessages.sendToServer(new SetPlatformEnabledPacket(
                screen.getBlockPos(), 
                platform.getId(), 
                platform.isEnabled()
            ));
        }
        
        private void onSetPath(Button button) {
            LOGGER.debug("Setting path for platform {} ({})", platform.getName(), platform.getId());
            screen.showPathEditor(platform.getId());
        }
        
        private void onDelete(Button button) {
            LOGGER.debug("Deleting platform {} ({})", platform.getName(), platform.getId());
            ModMessages.sendToServer(new DeletePlatformPacket(
                screen.getBlockPos(), 
                platform.getId()
            ));
        }
        
        public boolean isVisible() {
            return visible;
        }
        
        public void setVisible(boolean visible) {
            this.visible = visible;
            toggleButton.visible = visible;
            setPathButton.visible = visible;
            deleteButton.visible = visible;
        }
    }
} 