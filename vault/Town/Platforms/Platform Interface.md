---
tags:
  - detail
  - town
---
# Platform Interface

**Breadcrumb**: Town > Platforms > Platform Interface
**TL;DR**: Each platform is a placed Platform Interface block inside the town boundary; placement links it to the town's platform list, the block's screen edits path/settings, breaking the block removes the platform, and removing the Town Centre tears down linked interfaces and dismisses tourists on them.

## What it does
Players do not add platforms from a button on the Town Centre. Instead they craft and place **Platform Interface** blocks (`PlatformInterfaceBlock`) within their town's protected radius. Each accepted placement becomes one of up to ten platforms for that town. The interface block stores the town id, platform id, and town centre position so it stays tied to the correct town after reload. Players configure the tourist path, destinations, search radius, spread, and on/off state from the **Platform Interface** screen (`PlatformInterfaceScreen`). The Town Centre's platform tab (`PlatformManagementScreenV2`) is read-only and directs players to the block.

## How it works (process view)
1. **Placement** — `PlatformPlacement.evaluate` checks the block position against every known town: it must lie inside a town's boundary, that town must be under the 10-platform cap, the position must not duplicate an existing interface, and it must be at least 5 blocks away (3D) from any other interface in the same town (`MIN_SEPARATION`). On success, the block entity calls `TownInterfaceEntity.addInterfacePlatform`, which delegates to `PlatformManager.addInterfacePlatform` and returns a new platform UUID stored on the interface entity.
2. **Link after load** — When a Platform Interface block entity loads, `PlatformManager.relink(platformId, interfacePos)` either confirms the existing entry (`LINKED`), recreates a missing platform row (`RELINKED`), or rejects cap/duplicate/wrong-owner cases.
3. **Lamp** — The interface includes a `PlatformLamp` that reflects enabled state: a disabled platform forces light level 0 (unlit); enabled platforms use the normal lit appearance.
4. **Break interface** — `PlatformInterfaceEntity.onBroken` removes that platform from the town via `removePlatform` and clears the block's link data.
5. **Break Town Centre** — `TownInterfaceEntity.releasePlatformsForRemovedTown` walks interface positions, marks loaded interfaces inactive with reason `businesscraft.platform_interface.town_gone`, and dismisses tourists currently standing on those platforms.
6. **Worked example** — A town with boundary radius 50 has its centre at (0, 64, 0). A player places a Platform Interface at (20, 64, 10) inside the circle → placement accepts, platform "Platform 1" is created with `interfacePos` at that block. They open the block UI, set path endpoints and a search radius of 10 (default), spread 0. A second interface at (22, 64, 10) is rejected as too close; one at (200, 64, 0) is rejected as outside the boundary.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `PlatformPlacement.evaluate` | `common/src/main/java/com/quackers29/businesscraft/platform/PlatformPlacement.java` | Placement rules: inside boundary, cap, duplicate, min separation |
| `PlatformInterfaceBlock` / `PlatformInterfaceEntity` | `common/src/main/java/com/quackers29/businesscraft/block/` | Block placement, NBT link fields, `onBroken`, lamp |
| `PlatformManager.addInterfacePlatform` | `common/src/main/java/com/quackers29/businesscraft/town/data/PlatformManager.java` | Creates platform with `interfacePos`; rejects cap/duplicate/too close |
| `PlatformManager.relink` | same | `LINKED` / `RELINKED` / `AT_CAP` / `DUPLICATE` / `NOT_OURS` |
| `TownInterfaceEntity.addInterfacePlatform` | `common/src/main/java/com/quackers29/businesscraft/block/entity/TownInterfaceEntity.java` | Server entry when interface registers |
| `TownInterfaceEntity.releasePlatformsForRemovedTown` | same | Town Centre removal cleanup |
| `PlatformInterfaceScreen` | client UI package | Edits path, enabled, search radius, spread, destinations |
| `PlatformManagementScreenV2` | client UI package | Read-only list; tells player to use the block |

## Rules & formulas (exact)
- **Cap**: `MAX_PLATFORMS = 10` per town (not configurable).
- **Separation**: `PlatformPlacement.MIN_SEPARATION` = 5 blocks (Euclidean 3D) between interface positions in the same town.
- **Boundary**: Placement uses each town's effective border radius (modifier or `general.defaultBorderRadius`, default 50) — not population-based growth.
- **Inactive reason**: `businesscraft.platform_interface.town_gone` when the Town Centre is removed.
- **`addPlatform()` on PlatformManager**: test/showcase helper only; invents spaced positions — not the player flow.

## Edge cases & behaviors
- Placement with no enclosing town → rejected (`OUTSIDE` or equivalent).
- At 10 platforms → `AT_CAP`; `addInterfacePlatform` returns null.
- Load drops any saved platform entry with no `interfacePos` (see [[Town/Platforms/Platform Management]]).
- Disabled platform: excluded from `getEnabledPlatforms()`; lamp unlit; tourists do not spawn there.

## Test coverage
- `PlatformInterfaceLinkTest` — placement decisions, `addInterfacePlatform`, relink, separation, cap.
- `PlatformManagerTest` — manager cap, NBT, `addInterfacePlatform` / relink paths (see manager note).

## Open questions
- `TownInterfaceEntity` still stores town-level `pathStart` / `pathEnd` and `SetPathCreationModePacket` is registered, but no current UI sends path creation to the Town Centre — legacy surface area only.
- `PlatformManager.addPlatform()` remains for tests/showcase; players use interface blocks only.

## Related
- [[Town/Platforms/Platform Management]]
- [[Town/Platforms/Platform Data Model]]
- [[Town/Decisions/Town Centre and Platform Decisions]]
- [[Tourists/Spawning/Tourist Spawning and Destination Selection]]
