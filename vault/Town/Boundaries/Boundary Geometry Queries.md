---
tags:
  - detail
  - town
---
# Boundary Geometry Queries

**Breadcrumb**: Town > Boundaries > Boundary Geometry Queries
**TL;DR**: Each Town exposes its current boundary radius (upgrade/kit "border" modifier cast to int, or `ConfigLoader.defaultBorderRadius` fallback — default 50) and answers three spatial queries using 3D euclidean BlockPos distance: is a point inside (distSqr <= r² inclusive), would two towns' circles overlap (sqrt(distSqr) < r1+r2), and what center separation guarantees no overlap (r1+r2 or own r if other null). Population is not a radius source.

## What it does
Towns maintain a circular (in practice cylindrical for gameplay) exclusion zone whose radius comes from upgrades rather than raw population. Other systems (UI, packets, contract courier range checks, player boundary tracking, notifications, and the placement/expansion validators) ask a town "how big are you right now?" via getBoundaryRadius(), or "is this spot inside you?" / "would we collide?" via the three predicate helpers. The geometry is deliberately simple so it can be evaluated both server-side for enforcement and client-side for rendering and feedback.

## How it works (process view)
- getBoundaryRadius() is the single source of truth for a town's current size in blocks. It reads the live "border" float modifier from the town's TownUpgradeComponent (kits, research, or production can raise it). If the modifier is missing or <= 0, it falls back to `ConfigLoader.defaultBorderRadius` (50).
- isPositionInside(BlockPos) answers "does this coordinate belong to my territory?" for things like "should this player receive the town's chat broadcasts?" or server decisions. It uses 3D distance (distSqr includes Y) and treats the boundary itself as inside (<=).
- wouldOverlapWith(Town) and getMinimumDistanceRequired(Town) are the symmetric pair used when one town wants to know about another. Overlap is a strict less-than on the sum of radii; the min-distance helper just returns the sum (or the querying town's own radius when the other is null).
- Worked example: two fresh towns (both getBoundaryRadius()=50) have centers 70 blocks apart (euclidean). 70 < (50+50) is true → wouldOverlapWith returns true and getMinimumDistanceRequired returns 100. A BlockPos exactly 50 blocks from the first town's center satisfies 50² (2500) <= 50² (2500) → isPositionInside returns true (boundary inclusive). A town with an active "border:30" upgrade would report radius 30 and change all three answers accordingly.
- The radius value is read live from the upgrade map at call time; tests (and in theory hot research) can change it and see immediate effect on the spatial answers.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `Town.getBoundaryRadius()` | `common/src/main/java/com/quackers29/businesscraft/town/Town.java` | Returns current radius in blocks: (int) upgrades.getModifier("border") or `ConfigLoader.defaultBorderRadius` when <=0 |
| `Town.isPositionInside(BlockPos)` | same:328 | Null-safe; returns position.distSqr(this.position) <= (r * r) using 3D distSqr |
| `Town.wouldOverlapWith(Town)` | same:359 | Null-safe; sqrt(this.pos.distSqr(other.pos)) < (this.r + other.r) |
| `Town.getMinimumDistanceRequired(Town)` | same:380 | Null-safe; returns this.r + (other==null ? 0 : other.r) |

## Rules & formulas (exact)
From Town.java (exact control flow, written from the code):

```java
// getBoundaryRadius
float borderMod = upgrades.getModifier("border");
if (borderMod <= 0) {
    borderMod = 50;
}
int radius = (int) borderMod;
return radius;
```

```java
// isPositionInside
if (pos == null) return false;
double distanceSqr = this.position.distSqr(pos);
int radius = getBoundaryRadius();
return distanceSqr <= (radius * radius);
```

```java
// wouldOverlapWith
if (otherTown == null) return false;
double distance = Math.sqrt(this.position.distSqr(otherTown.position));
double requiredDistance = this.getBoundaryRadius() + otherTown.getBoundaryRadius();
return distance < requiredDistance;
```

```java
// getMinimumDistanceRequired
if (otherTown == null)
    return this.getBoundaryRadius();
double minDistance = this.getBoundaryRadius() + otherTown.getBoundaryRadius();
return minDistance;
```

- Radius is always a non-negative int (fallback guarantees >=50 for towns without explicit border mod).
- Inside test is inclusive of the boundary circle/sphere.
- Overlap test is strict `<` (centers exactly at sum of radii do not overlap per this method).
- All three queries call getBoundaryRadius() internally, so a single modifier change affects all of them consistently.
- No config values feed these methods directly (the 50 is a hardcoded legacy safety default in the method).

## Edge cases & behaviors
- Null pos to isPositionInside → false (no exception).
- Null otherTown to wouldOverlapWith → false.
- Null otherTown to getMinimumDistanceRequired → returns the caller's own current radius (not a global default).
- Fresh/un-upgraded town (no "border" entry in the modifier map) → getBoundaryRadius returns 50 (the <=0 branch).
- Explicit border <=0 (e.g. 0.0f or -3.2f from a weird upgrade) → treated as "absent" and yields 50.
- Radius exactly 0 (possible if an upgrade sets border:0 and the <=0 guard were removed, or via reflection in tests) → inside only the exact center point (distSqr<=0), overlap only if centers coincide (d<0 false unless identical pos).
- Exact boundary distance for inside: distSqr == r*r → true (inclusive).
- Exact sum for overlap: d == (r1+r2) → false (strict <).
- Self-comparison (town.wouldOverlapWith(sameTown)): d=0 < (r+r) is true for any r>0.
- Integer truncation in radius: a border modifier of 50.9f yields 50; 30.1f yields 30.
- The implementation always uses 3D distSqr (includes Y). Comments in the source discuss cylindrical (ignore Y) vs spherical alternatives but the executed path is the 3D one to match wouldOverlapWith.
- markDirty is not called by these getters/queries (read-only).

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/town/TownTest.java` (extended for T-036; see also T-035 work-unit tests in same file)
- 17 new tests added (all pass, full suite green).
- Covered: getBoundaryRadius fallback (50) and explicit modifier values via reflection injection into the upgrades map; isPositionInside null/center/exact-boundary/inside/outside; wouldOverlapWith null/self/exact-sum/strict-overlap cases; getMinimumDistanceRequired null/other cases. All formulas exercised with hand-computed expectations in comments (e.g. 50²=2500, 99<100, 70<80). McBootstrap + Town ctor + config save/restore + "border" modifier reflection (pattern established by T-035 wu_cap tests).
- Not covered: live upgrade research flow that mutates the border modifier (would require full TownService + research AI + McBootstrap registry for items), rendering or packet paths that consume the radius, the parallel geometry helpers that live in TownBoundaryService (already covered by T-008).

## Open questions
- **Service vs Town min-distance helpers**: TownBoundaryService.getMinimumDistanceRequired substitutes `defaultBorderRadius` on a null side; Town.getMinimumDistanceRequired substitutes the present town's own radius. Callers must know which contract they want. Placement new-side now matches the same default as getBoundaryRadius() fallback (aligned as of border cleanup).
- Optional pop-linked growth: production recipe `border_expansion_production` (`pop:>80%pop_cap` → `border:1`), unlocked by `border_expansion` upgrade — not automatic from population alone.
- isPositionInside and the two Town-to-Town helpers are only a partial API; many systems call getBoundaryRadius() and compare themselves.
- 3D vs 2D distance is not configurable; PlayerBoundaryTracker uses horizontal distance.

## Related
- [[Town/Town Overview]]
- [[Town/Boundaries/Town Distance Validation]] (T-008 — the services that gate placement and expansion using getBoundaryRadius() values and their own parallel min-distance math)
- [[Town/Visits/Visit Buffer]] (T-010)
- [[Town/Resources/Work Unit Accounting]] (T-035 — same TownTest.java extended here; same reflection pattern for upgrade modifiers)
