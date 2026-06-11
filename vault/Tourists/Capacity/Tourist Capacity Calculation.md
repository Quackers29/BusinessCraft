---
tags:
  - detail
  - tourists
---
# Tourist Capacity Calculation

**Breadcrumb**: Tourists > Capacity > Tourist Capacity Calculation
**TL;DR**: A town can spawn tourists only when tourist spawning is enabled and its population >= ConfigLoader.minPopForTourists (default 5); the live tourist cap is the integer value of the "tourist_cap" upgrade modifier (0 if unset), and add attempts also gate against the hard ConfigLoader.maxTouristsPerTown.

## What it does
Towns only generate tourists once they have enough population to "support" a visitor economy. The current cap on concurrent tourists is driven by the town's upgrade "tourist_cap" modifier rather than a direct pop formula (earlier versions used pop directly). This prevents tiny starter towns from flooding the world with tourists and lets upgrades (and production recipes) explicitly control tourist throughput.

## How it works (process view)
- When code wants to know if tourists are allowed: `TownService.canSpawnTourists(town)` returns true only if the town's `touristSpawningEnabled` flag is on **and** `population >= minPopForTourists`.
- The maximum concurrent tourists the town will allow is `TownService.calculateMaxTourists(town)` which simply reads the floating-point "tourist_cap" modifier from the upgrade system and truncates it to int. If no upgrade or production has granted a tourist_cap effect yet, this is 0.
- `Town.canAddMoreTourists()` (still used by spawning helper) layers an additional hard cap: even if the modifier says 30, you cannot exceed `ConfigLoader.maxTouristsPerTown` (a global safety).
- `TownService.addTourist(town)` re-validates canSpawn + current < max before incrementing the count (and symmetrically for remove).
- **Worked example**: minPopForTourists=5, town pop=6, spawning enabled, tourist_cap modifier set to 25.0 by an unlocked upgrade → canSpawn=true, maxTourists=25, canAddMoreTourists=true until touristCount reaches 25 (or the global maxTouristsPerTown, whichever is lower).
- The min pop and the tourist_cap value live in `businesscraft.toml` under `[economy]` / produced by production "tourist_cap" effects; they hot-reload because the static fields and modifier maps are read live.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `TownService.canSpawnTourists(Town)` | `common/src/main/java/com/quackers29/businesscraft/town/service/TownService.java` (lines ~76-103) | Returns success(true) iff enabled && pop >= ConfigLoader.minPopForTourists; caches last result for log throttling |
| `TownService.calculateMaxTourists(Town)` | same file (lines ~113-130) | Returns success( (int) upgrades.getModifier("tourist_cap") ); 0 when no modifier present (pre-upgrade or locked) |
| `TownService.addTourist(Town)` / `removeTourist(Town)` | same file (lines ~140-210) | Orchestrate the checks above + mutate via town.setTouristCount; return explicit TownError on failure |
| `Town.canAddMoreTourists()` | `common/src/main/java/com/quackers29/businesscraft/town/Town.java` (lines ~410-424) | Legacy path still used by TouristSpawningHelper: !canSpawn || touristCount >= maxTouristsPerTown || touristCount >= (deprecated) calculate... |
| `Town.setPopulation(int)` / `getPopulation()` | same file (lines ~876-878, 872-873) | Direct passthrough to TownEconomyComponent; used by spawn eligibility |
| `Town.setTouristSpawningEnabled(boolean)` / `isTouristSpawningEnabled()` | same file (~888-889, 922-924) | Simple flag (defaults true in ctor) |
| `TownUpgradeComponent.getModifier("tourist_cap")` / `addFlatModifier` | `common/src/main/java/com/quackers29/businesscraft/town/components/TownUpgradeComponent.java` | Supplies the runtime cap value; flat modifiers bypass full research tree for tests/defaults |
| `ConfigLoader.minPopForTourists`, `maxTouristsPerTown`, `dailyTickInterval` | `common/src/main/java/com/quackers29/businesscraft/config/ConfigLoader.java` | Tunables read live at decision time |

## Rules & formulas (exact)
From TownService.java:

```java
boolean canSpawn = enabled && population >= minRequired;  // minRequired = ConfigLoader.minPopForTourists
...
float modifier = town.getUpgrades().getModifier("tourist_cap");
int maxTourists = (int) modifier;
```

- `canSpawnTourists` short-circuits on the enabled flag first (no pop check if spawning turned off in UI).
- `calculateMaxTourists` performs a raw `(int)` cast — positive fractional values truncate toward zero (25.9 → 25); a modifier of 0.9 becomes 0.
- No clamping inside calculateMaxTourists; the caller (addTourist or Town.canAddMoreTourists) decides what to do with 0.
- The hard `ConfigLoader.maxTouristsPerTown` is enforced **only** in the Town.canAddMoreTourists wrapper and is independent of the per-town modifier.
- addTourist performs the canSpawn + max checks again (defensive); on success it does `town.setTouristCount(current + 1)` directly.
- removeTourist only checks current > 0 (no cap or enabled test) and decrements; it can take count negative in theory if external code misuses the setter.

## Edge cases & behaviors
- `minPopForTourists = 0` (or config set low) → any enabled town with pop >= 0 can spawn.
- Population exactly == minPopForTourists → allowed.
- Population just below → disallowed (e.g. 4 < 5).
- Spawning disabled (via settings) → canSpawn=false even with huge pop.
- No "tourist_cap" modifier present (fresh town, no basic settlement upgrade) → calculateMaxTourists returns 0; canAddMoreTourists will be false once any tourists exist (or immediately).
- Fractional modifier (e.g. 12.7 from math in production) → 12 after (int) cast.
- Negative modifier? getModifier returns 0f for unknown, but if somehow negative the cast gives 0 or negative; callers treat <= current as full.
- `maxTouristsPerTown` (global) can be lower than the town's tourist_cap — the Town.canAddMoreTourists check makes the global win.
- Deprecated calculateMaxTouristsFromPopulation / getMaxTourists on Town still work by delegating; new code should go through TownService.
- Tourist count can be set directly via setTouristCount (used by service); no auto-clamping there.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/town/service/TownServiceTest.java`
- Covered: happy path (enabled + pop >= min, positive cap), min-pop boundary, disabled flag, zero-cap when no modifier, fractional truncation on cast, interaction with global maxTouristsPerTown, add/remove success and error paths via the service, config hot values via save/restore.
- Not covered: full production recipe unlocking of tourist_cap (would need ProductionRegistry + data), live TownInterfaceEntity spawn loop, UI container data bits, negative tourist counts from external misuse.
- Uses McBootstrap (for Town construction safety in boundary cases) + direct Town population/spawning/upgrades modifier setup; no Level or entities required.
- 16 tests; all formulas have hand-computed assertions in comments; full suite green.

## Open questions
- The global maxTouristsPerTown and the per-town tourist_cap from upgrades are two separate caps applied in different places (Town vs TownService). Is the intent that the upgrade value should be the authoritative "soft" target and the config only a safety ceiling? Current code makes them interact only in the legacy canAddMoreTourists path.
- When tourist_cap modifier is 0, should canAddMoreTourists allow the first tourist (bootstrap) or hard-block? Current behavior blocks once count >= 0, i.e. immediately.
- Deprecated methods on Town still delegate through a freshly allocated TownService + TownValidationService on every call — cheap but worth noting if hot paths ever appear.
- The service caches only the last canSpawn boolean per town UUID for logging; calculateMaxTourists has no cache.

## Related
- [[Tourists/Capacity/Tourist Allocation]] (T-009 — the fair destination picker that runs at spawn time)
- [[Town/Town Overview]] — population growth from tourism feeds back into capacity
- [[Economy/Tourist Payments/Distance Payment Calculation]] — only possible once capacity allows tourists to exist and travel
