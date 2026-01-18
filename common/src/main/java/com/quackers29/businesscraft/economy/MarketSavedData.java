package com.quackers29.businesscraft.economy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import com.quackers29.businesscraft.api.PlatformAccess;

/**
 * Persists GlobalMarket data.
 * MUST only be attached to the Overworld (or primary server level) to ensure
 * singleton consistency.
 */
public class MarketSavedData extends SavedData {
    public static final String NAME = PlatformAccess.getPlatform().getModId() + "_global_market";

    private final GlobalMarket market;

    public MarketSavedData() {
        this.market = GlobalMarket.get();
        this.market.setDirtyCallback(this::setDirty);
        // Do NOT reset market here, as this might be called AFTER load
        // But create() is called when no data exists, so we should allow fresh state.
        // Since GlobalMarket is singleton, we assume it's already initialized.
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        market.save(tag);
        return tag;
    }

    public void loadFromNbt(CompoundTag tag) {
        market.load(tag);
    }

    public static MarketSavedData create() {
        // If we are creating fresh data, it means no file existed.
        // We should reset the global market to a clean state.
        GlobalMarket.get().reset();
        return new MarketSavedData();
    }

    public static MarketSavedData load(CompoundTag tag) {
        MarketSavedData data = new MarketSavedData();
        data.loadFromNbt(tag);
        return data;
    }
}
