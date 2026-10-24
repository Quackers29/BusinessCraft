package com.quackers29.businesscraft.block.entity;

import com.quackers29.businesscraft.api.PlatformAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * Link from a Town Interface block to one platform on a town centre.
 * Holds ids only. Platform settings stay on the centre block entity.
 * <p>
 * The registry block id is {@code platform_interface}. The player-facing name
 * is Town Interface. The centre keeps {@code town_interface} so old worlds load.
 */
public class PlatformInterfaceEntity extends BlockEntity {
    private UUID townId;
    private UUID platformId;
    private String cachedTownName = "";
    private String cachedPlatformName = "";

    public PlatformInterfaceEntity(BlockPos pos, BlockState state) {
        super(PlatformAccess.getBlockEntities().getPlatformInterfaceEntityType(), pos, state);
    }

    public void setLink(UUID townId, UUID platformId, String townName, String platformName) {
        this.townId = townId;
        this.platformId = platformId;
        rememberNames(townName, platformName);
        setChanged();
    }

    public void rememberNames(String townName, String platformName) {
        String nextTown = townName == null ? "" : townName;
        String nextPlatform = platformName == null ? "" : platformName;
        if (nextTown.equals(cachedTownName) && nextPlatform.equals(cachedPlatformName)) {
            return;
        }
        this.cachedTownName = nextTown;
        this.cachedPlatformName = nextPlatform;
        setChanged();
    }

    public UUID getTownId() {
        return townId;
    }

    public UUID getPlatformId() {
        return platformId;
    }

    public String getCachedTownName() {
        return cachedTownName;
    }

    public String getCachedPlatformName() {
        return cachedPlatformName;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeLink(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        readLink(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        writeLink(tag);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void writeLink(CompoundTag tag) {
        if (townId != null) {
            tag.putUUID("TownId", townId);
        }
        if (platformId != null) {
            tag.putUUID("PlatformId", platformId);
        }
        tag.putString("TownName", cachedTownName);
        tag.putString("PlatformName", cachedPlatformName);
    }

    private void readLink(CompoundTag tag) {
        townId = tag.hasUUID("TownId") ? tag.getUUID("TownId") : null;
        platformId = tag.hasUUID("PlatformId") ? tag.getUUID("PlatformId") : null;
        cachedTownName = tag.contains("TownName") ? tag.getString("TownName") : "";
        cachedPlatformName = tag.contains("PlatformName") ? tag.getString("PlatformName") : "";
    }
}
