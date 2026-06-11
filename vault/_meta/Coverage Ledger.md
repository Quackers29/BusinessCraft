# Coverage Ledger

Master tracking for the Test + Docs Loop (`tasks/test_doc_loop.md`). Rows are in **priority order** — loop agents take the first `TODO` from the top. Statuses: `TODO`, `IN-PROGRESS`, `DONE`, `DOC-ONLY`, `NEEDS-MC`, `BLOCKED`, `BUG-FOUND` (definitions in the protocol doc).

All code paths are relative to `common/src/main/java/com/quackers29/businesscraft/`. Loop agents must verify testability themselves.

**2026-06-11 senior review**: All dates corrected (agents had invented sequential future dates — use the real date from `wsl date +%F`). Six former `NEEDS-MC` rows re-opened to `TODO`: the new `McBootstrap` fixture (`common/src/test/java/.../testutil/McBootstrap.java`) initializes vanilla registries so ItemStack-dependent logic is now testable. Re-opened rows should EXTEND their existing test files/vault notes, not rewrite them. Targets needing a live Level/world/entities remain NEEDS-MC.

| ID | Vault note path | Code target | Status | Test file | Date | Notes |
|----|----------------|-------------|--------|-----------|------|-------|
| T-001 | Economy/Tourist Payments/Distance Payment Calculation | `town/data/VisitorProcessingHelper.java` | DONE | `.../town/data/VisitorProcessingHelperTest.java` | 2026-06-11 | 10 tests via reflection; count=0 quirk pinned |
| T-002 | Economy/Milestones/Distance Milestone Resolution | `town/data/DistanceMilestoneHelper.java` | DONE | `.../town/data/DistanceMilestoneHelperTest.java` | 2026-06-11 | 17 tests (McBootstrap + registry double); highest-wins selection + scaling + bad-reward resilience covered; count=0 quirk pinned |
| T-003 | Trade/Contracts/Auction Resolution | `contract/ContractBoard.java` | DONE | `.../contract/ContractBoardTest.java` | 2026-06-11 | 7 new courier cost tests (McBootstrap + Town/BlockPos); total 18; core bid math + formula covered |
| T-004 | Trade/Contracts/Sell Contract Lifecycle | `contract/SellContract.java` + `contract/Contract.java` | DONE | `.../contract/SellContractTest.java` | 2026-06-11 | 20 tests; state machine + NBT roundtrips |
| T-005 | Trade/Contracts/Courier Delivery Rewards | `contract/CourierContract.java` + `ContractBoard.processCourierDelivery()` | DONE | `.../contract/CourierContractTest.java` | 2026-06-11 | 12 tests; payout path may now be reachable with McBootstrap (optional follow-up) |
| T-006 | Trade/Global Market/Price Calculation | `economy/GlobalMarket.java` | DONE | `.../economy/GlobalMarketTest.java` | 2026-06-11 | 22 tests; 90/10 blend, 5% drop, MIN floor |
| T-007 | Town/Resources/Resource Storage Operations | `town/components/TownResources.java` + `TownEconomyComponent.java` | DONE | `.../town/components/TownResourcesTest.java` | 2026-06-11 | 22 tests (18 new); overflow/zero-retention/NBT + pop quirk pinned |
| T-008 | Town/Boundaries/Town Distance Validation | `town/service/TownBoundaryService.java` + `TownValidationService.java` | TODO | `.../town/service/TownValidationServiceTest.java`, `.../TownBoundaryServiceTest.java` | 2026-06-11 | RE-OPENED: verify whether McBootstrap unblocks Town ctor for placement/expansion math; extend the 27 existing tests |
| T-009 | Tourists/Capacity/Tourist Allocation | `town/utils/TouristAllocationTracker.java` | DONE | `.../town/utils/TouristAllocationTrackerTest.java` | 2026-06-11 | 12 tests; recordSpawn dead in prod (quirk pinned) |
| T-010 | Town/Visits/Visit Buffer | `town/data/VisitBuffer.java` | DONE | `.../town/data/VisitBufferTest.java` | 2026-06-11 | 20 tests; global-timer coalescing pinned |
| T-011 | Town/Leaderboard/Ranking Calculation | `town/data/TownLeaderboardData.java` | DONE | `.../town/data/TownLeaderboardDataTest.java` | 2026-06-11 | 15 DTO tests; sort/column logic is screen-only (documented) |
| T-012 | Town/Payment Board/Reward Claims | `town/data/TownPaymentBoard.java` + `RewardEntry.java` + `ClaimStatus.java` | TODO | `.../testutil/McBootstrapValidationTest.java` (starter) | 2026-06-11 | RE-OPENED: McBootstrap proven on this exact target (4 starter tests exist); write full eligibility/expiry/claim/trim coverage per the vault note |
| T-013 | Town/Storage/Slot-Based Storage | `town/data/SlotBasedStorage.java` | TODO | | 2026-06-11 | RE-OPENED: McBootstrap unblocks ItemStack ctor; rules already fully documented in vault note — write the tests |
| T-014 | Config/Configuration Loading | `config/ConfigLoader.java` | DONE | `.../config/ConfigLoaderTest.java` | 2026-06-11 | 16 tests; empty-vs-invalid milestone list quirk pinned |
| T-015 | Town/Platforms/Platform Data Model | `platform/Platform.java` | DONE | `.../platform/PlatformTest.java` | 2026-06-11 | 25 tests; NBT roundtrips, dest sets, defensive copies |

## Adding new rows
When the seeds run out (or you find a better target while reading code), append rows with the next T-### ID. Keep the vault note path in `System/Subsystem/Process Name` form, matching the area taxonomy in `vault/Home.md`. Big classes (e.g. `Town.java`) should be split across multiple rows by concern.
