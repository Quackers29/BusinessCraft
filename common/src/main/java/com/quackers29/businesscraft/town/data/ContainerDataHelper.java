package com.quackers29.businesscraft.town.data;

import net.minecraft.world.inventory.ContainerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;

import java.util.*;
import java.util.function.IntSupplier;
import java.util.function.IntConsumer;

/**
 * A modular, user-friendly ContainerData implementation that allows dynamic registration
 * of data fields with automatic synchronization and type safety.
 * 
 * This replaces the static, hardcoded ContainerData approach with a flexible system
 * where data fields can be registered by name with their getter/setter functions.
 */
public class ContainerDataHelper implements ContainerData {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerDataHelper.class);
    
    /**
     * Represents a data field that can be synchronized between client and server
     */
    public static class DataField {
        private final String name;
        private final IntSupplier getter;
        private final IntConsumer setter;
        private final String description;
        private int cachedValue = 0;
        private boolean isDirty = true;
        
        public DataField(String name, IntSupplier getter, IntConsumer setter, String description) {
            this.name = name;
            this.getter = getter;
            this.setter = setter;
            this.description = description;
        }
        
        public DataField(String name, IntSupplier getter, String description) {
            this(name, getter, null, description);
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public boolean isReadOnly() { return setter == null; }
        
        public int getValue() {
            if (isDirty || getter != null) {
                try {
                    int newValue = getter != null ? getter.getAsInt() : cachedValue;
                    if (newValue != cachedValue) {
                        cachedValue = newValue;
                    }
                    isDirty = false;
                } catch (Exception e) {
                    LOGGER.warn("Error getting value for field '{}': {}", name, e.getMessage());
                }
            }
            return cachedValue;
        }
        
        public void setValue(int value) {
            if (setter != null) {
                try {
                    setter.accept(value);
                    cachedValue = value;
                    isDirty = false;
                } catch (Exception e) {
                    LOGGER.warn("Error setting value for field '{}': {}", name, e.getMessage());
                }
            } else {
                LOGGER.warn("Attempted to set value on read-only field '{}'", name);
            }
        }
        
        public void markDirty() {
            isDirty = true;
        }
    }
    
    private final Map<String, DataField> fieldsByName = new LinkedHashMap<>();
    private final List<DataField> fieldsByIndex = new ArrayList<>();
    private final String contextName;
    
    public ContainerDataHelper(String contextName) {
        this.contextName = contextName;
    }
    
    /**
     * Registers a read-write data field
     * 
     * @param name The name of the field (for debugging and lookup)
     * @param getter Function to get the current value
     * @param setter Function to set a new value
     * @param description Human-readable description of what this field represents
     * @return The index of the registered field
     */
    public int registerField(String name, IntSupplier getter, IntConsumer setter, String description) {
        if (fieldsByName.containsKey(name)) {
            throw new IllegalArgumentException("Field '" + name + "' is already registered");
        }
        
        DataField field = new DataField(name, getter, setter, description);
        fieldsByName.put(name, field);
        fieldsByIndex.add(field);
        
        int index = fieldsByIndex.size() - 1;
        DebugConfig.debug(LOGGER, DebugConfig.NBT_DATA_HELPER, "Registered field '{}' at index {} in context '{}': {}", name, index, contextName, description);
        return index;
    }
    
    /**
     * Registers a read-only data field
     * 
     * @param name The name of the field
     * @param getter Function to get the current value
     * @param description Human-readable description
     * @return The index of the registered field
     */
    public int registerReadOnlyField(String name, IntSupplier getter, String description) {
        return registerField(name, getter, null, description + " (read-only)");
    }
    
    /**
     * Gets a field by name for direct access
     */
    public DataField getField(String name) {
        return fieldsByName.get(name);
    }
    
    /**
     * Gets the value of a field by name
     */
    public int getValue(String name) {
        DataField field = fieldsByName.get(name);
        if (field != null) {
            return field.getValue();
        }
        LOGGER.warn("Attempted to get value of unknown field '{}' in context '{}'", name, contextName);
        return 0;
    }
    
    /**
     * Sets the value of a field by name
     */
    public void setValue(String name, int value) {
        DataField field = fieldsByName.get(name);
        if (field != null) {
            field.setValue(value);
        } else {
            LOGGER.warn("Attempted to set value of unknown field '{}' in context '{}'", name, contextName);
        }
    }
    
    /**
     * Marks all fields as dirty to force refresh on next access
     */
    public void markAllDirty() {
        fieldsByIndex.forEach(DataField::markDirty);
    }
    
    /**
     * Marks a specific field as dirty
     */
    public void markDirty(String name) {
        DataField field = fieldsByName.get(name);
        if (field != null) {
            field.markDirty();
        }
    }
    
    /**
     * Gets all registered field names
     */
    public Set<String> getFieldNames() {
        return Collections.unmodifiableSet(fieldsByName.keySet());
    }
    
    /**
     * Gets debug information about all fields
     */
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("ContainerData '").append(contextName).append("' (").append(fieldsByIndex.size()).append(" fields):\n");
        
        for (int i = 0; i < fieldsByIndex.size(); i++) {
            DataField field = fieldsByIndex.get(i);
            sb.append(String.format("  [%d] %s = %d (%s)%s\n", 
                i, field.getName(), field.getValue(), field.getDescription(),
                field.isReadOnly() ? " [READ-ONLY]" : ""));
        }
        
        return sb.toString();
    }
    
    // ContainerData implementation
    
    @Override
    public int get(int index) {
        if (index >= 0 && index < fieldsByIndex.size()) {
            return fieldsByIndex.get(index).getValue();
        }
        LOGGER.warn("Attempted to get invalid index {} in context '{}' (max: {})", 
            index, contextName, fieldsByIndex.size() - 1);
        return 0;
    }
    
    @Override
    public void set(int index, int value) {
        if (index >= 0 && index < fieldsByIndex.size()) {
            fieldsByIndex.get(index).setValue(value);
        } else {
            LOGGER.warn("Attempted to set invalid index {} in context '{}' (max: {})", 
                index, contextName, fieldsByIndex.size() - 1);
        }
    }
    
    @Override
    public int getCount() {
        return fieldsByIndex.size();
    }
    
    /**
     * Builder class for easy ContainerDataHelper construction
     */
    public static class Builder {
        private final ContainerDataHelper helper;
        
        public Builder(String contextName) {
            this.helper = new ContainerDataHelper(contextName);
        }
        
        /**
         * Adds a read-write field
         */
        public Builder addField(String name, IntSupplier getter, IntConsumer setter, String description) {
            helper.registerField(name, getter, setter, description);
            return this;
        }
        
        /**
         * Adds a read-only field
         */
        public Builder addReadOnlyField(String name, IntSupplier getter, String description) {
            helper.registerReadOnlyField(name, getter, description);
            return this;
        }
        
        /**
         * Builds the ContainerDataHelper
         */
        public ContainerDataHelper build() {
            return helper;
        }
    }
    
    /**
     * Creates a new builder for constructing a ContainerDataHelper
     */
    public static Builder builder(String contextName) {
        return new Builder(contextName);
    }
} 
