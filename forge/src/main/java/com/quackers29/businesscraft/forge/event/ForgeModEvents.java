package com.quackers29.businesscraft.forge.event;

import com.quackers29.businesscraft.client.ClientGlobalMarket;
import com.quackers29.businesscraft.event.TownEventHandler;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forge-specific event handlers
 */
@Mod.EventBusSubscriber(modid = "businesscraft")
public class ForgeModEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeModEvents.class);

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // Delegate path creation logic to common handler
        if (TownEventHandler.onRightClickBlock(event.getEntity(), event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /**
     * Reset client-side market cache when disconnecting from a server.
     * Prevents market prices from persisting across different worlds/servers.
     */
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        LOGGER.info("Client disconnecting - resetting ClientGlobalMarket");
        ClientGlobalMarket.get().reset();
    }
}
