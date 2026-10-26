---
tags:
  - detail
  - town
---
# Town Centre and Platform Decisions

**Breadcrumb**: Town > Decisions > Town Centre and Platform Decisions
**TL;DR**: Record of 2026-10-26 product/code alignment — one Town Centre block registers the town (no owner), Platform Interface blocks are the only player-facing way to add platforms, no pre-alpha save migration, tourists never walk, production/trading/contracts default off, CI is manual-only, VIP tourist transport contracts deferred.

## What it does
This note captures intentional design choices reflected in the current codebase so overview docs and future work do not drift back to older flows (Town Centre "Add Platform" button, population-grown borders, legacy "Main Platform" migration, or on-foot tourism).

## How it works (process view)
**Date: 2026-10-26**

| Decision | Choice |
|---|---|
| Town identity | Exactly one **Town Centre** block per town (`TownInterfaceBlock`, registry `town_interface`, lang **Town Centre**). Placing it registers the town. There is **no town owner** field. |
| Boundaries | Radius from border modifier or `general.defaultBorderRadius` (default **50**). **Not** tied to population. New towns must be ≥ `newRadius + existingRadius` apart from each other. |
| Platforms | **Platform Interface** blocks inside the boundary; max **10**; see [[Town/Platforms/Platform Interface]]. Town Centre platform UI is **read-only**. |
| Save compatibility | **No** `createLegacyPlatform` / "Main Platform" one-shot migration. Platforms without `interfacePos` are **dropped on load**. Pre-alpha worlds are not specially back-ported. |
| Tourist movement | **Never walk** — wait for minecarts / Create trains (`TouristEntity` near-zero speed). See [[Tourists/Looks/Tourist Looks Names and Ranks]]. |
| Subsystems | **Production**, **trading**, and **contracts** are **off by default** (locked) in current defaults. |
| VIP / transport contracts | Planned v1 milestone; **parked**, not implemented. |
| CI | `.github/workflows/build.yml` is **`workflow_dispatch` only** (manual builds). |
| Fare | Unchanged: `metersPerEmerald` default **100** (not part of this refactor). |

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Topic | Pointer |
|---|---|
| Town registration | `TownInterfaceBlock` placement → town manager |
| Platform add | `PlatformManager.addInterfacePlatform`, `PlatformPlacement` |
| Legacy removal | `createLegacyPlatform` removed from codebase |
| Tourist speed | `TouristEntity` attribute setup |

## Related
- [[Town/Town Overview]]
- [[Town/Platforms/Platform Interface]]
- [[Mod Overview]]
- [[Config/Subsystem Toggle Honesty]]
