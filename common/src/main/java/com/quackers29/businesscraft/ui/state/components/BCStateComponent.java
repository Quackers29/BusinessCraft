package com.quackers29.businesscraft.ui.state.components;

import com.quackers29.businesscraft.ui.components.basic.UIComponent;
import com.quackers29.businesscraft.ui.state.TownInterfaceState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Base component class that integrates with the state management system
 */
public abstract class BCStateComponent implements UIComponent {
    protected final TownInterfaceState state;
    protected boolean visible = true;
    protected int x, y;
    
    public BCStateComponent(TownInterfaceState state) {
        this.state = state;
    }
    
    @Override
    public void init(Consumer<Button> register) {
        // Components can override this to initialize their widgets
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        if (!visible) return;
        this.x = x;
        this.y = y;
        renderComponent(guiGraphics, mouseX, mouseY);
    }
    
    /**
     * Render the component's content
     * Components should override this instead of render()
     */
    protected abstract void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY);
    
    @Override
    public void tick() {
        // Components can override this for per-tick updates
    }
    
    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    @Override
    public boolean isVisible() {
        return visible;
    }
    
    @Override
    public int getX() {
        return x;
    }
    
    @Override
    public int getY() {
        return y;
    }
    
    /**
     * Helper method to create a button with state-aware behavior
     */
    protected Button createStateButton(String text, Consumer<Button> onPress) {
        return Button.builder(Component.literal(text), button -> onPress.accept(button))
            .pos(0, 0) // Position will be set in render
            .size(150, 20)
            .build();
    }
    
    /**
     * Helper method to create a button that updates state
     */
    protected Button createStateUpdateButton(String text, Runnable stateUpdate) {
        return createStateButton(text, button -> {
            stateUpdate.run();
        });
    }
    
    /**
     * Helper method to create a toggle button that updates state
     */
    protected Button createStateToggleButton(String text, Supplier<Boolean> stateGetter, Consumer<Boolean> stateSetter) {
        return createStateButton(text, button -> {
            stateSetter.accept(!stateGetter.get());
        });
    }
    
    /**
     * Helper method to create a button that updates state and performs an action
     */
    protected Button createStateActionButton(String text, Runnable stateUpdate, Runnable action) {
        return createStateButton(text, button -> {
            stateUpdate.run();
            action.run();
        });
    }
} 
