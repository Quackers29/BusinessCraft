package com.quackers29.businesscraft.debug.production;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * The only listener list. Game systems post events here; {@link ProductionEventLog}
 * is the sink that writes them. When nobody is listening the supplier is not called.
 */
public final class ProductionEventBus {
    private static final List<ProductionEventListener> LISTENERS = new CopyOnWriteArrayList<>();

    private ProductionEventBus() {
    }

    public static boolean listening() {
        return !LISTENERS.isEmpty();
    }

    public static void add(ProductionEventListener listener) {
        if (listener != null && !LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    public static void remove(ProductionEventListener listener) {
        LISTENERS.remove(listener);
    }

    public static void post(Supplier<ProductionEvent> event) {
        if (LISTENERS.isEmpty() || event == null) {
            return;
        }
        ProductionEvent value = event.get();
        if (value == null) {
            return;
        }
        for (ProductionEventListener listener : LISTENERS) {
            listener.onEvent(value);
        }
    }
}
