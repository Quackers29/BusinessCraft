---
tags:
  - detail
  - config
---
# Subsystem Toggle Honesty

**Breadcrumb**: Config > Feature Toggles > Server-Side Honesty
**TL;DR**: When trading, contracts, research, or production are turned off in settings, the server refuses player actions and town startup bonuses for those systems — while still giving every new town the basic tourism unlock so tourists can keep spawning.

## What it does
Server owners can disable whole subsystems in `businesscraft.toml` without removing UI tabs. Honesty means the game must not silently keep mutating those systems: players cannot complete trades or accept contracts, towns stop calculating research “shopping lists,” and new towns do not receive production stock or advanced research unlocks. Tourism stays alive even when research is off, because every town still receives the basic settlement unlock that grants tourist generation and a small tourist capacity.

## How it works (process view)
Feature flags already pause background town ticks (trading restocks, production recipes, contract AI, research progress). Toggle honesty adds the missing player-facing and founding-time gates:

1. **Player trade** — while trading is off, sending a trade from the UI does nothing on the server.
2. **Player contracts** — while contracts are off, bidding/accepting via the UI does nothing; depositing a contract item in the town interface also will not complete a courier delivery.
3. **Research wants** — while research is off, the town clears its list of resources it “wants” for the next upgrade and does not rebuild it.
4. **New town biome kit** — when a town is founded, its biome kit’s starting bonuses are filtered:
   - Population, happiness, pending tourists, capacity/border modifiers always apply.
   - If research is off, only the `basic_settlement` starting unlock is applied (other starting nodes like tourism/research boosts are skipped). That one unlock is what keeps tourist generation and a base tourist capacity.
   - If production is off, starting food/wood/money (and other resource stock) is skipped.

**Worked example**: A plains kit lists starting nodes `basic_settlement;tourism_basic;research_basic` and values `pop:6;food:1400;tourist_cap:3;border:10`, with research and production both off. The new town gets population 6, border 10, tourist_cap from the kit plus the basic settlement unlock, and the tourism recipe ownership flag — but not the tourism_basic/research_basic nodes, and not 1400 food.

UI tabs for disabled systems can still be visible; they simply do nothing when used. That is intentional for the 0.9 tourism-only beta.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `TradeResourcePacket.handle` | `common/.../network/packets/storage/TradeResourcePacket.java` | Early-return if `!ConfigLoader.tradingEnabled` |
| `BidContractPacket.handle` / `AcceptContractPacket.handle` | `common/.../network/packets/ui/` | Early-return if `!ConfigLoader.contractsEnabled` |
| `TownInterfaceEntity.processResourcesInSlot` | `common/.../block/entity/TownInterfaceEntity.java` | Contract-item delivery branch gated by `contractsEnabled` |
| `Town.calculateWants()` | `common/.../town/Town.java` | Clears `wantedResources` and returns if `!researchEnabled` |
| `TownManager.applyBiomeKit(Town, BiomeKit)` | `common/.../town/TownManager.java` | Toggle-aware kit apply; called from `registerTown` |
| `Town.tick()` | `common/.../town/Town.java` | Pre-existing tick gates for trading/production/contracts/research + Option C tourism |

## Rules & formulas (exact)
Config fields (Java defaults all `true` until Step 4 flips 0.9 tourism-only defaults):
- `ConfigLoader.tradingEnabled` ← `trading.enabled`
- `ConfigLoader.contractsEnabled` ← `contracts.enabled`
- `ConfigLoader.researchEnabled` ← `research.enabled`
- `ConfigLoader.productionEnabled` ← `production.enabled`
- `ConfigLoader.touristSystemEnabled` ← `tourists.enabled` (tourism recipe tick; not changed by Step 2)

**Packet / deposit guards** (authoritative; UI may still send packets):
```java
if (!ConfigLoader.tradingEnabled) return;      // TradeResourcePacket
if (!ConfigLoader.contractsEnabled) return;    // Bid + Accept
// processResourcesInSlot: contract branch only when contractsEnabled
```

**calculateWants**:
```java
wantedResources.clear();
if (!ConfigLoader.researchEnabled) return;
// else TownResearchAI target + upgrade costs…
```

**applyBiomeKit startingNodes**:
- Parse `nodeId` and optional `:level` (same as before).
- Unlock only if `researchEnabled || "basic_settlement".equals(nodeId)`.
- Unlocking `basic_settlement` still cascades to effect names that match other node ids (e.g. `farming_basic` in production `upgrades.csv`). Harmless while production is off (those recipes do not tick).

**applyBiomeKit startingValues**:
- Always: `pop`/`population` → `setPopulation`; `happiness` → `setHappiness`; `tourist` → `addPendingTouristSpawns`; keys ending `_cap`/`_modifier` or equal to `border` → `addFlatModifier`.
- Else if `!productionEnabled`: skip (no resource stock).
- Else: resolve via `ResourceRegistry` + `PlatformAccess.getRegistry().getItem` and `town.addResource`.

Production and research have **no** C2S action packets; their progress is already gated in `Town.tick()`. Open/list/detail contract packets remain unguarded (read-only UI).

## Edge cases & behaviors
- Research off without keeping `basic_settlement` would zero `tourism` / `tourist_cap` and kill Option C — the node exception exists specifically to prevent that.
- `getHappiness()` returns base + upgrade `happiness` modifier (clamped 0–100); kit `happiness` sets the base only.
- Kit `tourist_cap` flat modifier stacks with `tourist_cap` from unlocked nodes.
- Deposit slot still deposits ordinary items into town resources when contracts are off (research AI / trading stock feed); research cannot start without `upgrades.tick()`.
- Oasis kits with `pop:4` vs `minPopForTourists=5` remain a Step 4 spawn-eligibility concern, not a toggle filter issue.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/town/ToggleHonestyTest.java`
- Covered (4 tests): research-off keeps `basic_settlement` + tourism/`tourist_cap`, skips other nodes; research-on unlocks all listed nodes; production-off skips food grants / production-on adds them; `calculateWants` clears stale wants when research off.
- Not covered (by design): live packet `handle()` paths (need network harness); deposit-slot entity path (needs block entity + level).

## Open questions
- `basic_settlement` cascade unlocks `farming_basic` — research-tree honesty leak when research is off; acceptable for 0.9 while production does not tick.
- Resource grants are gated only by `productionEnabled`, not `tradingEnabled`. Fine for 0.9 defaults (both off); if production off + trading on, new towns get no starting money unless that rule is revisited.
- Packet-handler early returns are untested directly; behavior is one-line and mirrored by ConfigLoader flags covered elsewhere.

## Related
- [[Config/Configuration Loading]]
- [[Config/Config Overview]]
- [[Tourists/Tourists Overview]] (Option C tourism recipe ownership)
- [[Production/Upgrades/Upgrade Registry Loading and Lookup]] (`basic_settlement` effects)
- [[Trade/Trade Overview]]
- [[Town/Town Overview]]
