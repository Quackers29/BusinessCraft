package com.quackers29.businesscraft.client;

import com.quackers29.businesscraft.BusinessCraft;
import com.quackers29.businesscraft.client.renderer.TouristRenderer;
import com.quackers29.businesscraft.init.ModEntityTypes;
import com.quackers29.businesscraft.platform.PlatformServices;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Platform-agnostic client setup.
 * Uses EventHelper abstraction for cross-platform compatibility.
 */
public class ClientSetup {

    /**
     * Initialize client-side platform-agnostic event handlers.
     * Should be called during client setup.
     */
    public static void initialize() {
        PlatformServices.getEventHelper().registerEntityRendererRegistrationEvent(() -> ClientSetup.onEntityRendererRegistration());
    }
    
    /**
     * Platform-agnostic entity renderer registration handler.
     */
    private static void onEntityRendererRegistration() {
        // This will be called by the platform-specific implementation
        // The actual registration is handled in ForgeEventHelper for Forge
    }
} 