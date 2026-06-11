---
tags:
  - detail
  - town
---
# Town Distance Validation

**Breadcrumb**: Town > Boundaries > Town Distance Validation
**TL;DR**: Placement of new towns and boundary expansion are rejected if euclidean distance to any other town is strictly less than (new-boundary + other.getBoundaryRadius()); new-town boundary is Config.defaultStartingPopulation (default 5), while Town.getBoundaryRadius() returns upgrade "border" or 50; TownValidationService enforces name (len 1-50 + charset + basic blocklist), BlockPos Y/X/Z limits, search radius 1-100, and tourist/resource param sanity using Result error carriers.

## What it does
These two services implement the "founding a town" and "growing a town" spatial and input validation rules. When a player places a Town Interface block, TownManager calls TownBoundaryService.checkTownPlacement to ensure the new settlement is far enough from existing ones given their (population/upgrade-derived) boundaries. As towns grow via tourism, checkBoundaryExpansion prevents one town's growth from overlapping another. TownValidationService is the gate for TownService.createTown and settings updates — it rejects bad names, impossible coordinates, and inconsistent tourist config before any Town is allocated.

## How it works (process view)
- New town placement (TownManager / TownInterface placement): compute straight-line distance (sqrt of BlockPos.distSqr) between the clicked BlockPos and every existing town's position. New town is treated as having boundary = ConfigLoader.defaultStartingPopulation (hardcoded 5 in the check, even though ctor will set pop to that). Each existing town contributes its current getBoundaryRadius() (today: 50 unless a "border" upgrade modifier is active). If any distance < (5 + their radius) → BOUNDARY_CONFLICT, town creation fails with a human-readable message including the numbers.
- Boundary expansion (population growth path): when a town's pop would increase, checkBoundaryExpansion uses the *proposed* newPopulation directly as the "new boundary radius" (1:1 comment) + others' radii. If any existing town would be inside the enlarged circle → EXPANSION_CONFLICT.
- Name validation runs on create and on settings update: null/blank, too short (<1), too long (>50), characters outside [a-zA-Z0-9 \-_'.], or containing any of a small hardcoded lowercase list ("admin", "server", "null", ...).
- Position validation: Y must be -64..320 inclusive? (rejects < -64 or >320), |X| and |Z| must be <= 30 000 000.
- Tourist params: negative counts rejected; also if population < Config.minPopForTourists (default 5) and you ask for maxTourists > 0 → INSUFFICIENT_POPULATION.
- Resource guards (initial or ongoing): reject null items, zero/negative/overflow amounts, too many distinct types (>100), or single-stack amounts > ~3.5k (6 double-chest rows). These paths mention the Item via .getDescription() so only null-item cases are safe in pure tests.
- Worked example (placement): defaultStartingPopulation=5, two fresh towns have getBoundaryRadius()=50. Place second town 40 blocks away (euclidean). dist=40, required=5+50=55 → 40 < 55 → placement fails with BOUNDARY_CONFLICT. At 55 or more → allowed.
- Config values (defaultStartingPopulation, minPopForTourists) are live statics from ConfigLoader and can be overridden in tests (save/restore pattern).

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `TownBoundaryService.calculateBoundaryRadius(Town)` | `common/src/main/java/com/quackers29/businesscraft/town/service/TownBoundaryService.java:27` | Returns (int)town.getPopulation() or defaultStartingPopulation if town null. (Note: separate from Town.getBoundaryRadius().) |
| `TownBoundaryService.checkTownPlacement(BlockPos, Collection<Town>)` | same:48 | Core placement gate used by TownManager. Uses fixed new-town boundary = defaultStartingPopulation + existing.getBoundaryRadius(); returns TownError on conflict. |
| `TownBoundaryService.getMinimumDistanceRequired(Town, Town)` | same:93 | Symmetric min-dist calculator with null handling (defaults to defaultStartingPopulation on the null side). |
| `TownBoundaryService.checkBoundaryExpansion(Town, int newPopulation, Collection<Town>)` | same:123 | Uses newPopulation directly as proposed radius (1:1) vs existing radii; no-op if newPop <= current pop. |
| `TownValidationService.validateTownCreation(CreateTownRequest)` | `common/src/main/java/com/quackers29/businesscraft/town/service/TownValidationService.java:32` | Orchestrates name + position + (optional) initial resources validation; maps ValidationError to TownError at TownService layer. |
| `TownValidationService.validateTownSettings(TownSettings)` | same:67 | Validates only the non-null fields being changed (name or searchRadius); tourist bool is accepted as-is. |
| `TownValidationService.validateTownName(String)` | same:100 | Length 1-50, regex ^[a-zA-Z0-9\s\-_'.]+$, plus private containsInappropriateContent blocklist on lowercased name. |
| `TownValidationService.validatePosition(BlockPos)` | same:141 | Rejects null, Y < -64, Y > 320, |X| > 30M, |Z| > 30M. |
| `TownValidationService.validateSearchRadius(int)` | same:177 | 1..100 inclusive. |
| `TownValidationService.validateInitialResources(...)` / `validateResourceManagement(...)` | same:197,276 | Item-aware amount and count checks (some paths call item.getDescription(); null item and empty maps are safe). |
| `TownValidationService.validateTouristManagement(int,int,int)` | same:242 | Negatives + the pop < minPopForTourists && max>0 rule. |
| `TownValidationService.validateVisitorProcessing(UUID, UUID)` | same:313 | Only visitorId null is fatal; origin may be null (player visitors). |
| `TownValidationService.containsInappropriateContent(String)` (private) | same:331 | Keyword contains() over fixed array including "admin","server","moderator","null","undefined","error",... (input expected lower). |
| `Result<T,E>` / `BCError.TownError` / `BCError.ValidationError` | `common/src/main/java/com/quackers29/businesscraft/util/Result.java`, `BCError.java` | Success/failure carriers; errors carry code + message only. |

## Rules & formulas (exact)
From TownBoundaryService (exact control flow):

```java
// calculateBoundaryRadius
if (town == null) return ConfigLoader.defaultStartingPopulation;
return (int) town.getPopulation();
```

```java
// checkTownPlacement
if (newTownPos == null) return failure(INVALID_POSITION, "...");
if (existingTowns == null || existingTowns.isEmpty()) return success;
int newTownBoundary = ConfigLoader.defaultStartingPopulation;
for (Town e : existingTowns) {
    double d = Math.sqrt(newTownPos.distSqr(e.getPosition()));
    double req = newTownBoundary + e.getBoundaryRadius();
    if (d < req) return failure(BOUNDARY_CONFLICT, formatted msg with d, req, newB, eB);
}
return success;
```

```java
// getMinimumDistanceRequired
if (t1==null && t2==null) return defaultStartingPopulation * 2.0;
if (t1==null) return default + t2.getBoundaryRadius();
if (t2==null) return t1.getBoundaryRadius() + default;
return t1.getBoundaryRadius() + t2.getBoundaryRadius();
```

```java
// checkBoundaryExpansion
if (expanding == null) failure(INVALID_TOWN);
if (newPopulation <= expanding.getPopulation()) return success; // not expanding
int newB = newPopulation; // 1:1
int curB = expanding.getBoundaryRadius();
... then same sqrt + (newB + other.getBoundaryRadius()) < check for others (skip self by id)
```

From TownValidationService:

- MIN_TOWN_NAME_LENGTH=1, MAX=50
- MIN_SEARCH_RADIUS=1, MAX=100
- Name regex: `^[a-zA-Z0-9\s\-_'.]+$` (letters, digits, space, hyphen, underscore, apostrophe, period)
- Inappropriate list (exact): "admin", "server", "moderator", "owner", "staff", "op", "null", "undefined", "error", "exception", "debug"
- Position Y: reject if < -64 or > 320
- Position X/Z: reject if abs > 30_000_000
- For resources in initial: if map == null or isEmpty() → success. Else per-entry: item==null → NULL_ITEM; amount==null || <=0 → INVALID_AMOUNT (message uses item desc); amount > 64*9*6 (=3456) → AMOUNT_TOO_LARGE; total distinct types >100 → TOO_MANY_ITEM_TYPES.
- Tourist: current<0 or max<0 or pop<0 → specific NEGATIVE_* ; additionally `if (population < ConfigLoader.minPopForTourists && maxTourists > 0)` → INSUFFICIENT_POPULATION
- Resource mgmt: item null → NULL_ITEM; amount==0 → ZERO_AMOUNT; current<0 → NEGATIVE_CURRENT; (amount<0 && -amount > current) → INSUFFICIENT; (amount>0 && current > INT_MAX - amount) → OVERFLOW. Messages embed item desc.

Config values read live: `ConfigLoader.defaultStartingPopulation`, `ConfigLoader.minPopForTourists`.

## Edge cases & behaviors
- Null town to calculateBoundaryRadius → returns defaultStartingPopulation (5), no exception.
- Null/empty existing list to checkTownPlacement → immediate success (first town always ok).
- Distance exactly equal to required → allowed (only `<` is conflict).
- Town with pop=0 (possible via set) → service calculate returns 0; Town.getBoundaryRadius() still returns 50 (border fallback), so placement math for "existing" uses 50.
- Both towns null to getMinimumDistanceRequired → 2 * default (10).
- Name "Null Town" (mixed case) → lowercased check hits "null" → INAPPROPRIATE_NAME.
- Name with only spaces after trim → EMPTY_NAME.
- Name length exactly 1 or exactly 50 → allowed (by < / > checks).
- BlockPos with Y=-64 or Y=320 → allowed (the checks are strict < -64 / > 320).
- X=30000000 or Z=-30000000 → allowed; 30000001 → POSITION_TOO_FAR.
- validateInitialResources with resources map explicitly empty (not null) → success.
- validateInitialResources with a null-key entry in the map → hits null item check (before any getDescription).
- validateInitialResources with amount=0 or negative for a real item entry → would call getDescription() in error msg (Item bootstrap risk in pure test).
- validateResourceManagement(null, ...) → NULL_ITEM (no desc call).
- validateTouristManagement(0, 10, 3) when minPop=5 → INSUFFICIENT_POPULATION.
- validateVisitorProcessing(null, someOrigin) → NULL_VISITOR_ID.
- Origin UUID null is explicitly allowed for player visitors.
- The private inappropriate check is a simple linear contains over 11 words; very short names or substrings can trigger (e.g. a town named "debugville").
- All Result-returning validators return a fresh *Error object on failure; success is always Result.success(null).

## Test coverage
- Test files:
  - `common/src/test/java/com/quackers29/businesscraft/town/service/TownValidationServiceTest.java` (22 tests)
  - `common/src/test/java/com/quackers29/businesscraft/town/service/TownBoundaryServiceTest.java` (20 tests: 5 guards + 15 math via McBootstrap)
- Covered:
  - TownValidationService: all name validation paths (null/empty/len/regex/inappropriate via reflection on private containsInappropriateContent), position (BlockPos Y/X/Z bounds + null), search radius, tourist management (negatives + insufficient pop vs Config.minPopForTourists), visitor processing, creation/settings orchestration (empty resources path), null-item resource guards. Pure BlockPos/UUID/String paths; no real Town required.
  - TownBoundaryService (McBootstrap + real Town instances): 15 new math tests covering calculateBoundaryRadius on Town after setPopulation (returns pop as int, distinct from Town.getBoundaryRadius() — discrepancy pinned); checkTownPlacement with realistic fresh-Town boundaries (new side hardcoded to defaultStartingPopulation=5, existing side uses .getBoundaryRadius()=50 for un-upgraded → 55 threshold), euclidean sqrt(distSqr), `<` only is conflict (equal allowed), null/empty fast-paths, Config override for new-side; getMinimumDistanceRequired with real towns (sums radii) and null mixes; checkBoundaryExpansion (proposed newPopulation used 1:1 as radius for expander side, others use their getBoundaryRadius(), no-op if <= current pop, skip-self by UUID, conflict detection, null-town guard).
- McBootstrap: @BeforeAll in TownBoundaryServiceTest initializes vanilla registries so `new Town(UUID, BlockPos, name)` succeeds (components and UpgradeRegistry statics now load). Registry stub + Config save/restore still used in @BeforeEach for determinism and any residual platform access. (This unblocks the placement/expansion paths that were previously NEEDS-MC.)
- Reflection: only for the private containsInappropriateContent in ValidationService (unchanged).
- The pop-vs-border discrepancy and 1:1 expansion comment remain recorded under Open questions (behavior is pinned by the new math tests).

## Open questions
- **Boundary source mismatch**: TownBoundaryService.calculateBoundaryRadius and its expansion/placement "new side" math still treat boundary as 1:1 with population (or defaultStartingPopulation), while Town.getBoundaryRadius() (used for the "existing" side and by other systems) was changed to read upgrades.getModifier("border") with hard fallback to 50 (see comments in Town.java:291-301 about legacy vs "starting 'border:50'"). Placement for a brand-new town always forces 5 on the new side. This is a discrepancy between the service and the Town data object; may be transitional or a source of future confusion when pop-based growth is supposed to increase effective borders.
- The 1:1 comment in checkBoundaryExpansion ("int newBoundaryRadius = newPopulation; // 1:1 ratio") no longer matches Town's published getBoundaryRadius behavior.
- Inappropriate content list is hardcoded, includes technical words ("null", "error", "debug") and will reject names containing them as substrings. No way for server admins to customize or localize today.
- Error messages for resource amount problems embed `item.getDescription().getString()` — this couples validation errors to MC Item rendering and makes some error paths untestable in pure JUnit (same limitation as T-007). The codes are still testable via the null-item branches.
- No validation yet for duplicate town names at the service layer (that appears to be handled higher in TownManager or by the world).
- BlockPos far-limit 30M is close to MC's ±30M world border; the Y limits (-64/320) match 1.18+ build limits. These are "reasonable" rather than physics-enforced.

## Related
- [[Town/Town Overview]]
- [[Town/Resources/Resource Storage Operations]] (T-007)
- [[Economy/Tourist Payments/Distance Payment Calculation]] (population growth from visits indirectly affects future boundary checks)
- [[Town/Visits/Visit Buffer]] (T-010 — once implemented)
