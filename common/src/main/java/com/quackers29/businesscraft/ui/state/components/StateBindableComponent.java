package com.quackers29.businesscraft.ui.state.components;

import com.quackers29.businesscraft.ui.components.basic.BCComponent;
import com.quackers29.businesscraft.ui.state.TownInterfaceState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import java.util.function.Consumer;
import com.quackers29.businesscraft.ui.components.basic.BCComponent;
import com.quackers29.businesscraft.ui.components.basic.BCButton;
import java.util.function.Function;
import com.quackers29.businesscraft.ui.components.basic.BCComponent;
import com.quackers29.businesscraft.ui.components.basic.BCButton;
import java.util.function.Supplier;
import com.quackers29.businesscraft.ui.components.basic.BCComponent;
import com.quackers29.businesscraft.ui.components.basic.BCButton;

/**
 * Component that binds to the state management system with two-way binding.
 * This allows UI components to both read from and write to the state system.
 *
 * @param <T> The type of data to bind to
 */
public abstract class StateBindableComponent<T> extends BCComponent {
    
    protected final TownInterfaceState state;
    protected Supplier<T> stateGetter;
    protected Consumer<T> stateSetter;
    protected Function<T, String> formatter;
    
    // Cached value
    protected T currentValue;
    
    /**
     * Create a new component bound to a state value
     *
     * @param width Width of the component
     * @param height Height of the component
     * @param state The state object to bind to
     * @param stateGetter Function to get the value from state
     * @param stateSetter Function to set the value in state
     * @param formatter Function to format the value for display
     */
    public StateBindableComponent(
        int width, 
        int height, 
        TownInterfaceState state,
        Supplier<T> stateGetter,
        Consumer<T> stateSetter,
        Function<T, String> formatter
    ) {
        super(width, height);
        this.state = state;
        this.stateGetter = stateGetter;
        this.stateSetter = stateSetter;
        this.formatter = formatter;
        
        // Initial value
        refreshFromState();
        
        // Register as a state listener to receive updates
        if (state != null) {
            state.addStateChangeListener(this::onStateChanged);
        }
    }
    
    /**
     * Handle state changes
     */
    protected void onStateChanged(TownInterfaceState state) {
        refreshFromState();
    }
    
    /**
     * Refresh the component's value from the state
     */
    protected void refreshFromState() {
        if (stateGetter != null) {
            T newValue = stateGetter.get();
            if (!isEqual(newValue, currentValue)) {
                currentValue = newValue;
                onValueChanged();
            }
        }
    }
    
    /**
     * Update the state with a new value
     */
    protected void updateState(T newValue) {
        if (stateSetter != null && !isEqual(newValue, currentValue)) {
            stateSetter.accept(newValue);
            currentValue = newValue;
            onValueChanged();
        }
    }
    
    /**
     * Called when the value changes
     */
    protected void onValueChanged() {
        // Subclasses can override this to handle value changes
    }
    
    /**
     * Format the current value for display
     */
    protected String formatValue() {
        return formatter != null && currentValue != null 
            ? formatter.apply(currentValue) 
            : (currentValue != null ? currentValue.toString() : "");
    }
    
    /**
     * Check if two values are equal, handling null values
     */
    protected boolean isEqual(T value1, T value2) {
        if (value1 == null && value2 == null) {
            return true;
        }
        if (value1 == null || value2 == null) {
            return false;
        }
        return value1.equals(value2);
    }
} 