package com.yourdomain.businesscraft.ui.state.components;

import com.yourdomain.businesscraft.ui.state.TownInterfaceState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import java.util.List;
import java.util.function.Consumer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StateVisitHistoryComponent extends BCStateComponent {
    private static final int ITEM_HEIGHT = 20;
    private static final int SCROLL_BUTTON_HEIGHT = 15;
    private static final int MAX_VISIBLE_ITEMS = 5;
    private static final int COMPONENT_WIDTH = 170;
    private static final int COMPONENT_HEIGHT = MAX_VISIBLE_ITEMS * ITEM_HEIGHT;
    
    private int scrollOffset = 0;
    private Button scrollUpButton;
    private Button scrollDownButton;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm");
    
    public StateVisitHistoryComponent(TownInterfaceState state) {
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
        List<TownInterfaceState.VisitHistoryEntry> history = state.getVisitHistory();
        
        // Update scroll buttons visibility
        scrollUpButton.visible = scrollOffset > 0;
        scrollDownButton.visible = canScrollDown();
        
        // Position scroll buttons
        scrollUpButton.setPosition(x + 150, y);
        scrollDownButton.setPosition(x + 150, y + (MAX_VISIBLE_ITEMS * ITEM_HEIGHT) - SCROLL_BUTTON_HEIGHT);
        
        // Render history items
        int visibleCount = Math.min(MAX_VISIBLE_ITEMS, history.size() - scrollOffset);
        for (int i = 0; i < visibleCount; i++) {
            TownInterfaceState.VisitHistoryEntry entry = history.get(i + scrollOffset);
            renderHistoryItem(guiGraphics, x, y + (i * ITEM_HEIGHT), entry);
        }
    }
    
    private void renderHistoryItem(GuiGraphics guiGraphics, int x, int y, TownInterfaceState.VisitHistoryEntry entry) {
        // Render town name and direction
        String townInfo = entry.getTownName() + " (" + entry.getDirection() + ")";
        guiGraphics.drawString(
            Minecraft.getInstance().font,
            townInfo,
            x,
            y + 4,
            0xFFFFFF
        );
        
        // Render timestamp and count
        String timestamp = dateFormat.format(new Date(entry.getTimestamp()));
        String countInfo = timestamp + " - " + entry.getCount() + " visitors";
        guiGraphics.drawString(
            Minecraft.getInstance().font,
            countInfo,
            x,
            y + 14,
            0x808080
        );
    }
    
    private boolean canScrollDown() {
        return scrollOffset < Math.max(0, state.getVisitHistory().size() - MAX_VISIBLE_ITEMS);
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