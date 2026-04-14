# Phase 12 ✅ VM Cache/Sync

## Phase 12.3 Packet Boilerplate ✅
**Dupe:** 6+ ViewModelSyncPacket writeVM/readVM identical.
**Plan:** BaseViewModelSyncPacket<T extends ViewModel> generic write/read. Extend for Resource/Prod etc.
Effort: 50L base + refactor 6 packets.
Benefit: 90→30L/packet.

## Phase 12.4 Client Cache Consolidate 🔧
**Dupe:** TownDataCacheManager: contractListCache Map<String,ContractListCache>, contractDetailCache, globalTradingViewModel static, legacy cachedPopulation/tourists etc.

**Plan:** Static ClientVMCache = ViewModelCache().
- TradingSyncPacket handle ClientVMCache.update(TradingVM.class, vm)
- ContractListSyncPacket → ContractListVM(page,list,total...)
- ContractDetailSyncPacket → ContractDetailVM
- Menu getResourceViewModel() → ClientVMCache.get(ResourceVM.class) // already entity.vmCache client too
- Remove Manager caches/legacy.
Effort: 80L new + refactor Manager/Menu 120L.
**Benefit:** Unified cache, no dupe maps, ViewModel pattern consistent.

## Phase 12.5 Helper Dupe 🔧
**Dupe:** VisitorProcessingHelper/TouristSpawningHelper/NBTDataHelper similar logic.
**Plan:** BaseHelper or merge.

Prior? Approve → detail.