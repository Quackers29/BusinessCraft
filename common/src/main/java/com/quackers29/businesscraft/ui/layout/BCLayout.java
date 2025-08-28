package com.quackers29.businesscraft.ui.layout;

import com.quackers29.businesscraft.ui.components.basic.BCPanel;
import com.quackers29.businesscraft.ui.components.basic.UIComponent;

import com.quackers29.businesscraft.ui.components.basic.BCPanel;
import com.quackers29.businesscraft.ui.components.basic.UIComponent;
import java.util.List;

/**
 * Layout manager interface for BusinessCraft UI system.
 * Handles arranging components within a container.
 */
public interface BCLayout {
    /**
     * Layout components within the given container
     * @param container The containing panel
     * @param components The components to layout
     */
    void layout(BCPanel container, List<UIComponent> components);
}