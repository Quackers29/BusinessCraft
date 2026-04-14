# Phase 12 ✅ VM Cache/Sync

## Phase 12.3 Packet Boilerplate 🔧
**Dupe:** 6+ ViewModelSyncPacket writeVM/readVM identical.
**Plan:** BaseViewModelSyncPacket<T extends ViewModel> generic write/read. Extend for Resource/Prod etc.
Effort: 50L base + refactor 6 packets.
Benefit: 90→30L/packet.

## Phase 12.4 Client Cache Consolidate 🔧
**Dupe:** TownDataCacheManager client resources/wanted/escrow/visitHistory separate maps.
**Plan:** ClientViewModelCache like server vmCache. Single map + sync helpers.
Effort: 100L. 
TownInterfaceEntity.getClientResources() → cache.get(ClientResourceVM.class).resources

## Phase 12.5 Helper Dupe 🔧
**Dupe:** VisitorProcessingHelper/TouristSpawningHelper/NBTDataHelper similar logic.
**Plan:** BaseHelper or merge.

Prior? Approve → detail.