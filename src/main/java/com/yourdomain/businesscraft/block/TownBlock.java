package com.yourdomain.businesscraft.block;

import com.yourdomain.businesscraft.block.entity.ModBlockEntities;
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.config.ConfigLoader;
import com.yourdomain.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Random;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public class TownBlock extends BaseEntityBlock {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownBlock.class);

    public TownBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.STONE)
                .strength(3.5F)
                .requiresCorrectToolForDrops());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof TownBlockEntity townBlock) {
                NetworkHooks.openScreen((ServerPlayer) player, townBlock, buf -> {
                    buf.writeBlockPos(pos);
                });
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TownBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.TOWN_BLOCK_ENTITY.get(),
                (lvl, pos, blockState, blockEntity) -> ((TownBlockEntity) blockEntity).tick(lvl, pos, blockState,
                        (TownBlockEntity) blockEntity));
    }

    private String getRandomTownName() {
        if (ConfigLoader.townNames == null || ConfigLoader.townNames.isEmpty()) {
            return "DefaultTown"; // Fallback name
        }
        int index = new Random().nextInt(ConfigLoader.townNames.size());
        return ConfigLoader.townNames.get(index);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TownBlockEntity townBlock) {
                if (level instanceof ServerLevel serverLevel) {
                    TownManager.init(serverLevel);
                    String newTownName = getRandomTownName();
                    UUID townId = TownManager.getInstance().registerTown(pos, newTownName);
                    townBlock.setTownId(townId);
                    townBlock.setChanged();
                    serverLevel.sendBlockUpdated(pos, state, state, 3);
                }
            }
        }
    }
}