package com.yourdomain.businesscraft.event;

import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ModEvents {
    private static BlockPos activeTownBlockPos = null;

    public static void setActiveTownBlock(BlockPos pos) {
        activeTownBlockPos = pos;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (activeTownBlockPos != null) {
            Level level = event.getLevel();
            BlockEntity be = level.getBlockEntity(activeTownBlockPos);
            
            if (be instanceof TownBlockEntity townBlock && townBlock.isInPathCreationMode()) {
                BlockPos clickedPos = event.getPos();
                
                if (!townBlock.isValidPathDistance(clickedPos)) {
                    event.getEntity().sendSystemMessage(Component.literal("Point too far from town!"));
                    event.setCanceled(true);
                    return;
                }

                if (townBlock.getPathStart() == null) {
                    townBlock.setPathStart(clickedPos);
                    event.getEntity().sendSystemMessage(Component.literal("First point set!"));
                } else {
                    townBlock.setPathEnd(clickedPos);
                    townBlock.setPathCreationMode(false);
                    activeTownBlockPos = null;
                    event.getEntity().sendSystemMessage(Component.literal("Path created!"));
                }
                event.setCanceled(true);
            }
        }
    }
}