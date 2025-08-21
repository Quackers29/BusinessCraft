package com.quackers29.businesscraft.init;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Platform-agnostic registry definitions for BusinessCraft.
 * This class defines WHAT to register without depending on specific platform APIs.
 * Platform modules use these definitions to create actual registrations.
 * 
 * Enhanced MultiLoader approach: Common module defines the data structure,
 * platform modules implement the registration using their specific APIs.
 */
public class RegistryDefinitions {
    
    // Registry definition storage
    private static final Map<String, BlockDefinition> BLOCKS = new HashMap<>();
    private static final Map<String, ItemDefinition> ITEMS = new HashMap<>();
    private static final Map<String, BlockEntityDefinition> BLOCK_ENTITIES = new HashMap<>();
    private static final Map<String, EntityDefinition> ENTITIES = new HashMap<>();
    private static final Map<String, MenuDefinition> MENUS = new HashMap<>();
    
    /**
     * Initialize all registry definitions.
     * This defines what should be registered on each platform.
     */
    public static void initialize() {
        defineBlocks();
        defineBlockEntities();
        defineEntities();
        defineMenus();
    }
    
    private static void defineBlocks() {
        // Town Interface Block definition
        BLOCKS.put("town_interface", new BlockDefinition(
            "town_interface",
            "TownInterfaceBlock", // Class name to be instantiated by platform
            Map.of(
                "mapColor", "STONE",
                "strength", "3.0f",
                "sound", "STONE",
                "requiresCorrectTool", "true"
            )
        ));
        
        // Block Item definition
        ITEMS.put("town_interface", new ItemDefinition(
            "town_interface",
            "BlockItem", // Standard BlockItem
            "town_interface" // References the block
        ));
    }
    
    private static void defineBlockEntities() {
        BLOCK_ENTITIES.put("town_interface", new BlockEntityDefinition(
            "town_interface",
            "TownInterfaceEntity",
            "town_interface" // Block reference
        ));
    }
    
    private static void defineEntities() {
        ENTITIES.put("tourist", new EntityDefinition(
            "tourist",
            "TouristEntity"
        ));
    }
    
    private static void defineMenus() {
        MENUS.put("town_interface", new MenuDefinition(
            "town_interface",
            "TownInterfaceMenu"
        ));
        
        MENUS.put("storage", new MenuDefinition(
            "storage", 
            "StorageMenu"
        ));
        
        MENUS.put("trade", new MenuDefinition(
            "trade",
            "TradeMenu"
        ));
        
        MENUS.put("payment_board", new MenuDefinition(
            "payment_board",
            "PaymentBoardMenu"
        ));
    }
    
    // Getter methods for platform modules
    public static Map<String, BlockDefinition> getBlocks() { return BLOCKS; }
    public static Map<String, ItemDefinition> getItems() { return ITEMS; }
    public static Map<String, BlockEntityDefinition> getBlockEntities() { return BLOCK_ENTITIES; }
    public static Map<String, EntityDefinition> getEntities() { return ENTITIES; }
    public static Map<String, MenuDefinition> getMenus() { return MENUS; }
    
    // Definition classes
    public static class BlockDefinition {
        public final String name;
        public final String className;
        public final Map<String, String> properties;
        
        public BlockDefinition(String name, String className, Map<String, String> properties) {
            this.name = name;
            this.className = className;
            this.properties = properties;
        }
    }
    
    public static class ItemDefinition {
        public final String name;
        public final String className;
        public final String blockReference;
        
        public ItemDefinition(String name, String className, String blockReference) {
            this.name = name;
            this.className = className;
            this.blockReference = blockReference;
        }
    }
    
    public static class BlockEntityDefinition {
        public final String name;
        public final String className;
        public final String blockReference;
        
        public BlockEntityDefinition(String name, String className, String blockReference) {
            this.name = name;
            this.className = className;
            this.blockReference = blockReference;
        }
    }
    
    public static class EntityDefinition {
        public final String name;
        public final String className;
        
        public EntityDefinition(String name, String className) {
            this.name = name;
            this.className = className;
        }
    }
    
    public static class MenuDefinition {
        public final String name;
        public final String className;
        
        public MenuDefinition(String name, String className) {
            this.name = name;
            this.className = className;
        }
    }
}