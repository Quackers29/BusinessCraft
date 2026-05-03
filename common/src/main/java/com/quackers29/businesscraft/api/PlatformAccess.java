package com.quackers29.businesscraft.api;

public class PlatformAccess {
    public static PlatformHelper platform;
    public static RegistryHelper registry;
    public static EventHelper events;
    public static NetworkHelper network;
    public static MenuHelper menus;
    public static EntityHelper entities;
    public static BlockEntityHelper blockEntities;
    public static MenuTypeHelper menuTypes;
    public static ItemHandlerHelper itemHandlers;
    public static NetworkMessages networkMessages;
    public static ClientHelper client;
    public static RenderHelper render;

    public static PlatformHelper getPlatform() {
        if (platform == null) {
            throw new IllegalStateException("Platform not initialized yet");
        }
        return platform;
    }

    public static RegistryHelper getRegistry() {
        if (registry == null) {
            throw new IllegalStateException("Registry not initialized yet");
        }
        return registry;
    }

    public static EventHelper getEvents() {
        if (events == null) {
            throw new IllegalStateException("Events not initialized yet");
        }
        return events;
    }

    public static NetworkHelper getNetwork() {
        if (network == null) {
            throw new IllegalStateException("Network not initialized yet");
        }
        return network;
    }

    public static MenuHelper getMenus() {
        if (menus == null) {
            throw new IllegalStateException("Menus not initialized yet");
        }
        return menus;
    }

    public static EntityHelper getEntities() {
        if (entities == null) {
            throw new IllegalStateException("Entities not initialized yet");
        }
        return entities;
    }

    public static BlockEntityHelper getBlockEntities() {
        if (blockEntities == null) {
            throw new IllegalStateException("BlockEntities not initialized yet");
        }
        return blockEntities;
    }

    public static MenuTypeHelper getMenuTypes() {
        if (menuTypes == null) {
            throw new IllegalStateException("MenuTypes not initialized yet");
        }
        return menuTypes;
    }

    public static ItemHandlerHelper getItemHandlers() {
        if (itemHandlers == null) {
            throw new IllegalStateException("ItemHandlers not initialized yet");
        }
        return itemHandlers;
    }

    public static NetworkMessages getNetworkMessages() {
        if (networkMessages == null) {
            throw new IllegalStateException("NetworkMessages not initialized yet");
        }
        return networkMessages;
    }

    public static ClientHelper getClient() {
        return client;
    }

    public static RenderHelper getRender() {
        return render;
    }

    public static ITouristHelper touristHelper;

    public static ITouristHelper getTouristHelper() {
        if (touristHelper == null) {
            throw new IllegalStateException("TouristHelper not initialized yet");
        }
        return touristHelper;
    }
}
