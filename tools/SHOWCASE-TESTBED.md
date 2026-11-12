# Showcase testbed

The testbed reuses the **two-town** showcase layout (town A on the pad, town B at minimum legal distance) and runs **pluggable checks** you can extend while developing features.

## Choosing what to run

| You want | Command |
|---|---|
| Screenshots for what you changed (the default) | `bash tools/showcase-run.sh` |
| See the plan without running it | `bash tools/showcase-run.sh --dry-run` |
| Specific routes or groups | `bash tools/showcase-run.sh --routes town/map,platform` |
| Everything, before a release or after a shared UI/harness change | `bash tools/showcase-run.sh --regression` (or `bash tools/ci.sh --full`) |
| One scene by hand | `bash tools/showcase.sh <mode> <forge\|fabric> [args]` |

**Targeted mode.** `tools/showcase-select.py` lists the files that differ from `origin/main` (committed, uncommitted and untracked) and looks each one up in `tools/showcase-targets.json`. A rule adds routes (`route:town/payment`) and scenes (`mode:tourists`). Route tokens are an exact route name or a group: `platform` means every `platform/...` route. The routes run in one client session through `showcase.sh routes <loader> <list>`, which sets `-PshowcaseRoutes` and checks only those routes in the manifest (every selected route must be ok and have its PNG; an unknown route name fails the run).

- A change to shared UI building blocks (theme, components, modals, grid, layout) selects the whole `all-views` walk.
- A change to the harness itself (`client/showcase/`, the `tools/showcase*`/`mp-smoke`/`ci.sh` scripts, the `build.gradle` run config) selects the regression set.
- A code change that no rule covers runs the `town` scene as a sanity check, and the unmapped paths are printed (`# unmapped: ...`). Add a rule for them.
- Docs, vault notes, tests, tasks and data files select nothing.

`ShowcaseRouteSelectionTest` fails when `showcase-targets.json` names a route that does not exist, so the map cannot drift when routes are renamed.

**Regression set** (`--regression`, one loader per run, `--loader both` for two): `all`, `town`, `block`, `all-views`, `two-towns`, `testbed`. `tourists`, `tourist-screen`, `jade` and `map-sweep` are not in it; targeted mode picks them when their files change, or run them by hand.

## Setup on a new machine

- **Template world.** Every run copies a flat world. `showcase.sh` and `multiplayer-smoke.sh` look for it in this order: `SHOWCASE_TEMPLATE` (environment), `showcaseTemplate=` in `gradle.properties`, `/opt/businesscraft/superflat-template` (the cloud image), then `~/.cache/businesscraft/superflat-template`. When the chosen path has no `level.dat` it is built by booting the Forge dev server once (`bash tools/showcase-template.sh [dir]`, about 20 s after Gradle is warm). Set `SHOWCASE_TEMPLATE` to put it somewhere else.
- **Display.** `DISPLAY` is used as is. On WSL2, WSLg sets `DISPLAY=:0` and real frames render with no extra setup. With no display, the scripts re-run themselves under `xvfb-run -a` when it is installed (`sudo apt install xvfb`).
- **Gradle daemon.** Local runs reuse the daemon. `CI=true` or `BC_NO_DAEMON=1` adds `--no-daemon`.
- **Sound and narrator noise.** `ALSOFT_DRIVERS=null` is exported so a machine without audio logs no OpenAL error. The narrator (flite) error cannot be switched off; `tools/known-noise.txt` lists the expected `ERROR` lines the scripts hide from their summary. The raw logs keep everything.
- **Fixed lighting.** Every showcase and smoke scene sets the time to noon with the daylight and weather cycles off (`ShowcaseWorldBuild.pinDaytime`). Only the platform lamp shots change the time on purpose.

## Run

```bash
bash tools/showcase.sh testbed forge
bash tools/showcase.sh testbed fabric
bash tools/showcase.sh testbed forge trade-contract
bash tools/showcase.sh testbed fabric trade-contract,example-dev-item
```

Omit check names to run the default set (`trade-contract`). `example-dev-item` is a template and is not in that set; pass its name to run it.

Showcase runs pin town names: the first Town Centre placed in the run is Brookside and the second is Hillcrest. Gameplay outside the harness still picks a name at random.

`all-views` writes a `ui` object on each manifest route (screen simple name, active tab, button ids, scaled width and height) and fails that route when the open screen is not the route's last `wait-for-screen`. Planned-shot modes (`all`, `town`, `block`, `platform`, `kiosk`, `lamp`) write `showcase-shots.json` from each grab. The shell requires every non-warmup shot from `ShowcaseSelection` to be present and saved. A grab callback that is not a successful screenshot fails the run. The two-client smoke (`tools/multiplayer-smoke.sh`) uses the same town-name list, the same `ui` snapshot, and the same shot manifest for `mp_nametags`, `mp_track`, and `mp_platform`.

## Output layout

Every `tools/showcase.sh` run writes a timestamped folder and ends with one line to grep:

```text
RESULT <mode> <loader> PASS|FAIL <seconds>s shots=<n> [failures=<n>] dir=<run dir>
```

```text
showcase-output/<YYYY-MM-DD_HHMMSS>_<mode>_<loader>/
  result.json              # {status, mode, loader, seconds, shots, failures[], dir}
  screenshots/
  logs/client.log          # Minecraft latest.log (full client session)
  logs/server.log          # Dedicated server log when present; else integrated note
  logs/gradle-console.log  # Gradle + JVM console (the script never prints from it)
  run-meta.json
  … mode-specific JSON / steps.log / checks.log / index.html …
```

Symlinks: `showcase-output/latest`, `showcase-output/latest_<mode>_<loader>`, and `showcase-output/index.html`.

Set `SHOWCASE_ARTIFACT_DIR=/opt/cursor/artifacts/...` to mirror a copy elsewhere. The `showcase-output/` tree is gitignored.

Dedicated-server agreement between two clients (Forge, then Fabric) is `bash tools/multiplayer-smoke.sh`. See [MULTIPLAYER-SMOKE.md](MULTIPLAYER-SMOKE.md).

## Registered checks

| Name | Purpose |
|------|---------|
| `trade-contract` | Sell auction from town A, bid from town B, contract board + detail UI, time-skipped delivery |
| `example-dev-item` | Starter template (opens trade from town B). Opt-in; not part of the default set |

## Add a new check

1. Implement `ShowcaseTestbedCheck` in `common/.../client/showcase/testbed/`.
2. Register it in `ShowcaseTestbedCheckRegistry` static block.
3. Run: `bash tools/showcase.sh testbed forge your-check-name`

### Example skeleton

```java
public final class MyFeatureCheck implements ShowcaseTestbedCheck {
    @Override
    public String name() {
        return "my-feature";
    }

    @Override
    public void begin(Minecraft minecraft, ShowcaseTestbedContext context) {
        Showcase.setActiveTownBlockPos(context.townBPos());
    }

    @Override
    public boolean tick(Minecraft minecraft, ShowcaseTestbedContext context) {
        // Drive UI via ShowcaseNavigator, or schedule server work on
        // minecraft.getSingleplayerServer().execute(...)
        return false; // return true when finished
    }

    @Override
    public boolean passed() {
        return true;
    }

    @Override
    public Map<String, Object> details() {
        return Map.of();
    }
}
```

Register:

```java
register("my-feature", MyFeatureCheck::new);
```

Use `context.townAPos()`, `context.townBPos()`, `context.townAId()`, and `context.put(...)` to share data between ticks.

## Play session: look around by hand

`tools/showcase-play.sh` is the human entry point. The smoke screenshots and exits; this stays up until you stop it.

```bash
bash tools/showcase-play.sh start            # Forge (default); `start fabric` for Fabric
bash tools/showcase-play.sh status           # running? loader, port, join addresses, logs
bash tools/showcase-play.sh stop             # save the world, stop the server and the client it opened
```

`start` resolves the dev server and client commands with Gradle (daemon reused locally), copies the superflat template to `<loader>/run-play-server/world`, boots a dedicated dev server (`online-mode=false`, view distance 10), waits for `Done (` and the world-build marker (`showcase-play-ready.json`), then opens one visible client (`<loader>/run-play-client`, render distance 12, 144 FPS, `pauseOnLostFocus:false`, `skipMultiplayerWarning:true`, 1280x720) connected with `--quickPlayMultiplayer`. Warm Gradle: about 30 s. With no display it starts the server only (never xvfb: it would die with the script). `BC_PLAY_NAME` sets the player name, `BC_PLAY_NO_CLIENT=1` skips the client.

Join from elsewhere: `localhost:<port>` (WSL2 forwards it to Windows), or the LAN address `status` prints. Port 25565 (Forge) or 25566 (Fabric), the next free one when busy. Minecraft 1.20.1, offline mode, any username, BusinessCraft for that loader installed.

The pid/start-time record lives in `showcase-output/play-session.env`. `stop` signals only those pids (start time must match, so a reused pid is never touched), is safe to repeat, and leaves a client you started yourself alone. Logs: `showcase-output/<timestamp>_play_<loader>/logs`.

### What the world holds

Four towns from `ShowcasePlayLayout`: Oakvale (0,0), Hillcrest (0,-120), Rivertown (0,-240), Pinewood (150,-120), at least 100 apart. You spawn south of Oakvale facing the Town Centre.

- Town Centres: stocked resources, visit history, payment-board rewards, tourist capacity, open auctions (Pinewood diamonds, Hillcrest coal, Oakvale bread) and one completed trade.
- Oakvale ticket machines: SPECIFIC (on the rail line), AUTO (learned routes 5:3, radius 5, spread 4), ANY, and one switched off (dark lamp). Hillcrest, Rivertown and Pinewood each have a line platform.
- Rail line: carts wait at each departure stop, tourists board, a scripted dispatcher drives them 113 blocks north to the next town, then resets. Carts are driven by `ShowcasePlayServer.dispatch`, not redstone.
- Five named tourists by the spawn, a chest with the Conductor's Clipboard and a guide book, 13 signs. Noon, no daylight or weather cycle.

Code: `ShowcasePlayServer` (build and dispatcher), `ShowcasePlayLayout` (plan, unit tested). It is selected with `-PmpSmoke=play` and hooked from `MultiplayerSmokeServer`. It is not part of targeted showcase selection (`showcase-targets.json` ignores it).
