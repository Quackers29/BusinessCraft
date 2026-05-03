package com.quackers29.businesscraft.api;

public interface MenuHelper {
    <M, U> void registerScreenFactory(
            Object menuType,
            MenuHelper.ScreenFactory<M, U> screenFactory
    );

    @FunctionalInterface
    interface ScreenFactory<M, U> {
        U create(M menu, Object inventory, Object title);
    }
}
