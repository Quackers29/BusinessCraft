package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.ItemHandlerHelper;
import com.quackers29.businesscraft.api.SlotBasedStorageAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric implementation of ItemHandlerHelper
 * Uses reflection to access Forge's ItemStackHandler and LazyOptional classes
 * (available at runtime via common module dependencies)
 */
public class FabricItemHandlerHelper implements ItemHandlerHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricItemHandlerHelper.class);
    
    // Cache reflected classes for performance
    private static Class<?> itemStackHandlerClass;
    private static Class<?> lazyOptionalClass;
    private static Class<?> slotItemHandlerClass;
    private static Class<?> iItemHandlerClass;
    private static Class<?> itemStackClass;
    private static Class<?> compoundTagClass;
    private static Object emptyItemStack;
    
    static {
        try {
            ClassLoader classLoader = FabricItemHandlerHelper.class.getClassLoader();
            itemStackHandlerClass = classLoader.loadClass("net.minecraftforge.items.ItemStackHandler");
            lazyOptionalClass = classLoader.loadClass("net.minecraftforge.common.util.LazyOptional");
            slotItemHandlerClass = classLoader.loadClass("net.minecraftforge.items.SlotItemHandler");
            iItemHandlerClass = classLoader.loadClass("net.minecraftforge.items.IItemHandler");
            itemStackClass = classLoader.loadClass("net.minecraft.world.item.ItemStack");
            compoundTagClass = classLoader.loadClass("net.minecraft.nbt.CompoundTag");
            
            // Get empty ItemStack
            java.lang.reflect.Field emptyField = itemStackClass.getField("EMPTY");
            emptyItemStack = emptyField.get(null);
        } catch (Exception e) {
            LOGGER.error("Error initializing FabricItemHandlerHelper reflection classes", e);
        }
    }
    
    // @Override - temporarily removed due to classpath issue with common module interfaces
    public Object createItemStackHandler(int size) {
        try {
            java.lang.reflect.Constructor<?> constructor = itemStackHandlerClass.getConstructor(int.class);
            return constructor.newInstance(size);
        } catch (Exception e) {
            LOGGER.error("Error creating ItemStackHandler", e);
            throw new RuntimeException("Failed to create ItemStackHandler", e);
        }
    }

    // @Override - temporarily removed due to classpath issue with common module interfaces
    public int getSlots(Object itemHandler) {
        try {
            if (itemStackHandlerClass.isInstance(itemHandler)) {
                java.lang.reflect.Method method = itemStackHandlerClass.getMethod("getSlots");
                return (Integer) method.invoke(itemHandler);
            }
            throw new IllegalArgumentException("Invalid item handler type");
        } catch (Exception e) {
            LOGGER.error("Error getting slots", e);
            throw new RuntimeException("Failed to get slots", e);
        }
    }

    // @Override - temporarily removed due to classpath issue with common module interfaces
    public void setStackInSlot(Object itemHandler, int slot, Object stack) {
        try {
            if (itemStackHandlerClass.isInstance(itemHandler) && itemStackClass.isInstance(stack)) {
                java.lang.reflect.Method method = itemStackHandlerClass.getMethod("setStackInSlot", int.class, itemStackClass);
                method.invoke(itemHandler, slot, stack);
            } else {
                throw new IllegalArgumentException("Invalid item handler or stack type");
            }
        } catch (Exception e) {
            LOGGER.error("Error setting stack in slot", e);
            throw new RuntimeException("Failed to set stack in slot", e);
        }
    }

    // @Override - temporarily removed due to classpath issue with common module interfaces
    public Object getStackInSlot(Object itemHandler, int slot) {
        try {
            if (itemStackHandlerClass.isInstance(itemHandler)) {
                java.lang.reflect.Method method = itemStackHandlerClass.getMethod("getStackInSlot", int.class);
                return method.invoke(itemHandler, slot);
            }
            throw new IllegalArgumentException("Invalid item handler type");
        } catch (Exception e) {
            LOGGER.error("Error getting stack in slot", e);
            throw new RuntimeException("Failed to get stack in slot", e);
        }
    }

    // @Override - temporarily removed due to classpath issue with common module interfaces
    public Object createSlot(Object itemHandler, int index, int x, int y) {
        try {
            if (itemStackHandlerClass.isInstance(itemHandler)) {
                java.lang.reflect.Constructor<?> constructor = slotItemHandlerClass.getConstructor(
                    itemStackHandlerClass, int.class, int.class, int.class
                );
                return constructor.newInstance(itemHandler, index, x, y);
            }
            throw new IllegalArgumentException("Invalid item handler type");
        } catch (Exception e) {
            LOGGER.error("Error creating slot", e);
            throw new RuntimeException("Failed to create slot", e);
        }
    }

    // @Override - temporarily removed due to classpath issue with common module interfaces
    public Object createWithdrawalOnlySlot(Object itemHandler, int index, int x, int y) {
        // For Fabric, we'll create a regular slot and override behavior via reflection if needed
        // For now, just return a regular slot (withdrawal-only behavior handled in menu code)
        return createSlot(itemHandler, index, x, y);
    }

    // @Override - temporarily removed due to classpath issue with common module interfaces
    public Object createLazyOptional(Object itemHandler) {
        try {
            if (iItemHandlerClass.isInstance(itemHandler)) {
                java.lang.reflect.Method ofMethod = lazyOptionalClass.getMethod("of", java.util.function.Supplier.class);
                java.util.function.Supplier<Object> supplier = () -> itemHandler;
                return ofMethod.invoke(null, supplier);
            }
            throw new IllegalArgumentException("Invalid item handler type");
        } catch (Exception e) {
            LOGGER.error("Error creating LazyOptional", e);
            throw new RuntimeException("Failed to create LazyOptional", e);
        }
    }

    // @Override - temporarily removed due to classpath issue with common module interfaces
    public boolean isItemHandlerCapability(Object capability) {
        try {
            // Check if capability is ForgeCapabilities.ITEM_HANDLER
            Class<?> forgeCapabilitiesClass = Class.forName("net.minecraftforge.common.capabilities.ForgeCapabilities");
            java.lang.reflect.Field itemHandlerField = forgeCapabilitiesClass.getField("ITEM_HANDLER");
            Object itemHandlerCapability = itemHandlerField.get(null);
            return capability == itemHandlerCapability;
        } catch (Exception e) {
            LOGGER.warn("Error checking item handler capability", e);
            return false;
        }
    }

    // @Override - temporarily removed due to classpath issue with common module interfaces
    public Object getEmptyLazyOptional() {
        try {
            java.lang.reflect.Method emptyMethod = lazyOptionalClass.getMethod("empty");
            return emptyMethod.invoke(null);
        } catch (Exception e) {
            LOGGER.error("Error getting empty LazyOptional", e);
            throw new RuntimeException("Failed to get empty LazyOptional", e);
        }
    }

    // @Override - temporarily removed due to classpath issue with common module interfaces
    public Object castLazyOptional(Object lazyOptional, Object capability) {
        try {
            if (lazyOptionalClass.isInstance(lazyOptional)) {
                java.lang.reflect.Method castMethod = lazyOptionalClass.getMethod("cast");
                return castMethod.invoke(lazyOptional);
            }
            return lazyOptional;
        } catch (Exception e) {
            LOGGER.error("Error casting LazyOptional", e);
            return lazyOptional;
        }
    }

    // @Override - temporarily removed due to classpath issue with common module interfaces
    public Object createCustomItemStackHandler(int size, Runnable onContentsChanged) {
        try {
            // Create a regular ItemStackHandler
            // Note: Callback functionality would require subclassing, which is complex with reflection
            // For now, return a regular handler - callback can be handled separately if needed
            java.lang.reflect.Constructor<?> constructor = itemStackHandlerClass.getConstructor(int.class);
            Object handler = constructor.newInstance(size);
            
            // Store callback for potential future use
            // TODO: Implement proper callback mechanism if needed
            return handler;
        } catch (Exception e) {
            LOGGER.error("Error creating custom ItemStackHandler", e);
            throw new RuntimeException("Failed to create custom ItemStackHandler", e);
        }
    }

    // @Override - temporarily removed due to classpath issue with common module interfaces
    public void invalidateLazyOptional(Object lazyOptional) {
        try {
            if (lazyOptionalClass.isInstance(lazyOptional)) {
                java.lang.reflect.Method invalidateMethod = lazyOptionalClass.getMethod("invalidate");
                invalidateMethod.invoke(lazyOptional);
            }
        } catch (Exception e) {
            LOGGER.error("Error invalidating LazyOptional", e);
        }
    }

    // @Override - temporarily removed due to classpath issue with common module interfaces
    public Object serializeNBT(Object itemHandler) {
        try {
            if (itemStackHandlerClass.isInstance(itemHandler)) {
                java.lang.reflect.Method method = itemStackHandlerClass.getMethod("serializeNBT");
                return method.invoke(itemHandler);
            }
            throw new IllegalArgumentException("Invalid item handler type");
        } catch (Exception e) {
            LOGGER.error("Error serializing NBT", e);
            throw new RuntimeException("Failed to serialize NBT", e);
        }
    }

    // @Override - temporarily removed due to classpath issue with common module interfaces
    public void deserializeNBT(Object itemHandler, Object nbt) {
        try {
            if (itemStackHandlerClass.isInstance(itemHandler) && compoundTagClass.isInstance(nbt)) {
                java.lang.reflect.Method method = itemStackHandlerClass.getMethod("deserializeNBT", compoundTagClass);
                method.invoke(itemHandler, nbt);
            } else {
                throw new IllegalArgumentException("Invalid item handler or NBT type");
            }
        } catch (Exception e) {
            LOGGER.error("Error deserializing NBT", e);
            throw new RuntimeException("Failed to deserialize NBT", e);
        }
    }

    // @Override - temporarily removed due to classpath issue with common module interfaces
    public Object extractItem(Object itemHandler, int slot, int amount, boolean simulate) {
        try {
            if (iItemHandlerClass.isInstance(itemHandler)) {
                java.lang.reflect.Method method = iItemHandlerClass.getMethod("extractItem", int.class, int.class, boolean.class);
                Object result = method.invoke(itemHandler, slot, amount, simulate);
                return result != null ? result : emptyItemStack;
            }
            return emptyItemStack;
        } catch (Exception e) {
            LOGGER.error("Error extracting item", e);
            return emptyItemStack;
        }
    }

    // @Override - temporarily removed due to classpath issue with common module interfaces
    public Object insertItem(Object itemHandler, int slot, Object stack, boolean simulate) {
        try {
            if (iItemHandlerClass.isInstance(itemHandler) && itemStackClass.isInstance(stack)) {
                java.lang.reflect.Method method = iItemHandlerClass.getMethod("insertItem", int.class, itemStackClass, boolean.class);
                Object result = method.invoke(itemHandler, slot, stack, simulate);
                return result != null ? result : stack;
            }
            return stack;
        } catch (Exception e) {
            LOGGER.error("Error inserting item", e);
            return stack;
        }
    }
    
    // @Override - temporarily removed due to classpath issue with common module interfaces
    public Object createStorageWrapper(SlotBasedStorageAccess storage) {
        try {
            // Create a wrapper class that extends ItemStackHandler and delegates to SlotBasedStorage
            return new SlotBasedItemStackHandlerWrapper(storage);
        } catch (Exception e) {
            LOGGER.error("Error creating storage wrapper", e);
            throw new RuntimeException("Failed to create storage wrapper", e);
        }
    }
    
    /**
     * ItemStackHandler wrapper for SlotBasedStorage
     * Similar to Forge's SlotBasedItemStackHandler
     */
    private static class SlotBasedItemStackHandlerWrapper {
        private final SlotBasedStorageAccess storage;
        private Object itemStackHandler;
        
        public SlotBasedItemStackHandlerWrapper(SlotBasedStorageAccess storage) {
            this.storage = storage;
            try {
                java.lang.reflect.Constructor<?> constructor = itemStackHandlerClass.getConstructor(int.class);
                this.itemStackHandler = constructor.newInstance(storage.getSlotCount());
                
                // Sync initial state from storage
                // Use reflection to call getSlot() to avoid compile-time ItemStack dependency
                try {
                    java.lang.reflect.Method getSlotMethod = SlotBasedStorageAccess.class.getMethod("getSlot", int.class);
                    for (int i = 0; i < storage.getSlotCount(); i++) {
                        Object stack = getSlotMethod.invoke(storage, i);
                        if (stack != null) {
                            // Check if stack is empty using reflection
                            try {
                                java.lang.reflect.Method isEmptyMethod = itemStackClass.getMethod("isEmpty");
                                boolean isEmpty = (Boolean) isEmptyMethod.invoke(stack);
                                if (!isEmpty) {
                                    java.lang.reflect.Method setMethod = itemStackHandlerClass.getMethod("setStackInSlot", int.class, itemStackClass);
                                    setMethod.invoke(itemStackHandler, i, stack);
                                }
                            } catch (Exception e) {
                                // If isEmpty check fails, try to set anyway
                                try {
                                    java.lang.reflect.Method setMethod = itemStackHandlerClass.getMethod("setStackInSlot", int.class, itemStackClass);
                                    setMethod.invoke(itemStackHandler, i, stack);
                                } catch (Exception e2) {
                                    LOGGER.warn("Failed to sync storage slot {} to ItemStackHandler", i, e2);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to sync storage to ItemStackHandler", e);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to create ItemStackHandler wrapper", e);
            }
        }
        
        public Object getItemStackHandler() {
            return itemStackHandler;
        }
        
        // Note: Full implementation would need to override all ItemStackHandler methods
        // to delegate to storage and sync changes. This is a simplified version.
    }
}
