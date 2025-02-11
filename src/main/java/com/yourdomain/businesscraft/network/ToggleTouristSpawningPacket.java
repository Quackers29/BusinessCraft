package com.yourdomain.businesscraft.network;

import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.town.Town;
import com.yourdomain.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ToggleTouristSpawningPacket {
    private final BlockPos pos;

    public ToggleTouristSpawningPacket(BlockPos pos) {
        this.pos = pos;
    }

    public ToggleTouristSpawningPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                Level level = player.level();
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof TownBlockEntity townBlock) {
                    UUID townId = townBlock.getTownId();
                    if (townId != null) {
<<<<<<< Updated upstream
                        Town town = TownManager.getInstance().getTown(townId);
                        if (town != null) {
                            town.setTouristSpawningEnabled(!town.canSpawnTourists());
                            townBlock.setChanged();
=======
                        ServerLevel serverLevel = (ServerLevel) level;
                        Town town = TownManager.get(serverLevel).getTown(townId);
                        if (town != null) {
                            boolean newState = !town.isTouristSpawningEnabled();
                            LOGGER.info("Toggling tourist spawning to {} for town {}", newState, townId);
                            town.setTouristSpawningEnabled(newState);
                            townBlock.syncTownData();
                            TownManager.get(serverLevel).getSavedData().setDirty();
>>>>>>> Stashed changes
                        }
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
} 