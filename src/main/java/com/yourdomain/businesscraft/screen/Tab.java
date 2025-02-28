package com.yourdomain.businesscraft.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.narration.NarratedElementType;

/**
 * Base class for tabbed content in screens
 */
public abstract class Tab implements GuiEventListener, NarratableEntry {
    protected final Component title;
    protected int contentLeft;
    protected int contentTop;
    protected int contentWidth;
    protected int contentHeight;
    protected boolean visible = true;
    
    public Tab(Component title) {
        this.title = title;
    }
    
    /**
     * Initialize the tab with the parent screen dimensions
     */
    public void init(int leftPos, int topPos, int width, int height) {
        this.contentLeft = leftPos;
        this.contentTop = topPos;
        this.contentWidth = width;
        this.contentHeight = height;
    }
    
    /**
     * Render the tab contents
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;
        
        // Draw tab background
        graphics.fill(
            contentLeft, contentTop, 
            contentLeft + contentWidth, contentTop + contentHeight, 
            0x80000000
        );
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }
    
    @Override
    public boolean mouseReleased(double p_94695_, double p_94696_, int p_94697_) {
        return false;
    }
    
    @Override
    public boolean mouseDragged(double p_94680_, double p_94681_, int p_94682_, double p_94683_, double p_94684_) {
        return false;
    }
    
    @Override
    public boolean mouseScrolled(double p_94690_, double p_94691_, double p_94692_) {
        return false;
    }
    
    @Override
    public boolean keyPressed(int p_94685_, int p_94686_, int p_94687_) {
        return false;
    }
    
    @Override
    public boolean keyReleased(int p_94715_, int p_94716_, int p_94717_) {
        return false;
    }
    
    @Override
    public boolean charTyped(char p_94700_, int p_94701_) {
        return false;
    }
    
    @Override
    public NarrationPriority narrationPriority() {
        return visible ? NarrationPriority.HOVERED : NarrationPriority.NONE;
    }
    
    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
        if (visible) {
            narrationElementOutput.add(NarratedElementType.TITLE, title);
        }
    }
    
    /**
     * Sets visibility of the tab
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    /**
     * Gets visibility of the tab
     */
    public boolean isVisible() {
        return visible;
    }
    
    @Override
    public boolean isFocused() {
        return false;
    }
    
    @Override
    public void setFocused(boolean focused) {
        // No focus handling in base class
    }
} 