---
tags:
  - detail
  - production
  - config
---
# Production Event Log

**Breadcrumb**: Production > Debug > Production Event Log
**TL;DR**: One CSV records a town when a recipe, research node, upgrade, population, border, work units, or contract actually changes stage, and a per-town summary is written when the run ends.

## What it does
The log is for reading what the production side did during a showcase or a headless run. It stays off until `[debug] productionEventLog` is true, or a launch flag turns it on for that process. Nothing is written while it is off, and the game does not build the row.

## How it works (process view)
A recipe logs when a cycle finishes, and when it becomes stalled or starts again. The stall is one row for the change, not a row every tick. Research logs when it is paid for and when it completes. An upgrade logs when its level goes up. Population, border radius, and work units log when the number changes. A contract logs when it is listed, bid on, won, given to a courier (including snail mail), delivered, expired, or cancelled. Founding a town logs one row.

The file is `production-events-<stamp>.csv`. Columns are `real_time`, `game_tick`, `town_name`, `town_id`, `event_type`, `subject_id`, `amount`, `details`, `stock`. `details` is `key=value` pairs separated by semicolons. Resource lists inside a field are `id:amount` separated by commas, and that field is quoted. The server flushes every row. On shutdown the same directory gets `production-summary-<stamp>.csv` and `.md` with per-town produced, consumed, research bought, research cost, contract stage counts, and stall reasons.

An empty directory setting uses `<server run dir>/businesscraft-debug`. `bash tools/showcase-play.sh start --production` writes straight into `showcase-output/<run>/production/` and prints the path on start and stop. `/bc debug production [town]` appends one snapshot row per town while the log is already open.

The headless run uses the same listener and the same town tick. It seeds the four showcase towns from `ShowcasePlayLayout` and the bundled CSVs, with the plains Farming Village kit so the result does not depend on the random kit the play world rolls. `./gradlew test` runs 1000 ticks. `./gradlew productionSim` runs 6000 ticks into `common/build/production-sim/`. `-Dbusinesscraft.productionSim.ticks`, `.dir`, and `.seed` change that.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `ProductionEvents` | `common/src/main/java/com/quackers29/businesscraft/debug/production/ProductionEvents.java` | Builds one event at a stage change. Returns immediately when no listener is registered. |
| `ProductionEventBus` | `common/src/main/java/com/quackers29/businesscraft/debug/production/ProductionEventBus.java` | The only listener list. Does not call the supplier when the list is empty. |
| `ProductionEventLog` | `common/src/main/java/com/quackers29/businesscraft/debug/production/ProductionEventLog.java` | The sink. Opens the CSV, flushes each row, fsyncs every 25 rows, writes the summary on close. |
| `ProductionSimulation.run` | `common/src/main/java/com/quackers29/businesscraft/debug/production/ProductionSimulation.java` | Headless ticks. Copies the bundled CSVs, seeds the showcase towns, ticks, closes the same log. |
| `ShowcaseTownSeed.apply` | `common/src/main/java/com/quackers29/businesscraft/client/showcase/ShowcaseTownSeed.java` | Shared stock and population seed for the play world and the simulation. |
| `ConfigLoader.applyRunOverrides` | `common/src/main/java/com/quackers29/businesscraft/config/ConfigLoader.java` | `-Dbusinesscraft.production.enabled`, `research.enabled`, `contracts.enabled`, `debug.productionEventLog`, and `debug.productionEventLogDir` override the toml for that process only. |

## Rules & formulas (exact)
- Stall reasons are `conditions`, `missing_input:<id>`, `output_full:<id>`, and `spawning_disabled`. Leaving a stall logs `recipe_resumed`. A locked recipe (modifier at or below 0) is not a stall. `population_maintenance` with no food still advances and logs `recipe_completed` when the starvation cycle finishes.
- Stall state is memory only. After a reload the next stall logs once.
- `amount` is total output units for a recipe, the new level for research and upgrades, the new value for population, border, and work units, the quantity for a contract, and population for town creation and snapshots.
- Contract rows use the issuer for listed, expired, and cancelled, the bidder for a bid, the winner for won and delivered, and the issuer for courier dispatched. Snail mail's counterpart name is `snail_mail`.
- The summary counts `research_bought` from `research_started` rows and sums `outputs`, `inputs`, and `cost` from the detail field.
- The simulation clock is 50 ms per tick from a fixed zero. Contract and research randomness use `BcRandom` after `reseed`. Town ids are `UUID.nameUUIDFromBytes("businesscraft:showcase:" + name)`. Towns are ticked in layout order. The default seed is 29.
- Live servers keep wall-clock time and `UUID.randomUUID()` for contract ids. The play world's biome kit is still the random kit for that chunk; the simulation always uses Farming Village.

## Edge cases & behaviors
- Joining a world does not emit a border row per loaded town. Load sets a flag, then baselines the radius already in the save.
- The implicit starting border of 50 is baselined in the town constructor, so it is not an event. A kit that sets `border:10` does log the change.
- A player courier needs a live player. The headless path reaches snail mail instead.
- Auction close, courier acceptance, and snail-mail travel are wall-clock minutes. 1000 simulated ticks is 50 seconds, so a default test can list a contract without finishing delivery. 6000 ticks is five minutes.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/debug/production/ProductionEventLogTest.java`
- CSV quoting, toggle off writes nothing and does not build the row, summary totals match the rows, a one-tick recipe logs one completion per cycle, and a full output logs one stall across ten ticks.
- Test file: `common/src/test/java/com/quackers29/businesscraft/debug/production/ProductionSimulationTest.java`
- Events exist, including town creation and a recipe completion. No two rows share tick, town, type, subject, and details. Summary totals recomputed from the CSV match the summary file. Two runs with seed 29 match apart from `real_time`.

## Open questions
- The generated biome stub used to put the weight before the description. The loader accepts that order and the bundled order. A numeric fourth column is the stub.
- Snail-mail delivery time is distance times `contractSnailMailDeliveryMinutesPerMeter`. The Oakvale–Hillcrest gap is 120 blocks, so a delivered row needs a longer run than `productionSim`'s 6000 ticks.

## Related
- [[Production/Production Overview]]
- [[Config/Subsystem Toggle Honesty]]
