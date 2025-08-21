package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.platform.EventHelper;
import com.quackers29.businesscraft.platform.EventHelper.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Forge implementation of the EventHelper interface using MinecraftForge event bus.
 */
public class ForgeEventHelper implements EventHelper {
    
    private final List<BlockInteractionHandler> blockInteractionHandlers = new ArrayList<>();
    private final List<ServerStartingHandler> serverStartingHandlers = new ArrayList<>();
    private final List<ServerStoppingHandler> serverStoppingHandlers = new ArrayList<>();
    private final List<PlayerLoginHandler> playerLoginHandlers = new ArrayList<>();
    private final List<PlayerLogoutHandler> playerLogoutHandlers = new ArrayList<>();
    private final List<ClientSetupHandler> clientSetupHandlers = new ArrayList<>();
    private final List<EntityRendererRegistrationHandler> entityRendererRegistrationHandlers = new ArrayList<>();
    private final List<GuiOverlayRegistrationHandler> guiOverlayRegistrationHandlers = new ArrayList<>();
    private final List<RenderLevelHandler> renderLevelHandlers = new ArrayList<>();
    private final List<PlayerTickHandler> playerTickHandlers = new ArrayList<>();
    private final List<EntityAttributeRegistrationHandler> entityAttributeRegistrationHandlers = new ArrayList<>();
    
    public ForgeEventHelper() {
        // Register this instance as an event listener for Forge events (game events)
        MinecraftForge.EVENT_BUS.register(this);
        // Register a separate instance for mod events to avoid event bus conflicts
        FMLJavaModLoadingContext.get().getModEventBus().register(new ModEventHandler());
    }
    
    @Override
    public void registerBlockInteractionEvent(BlockInteractionHandler handler) {
        blockInteractionHandlers.add(handler);
    }
    
    @Override
    public void registerServerStartingEvent(ServerStartingHandler handler) {
        serverStartingHandlers.add(handler);
    }
    
    @Override
    public void registerServerStoppingEvent(ServerStoppingHandler handler) {
        serverStoppingHandlers.add(handler);
    }
    
    @Override
    public void registerPlayerLoginEvent(PlayerLoginHandler handler) {
        playerLoginHandlers.add(handler);
    }
    
    @Override
    public void registerPlayerLogoutEvent(PlayerLogoutHandler handler) {
        playerLogoutHandlers.add(handler);
    }
    
    @Override
    public void registerClientSetupEvent(ClientSetupHandler handler) {
        clientSetupHandlers.add(handler);
    }
    
    @Override
    public void registerEntityRendererRegistrationEvent(EntityRendererRegistrationHandler handler) {
        entityRendererRegistrationHandlers.add(handler);
    }
    
    @Override
    public void registerGuiOverlayRegistrationEvent(GuiOverlayRegistrationHandler handler) {
        guiOverlayRegistrationHandlers.add(handler);
    }
    
    @Override
    public void registerRenderLevelEvent(RenderLevelHandler handler) {
        renderLevelHandlers.add(handler);
    }
    
    @Override
    public void registerPlayerTickEvent(PlayerTickHandler handler) {
        playerTickHandlers.add(handler);
    }
    
    @Override
    public void registerEntityAttributeRegistrationEvent(EntityAttributeRegistrationHandler handler) {
        entityAttributeRegistrationHandlers.add(handler);
    }
    
    // Forge event handlers that delegate to our platform-agnostic handlers
    
    @SubscribeEvent
    public void onBlockInteraction(PlayerInteractEvent.RightClickBlock event) {
        for (BlockInteractionHandler handler : blockInteractionHandlers) {
            Object result = handler.onBlockInteraction(
                event.getEntity(),
                event.getLevel(),
                event.getHand(),
                event.getPos(),
                event.getLevel().getBlockState(event.getPos()),
                event.getHitVec()
            );
            
            if (result instanceof InteractionResult interactionResult && interactionResult != InteractionResult.PASS) {
                event.setCancellationResult(interactionResult);
                event.setCanceled(true);
                break;
            }
        }
    }
    
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        for (ServerStartingHandler handler : serverStartingHandlers) {
            handler.onServerStarting();
        }
    }
    
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        for (ServerStoppingHandler handler : serverStoppingHandlers) {
            handler.onServerStopping();
        }
    }
    
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        for (PlayerLoginHandler handler : playerLoginHandlers) {
            handler.onPlayerLogin(event.getEntity());
        }
    }
    
    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        for (PlayerLogoutHandler handler : playerLogoutHandlers) {
            handler.onPlayerLogout(event.getEntity());
        }
    }
    
    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            for (RenderLevelHandler handler : renderLevelHandlers) {
                handler.onRenderLevel(event.getPoseStack(), null, 
                                    event.getCamera().getEntity().level());
            }
        }
    }
    
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            for (PlayerTickHandler handler : playerTickHandlers) {
                handler.onPlayerTick(event.player);
            }
        }
    }
    
    /**
     * Inner class to handle mod bus events separately from game events
     */
    private class ModEventHandler {
        
        @SubscribeEvent
        public void onClientSetup(FMLClientSetupEvent event) {
            for (ClientSetupHandler handler : clientSetupHandlers) {
                handler.onClientSetup();
            }
        }
        
        @SubscribeEvent
        public void onEntityRendererRegistration(EntityRenderersEvent.RegisterRenderers event) {
            // Register the tourist renderer directly here since it's Forge-specific  
            event.registerEntityRenderer(com.quackers29.businesscraft.init.ModEntityTypes.TOURIST.get(), 
                                       com.quackers29.businesscraft.client.renderer.TouristRenderer::new);
            
            // Call any registered handlers (future extension point)
            for (EntityRendererRegistrationHandler handler : entityRendererRegistrationHandlers) {
                // This provides a hook for platform-agnostic code to register additional renderers
            }
        }
        
        @SubscribeEvent
        public void onGuiOverlayRegistration(RegisterGuiOverlaysEvent event) {
            // Register the town debug overlay directly here since it's Forge-specific
            event.registerAboveAll("town_debug_overlay", new com.quackers29.businesscraft.client.TownDebugOverlay());
            
            // Call any registered handlers (future extension point)
            for (GuiOverlayRegistrationHandler handler : guiOverlayRegistrationHandlers) {
                handler.registerOverlay("", null);
            }
        }
        
        @SubscribeEvent
        public void onEntityAttributeRegistration(EntityAttributeCreationEvent event) {
            // Register the tourist entity attributes directly since it's Forge-specific
            if (com.quackers29.businesscraft.init.ModEntityTypes.TOURIST != null) {
                event.put(com.quackers29.businesscraft.init.ModEntityTypes.TOURIST.get(), 
                         com.quackers29.businesscraft.entity.TouristEntity.createAttributes().build());
            }
            
            // Call any registered handlers (future extension point)
            for (EntityAttributeRegistrationHandler handler : entityAttributeRegistrationHandlers) {
                // This would be called if additional entity attribute registrations were needed
                // For now, we handle all entity attributes directly above
            }
        }
    }
}