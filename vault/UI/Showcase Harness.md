---
tags:
  - detail
  - ui
---
# Showcase Harness

**Breadcrumb**: UI > Showcase Harness
**TL;DR**: The dev client harness pins town names, records which screen and buttons were open, and fails a run when a planned screenshot did not save or the open screen is the wrong one. The two-player dedicated-server check uses the same rules.

## What it does
Developers run a throwaway client world to photograph town screens and a few scripted checks. The pictures are for people. The pass/fail decision is the manifest: every planned picture must exist, and each UI route must be on the screen it asked for.

## How it works (process view)
A showcase run places Town Centres in order. The first is always named Brookside and the second Hillcrest, so two runs of the same scene show the same names. A normal game still picks a town name at random.

The full UI walk writes, next to each picture, the screen name, the open tab, the button ids, and the scaled window size. If that screen is not the one the route waited for, the route fails.

The block, town, platform, kiosk, and lamp scenes (and the combined `all` scene) write a shot list. The shell checks that list against the shots the scene is supposed to take, skipping the warmup pause that does not save a picture. If the game reports that a picture failed to save, the run fails.

The default two-town testbed runs the trade-contract check only. The example template check runs only when named.

By default a showcase run only photographs what a change touches: the changed files are matched to UI routes and scenes, and only those run in one client session. The full set (every route, every scene) runs only when asked for. Every scene is lit the same way: noon, with the day and weather cycles off, so two runs differ only by what changed. A run ends with one `RESULT` line and a `result.json`, and a missing flat template world is built automatically the first time.

The two-player smoke uses the same name list, the same screen record, and the same shot list. Forge listens on 25565 and Fabric on 25566. Each player's report is compared with the other's.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `ShowcaseTownNames` | `common/src/main/java/com/quackers29/businesscraft/client/showcase/ShowcaseTownNames.java` | Brookside then Hillcrest in a showcase; Oakvale, Hillcrest, Rivertown when `businesscraft.multiplayerSmoke=setup` |
| `MultiplayerSmokePlan` | `common/src/main/java/com/quackers29/businesscraft/client/showcase/MultiplayerSmokePlan.java` | Ports 25565 / 25566 and the three client shot names |
| `TownInterfaceBlock.setPlacedBy` | `common/src/main/java/com/quackers29/businesscraft/block/TownInterfaceBlock.java` | Uses the pinned name during a showcase run |
| `ShowcaseUiSnapshot.capture` | `common/src/main/java/com/quackers29/businesscraft/client/showcase/ShowcaseUiSnapshot.java` | Screen, tab, button ids, scaled size |
| `ShowcaseRoute.expectedScreen` | `common/src/main/java/com/quackers29/businesscraft/client/showcase/ShowcaseRoute.java` | Last `wait-for-screen` class name |
| `ShowcaseShotManifest` | `common/src/main/java/com/quackers29/businesscraft/client/showcase/ShowcaseShotManifest.java` | `showcase-shots.json`; failure unless the callback key is `screenshot.success` |
| `ShowcaseSelection.writtenShots` | `common/src/main/java/com/quackers29/businesscraft/client/showcase/ShowcaseSelection.java` | Non-warmup shot names for a scene |
| `ShowcaseTestbedCheckRegistry` | `common/src/main/java/com/quackers29/businesscraft/client/showcase/testbed/ShowcaseTestbedCheckRegistry.java` | Default set is `trade-contract` only |

## Rules & formulas (exact)
- `ShowcaseTownNames.next()` returns `Brookside`, then `Hillcrest`, then `Brookside-3`, `Brookside-4`, and so on. `Showcase.tick` resets the counter once when a run starts. A rejected placement does not consume a name, because the name is chosen only after the placement check passes.
- While `businesscraft.multiplayerSmoke=setup`, `ShowcaseTownNames.next()` returns Oakvale, Hillcrest, Rivertown, then Oakvale-4. `MultiplayerSmokeServer.build` resets the counter and takes the first three. Outside both runs, `TownInterfaceBlock` still uses `new Random()` against `ConfigLoader.townNames`.
- `ui.scaledWidth` and `ui.scaledHeight` are `Screen.width` and `Screen.height` (GUI pixels). An empty screen records the window's scaled size and an empty screen name.
- Button ids come from the town bottom bar, the settings grid when that tab is active, the contract grid, and the platform-management grid. Other screens record an empty id list.
- `showcase-shots.json` `ok` is true only when no grab failed, no writes are still pending, and the saved rows match `expected`. `expected` is `ShowcaseSelection.writtenShots` for that scene. `all` writes 19 shots.
- `tools/showcase.sh` treats a missing manifest, `ok` not true, a failed row, or a missing PNG as a failed run. `tools/showcase-all-modes-smoke.sh` (the regression set) runs `all`, `town`, `block`, `all-views`, `two-towns`, and `testbed` for one loader. `all-views` is checked against its own manifest (every selected route ok with its PNG; a full run must select the whole registry) instead of a fixed 19.

- Targeted runs: `tools/showcase-run.sh` asks `tools/showcase-select.py` for a plan from `git diff origin/main` and `tools/showcase-targets.json`; `--routes a,b` names routes, `--regression` runs the full set, `tools/ci.sh` defaults to targeted and `--full` runs the regression set on both loaders. `ShowcaseRouteSelection` narrows the all-views walk (`-PshowcaseRoutes`, property `businesscraft.showcase.routes`): a token is a route name or a group before the slash, an unknown token fails the run, and the manifest lists `selected` so the shell checks exactly those routes.
- Daylight: `ShowcaseWorldBuild.pinDaytime` sets time 6000, clear weather, no daylight or weather cycle in every scene; the platform lamp shots set 18000 on purpose.
- `tools/showcase.sh` ends with `RESULT <mode> <loader> PASS|FAIL <n>s shots=<n> ...` and writes `result.json`; it prints only unexpected `ERROR` lines of the client log (not the Gradle console) after hiding the patterns in `tools/known-noise.txt`.

## Edge cases & behaviors
- `wait-for-popup` does not change the expected screen. The name popup stays on top of `TownInterfaceScreen`.
- `example-dev-item` stays registered so a named run can still start it. `resolve("all")` does not include it.
- Tourist lineup, tourist-screen, and map-sweep keep their own manifests. They are not in the all-modes smoke script.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/client/showcase/ShowcaseSelectionTest.java` (written-shot counts, expected screens, pinned names)
- Test file: `common/src/test/java/com/quackers29/businesscraft/client/showcase/testbed/ShowcaseTestbedCheckRegistryTest.java` (default set versus opt-in template)
- Test file: `common/src/test/java/com/quackers29/businesscraft/client/showcase/MultiplayerSmokePlanTest.java` (ports, shot names, multiplayer town line)
- Test file: `common/src/test/java/com/quackers29/businesscraft/client/showcase/ShowcaseRouteSelectionTest.java` (route and group tokens, unknown names, every route named in `tools/showcase-targets.json` exists)
- `python3 tools/showcase-select.py --self-test` covers the path-to-scene mapping.
- Not covered here: framebuffer pixels, or the dedicated-server boot smoke. The two-player run is [[Tourists/Spawning/Multiplayer Client Agreement]].

## Open questions
- None.

## Related
- [[Home]]
- [[Town/Town Overview]]
- [[UI/Screen Style]]
- [[Tourists/Spawning/Multiplayer Client Agreement]]
