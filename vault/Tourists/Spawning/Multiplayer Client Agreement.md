---
tags:
  - detail
  - tourists
---
# Multiplayer Client Agreement

**Breadcrumb**: Tourists > Spawning > Multiplayer Client Agreement
**TL;DR**: Two players on one dedicated server must see the same tourist names, clipboard routes, platform mode, enabled towns, learned routes, town list, and open screen; `tools/multiplayer-smoke.sh` fails the run when they do not or when a screenshot did not save.

## What it does
A dedicated server is the place a shared tourism line actually lives. Each player's game only shows what the server sent: name tags, the second clipboard line, the platform screen, and the town list. If those copies drift, one player can follow a route the other player never sees. This check boots one loader's dedicated server, joins two offline players, and compares what each screen is showing.

## How it works (process view)
Forge runs first on port 25565, then Fabric on port 25566. The server lays out Oakvale, Hillcrest, and Rivertown in a line, with plain minecart rail between them and no Create trains. It parks two named tourists in front of the Oakvale platform and seeds that platform's AUTO learned table (Hillcrest stronger than Rivertown) instead of waiting for a cart to finish the trip. Town recipe ticks and live tourist spawning stay frozen so the numbers cannot change between the two players.

BC_Alpha and BC_Beta join from separate game folders, each holding a conductor clipboard. They photograph the name tags, the track, and the open platform screen. Each report names the screen that was open, its tab, its buttons, and its scaled size, and lists whether each of the three pictures saved. The town names are fixed (Oakvale, Hillcrest, Rivertown). BC_Beta also does a reach check: it walks 120 blocks from the Oakvale platform, sends two platform packets for it, walks back, and the platform must still be enabled and AUTO, with the server having refused both ([[Town/Platforms/Packet Reach Checks]]). The smoke fails when the reports disagree, when a picture did not save, when the seeded scene is missing, or when a player crashes, disconnects, or times out.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `MultiplayerSmokeServer.build()` | `common/src/main/java/com/quackers29/businesscraft/client/showcase/MultiplayerSmokeServer.java` | Dedicated-server scene: towns, platforms, rails, seeded AUTO route, tourists |
| `MultiplayerSmokeClient.tick()` | `common/src/main/java/com/quackers29/businesscraft/client/showcase/MultiplayerSmokeClient.java` | Joins, screenshots, writes the observation plus `ui` and `shots` |
| `MultiplayerSmokePlan` | `common/src/main/java/com/quackers29/businesscraft/client/showcase/MultiplayerSmokePlan.java` | Forge port 25565, Fabric port 25566, shot names `mp_nametags`, `mp_track`, `mp_platform` |
| `ShowcaseShotManifest` | `common/src/main/java/com/quackers29/businesscraft/client/showcase/ShowcaseShotManifest.java` | `showcase-shots.json`; a grab that is not `screenshot.success` fails the client |
| `ShowcaseUiSnapshot.capture` | `common/src/main/java/com/quackers29/businesscraft/client/showcase/ShowcaseUiSnapshot.java` | Screen, tab, button ids, scaled size stored on each observation |
| `ShowcasePlatformPlacement.ensure()` | `common/src/main/java/com/quackers29/businesscraft/client/showcase/ShowcasePlatformPlacement.java` | Server-safe platform kiosk placement shared with the showcase |
| `PlatformLearnedRoutes.recordSuccess()` | `common/src/main/java/com/quackers29/businesscraft/platform/PlatformLearnedRoutes.java` | Same write an arrival uses; the smoke calls it directly |
| `tools/multiplayer-smoke.sh` | `tools/multiplayer-smoke.sh` | Boots one loader, compares the two observation files |

## Rules & formulas (exact)
- System property `businesscraft.multiplayerSmoke=setup` on the dedicated server, `observe` on the clients. Port property `businesscraft.multiplayerSmoke.port` (25565 Forge, 25566 Fabric). `online-mode=false`.
- Town centres are 120 apart. The seeded platform is AUTO, Hillcrest enabled only, `recordSuccess` weights 5 and 3 with the same trip length so the displayed share stays 5:3.
- `TownManager.tick` returns immediately while the property is `setup`, so population and tourist counts stay put.
- Town names are `ShowcaseTownNames` while the server property is `setup`: Oakvale, Hillcrest, Rivertown.
- Comparison ignores `viewer`. Float strengths may differ by 0.01. Both sides must still contain Oakvale, Hillcrest, Rivertown, mode `auto`, enabled town Hillcrest, learned routes Hillcrest and Rivertown, name tags Morgan and Eleanor, clipboard lines `Oakvale -> Hillcrest` and `Oakvale -> Rivertown`, screen `PlatformInterfaceScreen`, and saved shots `mp_nametags`, `mp_track`, `mp_platform`. The `ui` object (screen, tab, button ids, scaled size) is compared too.
- Reach stage (BC_Beta only, `businesscraft.multiplayerSmoke.reach=true`): `MultiplayerSmokePlan.REACH_TEST_DISTANCE` = 120 blocks along +X, packets `SetPlatformEnabledPacket(false)` and `SetPlatformDestinationModePacket(SPECIFIC)`. The observation gets `reach {sent, distance, packets, stateUnchanged}`; the comparator requires `sent`, `distance >= 100`, `stateUnchanged`, drops `reach` from the alpha/beta diff, and the script requires at least two `BC_REACH_REJECT player=BC_Beta` server log lines.
- If the default port is busy the script uses the next free one and passes it as `businesscraft.multiplayerSmoke.port`.
- `tools/multiplayer-smoke.sh` also requires each client's `showcase-shots.json` to list those three shots with `ok` true and the matching PNG on disk.
- Players are offline names `BC_Alpha` and `BC_Beta` with the vanilla offline UUID (`OfflinePlayer:` + name, MD5 version-3).

## Edge cases & behaviors
- A real minecart arrival is not waited on. The seed is the arrival write, called once at scene build.
- Opening the platform screen applies learned-route decay. The half-life is a week of ticks, so two players opening a few seconds apart stay inside the 0.01 strength tolerance.
- The join timer does not run while a loading overlay is up, and the first-launch accessibility screen is dismissed. Those two screens used to burn the whole timeout before the title screen existed.
- `ShowcasePlatforms` opens a client screen, so the dedicated server places kiosks through `ShowcasePlatformPlacement` instead.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/client/showcase/MultiplayerSmokePlanTest.java` (ports, shot names, pinned town line)
- The headless game run is `bash tools/multiplayer-smoke.sh`.
- `bash tools/multiplayer-smoke.sh --self-test` covers the comparator: agreement within tolerance, a clipboard mismatch, two empty town lists, a wrong screen name, and the reach result (missing, applied, or sent from too close).
- `common/src/test/java/com/quackers29/businesscraft/network/LoadedTownAccessTest.java` covers the distance rule the reach stage exercises.

## Open questions
- First shader compile on a fresh game directory dominates the runtime. Later runs of the same directory are faster; the script still starts from the directories it already created.

## Related
- [[Tourists/Spawning/Tourist Spawning and Destination Selection]]
- [[Tourists/Spawning/AUTO Destination Learning]]
- [[Tourists/Looks/Tourist Looks Names and Ranks]]
- [[Town/Data Synchronization/Client Data Synchronization]]
- [[Tourists/Tourists Overview]]
- [[UI/Showcase Harness]]
- [[Town/Platforms/Packet Reach Checks]]
