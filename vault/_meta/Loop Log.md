# Loop Log

One line per completed iteration: `YYYY-MM-DD | ID | STATUS | summary`

---
2026-06-11 | T-001 | DONE | Distance payment formula documented + 10 tests (reflection on private calculatePayment); count=0 quirk pinned; pilot iteration validated protocol
2026-06-11 | T-002 | NEEDS-MC | DistanceMilestoneHelper documented (8 tests for guards+getters); positive threshold+reward path untestable in pure JUnit (ItemStack static init crash on parse branch) — 1 iteration complete
2026-06-12 | T-003 | NEEDS-MC | Auction resolution documented (closeAuctions flow, courier cost ceil(dist/10), bid rounding, snail fallback); 11 tests for winner selection (getHighest* + addBid raise-only), Sell clamps, bid NBT roundtrip; courier+orchestration need MC bootstrap (Town ctor EIIE) — 1 iteration complete
2026-06-13 | T-004 | DONE | Sell contract lifecycle documented + 20 tests (clamps, delivery/courier/snail/complete states, expiry manip, exhaustive NBT roundtrips); extendExpiry from-now behavior pinned; full suite green — 1 iteration complete
2026-06-14 | T-005 | DONE | CourierContract delivery rewards documented + 12 tests (accumulator, isDelivered >=, full NBT incl. source pos + reward float, no-clamp pinning); reward is ctor input (no calc inside); wrong-town destination gate is in TownInterfaceEntity (not processCourierDelivery); full suite green — 1 iteration complete
2026-06-15 | T-006 | DONE | GlobalMarket price calc documented + 22 tests (default 1.0, 90/10 blend on recordTrade w/ long vol cast, 5% drop on recordFailedAuction, MIN floor everywhere, full load/save hygiene+repair, getPrices semantics); full suite green — 1 iteration complete
2026-06-16 | T-007 | NEEDS-MC | Resource storage ops documented in full; only 4 null-guard + population tests possible (Item construction triggers "Not bootstrapped" registry init, same as T-002); main add/remove/overflow/NBT math described from code — 1 iteration complete
2026-06-17 | T-008 | NEEDS-MC | Town Distance Validation: 27 tests (22 name/pos/radius/tourist/validation pure + 5 boundary null guards); full placement/expansion math + pop-vs-50-border discrepancy documented from code (Town ctor EIIE blocks); Town Overview + vault note updated; full suite green — 1 iteration complete

