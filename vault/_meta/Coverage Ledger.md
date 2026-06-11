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
| T-008 | Town/Boundaries/Town Distance Validation | `town/service/TownBoundaryService.java` + `TownValidationService.java` | DONE | `.../town/service/TownValidationServiceTest.java`, `.../TownBoundaryServiceTest.java` | 2026-06-11 | McBootstrap unblocks Town ctor; 20 boundary tests (15 new math) + 22 validation; full suite green |
| T-009 | Tourists/Capacity/Tourist Allocation | `town/utils/TouristAllocationTracker.java` | DONE | `.../town/utils/TouristAllocationTrackerTest.java` | 2026-06-11 | 12 tests; recordSpawn dead in prod (quirk pinned) |
| T-010 | Town/Visits/Visit Buffer | `town/data/VisitBuffer.java` | DONE | `.../town/data/VisitBufferTest.java` | 2026-06-11 | 20 tests; global-timer coalescing pinned |
| T-011 | Town/Leaderboard/Ranking Calculation | `town/data/TownLeaderboardData.java` | DONE | `.../town/data/TownLeaderboardDataTest.java` | 2026-06-11 | 15 DTO tests; sort/column logic is screen-only (documented) |
| T-012 | Town/Payment Board/Reward Claims | `town/data/TownPaymentBoard.java` + `RewardEntry.java` + `ClaimStatus.java` | BUG-FOUND | `.../town/data/RewardEntryTest.java`, `.../town/data/TownPaymentBoardTest.java` (+ starter) | 2026-06-11 | 33 tests (15+18); 7d/30d/100/elig/claim/trim/NBT/buffer covered w/ hand-computed; 2 @Disabled BUGs (partial toBuffer leak+loss) |
| T-013 | Town/Storage/Slot-Based Storage | `town/data/SlotBasedStorage.java` | DONE | `.../town/data/SlotBasedStorageTest.java` | 2026-06-11 | 29 tests (McBootstrap); two-pass add, NBT guard, tag asymmetry quirk pinned |
| T-014 | Config/Configuration Loading | `config/ConfigLoader.java` | DONE | `.../config/ConfigLoaderTest.java` | 2026-06-11 | 16 tests; empty-vs-invalid milestone list quirk pinned |
| T-015 | Town/Platforms/Platform Data Model | `platform/Platform.java` | DONE | `.../platform/PlatformTest.java` | 2026-06-11 | 25 tests; NBT roundtrips, dest sets, defensive copies |
| T-016 | Production/Upgrades/Effect Value Calculation | `production/UpgradeNode.java` | DONE | `.../production/UpgradeNodeTest.java` | 2026-06-11 | 14 tests; linear+exp scaling + level<=0 edges |
| T-017 | Core/Time/Time Display Formatting | `util/BCTimeUtils.java` | DONE | `.../util/BCTimeUtilsTest.java` | 2026-06-11 | 29 tests; all duration/expiry/timezone rules + edges |
| T-018 | Core/Util/Result Type | `util/Result.java` | DONE | `.../util/ResultTest.java` | 2026-06-11 | 32 tests; pure monad paths + null/edge/BCError cases (no bootstrap) |
| T-019 | Economy/Resources/Resource Type Expansion and Lookup | `economy/ResourceRegistry.java` + `economy/ResourceType.java` | DONE | `.../economy/ResourceRegistryTest.java` | 2026-06-11 | 15 tests; csv+expand heuristics+get*For covered |
| T-020 | Trade/Contracts/Contract List and Detail ViewModels | `contract/viewmodel/ContractSummaryViewModelBuilder.java` + `contract/viewmodel/ContractDetailViewModelBuilder.java` | DONE | `.../contract/ContractSummaryViewModelBuilderTest.java`, `.../contract/ContractDetailViewModelBuilderTest.java` | 2026-06-11 | 23 tests; tab filter/sort/paging/status/can*/bids + generic; status string diff quirk pinned |
| T-021 | Config/Data Parsing | `data/parsers/DataParser.java` | DONE | `.../data/parsers/DataParserTest.java` | 2026-06-11 | 16 pure tests; aliases/ops/edges/:= quirk pinned |
| T-022 | Trade/Contracts/Contract Item Creation and Inspection | `util/ContractItemHelper.java` | DONE | `.../util/ContractItemHelperTest.java` | 2026-06-11 | 12 tests; NPE-on-null quirk pinned |
| T-023 | Town/Platforms/Platform Management | `town/data/PlatformManager.java` | DONE | `.../town/data/PlatformManagerTest.java` | 2026-06-11 | 26 tests; cap/notify/NBT/legacy/client-snapshot/filter covered |
| T-024 | Town/Data Synchronization/Container Data Registration | `town/data/ContainerDataHelper.java` | DONE | `.../town/data/ContainerDataHelperTest.java` | 2026-06-11 | 22 pure tests (no bootstrap); dup/readonly/index/name/dirty/builder covered |
| T-025 | Production/Recipes/Estimated Effort Calculation | `production/ProductionRegistry.java` | DONE | `.../production/ProductionRegistryTest.java` | 2026-06-11 | 13 tests; effort recursion + 20× seeding + resolve/cycle quirks pinned via reflection |
| T-026 | Core/Util/Error Types | `util/BCError.java` | DONE | `.../util/BCErrorTest.java` | 2026-06-11 | 26 pure tests; all factories + null/num edges + toString; null-message quirk pinned |
| T-027 | Tourists/Capacity/Tourist Capacity Calculation | `town/service/TownService.java` | DONE | `.../town/service/TownServiceTest.java` | 2026-06-11 | 16 tests; spawn eligibility + tourist_cap modifier + add/remove paths |
| T-028 | Trade/Contracts/Bid Selection and Clamping | `contract/Contract.java` | DONE | `.../contract/ContractTest.java` | 2026-06-11 | 14 pure tests; bid clamp + highest + extend-from-now + NBT bids |
| T-029 | Town/Production/Recipe Execution and Dynamic Evaluation | `town/components/TownProductionComponent.java` | DONE | `.../town/components/TownProductionComponentTest.java` | 2026-06-11 | 25 tests; expression/rates/conditions/stall+starve; quirks pinned; full suite green |
| T-030 | Production/Upgrades/Upgrade Registry Loading and Lookup | `production/UpgradeRegistry.java` | DONE | `.../production/UpgradeRegistryTest.java` | 2026-06-11 | 7 tests; defaults+parse edges+quirk pinned |
| T-031 | Town/Trading/Stock and Capacity Resolution | `town/components/TownTradingComponent.java` | DONE | `.../town/components/TownTradingComponentTest.java` | 2026-06-11 | 20 tests (virtual stock + cap math + adjust clamp/delegate); alias branch noted |
| T-032 | Production/Upgrades/Upgrade Cost and Research Time Scaling | `town/components/TownUpgradeComponent.java` | DONE | `.../town/components/TownUpgradeComponentTest.java` | 2026-06-11 | 14 tests; pow+ceil scaling + repeatability + afford (virtual ids) |

## Adding new rows
When the seeds run out (or you find a better target while reading code), append rows with the next T-### ID. Keep the vault note path in `System/Subsystem/Process Name` form, matching the area taxonomy in `vault/Home.md`. Big classes (e.g. `Town.java`) should be split across multiple rows by concern.
