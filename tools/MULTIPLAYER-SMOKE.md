# Multiplayer smoke

Headless check that two players on one dedicated server see the same BusinessCraft scene. Forge runs first (port 25565), then Fabric (port 25566). One loader at a time. Create is not used: the link between towns is plain minecart rail.

```bash
bash tools/multiplayer-smoke.sh                 # Forge, then Fabric
bash tools/multiplayer-smoke.sh forge           # one loader
bash tools/multiplayer-smoke.sh fabric
bash tools/multiplayer-smoke.sh --self-test     # observation comparator only
```

The machine needs the same display and superflat template as `tools/showcase.sh`: `DISPLAY` as set (WSLg works), else `:1` or `:0` when those sockets exist, else `xvfb-run`; the template is resolved and built on first use as described in [SHOWCASE-TESTBED.md](SHOWCASE-TESTBED.md#setup-on-a-new-machine). If 25565 (Forge) or 25566 (Fabric) is busy, the next free port is used and handed to the clients with `-PmpSmokePort`.

## What it checks

The dedicated server builds three towns 120 blocks apart. Names come from `ShowcaseTownNames` (Oakvale, then Hillcrest, then Rivertown), not a random draw. It also builds a platform kiosk at each town and a rail spine. Live tourist spawning and town recipe ticks are held still so the snapshot cannot drift. One Oakvale platform is set to AUTO with Hillcrest enabled. The learned table is seeded with `PlatformLearnedRoutes.recordSuccess` (Hillcrest weight 5, Rivertown weight 3, same trip length). Riding a cart the full distance is too slow and too easy to miss on a headless run; the seed uses the same method a real arrival uses.

Two offline clients, `BC_Alpha` and `BC_Beta`, join that server from separate game directories (`<loader>/run-mp-alpha` and `run-mp-beta`). Each one screenshots the name tags, the track, and the platform screen, and writes `showcase-shots.json` for those three files. A grab that does not save fails that client. Each observation also includes a `ui` object: the open screen's simple name, active tab, button ids, and scaled size. The rest of the observation is the data those screens are showing: tourist first names, the clipboard route line, platform mode, enabled towns, learned routes, and the town list.

### Reach stage

BC_Beta, the second client, then closes the platform screen, walks 120 blocks away from the Oakvale platform, and sends two platform packets for it: `SetPlatformEnabledPacket` (off) and `SetPlatformDestinationModePacket` (SPECIFIC). It walks back, reopens the screen and requires the platform to still be enabled and AUTO. The server must also have logged a `BC_REACH_REJECT player=BC_Beta ...` line for each packet (the script counts at least two). BC_Beta's observation has an extra `reach` object (`sent`, `distance`, `stateUnchanged`) that `mp-smoke-compare.py` checks and leaves out of the alpha-versus-beta comparison. The rule being tested is in `LoadedTownAccess`: a player must be within 8 blocks of the Town Centre, or of the platform's own kiosk for platform actions.

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
  result.json           # same fields as a showcase run; the last stdout line is  RESULT mp-smoke <loader> PASS|FAIL ...
  logs/server.log  logs/server-console.log
  logs/alpha-client.log  logs/alpha-console.log
  logs/beta-client.log   logs/beta-console.log
  run-meta.json
```

`showcase-output/` is gitignored. `showcase-output/latest_mp-smoke_<loader>` points at the newest run.

## Timing

A cold client compile plus two headless windows is the slow part. Expect several minutes per loader on the cloud machine, about 90 seconds once Gradle is warm on a fast PC (the reach stage adds a few seconds). The server must print `MP_SMOKE_READY` within 7 minutes. Each client has 15 minutes to write its observation after launch. Heaps are 1536m for the server and 1280m for each client.
