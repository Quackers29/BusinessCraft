---
tags:
  - detail
  - ui
---
# Showcase Harness

**Breadcrumb**: UI > Showcase Harness
**TL;DR**: The dev client harness pins town names, records which screen and buttons were open, and fails a run when a planned screenshot did not save or the open screen is the wrong one.

## What it does
Developers run a throwaway client world to photograph town screens and a few scripted checks. The pictures are for people. The pass/fail decision is the manifest: every planned picture must exist, and each UI route must be on the screen it asked for.

## How it works (process view)
A showcase run places Town Centres in order. The first is always named Brookside and the second Hillcrest, so two runs of the same scene show the same names. A normal game still picks a town name at random.

The full UI walk writes, next to each picture, the screen name, the open tab, the button ids, and the scaled window size. If that screen is not the one the route waited for, the route fails.

The block, town, platform, kiosk, and lamp scenes (and the combined `all` scene) write a shot list. The shell checks that list against the shots the scene is supposed to take, skipping the warmup pause that does not save a picture. If the game reports that a picture failed to save, the run fails.

The default two-town testbed runs the trade-contract check only. The example template check runs only when named.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `ShowcaseTownNames` | `common/src/main/java/com/quackers29/businesscraft/client/showcase/ShowcaseTownNames.java` | Brookside, then Hillcrest, while `businesscraft.showcase` is set |
| `TownInterfaceBlock.setPlacedBy` | `common/src/main/java/com/quackers29/businesscraft/block/TownInterfaceBlock.java` | Uses the pinned name during a showcase run |
| `ShowcaseUiSnapshot.capture` | `common/src/main/java/com/quackers29/businesscraft/client/showcase/ShowcaseUiSnapshot.java` | Screen, tab, button ids, scaled size |
| `ShowcaseRoute.expectedScreen` | `common/src/main/java/com/quackers29/businesscraft/client/showcase/ShowcaseRoute.java` | Last `wait-for-screen` class name |
| `ShowcaseShotManifest` | `common/src/main/java/com/quackers29/businesscraft/client/showcase/ShowcaseShotManifest.java` | `showcase-shots.json`; failure unless the callback key is `screenshot.success` |
| `ShowcaseSelection.writtenShots` | `common/src/main/java/com/quackers29/businesscraft/client/showcase/ShowcaseSelection.java` | Non-warmup shot names for a scene |
| `ShowcaseTestbedCheckRegistry` | `common/src/main/java/com/quackers29/businesscraft/client/showcase/testbed/ShowcaseTestbedCheckRegistry.java` | Default set is `trade-contract` only |

## Rules & formulas (exact)
- `ShowcaseTownNames.next()` returns `Brookside`, then `Hillcrest`, then `Brookside-3`, `Brookside-4`, and so on. `Showcase.tick` resets the counter once when a run starts. A rejected placement does not consume a name, because the name is chosen only after the placement check passes.
- Outside a showcase run, `TownInterfaceBlock` still uses `new Random()` against `ConfigLoader.townNames`.
- `ui.scaledWidth` and `ui.scaledHeight` are `Screen.width` and `Screen.height` (GUI pixels). An empty screen records the window's scaled size and an empty screen name.
- Button ids come from the town bottom bar, the settings grid when that tab is active, the contract grid, and the platform-management grid. Other screens record an empty id list.
- `showcase-shots.json` `ok` is true only when no grab failed, no writes are still pending, and the saved rows match `expected`. `expected` is `ShowcaseSelection.writtenShots` for that scene. `all` writes 17 shots.
- `tools/showcase.sh` treats a missing manifest, `ok` not true, a failed row, or a missing PNG as a failed run. `tools/showcase-all-modes-smoke.sh` runs `all`, `town`, `block`, `all-views`, `two-towns`, and `testbed` for one loader.

## Edge cases & behaviors
- `wait-for-popup` does not change the expected screen. The name popup stays on top of `TownInterfaceScreen`.
- `example-dev-item` stays registered so a named run can still start it. `resolve("all")` does not include it.
- Tourist lineup, tourist-screen, and map-sweep keep their own manifests. They are not in the all-modes smoke script.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/client/showcase/ShowcaseSelectionTest.java` (written-shot counts, expected screens, pinned names)
- Test file: `common/src/test/java/com/quackers29/businesscraft/client/showcase/testbed/ShowcaseTestbedCheckRegistryTest.java` (default set versus opt-in template)
- Not covered here: framebuffer pixels, a second player, or the dedicated-server boot smoke.

## Open questions
- None.

## Related
- [[Home]]
- [[Town/Town Overview]]
