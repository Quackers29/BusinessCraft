---
tags:
  - detail
  - town
---
# Town Distance Validation

**Breadcrumb**: Town > Boundaries > Town Distance Validation
**TL;DR**: Placement of new towns and border expansion checks reject if euclidean distance to any other town is strictly less than (proposedBorder + other.getBoundaryRadius()). New-town and null sides use `ConfigLoader.defaultBorderRadius` (default **50**). `Town.getBoundaryRadius()` is the border upgrade/kit modifier (same default fallback). Population is **not** a boundary source; optional pop-linked growth is via production CSV (`border_expansion_production` with `pop:>80%pop_cap`).

## What it does
These two services implement the "founding a town" and "growing a town" spatial and input validation rules. When a player places a Town Interface block, TownManager calls TownBoundaryService.checkTownPlacement to ensure the new settlement is far enough from existing ones given their border radii. `checkBorderExpansion` validates a proposed new border radius against neighbors (for upgrade/production growth paths). TownValidationService is the gate for TownService.createTown and settings updates — it rejects bad names, impossible coordinates, and inconsistent tourist config before any Town is allocated.

## How it works (process view)
- New town placement: straight-line distance (sqrt of BlockPos.distSqr) between the clicked BlockPos and every existing town. New town contributes `ConfigLoader.defaultBorderRadius` (50). Each existing town contributes `getBoundaryRadius()` (border modifier, else same default). If any distance < (50 + their radius) → BOUNDARY_CONFLICT.
- Border expansion check: `checkBorderExpansion(town, proposedBorderRadius, others)` — no-op if proposed ≤ current radius; otherwise same sum-of-radii conflict test (skip self by id).
- Population does **not** feed these APIs. Crowded-population border growth is a production recipe option (see Related).
- Name / position / tourist / resource validation unchanged (TownValidationService).
- Worked example (placement): defaultBorderRadius=50, two fresh towns both radius 50. Place second town 99 blocks away → required 100 → BOUNDARY_CONFLICT. At 100 or more → allowed.
- Config values (`defaultBorderRadius`, `minPopForTourists`) are live statics from ConfigLoader and can be overridden in tests (save/restore pattern).

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `TownBoundaryService.calculateBoundaryRadius(Town)` | `TownBoundaryService.java` | Delegates to `town.getBoundaryRadius()`; null → `defaultBorderRadius`. |
| `TownBoundaryService.checkTownPlacement(BlockPos, Collection<Town>)` | same | Placement gate. New side = `defaultBorderRadius` + existing `.getBoundaryRadius()`. |
| `TownBoundaryService.getMinimumDistanceRequired(Town, Town)` | same | Sum of radii; null side → `defaultBorderRadius`. |
| `TownBoundaryService.checkBorderExpansion(Town, int, Collection)` | same | Proposed border radius vs neighbors; no-op if not increasing. |
| `TownValidationService.*` | `TownValidationService.java` | Name/position/tourist/resource gates (unchanged). |

## Rules & formulas (exact)

```java
// calculateBoundaryRadius
if (town == null) return ConfigLoader.defaultBorderRadius;
return town.getBoundaryRadius();

// checkTownPlacement
int newTownBoundary = ConfigLoader.defaultBorderRadius;
req = newTownBoundary + e.getBoundaryRadius();
if (d < req) → BOUNDARY_CONFLICT

// getMinimumDistanceRequired
null sides → defaultBorderRadius; else sum of getBoundaryRadius()

// checkBorderExpansion
if (proposedBorderRadius <= expanding.getBoundaryRadius()) return success;
req = proposedBorderRadius + other.getBoundaryRadius();
```

Config values read live: `ConfigLoader.defaultBorderRadius`, `ConfigLoader.minPopForTourists` (validation service).

## Edge cases & behaviors
- Null town to calculateBoundaryRadius → `defaultBorderRadius` (50), no exception.
- Null/empty existing list to checkTownPlacement → immediate success (first town always ok).
- Distance exactly equal to required → allowed (only `<` is conflict).
- Changing population does not change `getBoundaryRadius()` or placement math.
- Both towns null to getMinimumDistanceRequired → 2 × defaultBorderRadius (100).

## Test coverage
- `TownValidationServiceTest.java` (22 tests)
- `TownBoundaryServiceTest.java` — border-only math: calculateBoundaryRadius (null + fresh, pop ignored), placement threshold 100 for two fresh towns, custom `defaultBorderRadius`, `checkBorderExpansion` proposed-radius conflicts / skip-self / no-op.

## Open questions
- `checkBorderExpansion` is available for upgrade/production growth but production's `accumulateFlatModifier("border", …)` does not currently call it before applying — wire if neighbor-safe auto-growth is required.
- Inappropriate content list / Item description coupling in resource errors unchanged.

## Related
- [[Town/Boundaries/Boundary Geometry Queries]]
- [[Town/Town Overview]]
- Production recipe `border_expansion_production` (`productions.csv`) + unlock via `border_expansion` upgrade (`upgrades.csv`)
