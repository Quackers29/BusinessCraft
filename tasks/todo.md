# Phase 12: VM Cache Consolidation

## 12.1 Cache ✅ COMPLETE
- ViewModelCache.java: ConcurrentHashMap<Class,Object> cache + dirty. update(T), get(T), isDirty(Class), syncAllDirty(Consumer<Object> sender=packet.send).
- TownInterfaceEntity: private final ViewModelCache vmCache; getVmCache(). Removed 5 cached*VM fields/dupe sync logic. Saved ~250 lines.
- Build/test OK.

## 12.2 Dirty Sync Unification 🔧 PLAN
**Current problem:** 5 separate sync*ViewModelToNearbyPlayers (Resource/Prod/Upgrade/Interface/Trading). Tick every 10t (0.5s) builds/sends all to all players always. No change detection = network spam.

**Goal:** Poll %20 (1s) update VMs to cache/mark dirty. Sync dirty only. ~80% less packets.

**VMs:**
| VM | Builder | Packet | Pos? |
|----|---------|--------|------|
| TownResourceViewModel | buildResourceViewModel(town) | ResourceViewModelSyncPacket(pos,vm) | Y |
| ProductionStatusViewModel | buildProductionViewModel(town) | ProductionViewModelSyncPacket(pos,vm) | Y |
| UpgradeStatusViewModel | buildUpgradeViewModel(town) | UpgradeViewModelSyncPacket(pos,vm) | Y |
| TownInterfaceViewModel | build(town,this) | TownInterfaceViewModelSyncPacket(pos,vm) | Y |
| TradingViewModel | build(town) | TradingViewModelSyncPacket(vm) | N |

Market global separate (static timer).

**Steps (TownInterfaceEntity.java):**
1. **update*VM() private (5 methods):** Town t=getTown(); if(t) vm=Builder.build(t); vmCache.update(VM.class,vm); // poll always markDirty OK

2. **updateAllVMs():** call 5 update*VM().

3. **syncAllDirtyViewModelsToPlayers():**
   ```
   if(!(level instanceof ServerLevel sl)) return;
   vmCache.syncAllDirty(vm -> {
     BlockPos pos = getBlockPos();
     if(vm instanceof TownResourceViewModel r) {
       ResourceViewModelSyncPacket p = new ResourceViewModelSyncPacket(pos,r);
       sl.players().forEach(pl -> send(p,pl));
     }
     // +4 more instanceof
   });
   ```

4. **Tick ~922:** replace 5 sync* → if(ServerLevel) { updateAllVMs(); syncAllDirtyTownVMsToPlayers(); syncTradingToNearby(); syncMarketToAll(); }

   syncAllDirtyTownVMs: Resource/Prod/Upgrade/Interface (all players, pos).
   syncTradingToNearby keep (64b loop, no pos).

5. **Remove** 5 sync*ToNearby/ToPlayer methods (grep unused).

**Change detection:** Poll %20 always dirty fine (fast builders). Future: VM.hashCode/version.

**Trading:** Keep syncTradingViewModelToNearbyPlayers (64b). Others all players.

**Risks/fixes:**
- instanceof exhaustive (5 cases).
- Trading no pos (code).
- Null town (if-check).
- Import (full qual).
- Perf (poll 1s negligible).

**Verify:**
- `./gradlew clean build` Forge/Fabric.
- RunClient: tabs data sync.
- Hopper add resource: debug log Resource packet only?
- Network: less packets idle.

Approve → implement 5 targeted Edits.