package com.quackers29.businesscraft.fabric.block;

/**
 * Fabric-specific simple implementation of TownInterfaceBlock.
 * This provides basic block functionality for testing the registration system.
 */
public class TownInterfaceBlock {
    // Simple placeholder block for testing registration
    // The actual implementation would extend Minecraft's BaseEntityBlock
    // but for now this serves as a marker for the registration system

    public static final String BLOCK_ID = "businesscraft:town_interface";

    public TownInterfaceBlock(Object properties) {
        // Constructor that accepts Minecraft block properties
        // This would normally call super(properties) on BaseEntityBlock
    }

    // This is just a marker method to indicate this class is available
    public String getDescription() {
        return "Town Interface Block - Fabric Edition";
    }
}
