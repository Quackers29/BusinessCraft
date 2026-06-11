---
tags:
  - detail
  - town
---
# Resource Storage Operations

**Breadcrumb**: Town > Resources > Resource Storage Operations
**TL;DR**: `TownResources` is a long-count Item bag with null guards, `Math.addExact` overflow-to-MAX on add, `max(0, ...)` clamp on remove (0 entries are retained), and RL-string NBT roundtrips; `TownEconomyComponent` delegates resources and owns separately persisted population (set-if-≥0, remove-if-sufficient).

## What it does
Provides the authoritative "on-hand" resource storage for a town. Used for production inputs, trading stock, contract payouts, tourist fares paid in emeralds, and manual adjustments. Guarantees no negative counts and protects against long overflow for very large economies (max ~9 quintillion per item type). This is distinct from escrow (locked by contracts), personal per-player storage, wanted deficits, work units, and the claimable Payment Board.

## How it works (process view)
- Production, contracts, trade packets, TownService, and direct town calls invoke `addResource(item, delta)` where delta can be positive (gain) or negative (spend/remove).
- `consumeResource(item, amount)` is the checked variant used when you must have enough or the action fails (e.g. recipe inputs).
- Reads via `getResourceCount` / `getAllResources` (the latter returns a live unmodifiable view of the internal map, which may contain zero-valued entries).
- On every add/remove the code evaluates a debug log expression that reaches `PlatformAccess.getRegistry().getItemKey(...)` — this is a hidden coupling; in tests a stub registry is required even though the core math does not.
- Population lives alongside in `TownEconomyComponent` but is mutated only via explicit `setPopulation` / `removePopulation`; `addResource` does not touch it (despite javadoc).
- Persistence: `save`/`load` on the component roundtrips resources (via the inner `TownResources`) + population. Keys in NBT are `ResourceLocation.toString()` values (e.g. "minecraft:emerald").

**Worked example**: A town starts at 0 emeralds. A tourist arrival adds 12 via `addResource(EMERALD, 12)` → count = 12. Production consumes 5 via `consumeResource(EMERALD, 5)` → returns true, now 7. An over-spend `consumeResource(EMERALD, 10)` → returns false, still 7. A direct remove `addResource(EMERALD, -7)` clamps to 0 (and the map now contains the key with value 0). `getAllResources()` will surface that 0 entry. On save the NBT contains `"minecraft:emerald": 0`; on load it is re-inserted as 0 (sanitized).

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `TownResources.addResource(Item, long)` | `common/src/main/java/com/quackers29/businesscraft/town/components/TownResources.java` (lines 36-79) | Main mutation. +count: addExact or cap at MAX; -count: max(0) clamp + conditional put only if pre-op >0; null item ignored; count==0 no-op. Always evaluates registry key for debug. |
| `TownResources.consumeResource(Item, long)` | same (lines 98-108) | Atomic "have enough?" check then subtract. Rejects null/≤0 count/insufficient; never stores negative; returns success flag. No debug/registry side effect. |
| `TownResources.getResourceCount(Item)` / `getAllResources()` | same (87-89, 115-117) | Read paths. getOrDefault(0); returns unmodifiableMap (live view, can include 0L values). |
| `TownResources.save(CompoundTag)` / `load(CompoundTag)` | same (124-165) | NBT: resources stored under "resources" subtag as `RL.toString() → long`. On load: clear, sanitize <0→0, skip null/AIR items, requires registry for RL→Item. |
| `TownEconomyComponent.addResource` / `getResourceCount` / `getResources` | `common/src/main/java/com/quackers29/businesscraft/town/components/TownEconomyComponent.java` (22-43, 101-103) | Thin delegation to the inner TownResources (plus a null guard + isEmerald debug prep). Exposes the resources bag. |
| `TownEconomyComponent` population methods (`setPopulation`, `removePopulation`, `getPopulation`, load/save) | same (50-66, 74-89, 92-94) | Independent int pop. set only if ≥0; remove only if pop ≥ amount (no underflow); persisted as "population" int + resources. |
| `TownEconomyComponent` (as `TownComponent`) | same | Implements tick/save/load for the economy slice; wired into Town. |

## Rules & formulas (exact)
From `TownResources.java`:

```java
// add positive path
long current = resources.getOrDefault(item, 0L);
try {
    long result = Math.addExact(current, count);
    resources.put(item, result);
} catch (ArithmeticException e) {
    resources.put(item, Long.MAX_VALUE);
}
```

```java
// add negative path (count < 0)
long currentAmount = resources.getOrDefault(item, 0L);
long newAmount = Math.max(0, currentAmount + count);
if (currentAmount > 0) {
    resources.put(item, newAmount);   // note: 0 is explicitly stored
} else {
    if (isEmerald) { LOGGER.warn(...); }
}
```

- `count == 0` or `item == null`: early return, no mutation, no log.
- After remove-to-zero the entry **stays** in the map with value 0 (contrast with Town's escrow map which does `.remove(item)` when newAmount==0).
- `consumeResource`: classic check-then-act; subtraction only on success path; can produce 0 in map.
- Load sanitization:
  ```java
  long amount = ...;
  if (amount < 0) amount = 0;
  if (item != null && item != Items.AIR) resources.put(item, amount);
  ```
- Save includes every map entry (including 0s) under its RL string key.
- `TownEconomyComponent.addResource` does **not** update population (javadoc is stale); it only forwards after its own null check and isEmerald prep.
- Population remove:
  ```java
  if (population >= amount) population -= amount;
  ```
  (If amount ≤ 0 and this were called it could increase or no-op; currently no callers.)

Config: none for the storage math itself. Population default comes from `ConfigLoader.defaultStartingPopulation` (set at Town construction).

## Edge cases & behaviors
- `addResource(null, ...)` or `consumeResource(null, ...)`: ignored / false, no crash.
- `addResource(item, 0)`: no-op.
- Positive add that would overflow `Long`: caps at `Long.MAX_VALUE` (no exception escapes).
- Remove more than present (via negative add or via consume): clamps to 0 or returns false; never negative.
- `consumeResource(item, amount<=0)`: false immediately.
- Map after removes-to-zero: contains `item → 0L` entries; `getAllResources()` and iteration will report them; `getResourceCount` correctly returns 0.
- Load of negative amount: coerced to 0 (and stored).
- Load of AIR or registry-miss item: skipped (not inserted).
- Zero population set: allowed (`setPopulation(0)` succeeds).
- `removePopulation(negative)` (if ever called): would increase pop because `pop >= negative` is true for pop≥0 and `pop -= negative` adds.
- Debug logging: every non-zero add/remove path evaluates `PlatformAccess.getRegistry().getItemKey(item)` at the call site (even when `DebugConfig.TOWN_DATA_SYSTEMS == false`); this is why tests must stub the registry.
- `getAllResources()` returns a **live** unmodifiable wrapper — mutations via add/consume are visible to holders of the map reference.
- `TownEconomyComponent.getPopulation()` returns `long` (widening the internal `int`).

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/town/components/TownResourcesTest.java`
- Covered (only the bootstrap-safe surface): null-item guards on addResource and consumeResource; all population set/remove/get/clamp/default logic on TownEconomyComponent (4 tests total, all pass without ever constructing or referencing an Item).
- The add/remove/overflow (Math.addExact cap, max(0) clamp, zero retention), real consume, getAll with items, and all NBT save/load paths are **not unit tested** — any execution that reaches a non-null Item (even `new Item(new Item.Properties())`) triggers `ExceptionInInitializerError: Not bootstrapped` (Registries/BuiltInRegistries static init). Same limitation that forced NEEDS-MC on T-002/T-003 positive paths.
- Note: the TestRegistryHelper stub is present (for parity with T-002) but not exercised by the retained tests. Main formulas/edges fully documented from code review instead.

## Open questions
- Zero retention in the resources map: after `add(item, -current)` the map keeps `item→0`. Callers iterating `getAllResources()` will see zero stock entries. Escrow prunes zeros; resources does not. Is this intentional (for "I once had this resource" history) or a leak? UI layers may or may not filter.
- `removePopulation` is dead code (no call sites); its underflow/negative handling is therefore unexercised and has the "negative remove adds" quirk. Consider removing or hardening.
- Stale javadoc on `TownEconomyComponent.addResource`: "update population if applicable" — the implementation never does.
- The debug expression `PlatformAccess.getRegistry().getItemKey(...)` is evaluated unconditionally inside add/remove even in production when that debug flag is off. This creates an otherwise-unnecessary hard dependency on registry being live for all resource mutations. A future refactor could move the key lookup inside the `if (isEnabled)` block or use lazy formatting.
- Large-scale economy: Long.MAX is documented as the cap, but no callers currently appear to rely on "I received MAX emeralds" sentinel vs. a true huge number.

## Related
- [[Town/Town Overview]]
- [[Economy/Economy Overview]] (tourist payments and milestones land emeralds here)
- [[Trade/Trade Overview]] (auctions, courier, direct trade mutate these counts and escrow)
- [[Town/Storage/Slot-Based Storage]] (T-013 — higher-level slot UI/storage)
- [[Town/Payment Board/Reward Claims]] (T-012 — claimable rewards are separate from on-hand resources)
