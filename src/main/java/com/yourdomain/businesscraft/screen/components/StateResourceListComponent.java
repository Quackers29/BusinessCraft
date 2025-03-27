package com.yourdomain.businesscraft.screen.components;

import com.yourdomain.businesscraft.screen.state.TownInterfaceState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.function.Consumer;

public class StateResourceListComponent extends BCStateComponent {
    private static final int ITEM_HEIGHT = 20;
    private static final int SCROLL_BUTTON_HEIGHT = 15;
    private static final int MAX_VISIBLE_ITEMS = 5;
    private static final int COMPONENT_WIDTH = 170; // 150 for list + 20 for scroll buttons
    private static final int COMPONENT_HEIGHT = MAX_VISIBLE_ITEMS * ITEM_HEIGHT;
    
    private int scrollOffset = 0;
    private Button scrollUpButton;
    private Button scrollDownButton;
    
    public StateResourceListComponent(TownInterfaceState state) {
        super(state);
    }
    
    @Override
    public void init(Consumer<Button> register) {
        scrollUpButton = createStateButton("▲", button -> {
            if (scrollOffset > 0) {
                scrollOffset--;
            }
        });
        scrollDownButton = createStateButton("▼", button -> {
            if (canScrollDown()) {
                scrollOffset++;
            }
        });
        
        register.accept(scrollUpButton);
        register.accept(scrollDownButton);
    }
    
    @Override
    protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Map<String, Integer> resources = state.getResources();
        List<Map.Entry<String, Integer>> resourceList = new ArrayList<>(resources.entrySet());
        
        // Update scroll buttons visibility
        scrollUpButton.visible = scrollOffset > 0;
        scrollDownButton.visible = canScrollDown();
        
        // Position scroll buttons
        scrollUpButton.setPosition(x + 150, y);
        scrollDownButton.setPosition(x + 150, y + (MAX_VISIBLE_ITEMS * ITEM_HEIGHT) - SCROLL_BUTTON_HEIGHT);
        
        // Render resource items
        int visibleCount = Math.min(MAX_VISIBLE_ITEMS, resourceList.size() - scrollOffset);
        for (int i = 0; i < visibleCount; i++) {
            Map.Entry<String, Integer> resource = resourceList.get(i + scrollOffset);
            renderResourceItem(guiGraphics, x, y + (i * ITEM_HEIGHT), resource);
        }
    }
    
    private void renderResourceItem(GuiGraphics guiGraphics, int x, int y, Map.Entry<String, Integer> resource) {
        // Render resource name and amount
        guiGraphics.drawString(
            Minecraft.getInstance().font,
            resource.getKey() + ": " + resource.getValue(),
            x,
            y + 4,
            0xFFFFFF
        );
    }
    
    private boolean canScrollDown() {
        return scrollOffset < Math.max(0, state.getResources().size() - MAX_VISIBLE_ITEMS);
    }
    
    @Override
    public int getWidth() {
        return COMPONENT_WIDTH;
    }
    
    @Override
    public int getHeight() {
        return COMPONENT_HEIGHT;
    }
} 