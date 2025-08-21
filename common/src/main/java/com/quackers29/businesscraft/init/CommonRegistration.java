package com.quackers29.businesscraft.init;

/**
 * Common registration coordinator for platform-agnostic mod initialization.
 * This class defines the registration order and coordinates between platforms.
 * 
 * Enhanced MultiLoader approach: Common module defines WHAT to register,
 * platform modules define HOW to register using their specific APIs.
 */
public class CommonRegistration {
    
    /**
     * Initialize all common registration systems.
     * This is called by platform modules during mod initialization.
     */
    public static void initialize() {
        // Initialize registry definitions - defines WHAT to register
        RegistryDefinitions.initialize();
        
        // Define registration order - platforms will implement the actual registration
        // using their specific APIs (DeferredRegister, Registry.register, etc.)
        
        // 1. Blocks and Items (basic registrations)
        initializeBlocks();
        
        // 2. Block Entities (depend on blocks)
        initializeBlockEntities();
        
        // 3. Entity Types (independent registrations)
        initializeEntityTypes();
        
        // 4. Menu Types (for UI system)
        initializeMenuTypes();
        
        // 5. Networking (packet definitions)
        initializeNetworking();
    }
    
    /**
     * Initialize block registrations.
     * Platform modules should override this with their specific registration logic.
     */
    public static void initializeBlocks() {
        // This will be implemented by platform modules
        // Common module defines the registration structure
    }
    
    /**
     * Initialize block entity registrations.
     */
    public static void initializeBlockEntities() {
        // This will be implemented by platform modules
    }
    
    /**
     * Initialize entity type registrations.
     */
    public static void initializeEntityTypes() {
        // This will be implemented by platform modules
    }
    
    /**
     * Initialize menu type registrations.
     */
    public static void initializeMenuTypes() {
        // This will be implemented by platform modules
    }
    
    /**
     * Initialize networking packet definitions.
     */
    public static void initializeNetworking() {
        com.quackers29.businesscraft.network.CommonNetworking.initialize();
    }
}