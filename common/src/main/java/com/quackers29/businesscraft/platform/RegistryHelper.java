package com.quackers29.businesscraft.platform;

/**
 * Platform-agnostic interface for registry operations.
 * This interface provides a common API for registration across mod loaders.
 * 
 * Enhanced MultiLoader approach: Common module defines the interface,
 * platform modules implement using their specific APIs.
 */
public interface RegistryHelper {
    // Minimal interface definition for now
    // Platform implementations will provide the actual registry functionality
}