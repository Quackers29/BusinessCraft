# Loop Log

One line per completed iteration: `YYYY-MM-DD | ID | STATUS | summary`. Use the REAL current date (`wsl date +%F`), never a guessed one.

---
2026-06-11 | T-001 | DONE | Distance payment formula documented + 10 tests (reflection on private calculatePayment); count=0 quirk pinned; pilot iteration validated protocol
2026-06-11 | T-002 | NEEDS-MC | DistanceMilestoneHelper documented; 8 guard/getter tests; positive threshold+reward path blocked by ItemStack static init
2026-06-11 | T-003 | NEEDS-MC | Auction resolution documented (closeAuctions flow, courier cost ceil(dist/10), bid rounding, snail fallback); 11 tests; courier+orchestration blocked by Town ctor
2026-06-11 | T-004 | DONE | Sell contract lifecycle documented + 20 tests (clamps, state machine, NBT roundtrips); extendExpiry from-now pinned
2026-06-11 | T-005 | DONE | CourierContract delivery rewards documented + 12 tests (accumulator, NBT, no-clamp pinned); wrong-town gate lives in TownInterfaceEntity
2026-06-11 | T-006 | DONE | GlobalMarket price calc documented + 22 tests (90/10 blend, 5% failed-auction drop, MIN floor, load/save hygiene)
2026-06-11 | T-007 | NEEDS-MC | Resource storage ops documented; 4 guard/population tests; add/remove/overflow/NBT blocked by Item bootstrap
2026-06-11 | T-008 | NEEDS-MC | Town distance validation: 27 tests (name/pos/radius/tourist guards); placement/expansion math + pop-vs-50-border discrepancy documented; Town ctor blocked
2026-06-11 | T-009 | DONE | Tourist allocation (pop-proportional fairness) documented + 12 tests; recordSpawn dead in prod (quirk pinned)
2026-06-11 | T-010 | DONE | VisitBuffer batch/dedup/flush documented + 20 tests; global-timer cross-origin coalescing pinned as quirk
2026-06-11 | T-011 | DONE | TownLeaderboardData DTO documented + 15 tests (distanceTo, formatDistance); sort/column math is screen-only
2026-06-11 | T-012 | NEEDS-MC | Reward claims fully documented (eligibility, 7d expiry, cleanup, 100-cap trim); 0 tests (ItemStack bootstrap blocker)
2026-06-11 | T-013 | NEEDS-MC | Slot-based storage fully documented (two-pass addItem, NBT, 18-slot buffer); 0 tests (ItemStack bootstrap blocker)
2026-06-11 | T-014 | DONE | ConfigLoader TOML parsing/defaults/milestones documented + 16 tests; empty-vs-invalid milestone list quirk pinned
2026-06-11 | T-015 | DONE | Platform data model documented + 25 tests (NBT, destination sets, isComplete, defensive copies)
2026-06-11 | REVIEW | — | Senior review of T-002..T-015: all green (208 tests), quality high; built McBootstrap registry fixture (+4 validation tests on RewardEntry); re-opened T-002/003/007/008/012/013 to TODO; corrected invented dates; protocol tightened
2026-06-11 | T-002 | DONE | Distance milestone resolution extended (docs+tests); 9 positive-path tests added via McBootstrap (total 17); selection/scaling/parse resilience covered with hand-computed expects; count=0 zero-stack quirk pinned; full suite green
2026-06-11 | T-003 | DONE | Auction resolution extended (docs+tests); 7 courier cost distance tests added via McBootstrap+Town ctor (total 18); bid selection + ceil(dist/10) covered with hand-computed expects; full suite green; closeAuctions orchestration still NEEDS-MC
2026-06-11 | T-007 | DONE | Resource storage ops (TownResources + TownEconomyComponent): 22 tests total (18 new via McBootstrap); Math.addExact cap, max(0) clamp+zero retention, consume, getAll live view, NBT roundtrips+sanitize, delegation, pop quirk pinned; full suite green
2026-06-11 | T-008 | DONE | McBootstrap unblocks Town ctor; 20 TownBoundaryServiceTest (15 new placement/expansion/calc math with hand-computed dists) + 22 ValidationServiceTest; pop-vs-border discrepancy pinned by tests; full suite green; docs extended first
2026-06-11 | T-012 | BUG-FOUND | Reward claims (pre-written note extended); 33 tests (RewardEntry 15 + TownPaymentBoard 18); full elig/expiry/claim/7d-force/100-trim/30d-prune-by-ts/buffer-full/NBT + hand-computed; 2 @Disabled real bugs in toBuffer partial side-effects (leak while UNCLAIMED + excess loss on success); full suite green; docs first
2026-06-11 | T-013 | DONE | SlotBasedStorage documented (pre-written note); 29 tests via McBootstrap; two-pass addItem, NBT slotCount guard, copyFrom size handling, tag asymmetry (remove ignores tags) + 0-slot pinned; full suite green
2026-06-11 | T-016 | DONE | Effect value scaling (linear vs exponential by upgrade level) documented + 14 pure-logic tests; all hand-computed; full suite green; first Production area coverage + overview navigation added
2026-06-11 | T-017 | DONE | Time display formatting (BCTimeUtils) documented + 29 pure tests (durations, remaining, time-ago, isExpired, zoned dates, tz config); all hand-computed edges + save/restore; new Core area + overview; full suite green
2026-06-11 | T-018 | DONE | Result<T,E> monad documented + 32 pure tests (success/failure/map/flatMap/getOrElse/fromOperation/equals + null/BCError edges); pinned success!=failure-same-payload quirk; full suite green; docs first then tests
2026-06-11 | T-019 | DONE | Resource type csv load + expand fuzzy matching + getFor/getAllFor/getUnitValue documented + 15 tests (McBootstrap + TestRegistryHelper + platform stub + map snapshot); food saturation ratios + iron/wood/coal heuristics + parse edges with hand-computed; full suite green
2026-06-11 | T-020 | DONE | Contract VM builders (summary+detail) documented first + 23 pure tests (tab filter+sort+paging+status+canBid+bid-list+generic+time delegation); "Auction" vs "Auction Open" + unused player param pinned as quirks; full suite green
2026-06-11 | T-021 | DONE | DataParser (effects/conds/resources) documented first + 16 pure tests (no bootstrap); realistic upgrade strings, all operators/aliases/%/*, tolerant bad-input, := value quirk pinned; full suite green
2026-06-11 | T-022 | DONE | ContractItemHelper (create + inspectors + NBT/lore/fallback) documented + 12 tests (McBootstrap + TestRegistryHelper + map snapshot); capitalize + creationTime + paper fallback covered with hand-computed; null-NPE quirk pinned as unreachable; full suite green
2026-06-11 | T-023 | DONE | PlatformManager documented + 26 tests (cap, mutations+notify, defensive copies, getEnabled filter, save/load/updateClient NBT, legacy one-shot create, counts/mode/clear); full suite green; docs first
2026-06-11 | T-024 | DONE | ContainerDataHelper pure logic documented + 22 tests (registration/indexing, name+index get/set, readonly enforcement, unknown-name guards, markDirty/markAll, builder, clamping sim); full suite green; docs first
2026-06-11 | T-025 | DONE | ProductionRegistry effort/price estimator documented + 13 pure tests (reflection on calculateEstimatedValues + resolveQuantity, map snapshot/restore); simple yield, chained, min-of-recipes, 0-qty guard, expr resolve, cycle fallback quirk + AIOOBE-on-bad-expr pinned; full suite green; docs first then tests + overview nav updated
2026-06-11 | T-026 | DONE | BCError error factories + exact message templates documented + 26 pure tests (all 7 categories + null/num/empty edges + fromException); null-message quirk pinned; full suite green; docs first
2026-06-11 | T-027 | DONE | Tourist capacity (minPop + tourist_cap modifier) documented + 16 tests; hand-computed edges + config save/restore + TownService orchestration; full suite green; docs first
2026-06-11 | T-028 | DONE | Base Contract bid clamping (max per bidder), highest selection (tie pins iteration), extend/expire always from now, full bids NBT roundtrip; 14 pure tests (no bootstrap); quirk pinned (neg offer -> 0 via max(0)); full suite green; docs first.
