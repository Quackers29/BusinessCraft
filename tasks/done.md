# BusinessCraft - Completed Tasks

✅ **COMPLETED** - Phase 11 Global Toggles: Added server-wide enable/disable for tourists/contracts/research via TOML (ConfigLoader.*Enabled). Global overrides per-town tourist toggle. Wrapped ticks (Town.java) and spawns (TownInterfaceEntity). Hot-reload supported; tested Forge/Fabric.

✅ **COMPLETED** - Phase 12 VM Cache/Sync: ViewModelCache unified 5 caches. Dirty sync 4 townVMs (Resource/Prod/Upgrade/Interface). Tick poll1s update/sync dirty only. Removed 8 old sync* methods. Trading/market separate. Menu152 fixed.

✅ **COMPLETED** - Phase 12.3 Packet Boilerplate: BaseViewModelSyncPacket + IViewModel. Refact 4 packets Resource/Prod/Upgrade/TownInterfaceVM (~80→40L each, 160L saved). DebugConfig.NETWORK_PACKETS toggle. Fixes ctor/decode/import/T cast.

✅ **COMPLETED** - Tourist Behavior Enhancement: Added 3 custom AI Goals (`TouristGossipGoal`, `TouristGazeGoal`, `TouristTargetGazeGoal`) using only vanilla `Goal`, `LookControl`, `playSound(SoundEvents.VILLAGER_*)`, and `swing()`. Gossip (nearby tourists look + ambient), window/target gazing while riding, speed celebration on fast movement (>5b/s with arm wave), UI sounds (YES on open, NO on close), closer player look range (3.0F). Full integration in `TouristEntity` (registerGoals, tick, NBT, methods). Fabric client tested successfully (visual behaviors functional; audio limited by WSL). All testing checklist items verified.



✅ **COMPLETED** - Tiered Tourist Skins (manual approach): Added synced `DATA_SKIN_TIER` EntityDataAccessor on TouristEntity. Skin tier set at spawn via `syncSkinTierFromLevel()` (level 1→basic, 2→experienced, 3→luxury), updated on distance level-up and world reload. TouristRenderer reads stored tier from `getSkinTier()` with vanilla villager fallback. Hat layer unchanged. Resource paths: `tourist_basic.png`, `tourist_experienced.png`, `tourist_luxury.png` (experienced/luxury placeholders copied from basic until custom art added). Also added missing `getTotalDistanceTraveled()` getter for VisitorProcessingHelper. Forge/Fabric/common compile verified.
