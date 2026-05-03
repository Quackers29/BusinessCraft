# BusinessCraft - Completed Tasks

✅ **COMPLETED** - Phase 11 Global Toggles: Added server-wide enable/disable for tourists/contracts/research via TOML (ConfigLoader.*Enabled). Global overrides per-town tourist toggle. Wrapped ticks (Town.java) and spawns (TownInterfaceEntity). Hot-reload supported; tested Forge/Fabric.

✅ **COMPLETED** - Phase 12 VM Cache/Sync: ViewModelCache unified 5 caches. Dirty sync 4 townVMs (Resource/Prod/Upgrade/Interface). Tick poll1s update/sync dirty only. Removed 8 old sync* methods. Trading/market separate. Menu152 fixed.

✅ **COMPLETED** - Phase 12.3 Packet Boilerplate: BaseViewModelSyncPacket + IViewModel. Refact 4 packets Resource/Prod/Upgrade/TownInterfaceVM (~80→40L each, 160L saved). DebugConfig.NETWORK_PACKETS toggle. Fixes ctor/decode/import/T cast.
