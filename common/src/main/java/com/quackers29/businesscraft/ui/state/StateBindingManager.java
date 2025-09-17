package com.quackers29.businesscraft.ui.state;

import com.quackers29.businesscraft.ui.state.TownInterfaceState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages bindings between UI components and the state system.
 * Provides a centralized way to create, track, and update bindings.
 */
public class StateBindingManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(StateBindingManager.class);
    
    private final TownInterfaceState state;
    private final Map<String, Binding<?>> bindings = new HashMap<>();
    private final List<Consumer<TownInterfaceState>> stateChangeListeners = new ArrayList<>();
    
    /**
     * Create a new binding manager for the given state
     */
    public StateBindingManager(TownInterfaceState state) {
        this.state = state;
        
        // Register as a listener to the state
        if (state != null) {
            state.addStateChangeListener(this::onStateChanged);
        }
    }
    
    /**
     * Handle state changes and notify our listeners
     */
    private void onStateChanged(TownInterfaceState state) {
        stateChangeListeners.forEach(listener -> {
            try {
                listener.accept(state);
            } catch (Exception e) {
                LOGGER.error("Error in state change listener", e);
            }
        });
    }
    
    /**
     * Register a state change listener
     */
    public void addStateChangeListener(Consumer<TownInterfaceState> listener) {
        stateChangeListeners.add(listener);
    }
    
    /**
     * Create a binding for a string value
     */
    public Binding<String> bindString(String key, Supplier<String> getter, Consumer<String> setter) {
        Binding<String> binding = new Binding<>(key, getter, setter);
        bindings.put(key, binding);
        return binding;
    }
    
    /**
     * Create a binding for an integer value
     */
    public Binding<Integer> bindInt(String key, Supplier<Integer> getter, Consumer<Integer> setter) {
        Binding<Integer> binding = new Binding<>(key, getter, setter);
        bindings.put(key, binding);
        return binding;
    }
    
    /**
     * Create a binding for a boolean value
     */
    public Binding<Boolean> bindBoolean(String key, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        Binding<Boolean> binding = new Binding<>(key, getter, setter);
        bindings.put(key, binding);
        return binding;
    }
    
    /**
     * Get a binding by key
     */
    @SuppressWarnings("unchecked")
    public <T> Binding<T> getBinding(String key) {
        return (Binding<T>) bindings.get(key);
    }
    
    /**
     * Get the state object
     */
    public TownInterfaceState getState() {
        return state;
    }
    
    /**
     * Represents a binding between a state value and UI component
     */
    public static class Binding<T> {
        private final String key;
        private final Supplier<T> getter;
        private final Consumer<T> setter;
        private T cachedValue;
        private final List<Consumer<T>> valueChangeListeners = new ArrayList<>();
        
        /**
         * Create a new binding
         */
        public Binding(String key, Supplier<T> getter, Consumer<T> setter) {
            this.key = key;
            this.getter = getter;
            this.setter = setter;
            
            // Initialize cached value
            if (getter != null) {
                this.cachedValue = getter.get();
            }
        }
        
        /**
         * Get the current value from the state
         */
        public T getValue() {
            if (getter != null) {
                T newValue = getter.get();
                if (!isEqual(newValue, cachedValue)) {
                    cachedValue = newValue;
                }
                return newValue;
            }
            return cachedValue;
        }
        
        /**
         * Set a new value in the state
         */
        public void setValue(T value) {
            if (setter != null && !isEqual(value, cachedValue)) {
                setter.accept(value);
                cachedValue = value;
                notifyValueChanged();
            }
        }
        
        /**
         * Add a listener for value changes
         */
        public void addValueChangeListener(Consumer<T> listener) {
            valueChangeListeners.add(listener);
        }
        
        /**
         * Get the binding key
         */
        public String getKey() {
            return key;
        }
        
        /**
         * Notify all listeners of value changes
         */
        private void notifyValueChanged() {
            valueChangeListeners.forEach(listener -> {
                try {
                    listener.accept(cachedValue);
                } catch (Exception e) {
                    LoggerFactory.getLogger(StateBindingManager.class)
                        .error("Error in binding value change listener", e);
                }
            });
        }
        
        /**
         * Check if two values are equal, handling null values
         */
        private boolean isEqual(T value1, T value2) {
            if (value1 == null && value2 == null) {
                return true;
            }
            if (value1 == null || value2 == null) {
                return false;
            }
            return value1.equals(value2);
        }
    }
} 
