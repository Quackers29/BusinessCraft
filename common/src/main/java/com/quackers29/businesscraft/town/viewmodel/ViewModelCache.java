package com.quackers29.businesscraft.town.viewmodel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ViewModelCache {
    private final Map<Class<?>, Object> cache = new ConcurrentHashMap<>();
    private final Map<Class<?>, Boolean> dirty = new ConcurrentHashMap<>();

    public <T> void update(Class<T> type, T vm) {
        cache.put(type, vm);
        dirty.put(type, true);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        return (T) cache.get(type);
    }

    public boolean isDirty(Class<?> type) {
        return Boolean.TRUE.equals(dirty.get(type));
    }

    public void clearDirty(Class<?> type) {
        dirty.remove(type);
    }

    public void syncAllDirty(Consumer<Object> sender) {
        dirty.entrySet().removeIf(entry -> {
            Object vm = cache.get(entry.getKey());
            if (vm != null) {
                sender.accept(vm);
            }
            return true;
        });
    }

    public void clear() {
        cache.clear();
        dirty.clear();
    }
}
