package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TabComponent implements UIComponent {
    private final List<Tab> tabs = new ArrayList<>();
    private int activeTab = 0;
    private final int width;
    private final int height;
    private String activeTabId = "town";
    private boolean visible = true;

    public TabComponent(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void addTab(String id, Component title, List<UIComponent> components) {
        tabs.add(new Tab(id, title, components));
        if (tabs.size() == 1) activeTabId = id;
    }

    @Override
    public void init(Consumer<Button> register) {
        int tabWidth = width / tabs.size();
        for (int i = 0; i < tabs.size(); i++) {
            final int tabIndex = i;
            Tab tab = tabs.get(i);
            tab.button = Button.builder(tab.title, b -> {
                // Hide previous tab components
                tabs.get(activeTab).components.forEach(c -> 
                    c.setVisible(false));
                
                activeTab = tabIndex;
                activeTabId = tabs.get(tabIndex).id;
                
                // Only initialize components once
                if (!tab.initialized) {
                    tab.components.forEach(c -> c.init(register));
                    tab.initialized = true;
                }
                tab.components.forEach(c -> c.setVisible(true));
            })
            .pos(0, 0)
            .size(tabWidth - 2, height)
            .build();
            register.accept(tab.button);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        int tabWidth = width / tabs.size();
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            tab.button.setX(x + i * tabWidth);
            tab.button.setY(y);
            tab.button.render(guiGraphics, mouseX, mouseY, 0);
        }
    }

    public List<UIComponent> getActiveComponents() {
        return tabs.get(activeTab).components;
    }

    public String getActiveTabId() {
        return tabs.get(activeTab).id;
    }

    @Override public void tick() {}
    @Override public int getWidth() { return width; }
    @Override public int getHeight() { return height; }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    private static class Tab {
        final String id;
        final Component title;
        final List<UIComponent> components;
        Button button;
        boolean initialized = false;

        Tab(String id, Component title, List<UIComponent> components) {
            this.id = id;
            this.title = title;
            this.components = components;
        }
    }
} 