package com.quackers29.businesscraft.api;

/**
 * Interface for menu operations
 */
public interface MenuHelper {
    <M, U> void registerScreenFactory(Object menuType, ScreenFactory<M, U> screenFactory);

    /**
     * Factory interface for creating screen instances
     */
    interface ScreenFactory<M, U> {
        U create(M menu, Object inventory, Object access);
    }
}
