# Loop Log

One line per completed iteration: `YYYY-MM-DD | ID | STATUS | summary`

---
2026-06-11 | T-001 | DONE | Distance payment formula documented + 10 tests (reflection on private calculatePayment); count=0 quirk pinned; pilot iteration validated protocol
2026-06-11 | T-002 | NEEDS-MC | DistanceMilestoneHelper documented (8 tests for guards+getters); positive threshold+reward path untestable in pure JUnit (ItemStack static init crash on parse branch) — 1 iteration complete
2026-06-12 | T-003 | NEEDS-MC | Auction resolution documented (closeAuctions flow, courier cost ceil(dist/10), bid rounding, snail fallback); 11 tests for winner selection (getHighest* + addBid raise-only), Sell clamps, bid NBT roundtrip; courier+orchestration need MC bootstrap (Town ctor EIIE) — 1 iteration complete
2026-06-13 | T-004 | DONE | Sell contract lifecycle documented + 20 tests (clamps, delivery/courier/snail/complete states, expiry manip, exhaustive NBT roundtrips); extendExpiry from-now behavior pinned; full suite green — 1 iteration complete

