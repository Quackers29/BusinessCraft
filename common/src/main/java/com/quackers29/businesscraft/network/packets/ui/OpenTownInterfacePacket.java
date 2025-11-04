package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.menu.TownInterfaceMenu;
import net.minecraftforge.network.NetworkHooks;

/**
 * Packet to request the server to open the TownInterface menu properly.
 * This ensures proper ContainerData synchronization.
 */
public class OpenTownInterfacePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenTownInterfacePacket.class);
    
    private final BlockPos blockPos;
    
    public OpenTownInterfacePacket(BlockPos blockPos) {
        this.blockPos = blockPos;
    }
    
    public OpenTownInterfacePacket(FriendlyByteBuf buf) {
        this.blockPos = buf.readBlockPos();
    }
    
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
    }

    // Static methods for Forge network registration
    public static void encode(OpenTownInterfacePacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }

    public static OpenTownInterfacePacket decode(FriendlyByteBuf buf) {
        return new OpenTownInterfacePacket(buf);
    }
    
    public boolean handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            Object senderObj = PlatformAccess.getNetwork().getSender(context);
            if (senderObj instanceof ServerPlayer player) {
                // Get the block entity to ensure all town data is accessible
                BlockEntity entity = player.level().getBlockEntity(blockPos);
                if (entity instanceof TownInterfaceEntity townInterface) {
                    // Open the TownInterfaceScreen using NetworkHooks for proper sync
                    net.minecraftforge.network.NetworkHooks.openScreen(player, new MenuProvider() {
                        @Override
                        public Component getDisplayName() {
                            return Component.translatable("block.businesscraft.town_interface");
                        }

                        @Override
                        public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
                            // Create the TownInterfaceMenu using the town's position
                            return new TownInterfaceMenu(windowId, inventory, blockPos);
                        }
                    }, blockPos);
                } else {
                    LOGGER.error("Failed to get TownInterfaceEntity at position: {}", blockPos);
                }
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
        return true;
    }
} 
