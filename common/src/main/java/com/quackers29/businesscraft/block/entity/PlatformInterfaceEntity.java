package com.quackers29.businesscraft.block.entity;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.platform.PlatformTouristCleanup;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.town.data.PlatformManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * A placed platform. It never owns a town. While linked, the town's platform list holds the path,
 * enabled flag, and destination allow-list. This block only remembers which town and which platform.
 */
public class PlatformInterfaceEntity extends BlockEntity {
    public static final String REASON_TOWN_GONE = "businesscraft.platform_interface.town_gone";
    public static final String REASON_AT_CAP = "businesscraft.platform_interface.at_cap";
    public static final String REASON_DUPLICATE = "businesscraft.platform_interface.duplicate_owner";
    public static final String REASON_NOT_OWNER = "businesscraft.platform_interface.not_owner";
    public static final String REASON_NOT_LINKED = "businesscraft.platform_interface.not_linked";

    private UUID townId;
    private UUID platformId;
    private BlockPos townCentre;
    private boolean active;
    private String inactiveReason = REASON_NOT_LINKED;
    /** Chunk load has no shared hook on both loaders, so the first server tick refreshes the link. */
    private boolean refreshedAfterLoad;

    public PlatformInterfaceEntity(BlockPos pos, BlockState state) {
        super(platformInterfaceType(), pos, state);
    }

    @SuppressWarnings("unchecked")
    private static net.minecraft.world.level.block.entity.BlockEntityType<PlatformInterfaceEntity> platformInterfaceType() {
        return (net.minecraft.world.level.block.entity.BlockEntityType<PlatformInterfaceEntity>) PlatformAccess
                .getBlockEntities().getPlatformInterfaceEntityType();
    }

    public void link(UUID townId, UUID platformId, BlockPos townCentre) {
        this.townId = townId;
        this.platformId = platformId;
        this.townCentre = townCentre == null ? null : townCentre.immutable();
        this.active = true;
        this.inactiveReason = null;
        syncToClients();
    }

    public UUID getTownId() {
        return townId;
    }

    public UUID getPlatformId() {
        return platformId;
    }

    public BlockPos getTownCentre() {
        return townCentre;
    }

    public boolean isActive() {
        return active;
    }

    public String getInactiveReason() {
        return inactiveReason;
    }

    public void markInactive(String reasonKey) {
        String reason = reasonKey == null ? REASON_NOT_LINKED : reasonKey;
        boolean changed = active || !reason.equals(inactiveReason);
        active = false;
        inactiveReason = reason;
        if (changed) {
            syncToClients();
            announce(reason);
        }
    }

    public void markActive() {
        if (!active || inactiveReason != null) {
            active = true;
            inactiveReason = null;
            syncToClients();
        }
    }

    private void syncToClients() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            net.minecraft.world.level.block.state.BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void announce(String reasonKey) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        Component message = Component.translatable("businesscraft.platform_interface.inactive",
                Component.translatable(reasonKey));
        Vec3 centre = Vec3.atCenterOf(getBlockPos());
        for (ServerPlayer player : server.players()) {
            if (player.distanceToSqr(centre) <= 32.0 * 32.0) {
                player.displayClientMessage(message, false);
            }
        }
    }

    public void refreshLink(ServerLevel level) {
        if (townId == null || platformId == null) {
            if (active) {
                markInactive(REASON_NOT_LINKED);
            } else if (inactiveReason == null) {
                inactiveReason = REASON_NOT_LINKED;
            }
            return;
        }
        Town town = TownManager.get(level).getTown(townId);
        if (town == null) {
            markInactive(REASON_TOWN_GONE);
            return;
        }
        if (townCentre == null) {
            townCentre = town.getPosition().immutable();
        }
        if (!(level.getBlockEntity(townCentre) instanceof TownInterfaceEntity townEntity)) {
            return;
        }
        PlatformManager.RelinkResult result = townEntity.relinkPlatform(platformId, getBlockPos());
        switch (result) {
            case LINKED -> markActive();
            case RELINKED -> {
                markActive();
                townEntity.syncPlatforms();
            }
            case AT_CAP -> markInactive(REASON_AT_CAP);
            case DUPLICATE -> markInactive(REASON_DUPLICATE);
            case NOT_OURS -> markInactive(REASON_NOT_OWNER);
        }
    }

    /**
     * Called when this block is broken. Removes the platform it owns and dismisses loaded tourists on it.
     * A duplicate block that does not own the platform leaves the real one in place.
     */
    public void onBroken(ServerLevel level) {
        if (townId == null || platformId == null || townCentre == null) {
            return;
        }
        level.getChunkAt(townCentre);
        if (!(level.getBlockEntity(townCentre) instanceof TownInterfaceEntity town)) {
            return;
        }
        if (!townId.equals(town.getTownId())) {
            return;
        }
        Platform platform = town.getPlatform(platformId);
        if (platform == null || !getBlockPos().equals(platform.getInterfacePos())) {
            return;
        }
        town.removePlatform(platformId);
    }

    public void tickServer() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!refreshedAfterLoad || serverLevel.getGameTime() % 40L == 0L) {
            refreshedAfterLoad = true;
            refreshLink(serverLevel);
        }
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

    private void writeLink(CompoundTag tag) {
        if (townId != null) {
            tag.putUUID("TownId", townId);
        }
        if (platformId != null) {
            tag.putUUID("PlatformId", platformId);
        }
        if (townCentre != null) {
            tag.putInt("TownX", townCentre.getX());
            tag.putInt("TownY", townCentre.getY());
            tag.putInt("TownZ", townCentre.getZ());
        }
        tag.putBoolean("Active", active);
        if (inactiveReason != null) {
            tag.putString("InactiveReason", inactiveReason);
        }
    }

    private void readLink(CompoundTag tag) {
        townId = tag.hasUUID("TownId") ? tag.getUUID("TownId") : null;
        platformId = tag.hasUUID("PlatformId") ? tag.getUUID("PlatformId") : null;
        if (tag.contains("TownX")) {
            townCentre = new BlockPos(tag.getInt("TownX"), tag.getInt("TownY"), tag.getInt("TownZ"));
        } else {
            townCentre = null;
        }
        active = tag.getBoolean("Active");
        inactiveReason = tag.contains("InactiveReason") ? tag.getString("InactiveReason") : null;
        if (!active && inactiveReason == null) {
            inactiveReason = REASON_NOT_LINKED;
        }
    }
}
