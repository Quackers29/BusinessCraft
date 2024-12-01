package com.yourdomain.businesscraft.event;

import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ModEvents {
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
        if (blockEntity instanceof TownBlockEntity townBlock && townBlock.isInPathCreationMode()) {
            if (!townBlock.isValidPathDistance(event.getPos())) {
                event.getEntity().sendSystemMessage(Component.literal("Point too far from town!"));
                event.setCanceled(true);
                return;
            }

            if (townBlock.getPathStart() == null) {
                townBlock.setPathStart(event.getPos());
                event.getEntity().sendSystemMessage(Component.literal("First point set!"));
            } else {
                townBlock.setPathEnd(event.getPos());
                townBlock.setPathCreationMode(false);
                event.getEntity().sendSystemMessage(Component.literal("Path created!"));
            }
            event.setCanceled(true);
        }
    }
} 