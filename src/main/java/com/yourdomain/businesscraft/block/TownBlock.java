package com.yourdomain.businesscraft.block;

import com.yourdomain.businesscraft.block.entity.ModBlockEntities;
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraftforge.common.Tags.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.network.chat.Component;

public class TownBlock extends BaseEntityBlock {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownBlock.class);

    public TownBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0f)
                .requiresCorrectToolForDrops()
                .instrument(NoteBlockInstrument.BASS));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof TownBlockEntity townBlock) {
                if (townBlock.isInPathCreationMode()) {
                    BlockPos clickedPos = hit.getBlockPos();
                    if (!townBlock.isValidPathDistance(clickedPos)) {
                        player.sendSystemMessage(Component.literal("Too far from town block! Must be within 50 blocks."));
                        return InteractionResult.FAIL;
                    }
                    
                    if (townBlock.getPathStart() == null) {
                        townBlock.setPathStart(clickedPos);
                        player.sendSystemMessage(Component.literal("Start point set. Click another block to set end point."));
                    } else {
                        townBlock.setPathEnd(clickedPos);
                        townBlock.setPathCreationMode(false);
                        player.sendSystemMessage(Component.literal("Tourist path created!"));
                    }
                    return InteractionResult.SUCCESS;
                }
                
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
}