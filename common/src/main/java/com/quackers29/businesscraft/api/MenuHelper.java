package com.quackers29.businesscraft.api;

/**
 * Platform-agnostic interface for menu/screen operations.
 * Implementations will handle platform-specific screen registration.
 */
public interface MenuHelper {
    /**
     * Register a screen factory for a menu type
     */
    <M, U> void registerScreenFactory(
        Object menuType,
        MenuHelper.ScreenFactory<M, U> screenFactory
    );

    /**
     * Functional interface for screen factories
     */
    @FunctionalInterface
    interface ScreenFactory<M, U> {
        U create(M menu, Object inventory, Object title);
    }
}
