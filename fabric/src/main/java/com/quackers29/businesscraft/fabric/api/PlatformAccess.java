package com.quackers29.businesscraft.fabric.api;

import com.quackers29.businesscraft.api.*;

/**
 * Central access point for platform abstractions
 */
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
}
