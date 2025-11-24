package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.ItemHandlerHelper;
import com.quackers29.businesscraft.api.SlotBasedStorageAccess;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Fabric implementation of ItemHandlerHelper
 * Uses vanilla SimpleContainer instead of Forge's ItemStackHandler
 */
public class FabricItemHandlerHelper implements ItemHandlerHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricItemHandlerHelper.class);

    @Override
    public Object createItemStackHandler(int size) {
        return new SimpleContainer(size);
    }

    @Override
    public int getSlots(Object itemHandler) {
        if (itemHandler instanceof Container container) {
            return container.getContainerSize();
        }
        return 0;
    }

    @Override
    public void setStackInSlot(Object itemHandler, int slot, Object stack) {
        if (itemHandler instanceof Container container && stack instanceof ItemStack itemStack) {
            container.setItem(slot, itemStack);
        }
    }

    @Override
    public Object getStackInSlot(Object itemHandler, int slot) {
        if (itemHandler instanceof Container container) {
            return container.getItem(slot);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Object createSlot(Object itemHandler, int index, int x, int y) {
        if (itemHandler instanceof Container container) {
            return new net.minecraft.world.inventory.Slot(container, index, x, y);
        }
        return null;
    }

    @Override
    public Object createWithdrawalOnlySlot(Object itemHandler, int index, int x, int y) {
        if (itemHandler instanceof Container container) {
            return new net.minecraft.world.inventory.Slot(container, index, x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            };
        }
        return null;
    }

    @Override
    public Object createLazyOptional(Object itemHandler) {
        // Fabric doesn't use LazyOptional, so we just wrap it in a Java Optional or
        // return it directly
        // For compatibility with common code that expects an object, we'll use Optional
        return Optional.ofNullable(itemHandler);
    }

    @Override
    public boolean isItemHandlerCapability(Object capability) {
        // Fabric doesn't use Forge capabilities
        return false;
    }

    @Override
    public Object getEmptyLazyOptional() {
        return Optional.empty();
    }

    @Override
    public Object castLazyOptional(Object lazyOptional, Object capability) {
        // Since we don't use capabilities, just return the value if present
        if (lazyOptional instanceof Optional<?> optional) {
            return optional.orElse(null);
        }
        return lazyOptional;
    }

    @Override
    public Object createCustomItemStackHandler(int size, Runnable onContentsChanged) {
        // SimpleContainer with listener
        SimpleContainer container = new SimpleContainer(size);
        container.addListener(c -> onContentsChanged.run());
        return container;
    }

    @Override
    public void invalidateLazyOptional(Object lazyOptional) {
        // No-op for Optional
    }

    @Override
    public Object serializeNBT(Object itemHandler) {
        if (itemHandler instanceof SimpleContainer container) {
            ListTag listTag = new ListTag();
            for (int i = 0; i < container.getContainerSize(); ++i) {
                ItemStack itemStack = container.getItem(i);
                if (!itemStack.isEmpty()) {
                    CompoundTag compoundTag = new CompoundTag();
                    compoundTag.putByte("Slot", (byte) i);
                    itemStack.save(compoundTag);
                    listTag.add(compoundTag);
                }
            }
            CompoundTag root = new CompoundTag();
            root.put("Items", listTag);
            root.putInt("Size", container.getContainerSize());
            return root;
        }
        return new CompoundTag();
    }

    @Override
    public void deserializeNBT(Object itemHandler, Object nbt) {
        if (itemHandler instanceof SimpleContainer container && nbt instanceof CompoundTag tag) {
            if (tag.contains("Items", 9)) {
                ListTag listTag = tag.getList("Items", 10);
                for (int i = 0; i < listTag.size(); ++i) {
                    CompoundTag compoundTag = listTag.getCompound(i);
                    int j = compoundTag.getByte("Slot") & 255;
                    if (j >= 0 && j < container.getContainerSize()) {
                        container.setItem(j, ItemStack.of(compoundTag));
                    }
                }
            }
        }
    }

    @Override
    public Object extractItem(Object itemHandler, int slot, int amount, boolean simulate) {
        if (itemHandler instanceof Container container) {
            if (slot >= 0 && slot < container.getContainerSize()) {
                ItemStack stack = container.getItem(slot);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                int extract = Math.min(amount, stack.getCount());
                ItemStack result = stack.copy();
                result.setCount(extract);

                if (!simulate) {
                    container.removeItem(slot, extract);
                }

                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Object insertItem(Object itemHandler, int slot, Object stack, boolean simulate) {
        if (itemHandler instanceof Container container && stack instanceof ItemStack itemStack) {
            if (slot >= 0 && slot < container.getContainerSize()) {
                ItemStack existing = container.getItem(slot);

                if (existing.isEmpty()) {
                    int limit = Math.min(container.getMaxStackSize(), itemStack.getMaxStackSize());
                    int insert = Math.min(itemStack.getCount(), limit);

                    if (!simulate) {
                        ItemStack toInsert = itemStack.copy();
                        toInsert.setCount(insert);
                        container.setItem(slot, toInsert);
                    }

                    if (insert < itemStack.getCount()) {
                        ItemStack remainder = itemStack.copy();
                        remainder.setCount(itemStack.getCount() - insert);
                        return remainder;
                    }
                    return ItemStack.EMPTY;
                } else {
                    if (ItemStack.isSameItemSameTags(existing, itemStack)) {
                        int limit = Math.min(container.getMaxStackSize(), existing.getMaxStackSize());
                        int space = limit - existing.getCount();
                        int insert = Math.min(itemStack.getCount(), space);

                        if (insert > 0) {
                            if (!simulate) {
                                existing.grow(insert);
                            }

                            if (insert < itemStack.getCount()) {
                                ItemStack remainder = itemStack.copy();
                                remainder.setCount(itemStack.getCount() - insert);
                                return remainder;
                            }
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }
        }
        return stack; // Could not insert
    }

    @Override
    public Object createStorageWrapper(SlotBasedStorageAccess storage) {
        // Create a SimpleContainer that delegates to the storage access
        // This is a simplified wrapper
        return new SimpleContainer(storage.getSlotCount()) {
            @Override
            public ItemStack getItem(int slot) {
                Object stack = storage.getSlot(slot);
                return stack instanceof ItemStack ? (ItemStack) stack : ItemStack.EMPTY;
            }

            @Override
            public void setItem(int slot, ItemStack stack) {
                storage.setSlot(slot, stack);
            }

            @Override
            public boolean isEmpty() {
                for (int i = 0; i < getContainerSize(); i++) {
                    if (!getItem(i).isEmpty())
                        return false;
                }
                return true;
            }
        };
    }
}
