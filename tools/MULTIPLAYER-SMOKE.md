# Multiplayer smoke

Headless check that two players on one dedicated server see the same BusinessCraft scene. Forge runs first (port 25565), then Fabric (port 25566). One loader at a time. Create is not used: the link between towns is plain minecart rail.

```bash
bash tools/multiplayer-smoke.sh                 # Forge, then Fabric
bash tools/multiplayer-smoke.sh forge           # one loader
bash tools/multiplayer-smoke.sh fabric
bash tools/multiplayer-smoke.sh --self-test     # observation comparator only
```

The machine needs the same display and superflat template as `tools/showcase.sh` (`DISPLAY` :1 when `/tmp/.X11-unix/X1` exists, template from `SHOWCASE_TEMPLATE`, `showcaseTemplate` in `gradle.properties`, or `/opt/businesscraft/superflat-template`).

## What it checks

The dedicated server builds three towns 120 blocks apart. Names come from `ShowcaseTownNames` (Oakvale, then Hillcrest, then Rivertown), not a random draw. It also builds a platform kiosk at each town and a rail spine. Live tourist spawning and town recipe ticks are held still so the snapshot cannot drift. One Oakvale platform is set to AUTO with Hillcrest enabled. The learned table is seeded with `PlatformLearnedRoutes.recordSuccess` (Hillcrest weight 5, Rivertown weight 3, same trip length). Riding a cart the full distance is too slow and too easy to miss on a headless run; the seed uses the same method a real arrival uses.

Two offline clients, `BC_Alpha` and `BC_Beta`, join that server from separate game directories (`<loader>/run-mp-alpha` and `run-mp-beta`). Each one screenshots the name tags, the track, and the platform screen, and writes `showcase-shots.json` for those three files. A grab that does not save fails that client. Each observation also includes a `ui` object: the open screen's simple name, active tab, button ids, and scaled size. The rest of the observation is the data those screens are showing: tourist first names, the clipboard route line, platform mode, enabled towns, learned routes, and the town list.

The run fails if those two files disagree, if a required part of the seeded scene is missing, if a client crashes or disconnects, or if a stage times out. `viewer` is not compared. Learned-route strengths may differ by up to 0.01.

## Output

```text
showcase-output/<YYYY-MM-DD_HHMMSS>_mp-smoke_<loader>/
  screenshots/alpha/    screenshots/beta/
  observation-alpha.json
  observation-beta.json
  shots-alpha.json      shots-beta.json
  diff.txt              # unified diff of the comparable bodies, including ui and shots
  summary.txt
  logs/server.log  logs/server-console.log
  logs/alpha-client.log  logs/alpha-console.log
  logs/beta-client.log   logs/beta-console.log
  run-meta.json
```

`showcase-output/` is gitignored. `showcase-output/latest_mp-smoke_<loader>` points at the newest run.

## Timing

A cold client compile plus two headless windows is the slow part. Expect several minutes per loader on this cloud machine, longer the first time shaders compile. The server must print `MP_SMOKE_READY` within 7 minutes. Each client has 15 minutes to write its observation after launch. Heaps are 1536m for the server and 1280m for each client.
